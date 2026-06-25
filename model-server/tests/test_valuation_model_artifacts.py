import math
import pickle
from pathlib import Path

import pytest

from app.core.config import Settings
from app.schemas.apartment import ApartmentFeatures, ApartmentInferenceRequest
from app.services.valuation_service import (
    ApartmentValuationService,
    ModelPrediction,
    ValuationModelRepository,
)


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


class FakePredictModel:
    first_month = "202307"

    def __init__(self, predicted_log_price: float) -> None:
        self.predicted_log_price = predicted_log_price
        self.rows: list[dict] = []

    def predict(self, rows: list[dict]) -> list[float]:
        self.rows.extend(rows)
        return [self.predicted_log_price for _ in rows]


class StaticPredictionRepository:
    def __init__(self, prediction: ModelPrediction | None) -> None:
        self.prediction = prediction
        self.features: ApartmentFeatures | None = None

    def predict(self, features: ApartmentFeatures) -> ModelPrediction | None:
        self.features = features
        return self.prediction


def test_repository_loads_city_artifact_and_predicts_krw(tmp_path: Path) -> None:
    model = FakePredictModel(math.log(912_345_678))
    _write_artifact(tmp_path, "seoul", "seoul-run", model)
    repository = ValuationModelRepository(tmp_path)

    prediction = repository.predict(
        ApartmentFeatures(
            sido="서울특별시",
            sigungu="강남구",
            legalDong="삼성동",
            exclusiveAreaM2=84.95,
            floor=15,
            builtYear=2010,
            dealYear=2026,
            dealMonth=6,
            distanceToStationM=420,
        )
    )

    assert prediction is not None
    assert prediction.price_krw == 912_345_678
    assert prediction.model_version == "seoul-run"
    assert prediction.warning == "Loaded valuation artifact for seoul."
    loaded_model = repository._models["seoul"]
    row = loaded_model.rows[0]
    assert prediction.feature_row == row
    assert row["__bias__"] == 1
    assert row["log_land_area_m2"] == 0.0
    assert row["has_land_area"] == 0
    assert row["property_type"] == "apartment"
    assert row["house_type"] == "unknown"
    assert row["log_area_m2"] == math.log1p(84.95)
    assert row["floor"] == 15
    assert row["floor_band"] == "floor_13_18"
    assert row["low_floor"] == 0
    assert row["is_first_floor"] == 0
    assert row["is_floor_2_3"] == 0
    assert row["built_year"] == 2010
    assert row["age"] == 16
    assert row["age_band"] == "age_10_19"
    assert row["deal_year"] == 2026
    assert row["deal_month_index"] == 35
    assert row["calendar_month"] == "6"
    assert row["log_nearest_subway_distance_m"] == math.log1p(420)
    assert row["nearest_subway_distance_m_missing"] == 0
    assert row["estimated_max_floor"] == 15
    assert row["max_floor_source"] == "current_floor_estimate"
    assert row["relative_floor"] == 1.0
    assert row["relative_floor_bin"] == "relative_floor_100"
    assert row["floors_below_estimated_top"] == 0
    assert row["floors_below_estimated_top_bin"] == "below_top_0"
    assert row["is_estimated_top_floor"] == 1
    assert row["is_near_estimated_top_floor"] == 1
    assert row["kapt_max_floor"] == 0.0
    assert row["kapt_max_floor_missing"] == 1
    assert row["floors_below_kapt_top"] == 0
    assert row["floors_below_kapt_top_bin"] == "missing"
    assert row["kapt_relative_floor"] == 0.0
    assert row["kapt_relative_floor_bin"] == "missing"
    return
    assert loaded_model.rows == [
        {
            "__bias__": 1,
            "district": "강남구",
            "legal_dong": "삼성동",
            "log_area_m2": math.log(84.95),
            "floor": 15,
            "floor_band": "floor_13_18",
            "low_floor": 0,
            "is_first_floor": 0,
            "is_floor_2_3": 0,
            "built_year": 2010,
            "age": 16,
            "age_band": "age_10_19",
            "deal_year": 2026,
            "deal_month_index": 2026 * 12 + 6,
            "calendar_month": "6",
            "log_nearest_subway_distance_m": math.log1p(420),
        }
    ]


