import json
import math
import pickle
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from typing import Any, Protocol
from typing import List, Optional

from app.core.config import Settings
from app.schemas.apartment import (
    ApartmentFeatures,
    ApartmentInferenceRequest,
    PredictionInterval,
    ShapSupportedResponse,
    ShapValue,
    UnsupportedModelResponse,
    ValuationSupportedResponse,
)
from app.services.valuation_data_repository import LocalValuationDataRepository


BASE_VALUE_KRW = 500_000_000
SKELETON_WARNING = "Phase 1 deterministic skeleton response; not a real valuation model."
REQUIRED_FEATURES = ("exclusiveAreaM2", "builtYear")


@dataclass(frozen=True)
class FeatureContribution:
    feature: str
    label_ko: str
    value: float
    shap_value: int


@dataclass(frozen=True)
class ModelPrediction:
    price_krw: int
    model_version: str
    warning: str
    feature_row: dict[str, Any] | None = None


class ValuationPredictionRepository(Protocol):
    def predict(self, features: ApartmentFeatures) -> ModelPrediction | None:
        ...


class ValuationModelRepository:
    def __init__(self, artifacts_dir: str | Path, data_dir: str | Path | None = None) -> None:
        self.artifacts_dir = self._resolve_artifacts_dir(artifacts_dir)
        self.data_dir = self._resolve_data_dir(data_dir or "../data/valuation")
        self.data_repository = LocalValuationDataRepository(
            self.data_dir,
            accept_remaining_matches=True,
        )
        self._manifests: dict[str, dict[str, Any]] | None = None
        self._models: dict[str, Any] = {}

    def predict(self, features: ApartmentFeatures) -> ModelPrediction | None:
        city_code = self._city_code(features.sido)
        if city_code is None:
            return None

        model, manifest = self._load_city_model(city_code)
        if model is None:
            return None

        feature_row = self._feature_row(features, city_code, model)
        raw_prediction = model.predict([feature_row])
        log_price = float(raw_prediction[0])
        price_krw = max(round(math.exp(log_price)), 1)
        model_version = str(manifest.get("run_id") or manifest.get("model_type") or city_code)
        return ModelPrediction(
            price_krw=price_krw,
            model_version=model_version,
            warning=f"Loaded valuation artifact for {city_code}.",
            feature_row=feature_row,
        )

    def _load_city_model(self, city_code: str) -> tuple[Any | None, dict[str, Any]]:
        if city_code in self._models:
            manifest = self._manifests_by_city().get(city_code, {})
            return self._models[city_code], manifest

        manifest = self._manifests_by_city().get(city_code)
        if manifest is None:
            return None, {}

        model_path = Path(manifest["artifact_dir"]) / manifest.get("artifact_paths", {}).get("model", "model.pkl")
        with model_path.open("rb") as file:
            model = pickle.load(file)
        self._models[city_code] = model
        return model, manifest

    def _manifests_by_city(self) -> dict[str, dict[str, Any]]:
        if self._manifests is not None:
            return self._manifests

        manifests: dict[str, dict[str, Any]] = {}
        if self.artifacts_dir.is_dir():
            for manifest_path in self.artifacts_dir.rglob("run_manifest.json"):
                manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
                city_code = str(manifest.get("city_code", "")).strip().lower()
                if city_code:
                    manifest["artifact_dir"] = str(manifest_path.parent)
                    manifests[city_code] = manifest
        self._manifests = manifests
        return manifests

    def _feature_row(
        self,
        features: ApartmentFeatures,
        city_code: str,
        model: Any,
    ) -> dict[str, Any]:
        row: dict[str, Any] = {
            "__bias__": 1,
            "log_land_area_m2": 0.0,
            "has_land_area": 0,
            "property_type": "apartment",
            "house_type": "unknown",
        }
        if features.sigungu:
            row["district"] = features.sigungu
        if features.legalDong:
            row["legal_dong"] = features.legalDong

        area = self._positive_number(features.exclusiveAreaM2)
        if area is not None:
            row["log_area_m2"] = math.log1p(area)

        floor = self._integer(features.floor)
        if floor is not None:
            row["floor"] = floor
            row["floor_band"] = self._floor_band(floor)
            row["low_floor"] = 1 if floor <= 3 else 0
            row["is_first_floor"] = 1 if floor == 1 else 0
            row["is_floor_2_3"] = 1 if 2 <= floor <= 3 else 0

        built_year = self._integer(features.builtYear)
        deal_year = self._integer(features.dealYear)
        if built_year is not None:
            row["built_year"] = built_year
        if built_year is not None and deal_year is not None:
            age = max(deal_year - built_year, 0)
            row["age"] = age
            row["age_band"] = self._age_band(age)

        deal_month = self._integer(features.dealMonth)
        if deal_year is not None:
            row["deal_year"] = deal_year
        if deal_year is not None and deal_month is not None:
            row["deal_month_index"] = self._deal_month_index(model, deal_year, deal_month)
            row["calendar_month"] = str(deal_month)

        row.update(self.data_repository.features_for(features, city_code))
        subway_distance = self._number(features.distanceToStationM)
        if subway_distance is not None:
            row["log_nearest_subway_distance_m"] = math.log1p(max(subway_distance, 0))
            row["nearest_subway_distance_m_missing"] = 0

        self._add_target_encoding_features(row, features, model)
        return row

    def _city_code(self, sido: str | None) -> str | None:
        if not sido:
            return None
        normalized = sido.strip().lower()
        if "서울" in normalized or "seoul" in normalized:
            return "seoul"
        if "부산" in normalized or "busan" in normalized:
            return "busan"
        if "서울" in normalized or "seoul" in normalized or "쒖슱" in normalized:
            return "seoul"
        if "부산" in normalized or "busan" in normalized or "遺" in normalized:
            return "busan"
        return None

    def _add_target_encoding_features(
        self,
        row: dict[str, Any],
        features: ApartmentFeatures,
        model: Any,
    ) -> None:
        target_encodings = getattr(model, "target_encodings", None)
        if target_encodings is None:
            return

        global_mean = self._number(getattr(target_encodings, "global_mean", None))
        values = getattr(target_encodings, "values", {}) or {}
        counts = getattr(target_encodings, "counts", {}) or {}
        if global_mean is None:
            return

        for field_name, feature_value in (
            ("district", features.sigungu),
            ("legal_dong", features.legalDong),
        ):
            if not feature_value:
                continue
            field_values = values.get(field_name, {})
            field_counts = counts.get(field_name, {})
            smooth_value = self._number(field_values.get(feature_value))
            count = self._integer(field_counts.get(feature_value)) or 0
            if smooth_value is None:
                smooth_value = global_mean

            row[f"{field_name}_target_log_price_smooth"] = smooth_value
            row[f"{field_name}_target_log_price_delta"] = smooth_value - global_mean
            row[f"{field_name}_target_count_log1p"] = math.log1p(max(count, 0))

    def _floor_band(self, floor: int) -> str:
        if floor <= 1:
            return "floor_1"
        if floor <= 3:
            return "floor_2_3"
        if floor <= 7:
            return "floor_4_7"
        if floor <= 12:
            return "floor_8_12"
        if floor <= 18:
            return "floor_13_18"
        if floor <= 25:
            return "floor_19_25"
        return "floor_26_plus"

    def _age_band(self, age: int) -> str:
        if age <= 4:
            return "age_0_4"
        if age <= 9:
            return "age_5_9"
        if age <= 19:
            return "age_10_19"
        if age <= 29:
            return "age_20_29"
        if age <= 39:
            return "age_30_39"
        return "age_40_plus"

    def _deal_month_index(self, model: Any, deal_year: int, deal_month: int) -> int:
        first_month = str(getattr(model, "first_month", "") or "")
        if len(first_month) == 6 and first_month.isdigit():
            first_year = int(first_month[:4])
            first_month_number = int(first_month[4:])
            if 1 <= first_month_number <= 12:
                return (deal_year - first_year) * 12 + (deal_month - first_month_number)
        return deal_year * 12 + deal_month

    def _integer(self, value: Optional[int]) -> int | None:
        if value is None:
            return None
        return int(value)

    def _positive_number(self, value: Optional[float]) -> float | None:
        number = self._number(value)
        if number is None or number <= 0:
            return None
        return number

    def _number(self, value: Optional[float]) -> float | None:
        if value is None:
            return None
        return float(value)

    def _resolve_artifacts_dir(self, artifacts_dir: str | Path) -> Path:
        path = Path(artifacts_dir)
        if path.is_absolute():
            return path
        return Path(__file__).resolve().parents[2] / path

    def _resolve_data_dir(self, data_dir: str | Path) -> Path:
        path = Path(data_dir)
        if path.is_absolute():
            return path
        return Path(__file__).resolve().parents[2] / path


