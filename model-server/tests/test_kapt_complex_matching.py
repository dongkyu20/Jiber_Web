from pathlib import Path

import pytest

from app.schemas.apartment import ApartmentFeatures
from app.services.valuation_data_repository import LocalValuationDataRepository


SEOUL = "\uc11c\uc6b8\ud2b9\ubcc4\uc2dc"
BUSAN = "\ubd80\uc0b0\uad11\uc5ed\uc2dc"


def test_kapt_match_filters_allowed_complex_types_and_exact_matches_by_dong() -> None:
    repository = _repository(
        [
            _kapt_record(
                city_code="seoul",
                district="\uc885\ub85c\uad6c",
                legal_dong="\ud3c9\ub3d9",
                name="\uacbd\ud76c\uad81\uc790\uc7743\ub2e8\uc9c0",
                complex_type="\uc544\ud30c\ud2b8",
                household_count=589,
                source_complex_code="A002",
            ),
            _kapt_record(
                city_code="seoul",
                district="\uc885\ub85c\uad6c",
                legal_dong="\ud3c9\ub3d9",
                name="\uacbd\ud76c\uad81 \uc790\uc774(3\ub2e8\uc9c0)",
                complex_type="\uc5f0\ub9bd\uc8fc\ud0dd",
                household_count=99,
                source_complex_code="A001",
            ),
        ]
    )

    result = repository.match_kapt(
        ApartmentFeatures(
            sido=SEOUL,
            sigungu="\uc885\ub85c\uad6c",
            legalDong="\ud3c9\ub3d9",
            propertyName="\uacbd\ud76c\uad81 \uc790\uc774(3\ub2e8\uc9c0)",
        ),
        "seoul",
    )

    assert result.kind == "dong"
    assert result.score == pytest.approx(1.0)
    assert result.record is not None
    assert result.record["household_count"] == 589
    assert result.record["match_kind"] == "dong"
    assert result.record["match_score"] == pytest.approx(1.0)


def test_kapt_match_uses_last_ri_as_additional_legal_dong_key() -> None:
    repository = _repository(
        [
            _kapt_record(
                city_code="busan",
                district="\uae30\uc7a5\uad70",
                legal_dong="\ubaa8\uc804\ub9ac",
                name="\uc815\uad00\ub3d9\uc77c\uc2a4\uc704\ud2b83\ucc28",
                household_count=1500,
            )
        ]
    )

    result = repository.match_kapt(
        ApartmentFeatures(
            sido=BUSAN,
            sigungu="\uae30\uc7a5\uad70",
            legalDong="\uc815\uad00\uc74d \ubaa8\uc804\ub9ac",
            propertyName="\uc815\uad00 \ub3d9\uc77c\uc2a4\uc704\ud2b8 3\ucc28",
        ),
        "busan",
    )

    assert result.kind == "dong"
    assert result.record is not None
    assert result.record["household_count"] == 1500


def test_kapt_match_exact_name_without_dong_is_district_unique_or_ambiguous() -> None:
    unique_repository = _repository(
        [
            _kapt_record(
                city_code="seoul",
                district="\uc1a1\ud30c\uad6c",
                legal_dong="\uc7a0\uc2e4\ub3d9",
                name="\ud5ec\ub9ac\uc624\uc2dc\ud2f0",
                household_count=9510,
            )
        ]
    )

    unique_result = unique_repository.match_kapt(
        ApartmentFeatures(
            sido=SEOUL,
            sigungu="\uc1a1\ud30c\uad6c",
            legalDong="\uac00\ub77d\ub3d9",
            propertyName="\ud5ec\ub9ac\uc624\uc2dc\ud2f0",
        ),
        "seoul",
    )

    assert unique_result.kind == "district_unique"
    assert unique_result.record is not None
    assert unique_result.record["household_count"] == 9510

    ambiguous_repository = _repository(
        [
            _kapt_record(
                city_code="seoul",
                district="\uc1a1\ud30c\uad6c",
                legal_dong="\uc7a0\uc2e4\ub3d9",
                name="\uac19\uc740\ub2e8\uc9c0",
                household_count=100,
                source_complex_code="A002",
            ),
            _kapt_record(
                city_code="seoul",
                district="\uc1a1\ud30c\uad6c",
                legal_dong="\ubb38\uc815\ub3d9",
                name="\uac19\uc740\ub2e8\uc9c0",
                household_count=200,
                source_complex_code="A001",
            ),
        ]
    )

    ambiguous_result = ambiguous_repository.match_kapt(
        ApartmentFeatures(
            sido=SEOUL,
            sigungu="\uc1a1\ud30c\uad6c",
            legalDong="\uac00\ub77d\ub3d9",
            propertyName="\uac19\uc740\ub2e8\uc9c0",
        ),
        "seoul",
    )

    assert ambiguous_result.kind == "ambiguous_district"
    assert ambiguous_result.record is None
    assert ambiguous_result.score == pytest.approx(1.0)


