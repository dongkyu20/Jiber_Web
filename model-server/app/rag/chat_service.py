from __future__ import annotations

from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
import json
import re

from app.core.config import Settings, get_settings
from app.schemas.chat import ChatContext, RagConfig, RealEstateChatResponse
from app.schemas.chat import RealEstateChatRetrievalResponse


SKIP_EXTENSIONS = {".html", ".xlsx", ".xls", ".csv", ".json", ".ds_store"}

SKELETON_ANSWER = (
    "부동산 챗봇은 현재 계약 skeleton 단계입니다. "
    "실제 RAG corpus, vector index, embedding model, reranker, LLM provider는 사용자 구현 후 연결됩니다. "
    "현 단계에서는 투자 조언, 법률·세무 판단, 매수·매도 추천을 제공하지 않습니다."
)


@dataclass(frozen=True)
class ChunkRecord:
    text: str
    source: str


class RealEstateChatService:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.docs_dir = self._resolve_docs_dir()
        self.chunk_size = self._env_int("RAG_CHUNK_SIZE", 1200)
        self.chunk_overlap = self._env_int("RAG_CHUNK_OVERLAP", 150)
        self.top_k = self._env_int("RAG_TOP_K", 5)
        self.chunks = self._load_chunk_records()

    def answer(self, question: str, runtime_context: dict | None) -> RealEstateChatResponse:
        return RealEstateChatResponse(
            available=False,
            answer=SKELETON_ANSWER,
            contexts=[],
            model="chat-skeleton-v1",
            ragConfig=RagConfig(
                embedding="disabled",
                chunkSize=0,
                overlap=0,
                hybrid=False,
                rerank=False,
            ),
        )

    def retrieve(self, question: str, runtime_context: dict | None) -> RealEstateChatRetrievalResponse:
        retrieval_question = self._question_with_runtime_context(question, runtime_context)
        contexts = self._retrieve(retrieval_question)
        return RealEstateChatRetrievalResponse(
            contexts=[ChatContext(source=context.source, text=context.text) for context in contexts],
            ragConfig=self._rag_config(),
        )

    def _retrieve(self, query: str) -> list[ChunkRecord]:
        if not self.chunks:
            return []

        query_terms = self._tokenize(query)
        if not query_terms:
            return self.chunks[: self.top_k]

        ranked: list[tuple[float, ChunkRecord]] = []
        for record in self.chunks:
            terms = self._tokenize(record.text)
            if not terms:
                continue
            term_set = set(terms)
            overlap = sum(1 for term in query_terms if term in term_set)
            if overlap == 0:
                continue
            density = overlap / max(len(term_set), 1)
            source_bonus = 0.25 if any(term in record.source.lower() for term in query_terms) else 0
            ranked.append((overlap + density + source_bonus, record))

        ranked.sort(key=lambda item: item[0], reverse=True)
        return [record for _, record in ranked[: self.top_k]]

    def _rag_config(self) -> RagConfig:
        return RagConfig(
            embedding="keyword-local",
            chunkSize=self.chunk_size,
            overlap=self.chunk_overlap,
            hybrid=False,
            rerank=False,
        )

    def _load_chunk_records(self) -> list[ChunkRecord]:
        records: list[ChunkRecord] = []
        for source, content in self._load_documents(self.docs_dir):
            for chunk in self._chunk_text(content):
                records.append(ChunkRecord(text=chunk, source=source))
        return records

    def _load_documents(self, docs_dir: Path) -> list[tuple[str, str]]:
        if not docs_dir.exists():
            return []

        documents: list[tuple[str, str]] = []
        for path in sorted(docs_dir.rglob("*")):
            if not path.is_file() or path.name.startswith("."):
                continue
            extension = path.suffix.lower()
            if extension in SKIP_EXTENSIONS:
                continue
            relative_path = str(path.relative_to(docs_dir)).replace("\\", "/")
            content = self._read_document(path, extension)
            if len(content) >= 50:
                documents.append((relative_path, content))
        return documents

    def _read_document(self, path: Path, extension: str) -> str:
        if extension in {".txt", ".md"}:
            return path.read_text(encoding="utf-8", errors="ignore").strip()
        if extension == ".pdf":
            try:
                from pypdf import PdfReader

                reader = PdfReader(str(path))
                return "\n".join(page.extract_text() or "" for page in reader.pages).strip()
            except Exception:
                return ""
        return ""

    def _chunk_text(self, text: str) -> list[str]:
        if self.chunk_size <= 0:
            return [text]

        chunks: list[str] = []
        step = max(self.chunk_size - max(self.chunk_overlap, 0), 1)
        start = 0
        while start < len(text):
            chunk = text[start : start + self.chunk_size].strip()
            if chunk:
                chunks.append(chunk)
            start += step
        return chunks

    def _question_with_runtime_context(self, question: str, runtime_context: dict | None) -> str:
        if not runtime_context:
            return question
        return f"{question}\n\n[런타임 컨텍스트]\n{json.dumps(runtime_context, ensure_ascii=False)}"

    def _resolve_docs_dir(self) -> Path:
        import os

        configured = os.getenv("RAG_DOCS_DIR", "").strip()
        if configured:
            return Path(configured).expanduser().resolve()

        model_server_root = Path(__file__).resolve().parents[2]
        local_docs = model_server_root / "documents"
        if local_docs.exists():
            return local_docs.resolve()

        sibling_docs = model_server_root.parent / "rag-practice" / "documents"
        if sibling_docs.exists():
            return sibling_docs.resolve()

        return local_docs.resolve()

    @staticmethod
    def _tokenize(text: str) -> list[str]:
        normalized = text.lower()
        tokens = re.findall(r"[0-9a-z가-힣]{2,}", normalized)
        return tokens

    @staticmethod
    def _env_int(name: str, default: int) -> int:
        import os

        value = os.getenv(name, "").strip()
        if not value:
            return default
        try:
            return int(value)
        except ValueError:
            return default


@lru_cache(maxsize=1)
def get_rag_chat_service() -> RealEstateChatService:
    return RealEstateChatService(get_settings())