@lru_cache
def get_valuation_model_repository(artifacts_dir: str, data_dir: str) -> ValuationModelRepository:
    return ValuationModelRepository(artifacts_dir, data_dir=data_dir)


class ApartmentValuationService:
    def __init__(
        self,
        model_repository: ValuationPredictionRepository | None = None,
    ) -> None:
        self.model_repository = model_repository

    def valuation(
        self,
        request: ApartmentInferenceRequest,
        settings: Settings,
    ) -> ValuationSupportedResponse:
        fallback_contributions = self._fallback_contributions(request.features)
        model_prediction = self._model_prediction(request.features, settings)
        estimated_price = (
            model_prediction.price_krw
            if model_prediction is not None
            else self._prediction_from_contributions(fallback_contributions)
        )
        interval_width = round(estimated_price * 0.08)

        return ValuationSupportedResponse(
            supported=True,
            estimatedPrice=estimated_price,
            currency="KRW",
            predictionInterval=PredictionInterval(
                lower=estimated_price - interval_width,
                upper=estimated_price + interval_width,
            ),
            modelVersion=model_prediction.model_version if model_prediction else settings.model_version,
            baselineDate=self._baseline_date(request, settings),
            featureSetVersion=settings.feature_set_version,
            warnings=[model_prediction.warning] if model_prediction else [SKELETON_WARNING],
        )

    def shap(
        self,
        request: ApartmentInferenceRequest,
        settings: Settings,
    ) -> ShapSupportedResponse:
        model_prediction = self._model_prediction(request.features, settings)
        contributions = self._feature_contributions(
            request.features,
            model_prediction.feature_row if model_prediction else None,
        )
        fallback_prediction = self._prediction_from_contributions(
            self._fallback_contributions(request.features)
        )
        return ShapSupportedResponse(
            supported=True,
            baseValue=BASE_VALUE_KRW,
            prediction=(
                model_prediction.price_krw
                if model_prediction is not None
                else fallback_prediction
            ),
            currency="KRW",
            values=[
                ShapValue(
                    feature=contribution.feature,
                    labelKo=contribution.label_ko,
                    value=contribution.value,
                    shapValue=contribution.shap_value,
                    direction=self._direction(contribution.shap_value),
                )
                for contribution in contributions
            ],
            modelVersion=model_prediction.model_version if model_prediction else settings.model_version,
            baselineDate=self._baseline_date(request, settings),
            featureSetVersion=settings.feature_set_version,
        )

    def unsupported(
        self,
        request: ApartmentInferenceRequest,
        settings: Settings,
        missing_features: List[str],
    ) -> UnsupportedModelResponse:
        return UnsupportedModelResponse(
            supported=False,
            reason="INSUFFICIENT_DATA",
            missingFeatures=missing_features,
            modelVersion=settings.model_version,
            baselineDate=self._baseline_date(request, settings),
            featureSetVersion=settings.feature_set_version,
        )

    def missing_required_features(self, features: ApartmentFeatures) -> List[str]:
        return [
            feature_name
            for feature_name in REQUIRED_FEATURES
            if getattr(features, feature_name) is None
        ]

    def _feature_contributions(
        self,
        features: ApartmentFeatures,
        feature_row: dict[str, Any] | None = None,
    ) -> List[FeatureContribution]:
        row = feature_row or {}
        area = self._number(features.exclusiveAreaM2)
        floor = self._number(features.floor)
        legal_dong_delta = self._row_number(row, "legal_dong_target_log_price_delta") or 0
        district_delta = self._row_number(row, "district_target_log_price_delta") or 0
        age = self._age_value(features, row)
        complex_value, complex_shap = self._complex_scale_parking_contribution(features, row)
        transit_value, transit_shap = self._transit_access_contribution(features, row)
        park_value, park_shap = self._park_access_contribution(row)
        education_value, education_shap = self._education_access_contribution(row)

        return [
            FeatureContribution(
                feature="legalDongLocation",
                label_ko="\ubc95\uc815\ub3d9 \uc785\uc9c0",
                value=round(legal_dong_delta, 4),
                shap_value=self._money_delta(legal_dong_delta * 120_000_000, 180_000_000),
            ),
            FeatureContribution(
                feature="area",
                label_ko="\uba74\uc801",
                value=area,
                shap_value=self._money_delta((area - 59.0) * 2_800_000, 180_000_000),
            ),
            FeatureContribution(
                feature="complexScaleParking",
                label_ko="\ub2e8\uc9c0\uaddc\ubaa8/\uc8fc\ucc28",
                value=complex_value,
                shap_value=complex_shap,
            ),
            FeatureContribution(
                feature="age",
                label_ko="\ub178\ud6c4\ub3c4",
                value=age,
                shap_value=self._money_delta((20 - age) * 1_500_000, 80_000_000),
            ),
            FeatureContribution(
                feature="transitAccess",
                label_ko="\uad50\ud1b5 \uc811\uadfc\uc131",
                value=transit_value,
                shap_value=transit_shap,
            ),
            FeatureContribution(
                feature="parkAccess",
                label_ko="\uacf5\uc6d0 \uc811\uadfc\uc131",
                value=park_value,
                shap_value=park_shap,
            ),
            FeatureContribution(
                feature="educationAccess",
                label_ko="\uad50\uc721 \uc811\uadfc\uc131",
                value=education_value,
                shap_value=education_shap,
            ),
            FeatureContribution(
                feature="districtLocation",
                label_ko="\uc790\uce58\uad6c \uc785\uc9c0",
                value=round(district_delta, 4),
                shap_value=self._money_delta(district_delta * 100_000_000, 160_000_000),
            ),
            FeatureContribution(
                feature="floor",
                label_ko="\uce35\uc218",
                value=floor,
                shap_value=self._floor_contribution(features, row),
            ),
        ]

    def _fallback_contributions(
        self,
        features: ApartmentFeatures,
    ) -> List[FeatureContribution]:
        area = self._number(features.exclusiveAreaM2)
        floor = self._number(features.floor)
        built_year = self._number(features.builtYear)
        distance_to_station = self._number(features.distanceToStationM)

        return [
            FeatureContribution(
                feature="exclusiveAreaM2",
                label_ko="\uc804\uc6a9\uba74\uc801",
                value=area,
                shap_value=round(area * 3_000_000),
            ),
            FeatureContribution(
                feature="floor",
                label_ko="\uce35\uc218",
                value=floor,
                shap_value=round(floor * 800_000),
            ),
            FeatureContribution(
                feature="builtYear",
                label_ko="\uc900\uacf5\uc5f0\ub3c4",
                value=built_year,
                shap_value=round(max(built_year - 2000, 0) * 1_500_000),
            ),
            FeatureContribution(
                feature="distanceToStationM",
                label_ko="\uc5ed\uae4c\uc9c0 \uac70\ub9ac",
                value=distance_to_station,
                shap_value=round(distance_to_station * -10_000),
            ),
        ]

    def _prediction_from_contributions(
        self,
        contributions: List[FeatureContribution],
    ) -> int:
        return BASE_VALUE_KRW + sum(
            contribution.shap_value for contribution in contributions
        )

    def _baseline_date(
        self,
        request: ApartmentInferenceRequest,
        settings: Settings,
    ) -> str:
        return settings.model_baseline_date or request.asOfDate.isoformat()

    def _model_prediction(
        self,
        features: ApartmentFeatures,
        settings: Settings,
    ) -> ModelPrediction | None:
        repository = self.model_repository or get_valuation_model_repository(
            settings.valuation_artifacts_dir,
            settings.valuation_data_dir,
        )
        return repository.predict(features)

    def _direction(self, shap_value: int) -> str:
        if shap_value > 0:
            return "UP"
        if shap_value < 0:
            return "DOWN"
        return "NEUTRAL"

    def _age_value(
        self,
        features: ApartmentFeatures,
        feature_row: dict[str, Any],
    ) -> float:
        row_age = self._row_number(feature_row, "age")
        if row_age is not None:
            return row_age

        built_year = self._number(features.builtYear)
        deal_year = self._number(features.dealYear)
        if built_year and deal_year:
            return max(deal_year - built_year, 0)
        return 0

    def _complex_scale_parking_contribution(
        self,
        features: ApartmentFeatures,
        feature_row: dict[str, Any],
    ) -> tuple[float, int]:
        log_household_count = self._row_number(feature_row, "log_household_count")
        household_count = (
            max(math.expm1(log_household_count), 0)
            if log_household_count is not None
            else self._number(features.householdCount)
        )
        parking_per_household = self._row_number(feature_row, "parking_spaces_per_household")
        households_per_building = self._row_number(feature_row, "households_per_building")
        has_community_facilities = self._row_number(feature_row, "has_community_facilities")

        score = 0.0
        has_data = False
        if household_count > 0:
            has_data = True
            score += (
                min(math.log1p(household_count), math.log1p(2_500))
                - math.log1p(300)
            ) * 8
        if parking_per_household is not None:
            has_data = True
            score += (parking_per_household - 1.0) * 18
        if households_per_building is not None:
            has_data = True
            score += max(min((households_per_building - 60) / 100, 1), -0.5) * 8
        if has_community_facilities is not None:
            has_data = True
            score += 5 if has_community_facilities >= 1 else -3

        if not has_data:
            return 0, 0
        value = parking_per_household if parking_per_household is not None else household_count
        return round(value, 2), self._money_delta(score * 1_000_000, 80_000_000)

    def _transit_access_contribution(
        self,
        features: ApartmentFeatures,
        feature_row: dict[str, Any],
    ) -> tuple[float, int]:
        subway_distance = self._distance_from_log(
            feature_row,
            "log_nearest_subway_distance_m",
            fallback=features.distanceToStationM,
        )
        bus_distance = self._distance_from_log(feature_row, "log_nearest_bus_stop_distance_m")
        subway_count = self._count_bin_midpoint(feature_row.get("subway_count_radius_bin"))
        bus_count = self._count_bin_midpoint(feature_row.get("bus_stop_count_radius_bin"))

        has_data = any(
            value is not None
            for value in (subway_distance, bus_distance, subway_count, bus_count)
        )
        if not has_data:
            return 0, 0

        score = 0.0
        if subway_distance is not None:
            score += self._proximity_score(subway_distance, 1_000) * 0.65
        if bus_distance is not None:
            score += self._proximity_score(bus_distance, 500) * 0.25
        if subway_count is not None:
            score += min(subway_count, 10) * 2
        if bus_count is not None:
            score += min(bus_count, 20) * 0.8
        return round(score, 2), self._money_delta((score - 40) * 900_000, 90_000_000)

    def _park_access_contribution(
        self,
        feature_row: dict[str, Any],
    ) -> tuple[float, int]:
        park_distance = self._distance_from_log(feature_row, "log_nearest_park_distance_m")
        park_count = self._count_bin_midpoint(feature_row.get("park_count_radius_bin"))
        park_exists = self._row_number(feature_row, "park_exists")
        park_area_log = self._row_number(feature_row, "log_park_area_total_m2_radius")

        has_data = any(
            value is not None
            for value in (park_distance, park_count, park_exists, park_area_log)
        )
        if not has_data:
            return 0, 0

        score = 0.0
        if park_distance is not None:
            score += self._proximity_score(park_distance, 1_000) * 0.7
        if park_count is not None:
            score += min(park_count, 10) * 2
        if park_area_log is not None:
            score += min(park_area_log, 12) * 2
        if park_exists is not None:
            score += 5 if park_exists >= 1 else -5
        return round(score, 2), self._money_delta((score - 45) * 700_000, 60_000_000)

    def _education_access_contribution(
        self,
        feature_row: dict[str, Any],
    ) -> tuple[float, int]:
        school_count = self._count_bin_midpoint(feature_row.get("school_count_radius_bin"))
        academy_count = self._count_bin_midpoint(feature_row.get("academy_count_radius_bin"))
        elementary_distance = self._distance_from_log(
            feature_row,
            "log_nearest_elementary_school_distance_m",
        )
        middle_distance = self._distance_from_log(
            feature_row,
            "log_nearest_middle_school_distance_m",
        )

        has_data = any(
            value is not None
            for value in (school_count, academy_count, elementary_distance, middle_distance)
        )
        if not has_data:
            return 0, 0

        score = 0.0
        if school_count is not None:
            score += min(school_count, 10) * 4
        if academy_count is not None:
            score += min(academy_count, 20) * 1.5
        if elementary_distance is not None:
            score += self._proximity_score(elementary_distance, 1_000) * 0.25
        if middle_distance is not None:
            score += self._proximity_score(middle_distance, 1_500) * 0.15
        return round(score, 2), self._money_delta((score - 35) * 800_000, 70_000_000)

    def _floor_contribution(
        self,
        features: ApartmentFeatures,
        feature_row: dict[str, Any],
    ) -> int:
        floor = self._number(features.floor)
        relative_floor = self._row_number(feature_row, "kapt_relative_floor", "relative_floor")
        if relative_floor is not None:
            contribution = (relative_floor - 0.35) * 35_000_000
        else:
            contribution = floor * 800_000

        if floor <= 1:
            contribution -= 10_000_000
        elif floor <= 3:
            contribution -= 4_000_000
        return self._money_delta(contribution, 60_000_000)

    def _distance_from_log(
        self,
        feature_row: dict[str, Any],
        key: str,
        fallback: Any = None,
    ) -> float | None:
        log_distance = self._row_number(feature_row, key)
        if log_distance is not None:
            return max(math.expm1(log_distance), 0)
        return self._optional_number(fallback)

    def _proximity_score(self, distance: float, max_distance: float) -> float:
        return max(min((max_distance - distance) / max_distance, 1), 0) * 100

    def _count_bin_midpoint(self, value: Any) -> float | None:
        if value is None:
            return None
        return {
            "count_0": 0,
            "count_1_2": 1.5,
            "count_3_5": 4,
            "count_6_10": 8,
            "count_10_plus": 12,
            "count_11_20": 15.5,
            "count_21_plus": 25,
        }.get(str(value))

    def _row_number(
        self,
        feature_row: dict[str, Any],
        *keys: str,
    ) -> float | None:
        for key in keys:
            number = self._optional_number(feature_row.get(key))
            if number is not None:
                return number
        return None

    def _money_delta(self, value: float, limit: int) -> int:
        number = self._optional_number(value) or 0
        return round(max(min(number, limit), -limit))

    def _optional_number(self, value: Any) -> float | None:
        if value is None:
            return None
        try:
            number = float(value)
        except (TypeError, ValueError):
            return None
        if not math.isfinite(number):
            return None
        return number

    def _number(self, value: Optional[float]) -> float:
        if value is None:
            return 0
        return value