def test_kapt_match_uses_sequence_matcher_for_likely_and_possible_name_variants() -> None:
    likely_repository = _repository(
        [
            _kapt_record(
                city_code="seoul",
                district="\uc1a1\ud30c\uad6c",
                legal_dong="\uac00\ub77d\ub3d9",
                name="\ud5ec\ub9ac\uc624\uc2dc\ud2f0\uc544\ud30c\ud2b8",
                household_count=9510,
            )
        ]
    )

    likely_result = likely_repository.match_kapt(
        ApartmentFeatures(
            sido=SEOUL,
            sigungu="\uc1a1\ud30c\uad6c",
            legalDong="\uac00\ub77d\ub3d9",
            propertyName="\ud5ec\ub9ac\uc624\uc2dc\ud2f0",
        ),
        "seoul",
    )

    assert likely_result.kind == "likely_name_variant"
    assert likely_result.score >= 0.9
    assert likely_result.record is not None

    possible_repository = _repository(
        [
            _kapt_record(
                city_code="seoul",
                district="\uac15\ub0a8\uad6c",
                legal_dong="\uc5ed\uc0bc\ub3d9",
                name="\ub798\ubbf8\uc548\uadf8\ub808\uc774\uc2a4",
                household_count=464,
            )
        ]
    )

    possible_result = possible_repository.match_kapt(
        ApartmentFeatures(
            sido=SEOUL,
            sigungu="\uac15\ub0a8\uad6c",
            legalDong="\uc5ed\uc0bc\ub3d9",
            propertyName="\ub798\ubbf8\uc548\uadf8\ub808\uc774\ud2bc",
        ),
        "seoul",
    )

    assert possible_result.kind == "possible_name_variant"
    assert 0.72 <= possible_result.score < 0.9
    assert possible_result.record is not None


def test_kapt_match_keeps_ambiguous_name_variant_unselected_by_default() -> None:
    repository = _repository(
        [
            _kapt_record(
                city_code="seoul",
                district="\uac15\ub0a8\uad6c",
                legal_dong="\uc5ed\uc0bc\ub3d9",
                name="\ub798\ubbf8\uc548\uadf8\ub808\uc774\uc2a4",
                household_count=464,
                source_complex_code="B002",
            ),
            _kapt_record(
                city_code="seoul",
                district="\uac15\ub0a8\uad6c",
                legal_dong="\uc5ed\uc0bc\ub3d9",
                name="\ub798\ubbf8\uc548\uadf8\ub808\uc774\ub4dc",
                household_count=476,
                source_complex_code="B001",
            ),
        ]
    )

    result = repository.match_kapt(
        ApartmentFeatures(
            sido=SEOUL,
            sigungu="\uac15\ub0a8\uad6c",
            legalDong="\uc5ed\uc0bc\ub3d9",
            propertyName="\ub798\ubbf8\uc548\uadf8\ub808\uc774\ud2bc",
        ),
        "seoul",
    )

    assert result.kind == "ambiguous_name_variant"
    assert result.record is None
    assert result.score == pytest.approx(0.8571428571428571)


