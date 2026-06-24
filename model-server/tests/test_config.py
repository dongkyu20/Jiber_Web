from app.core.config import get_settings


def test_settings_load_parent_dotenv_when_environment_unset(tmp_path, monkeypatch) -> None:
    workspace = tmp_path / "workspace"
    service_dir = workspace / "model-server"
    service_dir.mkdir(parents=True)
    (workspace / ".env").write_text(
        "\n".join(
            [
                "MODEL_SERVER_INTERNAL_TOKEN=dotenv-token",
                "MODEL_VERSION=dotenv-model",
                "MODEL_BASELINE_DATE=2026-06-23",
                "MODEL_FEATURE_SET_VERSION=dotenv-feature-set",
                "VALUATION_ARTIFACTS_DIR=dotenv-artifacts",
                "VALUATION_DATA_DIR=dotenv-data",
            ]
        ),
        encoding="utf-8",
    )
    monkeypatch.chdir(service_dir)
    monkeypatch.delenv("JIBER_DOTENV_PATH", raising=False)
    monkeypatch.delenv("MODEL_SERVER_INTERNAL_TOKEN", raising=False)
    monkeypatch.delenv("MODEL_VERSION", raising=False)
    monkeypatch.delenv("MODEL_BASELINE_DATE", raising=False)
    monkeypatch.delenv("MODEL_FEATURE_SET_VERSION", raising=False)
    monkeypatch.delenv("VALUATION_ARTIFACTS_DIR", raising=False)
    monkeypatch.delenv("VALUATION_DATA_DIR", raising=False)
    get_settings.cache_clear()

    try:
        settings = get_settings()
    finally:
        get_settings.cache_clear()

    assert settings.internal_token == "dotenv-token"
    assert settings.model_version == "dotenv-model"
    assert settings.model_baseline_date == "2026-06-23"
    assert settings.feature_set_version == "dotenv-feature-set"
    assert settings.valuation_artifacts_dir == "dotenv-artifacts"
    assert settings.valuation_data_dir == "dotenv-data"
