import pytest
from fastapi.testclient import TestClient

from app.core.config import get_settings
from app.main import app
from app.rag.chat_service import ChunkRecord, RealEstateRagChatService


@pytest.fixture(autouse=True)
def clear_model_server_settings(monkeypatch):
    monkeypatch.delenv("MODEL_SERVER_INTERNAL_TOKEN", raising=False)
    get_settings.cache_clear()
    yield
    get_settings.cache_clear()


def test_model_server_app_importable_from_package() -> None:
    assert app.title == "Jiber Model Server"


def _client() -> TestClient:
    return TestClient(app)


def _apartment_payload() -> dict:
    return {
        "propertyId": 1001,
        "asOfDate": "2026-06-12",
        "features": {
            "sido": "서울특별시",
            "sigungu": "강남구",
            "legalDong": "역삼동",
            "exclusiveAreaM2": 84.95,
            "floor": 15,
            "builtYear": 2010,
            "dealYear": 2026,
            "dealMonth": 6,
            "distanceToStationM": 420,
        },
    }


def test_internal_token_empty_local_mode_allows_request(monkeypatch) -> None:
    monkeypatch.delenv("MODEL_SERVER_INTERNAL_TOKEN", raising=False)
    get_settings.cache_clear()
    try:
        response = _client().post(
            "/internal/v1/valuation/apartments",
            json=_apartment_payload(),
        )
    finally:
        get_settings.cache_clear()

    assert response.status_code == 200
    assert response.json()["supported"] is True


def test_health_check_returns_ok() -> None:
    response = _client().get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": "model-server"}


def test_valuation_apartments_success() -> None:
    response = _client().post("/internal/v1/valuation/apartments", json=_apartment_payload())

    assert response.status_code == 200
    body = response.json()
    assert body["supported"] is True
    assert body["estimatedPrice"] == 777_650_000
    assert body["currency"] == "KRW"
    assert body["predictionInterval"] == {
        "lower": 715_438_000,
        "upper": 839_862_000,
    }
    assert body["modelVersion"] == "hedonic-skeleton-v1"
    assert body["baselineDate"] == "2026-06-12"
    assert body["featureSetVersion"] == "apartment-basic-skeleton-v1"
    assert body["warnings"] == [
        "Phase 1 deterministic skeleton response; not a real valuation model."
    ]


def test_shap_apartments_success() -> None:
    response = _client().post("/internal/v1/shap/apartments", json=_apartment_payload())

    assert response.status_code == 200
    body = response.json()
    assert body["supported"] is True
    assert body["baseValue"] == 500_000_000
    assert body["prediction"] == 777_650_000
    assert body["currency"] == "KRW"
    assert body["modelVersion"] == "hedonic-skeleton-v1"
    assert body["baselineDate"] == "2026-06-12"
    assert body["featureSetVersion"] == "apartment-basic-skeleton-v1"
    assert body["values"] == [
        {
            "feature": "exclusiveAreaM2",
            "labelKo": "전용면적",
            "value": 84.95,
            "shapValue": 254_850_000,
            "direction": "UP",
        },
        {
            "feature": "floor",
            "labelKo": "층수",
            "value": 15,
            "shapValue": 12_000_000,
            "direction": "UP",
        },
        {
            "feature": "builtYear",
            "labelKo": "준공연도",
            "value": 2010,
            "shapValue": 15_000_000,
            "direction": "UP",
        },
        {
            "feature": "distanceToStationM",
            "labelKo": "역까지 거리",
            "value": 420,
            "shapValue": -4_200_000,
            "direction": "DOWN",
        },
    ]


def test_missing_required_features_returns_unsupported_payload() -> None:
    payload = _apartment_payload()
    payload["features"].pop("exclusiveAreaM2")
    payload["features"].pop("builtYear")

    response = _client().post("/internal/v1/valuation/apartments", json=payload)

    assert response.status_code == 200
    assert response.json() == {
        "supported": False,
        "reason": "INSUFFICIENT_DATA",
        "missingFeatures": ["exclusiveAreaM2", "builtYear"],
        "modelVersion": "hedonic-skeleton-v1",
        "baselineDate": "2026-06-12",
        "featureSetVersion": "apartment-basic-skeleton-v1",
    }


def test_internal_token_rejects_missing_authorization_header(monkeypatch) -> None:
    monkeypatch.setenv("MODEL_SERVER_INTERNAL_TOKEN", "test-token")
    get_settings.cache_clear()
    try:
        response = _client().post(
            "/internal/v1/valuation/apartments",
            json=_apartment_payload(),
        )
    finally:
        get_settings.cache_clear()

    assert response.status_code == 401
    assert response.json() == {"detail": "Unauthorized"}


def test_internal_token_accepts_valid_bearer_header(monkeypatch) -> None:
    monkeypatch.setenv("MODEL_SERVER_INTERNAL_TOKEN", "test-token")
    get_settings.cache_clear()
    try:
        response = _client().post(
            "/internal/v1/valuation/apartments",
            headers={"Authorization": "Bearer test-token"},
            json=_apartment_payload(),
        )
    finally:
        get_settings.cache_clear()

    assert response.status_code == 200
    assert response.json()["supported"] is True


def test_chat_fallback_answer_without_openai_client() -> None:
    service = RealEstateRagChatService.__new__(RealEstateRagChatService)
    service.openai_client = None
    service.settings = type("SettingsStub", (), {"rag_top_k_final": 1})()

    answer = service._llm_answer(
        "전세 계약 전에 확인할 것 알려줘",
        None,
        [ChunkRecord(text="전세계약 전에는 등기부등본, 보증금 반환 가능성, 선순위 권리를 확인해야 합니다.", source="safe_jeonse_contract_checklist.md")],
    )

    assert "OPENAI_API_KEY" in answer
    assert "safe_jeonse_contract_checklist.md" in answer
    assert "등기부등본" in answer