def test_kapt_match_accept_remaining_matches_marks_manual_review_statuses() -> None:
    ambiguous_repository = _repository(
        [
            _kapt_record(
                city_code="seoul",
                district="\uac15\ub0a8\uad6c",
                legal_dong="\uc5ed\uc0bc\ub3d9",
                name="\ub798\ubbf8\uc548\uadf8\ub808\uc774\uc2a4",
                household_count=464,
                source_complex_code="B002",
            ),
            _kapt_record(
                city_code="seoul",
                district="\uac15\ub0a8\uad6c",
                legal_dong="\uc5ed\uc0bc\ub3d9",
                name="\ub798\ubbf8\uc548\uadf8\ub808\uc774\ub4dc",
                household_count=476,
                source_complex_code="B001",
            ),
        ],
        accept_remaining_matches=True,
    )

    ambiguous_result = ambiguous_repository.match_kapt(
        ApartmentFeatures(
            sido=SEOUL,
            sigungu="\uac15\ub0a8\uad6c",
            legalDong="\uc5ed\uc0bc\ub3d9",
            propertyName="\ub798\ubbf8\uc548\uadf8\ub808\uc774\ud2bc",
        ),
        "seoul",
    )

    assert ambiguous_result.kind == "ambiguous_name_variant"
    assert ambiguous_result.record is not None
    assert ambiguous_result.record["source_complex_code"] == "B001"
    assert ambiguous_result.record["needs_manual_review"] is True

    remaining_repository = _repository(
        [
            _kapt_record(
                city_code="seoul",
                district="\uc1a1\ud30c\uad6c",
                legal_dong="\uc7a0\uc2e4\ub3d9",
                name="\uc7a0\uc2e4\ub9ac\uc13c\uce20",
                household_count=5563,
                source_complex_code="C001",
            )
        ],
        accept_remaining_matches=True,
    )

    remaining_result = remaining_repository.match_kapt(
        ApartmentFeatures(
            sido=SEOUL,
            sigungu="\uc1a1\ud30c\uad6c",
            legalDong="\uc7a0\uc2e4\ub3d9",
            propertyName="\uc11c\uba74\ub354\uc0f5\uc13c\ud2b8\ub7f4\uc2a4\ud0c0",
        ),
        "seoul",
    )

    assert remaining_result.kind == "counterpart_has_dong_but_name_absent"
    assert remaining_result.score < 0.72
    assert remaining_result.record is not None
    assert remaining_result.record["needs_manual_review"] is True


def test_kapt_match_metadata_does_not_become_model_features() -> None:
    repository = _repository(
        [
            _kapt_record(
                city_code="seoul",
                district="\uc1a1\ud30c\uad6c",
                legal_dong="\uac00\ub77d\ub3d9",
                name="\ud5ec\ub9ac\uc624\uc2dc\ud2f0\uc544\ud30c\ud2b8",
                household_count=9510,
                building_count=84,
                total_parking_spaces=12000,
                kapt_max_floor=35,
                has_community_facilities=1,
            )
        ]
    )

    row = repository.features_for(
        ApartmentFeatures(
            sido=SEOUL,
            sigungu="\uc1a1\ud30c\uad6c",
            legalDong="\uac00\ub77d\ub3d9",
            propertyName="\ud5ec\ub9ac\uc624\uc2dc\ud2f0",
            floor=15,
        ),
        "seoul",
    )

    assert row["log_household_count"] == pytest.approx(9.160204602)
    assert row["households_per_building"] == pytest.approx(9510 / 84)
    assert row["parking_spaces_per_household"] == pytest.approx(12000 / 9510)
    assert "match_kind" not in row
    assert "match_score" not in row
    assert "source_complex_code" not in row


def _repository(
    records: list[dict],
    *,
    accept_remaining_matches: bool = False,
) -> LocalValuationDataRepository:
    repository = LocalValuationDataRepository(
        Path("/does/not/matter"),
        accept_remaining_matches=accept_remaining_matches,
    )
    repository._kapt_records = records
    return repository


def _kapt_record(
    *,
    city_code: str,
    district: str,
    legal_dong: str,
    name: str,
    complex_type: str = "\uc544\ud30c\ud2b8",
    source_complex_code: str = "K001",
    household_count: int | None = None,
    building_count: int | None = None,
    total_parking_spaces: int | None = None,
    kapt_max_floor: int | None = None,
    has_community_facilities: int | None = None,
) -> dict:
    return {
        "city_code": city_code,
        "district": district,
        "legal_dong": legal_dong,
        "address": f"{SEOUL if city_code == 'seoul' else BUSAN} {district} {legal_dong}",
        "name": name,
        "complex_type": complex_type,
        "source_complex_code": source_complex_code,
        "normalized_name": "",
        "normalized_name_variant": "",
        "household_count": household_count,
        "building_count": building_count,
        "total_parking_spaces": total_parking_spaces,
        "kapt_max_floor": kapt_max_floor,
        "has_community_facilities": has_community_facilities,
    }
