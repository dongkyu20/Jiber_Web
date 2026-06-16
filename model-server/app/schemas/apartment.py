from datetime import date
from typing import List, Literal, Optional, Union

from pydantic import BaseModel, Field


class ApartmentFeatures(BaseModel):
    sido: Optional[str] = None
    sigungu: Optional[str] = None
    legalDong: Optional[str] = None
    exclusiveAreaM2: Optional[float] = Field(default=None, ge=0)
    floor: Optional[int] = None
    builtYear: Optional[int] = None
    dealYear: Optional[int] = None
    dealMonth: Optional[int] = Field(default=None, ge=1, le=12)
    distanceToStationM: Optional[float] = Field(default=None, ge=0)


class ApartmentInferenceRequest(BaseModel):
    propertyId: int
    asOfDate: date
    features: ApartmentFeatures


class PredictionInterval(BaseModel):
    lower: int
    upper: int


class UnsupportedModelResponse(BaseModel):
    supported: Literal[False]
    reason: Literal["INSUFFICIENT_DATA"]
    missingFeatures: List[str]
    modelVersion: str
    baselineDate: str
    featureSetVersion: str


class ValuationSupportedResponse(BaseModel):
    supported: Literal[True]
    estimatedPrice: int
    currency: Literal["KRW"]
    predictionInterval: PredictionInterval
    modelVersion: str
    baselineDate: str
    featureSetVersion: str
    warnings: List[str]


class ShapValue(BaseModel):
    feature: str
    labelKo: str
    value: float
    shapValue: int
    direction: Literal["UP", "DOWN", "NEUTRAL"]


class ShapSupportedResponse(BaseModel):
    supported: Literal[True]
    baseValue: int
    prediction: int
    currency: Literal["KRW"]
    values: List[ShapValue]
    modelVersion: str
    baselineDate: str
    featureSetVersion: str


ValuationResponse = Union[ValuationSupportedResponse, UnsupportedModelResponse]
ShapResponse = Union[ShapSupportedResponse, UnsupportedModelResponse]
