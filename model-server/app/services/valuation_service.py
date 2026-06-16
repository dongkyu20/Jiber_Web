from dataclasses import dataclass
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


BASE_VALUE_KRW = 500_000_000
SKELETON_WARNING = "Phase 1 deterministic skeleton response; not a real valuation model."
REQUIRED_FEATURES = ("exclusiveAreaM2", "builtYear")


@dataclass(frozen=True)
class FeatureContribution:
    feature: str
    label_ko: str
    value: float
    shap_value: int


class ApartmentValuationService:
    def valuation(
        self,
        request: ApartmentInferenceRequest,
        settings: Settings,
    ) -> ValuationSupportedResponse:
        contributions = self._feature_contributions(request.features)
        estimated_price = self._prediction_from_contributions(contributions)
        interval_width = round(estimated_price * 0.08)

        return ValuationSupportedResponse(
            supported=True,
            estimatedPrice=estimated_price,
            currency="KRW",
            predictionInterval=PredictionInterval(
                lower=estimated_price - interval_width,
                upper=estimated_price + interval_width,
            ),
            modelVersion=settings.model_version,
            baselineDate=self._baseline_date(request, settings),
            featureSetVersion=settings.feature_set_version,
            warnings=[SKELETON_WARNING],
        )

    def shap(
        self,
        request: ApartmentInferenceRequest,
        settings: Settings,
    ) -> ShapSupportedResponse:
        contributions = self._feature_contributions(request.features)
        return ShapSupportedResponse(
            supported=True,
            baseValue=BASE_VALUE_KRW,
            prediction=self._prediction_from_contributions(contributions),
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
            modelVersion=settings.model_version,
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
    ) -> List[FeatureContribution]:
        area = self._number(features.exclusiveAreaM2)
        floor = self._number(features.floor)
        built_year = self._number(features.builtYear)
        distance_to_station = self._number(features.distanceToStationM)

        return [
            FeatureContribution(
                feature="exclusiveAreaM2",
                label_ko="전용면적",
                value=area,
                shap_value=round(area * 3_000_000),
            ),
            FeatureContribution(
                feature="floor",
                label_ko="층수",
                value=floor,
                shap_value=round(floor * 800_000),
            ),
            FeatureContribution(
                feature="builtYear",
                label_ko="준공연도",
                value=built_year,
                shap_value=round(max(built_year - 2000, 0) * 1_500_000),
            ),
            FeatureContribution(
                feature="distanceToStationM",
                label_ko="역까지 거리",
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

    def _direction(self, shap_value: int) -> str:
        if shap_value > 0:
            return "UP"
        if shap_value < 0:
            return "DOWN"
        return "NEUTRAL"

    def _number(self, value: Optional[float]) -> float:
        if value is None:
            return 0
        return value
