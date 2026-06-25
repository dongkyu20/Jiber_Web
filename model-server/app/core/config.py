import os
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from typing import Optional


DEFAULT_MODEL_VERSION = "hedonic-skeleton-v1"
DEFAULT_FEATURE_SET_VERSION = "apartment-basic-skeleton-v1"
DEFAULT_RAG_DOCS_DIR = "documents/structured"
DEFAULT_RAG_CHUNK_SIZE = 1200
DEFAULT_RAG_CHUNK_OVERLAP = 200
DEFAULT_RAG_TOP_K_FINAL = 4
DEFAULT_VALUATION_ARTIFACTS_DIR = "artifacts/valuation"
DEFAULT_VALUATION_DATA_DIR = "../data/valuation"


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
    valuation_artifacts_dir: str
    valuation_data_dir: str


def _env_int(name: str, default: int) -> int:
    raw_value = os.getenv(name, "").strip()
    if not raw_value:
        return default
    try:
        return int(raw_value)
    except ValueError:
        return default


def _dotenv_candidates() -> list[Path]:
    explicit_path = os.getenv("JIBER_DOTENV_PATH")
    if explicit_path is not None:
        return [Path(explicit_path)]

    cwd = Path.cwd()
    return [cwd / ".env", *(parent / ".env" for parent in cwd.parents)]


def _read_dotenv() -> dict[str, str]:
    for candidate in _dotenv_candidates():
        if not candidate.is_file():
            continue

        values: dict[str, str] = {}
        for raw_line in candidate.read_text(encoding="utf-8").splitlines():
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue

            key, value = line.split("=", 1)
            key = key.strip()
            value = value.strip()
            if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
                value = value[1:-1]
            if key:
                values[key] = value
        return values

    return {}


def _setting_value(key: str, dotenv: dict[str, str]) -> str:
    value = os.getenv(key)
    if value is None:
        value = dotenv.get(key, "")
    return value.strip()


@lru_cache
def get_settings() -> Settings:
    dotenv = _read_dotenv()

    return Settings(
        internal_token=_setting_value("MODEL_SERVER_INTERNAL_TOKEN", dotenv),
        model_version=_setting_value("MODEL_VERSION", dotenv) or DEFAULT_MODEL_VERSION,
        model_baseline_date=_setting_value("MODEL_BASELINE_DATE", dotenv) or None,
        feature_set_version=(
            _setting_value("MODEL_FEATURE_SET_VERSION", dotenv) or DEFAULT_FEATURE_SET_VERSION
        ),
        rag_docs_dir=os.getenv("RAG_DOCS_DIR", "").strip() or DEFAULT_RAG_DOCS_DIR,
        rag_chunk_size=_env_int("RAG_CHUNK_SIZE", DEFAULT_RAG_CHUNK_SIZE),
        rag_chunk_overlap=_env_int("RAG_CHUNK_OVERLAP", DEFAULT_RAG_CHUNK_OVERLAP),
        rag_top_k_final=_env_int("RAG_TOP_K_FINAL", DEFAULT_RAG_TOP_K_FINAL),
        valuation_artifacts_dir=(
            _setting_value("VALUATION_ARTIFACTS_DIR", dotenv)
            or DEFAULT_VALUATION_ARTIFACTS_DIR
        ),
        valuation_data_dir=(
            _setting_value("VALUATION_DATA_DIR", dotenv)
            or DEFAULT_VALUATION_DATA_DIR
        ),
    )