def test_repository_selects_busan_artifact(tmp_path: Path) -> None:
    seoul_model = FakePredictModel(math.log(1_500_000_000))
    busan_model = FakePredictModel(math.log(456_000_000))
    _write_artifact(tmp_path, "seoul", "seoul-run", seoul_model)
    _write_artifact(tmp_path, "busan", "busan-run", busan_model)
    repository = ValuationModelRepository(tmp_path)

    prediction = repository.predict(
        ApartmentFeatures(
            sido="부산광역시",
            sigungu="해운대구",
            legalDong="중동",
            exclusiveAreaM2=59.5,
            floor=7,
            builtYear=2005,
            dealYear=2026,
            dealMonth=4,
            distanceToStationM=180,
        )
    )

    assert prediction is not None
    assert prediction.price_krw == 456_000_000
    assert prediction.model_version == "busan-run"
    assert "seoul" not in repository._models
    assert repository._models["busan"].rows[0]["district"] == "해운대구"


def test_repository_uses_current_floor_as_estimated_top_when_complex_history_is_absent(tmp_path: Path) -> None:
    model = FakePredictModel(math.log(700_000_000))
    _write_artifact(tmp_path, "seoul", "seoul-run", model)
    repository = ValuationModelRepository(tmp_path)

    repository.predict(
        ApartmentFeatures(
            sido="서울특별시",
            sigungu="강남구",
            legalDong="논현동",
            propertyName="규칙테스트",
            householdCount=0,
            exclusiveAreaM2=59.5,
            floor=3,
            builtYear=2020,
            dealYear=2026,
            dealMonth=1,
        )
    )

    row = repository._models["seoul"].rows[0]
    assert row["estimated_max_floor"] == 3
    assert row["relative_floor"] == 1.0
    assert row["relative_floor_bin"] == "relative_floor_100"
    assert row["floors_below_estimated_top"] == 0
    assert row["floors_below_estimated_top_bin"] == "below_top_0"
    assert row["is_estimated_top_floor"] == 1
    assert row["log_household_count"] == 0.0
    assert row["household_count_missing"] == 0


def test_repository_keeps_relative_floor_075_in_50_75_bucket(tmp_path: Path) -> None:
    model = FakePredictModel(math.log(700_000_000))
    _write_artifact(tmp_path, "seoul", "seoul-run", model)
    repository = ValuationModelRepository(tmp_path)
    repository.data_repository._complex_floor_records = [
        {
            "city_code": "seoul",
            "district": "강남구",
            "legal_dong": "논현동",
            "name": "경계테스트",
            "normalized_name": "경계테스트",
            "normalized_name_variant": "경계테스트",
            "estimated_max_floor": 4,
            "observation_count": 1,
        }
    ]
    repository.data_repository._kapt_records = []
    repository.data_repository._academy_records = []

    repository.predict(
        ApartmentFeatures(
            sido="서울특별시",
            sigungu="강남구",
            legalDong="논현동",
            propertyName="경계테스트",
            exclusiveAreaM2=59.5,
            floor=3,
            builtYear=2020,
            dealYear=2026,
            dealMonth=1,
        )
    )

    row = repository._models["seoul"].rows[0]
    assert row["relative_floor"] == pytest.approx(0.75)
    assert row["relative_floor_bin"] == "relative_floor_50_75"


def test_repository_finds_artifact_when_zip_extracts_nested_directory(tmp_path: Path) -> None:
    model = FakePredictModel(math.log(700_000_000))
    _write_artifact(tmp_path / "unzipped-model", "seoul", "nested-seoul-run", model)
    repository = ValuationModelRepository(tmp_path)

    prediction = repository.predict(
        ApartmentFeatures(
            sido="서울특별시",
            sigungu="강남구",
            legalDong="삼성동",
            exclusiveAreaM2=84.95,
            floor=15,
            builtYear=2010,
            dealYear=2026,
            dealMonth=6,
        )
    )

    assert prediction is not None
    assert prediction.price_krw == 700_000_000
    assert prediction.model_version == "nested-seoul-run"


