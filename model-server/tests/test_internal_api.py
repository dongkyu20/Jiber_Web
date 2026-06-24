import pytest
from fastapi.testclient import TestClient

from app.core.config import get_settings
from app.main import app
from app.rag.chat_service import get_rag_chat_service
from app.services.valuation_service import get_valuation_model_repository


SHAP_GROUP_FEATURES = [
    "legalDongLocation",
    "area",
    "complexScaleParking",
    "age",
    "transitAccess",
    "parkAccess",
    "educationAccess",
    "districtLocation",
    "floor",
]

SHAP_GROUP_LABELS = [
    "\ubc95\uc815\ub3d9 \uc785\uc9c0",
    "\uba74\uc801",
    "\ub2e8\uc9c0\uaddc\ubaa8/\uc8fc\ucc28",
    "\ub178\ud6c4\ub3c4",
    "\uad50\ud1b5 \uc811\uadfc\uc131",
    "\uacf5\uc6d0 \uc811\uadfc\uc131",
    "\uad50\uc721 \uc811\uadfc\uc131",
    "\uc790\uce58\uad6c \uc785\uc9c0",
    "\uce35\uc218",
]


@pytest.fixture(autouse=True)
def clear_model_server_settings(monkeypatch, tmp_path):
    monkeypatch.delenv("MODEL_SERVER_INTERNAL_TOKEN", raising=False)
    monkeypatch.setenv("VALUATION_ARTIFACTS_DIR", str(tmp_path / "no-artifacts"))
    monkeypatch.setenv("VALUATION_DATA_DIR", str(tmp_path / "no-data"))
    get_settings.cache_clear()
    get_rag_chat_service.cache_clear()
    get_valuation_model_repository.cache_clear()
    yield
    get_settings.cache_clear()
    get_rag_chat_service.cache_clear()
    get_valuation_model_repository.cache_clear()


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
    values = body["values"]
    assert [value["feature"] for value in values] == SHAP_GROUP_FEATURES
    assert [value["labelKo"] for value in values] == SHAP_GROUP_LABELS
    assert all(isinstance(value["shapValue"], int) for value in values)
    assert all(value["direction"] in {"UP", "DOWN", "NEUTRAL"} for value in values)


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


def test_chat_real_estate_returns_skeleton_unavailable_response() -> None:
    response = _client().post(
        "/internal/v1/chat/real-estate",
        json={
            "question": "전세 계약 전에 확인할 것 알려줘",
            "runtimeContext": {
                "source": "property-detail",
                "property": {
                    "propertyId": 1912,
                    "name": "경희궁롯데캐슬",
                },
            },
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["available"] is False
    assert body["answer"] == (
        "부동산 챗봇은 현재 Spring AI 답변 생성 경로에서 사용됩니다. "
        "model-server는 RAG 검색 컨텍스트만 반환하며 직접 답변을 생성하지 않습니다. "
        "현 단계에서는 투자 조언, 법률·세무 판단, 매수·매도 추천을 제공하지 않습니다."
    )
    assert body["contexts"]
    assert body["model"] == "chat-skeleton-v1"
    assert body["ragConfig"] == {
        "embedding": "lexical-bm25",
        "chunkSize": 1200,
        "overlap": 200,
        "hybrid": False,
        "rerank": False,
    }


def test_chat_retrieve_returns_2026_housing_trend_context() -> None:
    response = _client().post(
        "/internal/v1/chat/real-estate/retrieve",
        json={
            "question": "2026주택동향에 대해서 설명해줘",
            "runtimeContext": {},
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["contexts"]
    assert body["ragConfig"]["embedding"] == "lexical-bm25"
    assert any(
        "r_one_2026_05_housing_price_trend_report" in context["source"]
        for context in body["contexts"]
    )
    assert any(
        "전국주택가격동향조사" in context["text"] or "2026년 5월" in context["text"]
        for context in body["contexts"]
    )
