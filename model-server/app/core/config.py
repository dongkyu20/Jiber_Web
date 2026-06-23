import os
from dataclasses import dataclass
from functools import lru_cache
from typing import Optional


DEFAULT_MODEL_VERSION = "hedonic-skeleton-v1"
DEFAULT_FEATURE_SET_VERSION = "apartment-basic-skeleton-v1"
DEFAULT_RAG_DOCS_DIR = "documents/structured"
DEFAULT_RAG_CHUNK_SIZE = 1200
DEFAULT_RAG_CHUNK_OVERLAP = 200
DEFAULT_RAG_TOP_K_FINAL = 4


@dataclass(frozen=True)
class Settings:
    internal_token: str
    model_version: str
    model_baseline_date: Optional[str]
    feature_set_version: str
    rag_docs_dir: str
    rag_chunk_size: int
    rag_chunk_overlap: int
    rag_top_k_final: int


def _env_int(name: str, default: int) -> int:
    raw_value = os.getenv(name, "").strip()
    if not raw_value:
        return default
    try:
        return int(raw_value)
    except ValueError:
        return default


@lru_cache
def get_settings() -> Settings:
    return Settings(
        internal_token=os.getenv("MODEL_SERVER_INTERNAL_TOKEN", "").strip(),
        model_version=os.getenv("MODEL_VERSION", "").strip() or DEFAULT_MODEL_VERSION,
        model_baseline_date=os.getenv("MODEL_BASELINE_DATE", "").strip() or None,
        feature_set_version=(
            os.getenv("MODEL_FEATURE_SET_VERSION", "").strip()
            or DEFAULT_FEATURE_SET_VERSION
        ),
        rag_docs_dir=os.getenv("RAG_DOCS_DIR", "").strip() or DEFAULT_RAG_DOCS_DIR,
        rag_chunk_size=_env_int("RAG_CHUNK_SIZE", DEFAULT_RAG_CHUNK_SIZE),
        rag_chunk_overlap=_env_int("RAG_CHUNK_OVERLAP", DEFAULT_RAG_CHUNK_OVERLAP),
        rag_top_k_final=_env_int("RAG_TOP_K_FINAL", DEFAULT_RAG_TOP_K_FINAL),
    )