def test_service_uses_model_prediction_when_repository_can_predict() -> None:
    repository = StaticPredictionRepository(
        ModelPrediction(
            price_krw=912_345_678,
            model_version="seoul-run",
            warning="Loaded valuation artifact for seoul.",
        )
    )
    service = ApartmentValuationService(model_repository=repository)

    response = service.valuation(_request(), _settings())

    assert response.estimatedPrice == 912_345_678
    assert response.predictionInterval.lower == 839_358_024
    assert response.predictionInterval.upper == 985_333_332
    assert response.modelVersion == "seoul-run"
    assert response.warnings == ["Loaded valuation artifact for seoul."]
    assert repository.features == _request().features


def test_service_uses_model_prediction_in_shap_response() -> None:
    service = ApartmentValuationService(
        model_repository=StaticPredictionRepository(
            ModelPrediction(
                price_krw=912_345_678,
                model_version="seoul-run",
                warning="Loaded valuation artifact for seoul.",
                feature_row={
                    "legal_dong_target_log_price_delta": 0.08,
                    "district_target_log_price_delta": 0.03,
                    "log_household_count": math.log1p(850),
                    "parking_spaces_per_household": 1.25,
                    "households_per_building": 125,
                    "age": 16,
                    "deal_month_index": 2026 * 12 + 6,
                    "log_nearest_subway_distance_m": math.log1p(420),
                    "log_nearest_bus_stop_distance_m": math.log1p(120),
                    "bus_stop_count_radius_bin": "count_11_20",
                    "log_nearest_park_distance_m": math.log1p(350),
                    "park_exists": 1,
                    "log_park_area_total_m2_radius": math.log1p(25_000),
                    "school_count_radius_bin": "count_3_5",
                    "academy_count_radius_bin": "count_10_plus",
                    "kapt_relative_floor": 0.65,
                },
            )
        )
    )

    response = service.shap(_request(), _settings())

    assert response.prediction == 912_345_678
    assert response.modelVersion == "seoul-run"
    assert [value.feature for value in response.values] == SHAP_GROUP_FEATURES
    assert [value.labelKo for value in response.values] == SHAP_GROUP_LABELS
    assert all(value.direction in {"UP", "DOWN", "NEUTRAL"} for value in response.values)
    values_by_feature = {value.feature: value for value in response.values}
    assert values_by_feature["legalDongLocation"].shapValue > 0
    assert values_by_feature["transitAccess"].shapValue != 0
    assert values_by_feature["parkAccess"].shapValue != 0
    assert values_by_feature["educationAccess"].shapValue != 0


def test_service_keeps_skeleton_prediction_when_no_model_matches() -> None:
    service = ApartmentValuationService(model_repository=StaticPredictionRepository(None))

    response = service.valuation(_request(), _settings())

    assert response.estimatedPrice == 777_650_000
    assert response.modelVersion == "hedonic-skeleton-v1"


def _write_artifact(
    root: Path,
    city_code: str,
    run_id: str,
    model: FakePredictModel,
) -> None:
    artifact_dir = root / run_id
    artifact_dir.mkdir(parents=True)
    (artifact_dir / "run_manifest.json").write_text(
        f'{{"city_code": "{city_code}", "run_id": "{run_id}", "artifact_paths": {{"model": "model.pkl"}}}}',
        encoding="utf-8",
    )
    with (artifact_dir / "model.pkl").open("wb") as file:
        pickle.dump(model, file)


def _request() -> ApartmentInferenceRequest:
    return ApartmentInferenceRequest(
        propertyId=1001,
        asOfDate="2026-06-12",
        features=ApartmentFeatures(
            sido="서울특별시",
            sigungu="강남구",
            legalDong="삼성동",
            exclusiveAreaM2=84.95,
            floor=15,
            builtYear=2010,
            dealYear=2026,
            dealMonth=6,
            distanceToStationM=420,
        ),
    )


def _settings() -> Settings:
    return Settings(
        internal_token="",
        model_version="hedonic-skeleton-v1",
        model_baseline_date=None,
        feature_set_version="apartment-basic-skeleton-v1",
        rag_docs_dir="documents/structured",
        rag_chunk_size=1200,
        rag_chunk_overlap=200,
        rag_top_k_final=4,
        valuation_artifacts_dir="artifacts/valuation",
        valuation_data_dir="../data",
    )
