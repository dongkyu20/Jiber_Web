from __future__ import annotations

import re
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

from app.core.config import Settings, get_settings
from app.schemas.chat import (
    ChatContext,
    RagConfig,
    RealEstateChatResponse,
    RealEstateChatRetrievalResponse,
)
from rank_bm25 import BM25Okapi


SKELETON_ANSWER = (
    "부동산 챗봇은 현재 Spring AI 답변 생성 경로에서 사용됩니다. "
    "model-server는 RAG 검색 컨텍스트만 반환하며 직접 답변을 생성하지 않습니다. "
    "현 단계에서는 투자 조언, 법률·세무 판단, 매수·매도 추천을 제공하지 않습니다."
)

SUPPORTED_TEXT_SUFFIXES = {".md", ".txt"}


@dataclass(frozen=True)
class ChunkRecord:
    source: str
    title: str
    text: str


class RealEstateChatService:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.chunks = self._load_chunks()
        self._tokenized_chunks = [
            _tokenize(f"{chunk.source}\n{chunk.text}") for chunk in self.chunks
        ]
        self._bm25 = (
            BM25Okapi(self._tokenized_chunks) if self._tokenized_chunks else None
        )

    def answer(self, question: str, runtime_context: dict | None) -> RealEstateChatResponse:
        retrieval = self.retrieve(question, runtime_context)
        return RealEstateChatResponse(
            available=False,
            answer=SKELETON_ANSWER,
            contexts=retrieval.contexts,
            model="chat-skeleton-v1",
            ragConfig=retrieval.ragConfig,
        )

    def retrieve(self, question: str, runtime_context: dict | None) -> RealEstateChatRetrievalResponse:
        contexts = self._retrieve_contexts(question)
        return RealEstateChatRetrievalResponse(
            contexts=contexts,
            ragConfig=RagConfig(
                embedding="lexical-bm25",
                chunkSize=self.settings.rag_chunk_size,
                overlap=self.settings.rag_chunk_overlap,
                hybrid=False,
                rerank=False,
            ),
        )

    def _retrieve_contexts(self, question: str) -> list[ChatContext]:
        if not self._bm25 or not self.chunks:
            return []

        query_tokens = _tokenize(question)
        if not query_tokens:
            return []

        compact_query = _compact(question)
        scores = self._bm25.get_scores(query_tokens)
        ranked: list[tuple[float, ChunkRecord]] = []
        searchable_tokens = {
            token for token in query_tokens if len(token) >= 2 and not token.isdigit()
        } | {token for token in query_tokens if token.isdigit()}

        for index, chunk in enumerate(self.chunks):
            score = float(scores[index])
            compact_source = _compact(chunk.source)
            compact_title = _compact(chunk.title)
            compact_text = _compact(chunk.text)
            compact_head = _compact(chunk.text[:600])
            if compact_query and compact_query in f"{compact_source}{compact_text}":
                score += 8.0
            matched_tokens = 0
            for token in searchable_tokens:
                token_matched = False
                if token in compact_source:
                    score += 2.5
                    token_matched = True
                if token in compact_title:
                    score += 4.0
                    token_matched = True
                if token in compact_head:
                    score += 2.0
                    token_matched = True
                elif token in compact_text:
                    score += 0.75
                    token_matched = True
                if token_matched:
                    matched_tokens += 1
            if searchable_tokens:
                score += (matched_tokens / len(searchable_tokens)) * 5.0
            score -= _boilerplate_penalty(chunk.text)
            if score > 0:
                ranked.append((score, chunk))

        ranked.sort(key=lambda item: item[0], reverse=True)
        return [
            ChatContext(source=chunk.source, text=chunk.text)
            for _, chunk in ranked[: self.settings.rag_top_k_final]
        ]

    def _load_chunks(self) -> list[ChunkRecord]:
        docs_dir = _resolve_docs_dir(self.settings.rag_docs_dir)
        if not docs_dir.exists():
            return []

        chunks: list[ChunkRecord] = []
        for path in sorted(docs_dir.rglob("*")):
            if not path.is_file() or path.suffix.lower() not in SUPPORTED_TEXT_SUFFIXES:
                continue
            text = path.read_text(encoding="utf-8", errors="ignore").strip()
            if len(text) < 40:
                continue
            relative_source = path.relative_to(docs_dir.parent).as_posix()
            title = _extract_title(text) or path.stem
            for chunk_index, chunk_text in enumerate(
                _chunk_text(text, self.settings.rag_chunk_size, self.settings.rag_chunk_overlap),
                start=1,
            ):
                chunks.append(
                    ChunkRecord(
                        source=f"{relative_source}#chunk-{chunk_index}",
                        title=title,
                        text=chunk_text,
                    )
                )
        return chunks


def _resolve_docs_dir(configured_path: str) -> Path:
    docs_dir = Path(configured_path).expanduser()
    if docs_dir.is_absolute():
        return docs_dir
    model_server_root = Path(__file__).resolve().parents[2]
    return model_server_root / docs_dir


def _chunk_text(text: str, chunk_size: int, overlap: int) -> list[str]:
    chunk_size = max(chunk_size, 300)
    overlap = min(max(overlap, 0), chunk_size // 2)
    paragraphs = [paragraph.strip() for paragraph in re.split(r"\n\s*\n", text) if paragraph.strip()]
    chunks: list[str] = []
    current = ""

    for paragraph in paragraphs:
        candidate = f"{current}\n\n{paragraph}".strip() if current else paragraph
        if len(candidate) <= chunk_size:
            current = candidate
            continue
        if current:
            chunks.append(current)
        while len(paragraph) > chunk_size:
            chunks.append(paragraph[:chunk_size].strip())
            paragraph = paragraph[chunk_size - overlap :].strip()
        current = paragraph

    if current:
        chunks.append(current)
    return chunks


def _extract_title(text: str) -> str | None:
    frontmatter_title = re.search(r"(?m)^title:\s*(.+)$", text)
    if frontmatter_title:
        return frontmatter_title.group(1).strip()
    markdown_heading = re.search(r"(?m)^#\s+(.+)$", text)
    if markdown_heading:
        return markdown_heading.group(1).strip()
    return None


def _tokenize(text: str) -> list[str]:
    spaced = re.sub(r"(?<=[0-9])(?=[A-Za-z가-힣])|(?<=[A-Za-z가-힣])(?=[0-9])", " ", text.lower())
    raw_terms = re.findall(r"[0-9]+|[a-z]+|[가-힣]+", spaced)
    tokens: list[str] = []
    for term in raw_terms:
        tokens.append(term)
        if re.fullmatch(r"[가-힣]+", term) and len(term) >= 3:
            for size in range(2, min(4, len(term)) + 1):
                tokens.extend(term[index : index + size] for index in range(len(term) - size + 1))
    return tokens


def _compact(text: str) -> str:
    return "".join(re.findall(r"[0-9a-z가-힣]+", text.lower()))


def _boilerplate_penalty(text: str) -> float:
    compact_text = _compact(text[:1200])
    boilerplate_terms = (
        "본문으로바로가기",
        "로그인",
        "마이페이지",
        "화면크기",
        "폰트크기",
        "사이트맵",
        "통계db",
    )
    matches = sum(1 for term in boilerplate_terms if term in compact_text)
    if matches >= 5:
        return 30.0
    if matches >= 3:
        return 12.0
    return 0.0


@lru_cache(maxsize=1)
def get_rag_chat_service() -> RealEstateChatService:
    return RealEstateChatService(get_settings())
