import math
import pickle
import unicodedata
from pathlib import Path

import pytest

from app.schemas.apartment import ApartmentFeatures
from app.services.valuation_service import ValuationModelRepository


JUNGRANG_GU = "\uc911\ub791\uad6c"
SINNAE_DONG = "\uc2e0\ub0b4\ub3d9"
SINNAE_COMPLEX = "\uc2e0\ub0b4 \ub370\uc2dc\uc559\ud3ec\ub808"
SEOUL = "\uc11c\uc6b8\ud2b9\ubcc4\uc2dc"


class FakeTargetEncodings:
    global_mean = 20.0
    values = {
        "district": {JUNGRANG_GU: 20.4},
        "legal_dong": {SINNAE_DONG: 20.7},
    }
    counts = {
        "district": {JUNGRANG_GU: 400},
        "legal_dong": {SINNAE_DONG: 80},
    }


class FakePredictModel:
    target_encodings = FakeTargetEncodings()

    def __init__(self) -> None:
        self.rows: list[dict] = []

    def predict(self, rows: list[dict]) -> list[float]:
        self.rows.extend(rows)
        return [math.log(800_000_000) for _ in rows]


def test_repository_enriches_model_row_from_data_folder(tmp_path: Path) -> None:
    artifacts_dir = tmp_path / "artifacts"
    data_dir = tmp_path / "data"
    model = FakePredictModel()
    _write_artifact(artifacts_dir, model)
    _write_data_files(data_dir)
    repository = ValuationModelRepository(artifacts_dir, data_dir=data_dir)

    repository.predict(
        ApartmentFeatures(
            sido=SEOUL,
            sigungu=JUNGRANG_GU,
            legalDong=SINNAE_DONG,
            propertyName=SINNAE_COMPLEX,
            latitude=37.6151579,
            longitude=127.1101263,
            householdCount=1000,
            exclusiveAreaM2=84.95,
            floor=15,
            builtYear=2010,
            dealYear=2026,
            dealMonth=6,
            distanceToStationM=420,
        )
    )

    row = repository._models["seoul"].rows[0]
    assert row["estimated_max_floor"] == 28
    assert row["floors_below_estimated_top"] == 13
    assert row["floors_below_estimated_top_bin"] == "below_top_11_plus"
    assert row["relative_floor"] == pytest.approx(15 / 28)
    assert row["relative_floor_bin"] == "relative_floor_50_75"
    assert row["log_household_count"] == pytest.approx(math.log1p(1000))
    assert row["academy_count_radius_bin"] == "count_11_20"
    assert row["bus_stop_count_radius_bin"] == "count_1_2"
    assert row["school_count_radius_bin"] == "count_1_2"
    assert row["subway_count_radius_bin"] == "count_1_2"
    assert row["park_count_radius_bin"] == "count_1_2"
    assert row["log_nearest_bus_stop_distance_m"] > 0
    assert row["log_nearest_elementary_school_distance_m"] > 0
    assert row["log_nearest_park_distance_m"] > 0
    assert row["log_park_area_total_m2_radius"] == pytest.approx(math.log1p(12_000))
    assert row["park_exists"] == 1
    assert row["district_target_log_price_smooth"] == 20.4
    assert row["district_target_log_price_delta"] == pytest.approx(0.4)
    assert row["district_target_count_log1p"] == pytest.approx(math.log1p(400))
    assert row["legal_dong_target_log_price_smooth"] == 20.7
    assert row["legal_dong_target_log_price_delta"] == pytest.approx(0.7)
    assert row["legal_dong_target_count_log1p"] == pytest.approx(math.log1p(80))


def _write_artifact(root: Path, model: FakePredictModel) -> None:
    artifact_dir = root / "seoul-run"
    artifact_dir.mkdir(parents=True)
    (artifact_dir / "run_manifest.json").write_text(
        '{"city_code": "seoul", "run_id": "seoul-run", "artifact_paths": {"model": "model.pkl"}}',
        encoding="utf-8",
    )
    with (artifact_dir / "model.pkl").open("wb") as file:
        pickle.dump(model, file)


def _write_data_files(data_dir: Path) -> None:
    data_dir.mkdir()
    (data_dir / "seoul_busan_complex_floor_stats_201007_202606_merged.csv").write_text(
        "\n".join(
            [
                "city_code,property_type,district,lawd_cd,legal_dong,building_name,observed_max_floor,estimated_max_floor,observation_count,first_observed_yyyymm,last_observed_yyyymm,min_build_year,max_build_year,confidence",
                f"seoul,apartment,{JUNGRANG_GU},11260,{SINNAE_DONG},{SINNAE_COMPLEX},28,28,32,202401,202606,2010,2010,high",
            ]
        ),
        encoding="utf-8-sig",
    )
    (data_dir / _nfd("\uc544\ud30c\ud2b8\ub2e8\uc9c0\uc778\uadfc\ud559\uc6d0\uad50\uc2b5\uc18c2604_csv.csv")).write_text(
        "\n".join(
            [
                'HSMP_INNB|"PNU"|"RN_MNNMB"|"SIGNGU_CD"|"LNNO_ADRES"|"POTVALE_IFRA_HSMP_NM"|"HSMP_KIND_CD"|"DONG_CNT"|"NMHSH"|"USE_APRV_YMD"|"SSIZE_INSTUT_CNT"|"MSIZE_INSTUT_CNT"|"LGZ_INSTUT_CNT"|"GNRLZ_INSTUT_CNT"|"ETEX_INSTUT_CNT"|"FGGG_INSTUT_CNT"|"AAMAPE_INSTUT_CNT"|"READRM_CNT"|"INFO_INSTUT_CNT"|"SPCEDU_INSTUT_CNT"|"VCSK_INSTUT_CNT"|"ETC_INSTUT_CNT"',
                f'1|"1126010600"|"11260"|"11260"|"{SEOUL} {JUNGRANG_GU} {SINNAE_DONG} 817"|"{SINNAE_COMPLEX}"|"1"|"6"|"1000"|"20100101"|5|2|1|1|0|0|2|0|0|0|0|0',
            ]
        ),
        encoding="utf-8-sig",
    )
    (data_dir / _nfd("\uad6d\ud1a0\uad50\ud1b5\ubd80_\uc804\uad6d \ubc84\uc2a4\uc815\ub958\uc7a5 \uc704\uce58\uc815\ubcf4_20251031.csv")).write_text(
        "\n".join(
            [
                "\uc815\ub958\uc7a5\uc544\uc774\ub514,\uc815\ub958\uc7a5\uba85,\uc704\ub3c4,\uacbd\ub3c4,\ub370\uc774\ud130\uae30\uc900\uc77c\uc790,\uc815\ub958\uc7a5\ubc88\ud638,\ub3c4\uc2dc\ucf54\ub4dc,\ub3c4\uc2dc\uba85,\uad00\ub9ac\ub3c4\uc2dc\uba85",
                f"BUS1,\uc2e0\ub0b4\uc5ed,37.6153,127.1103,2026-01-01,1,11260,{SEOUL} {JUNGRANG_GU},\uc11c\uc6b8",
            ]
        ),
        encoding="utf-8-sig",
    )
    (data_dir / _nfd("\uad6d\uac00\ucca0\ub3c4\uacf5\ub2e8_\ucca0\ub3c4\uc5ed \uc815\ubcf4_20250711.csv")).write_text(
        "\n".join(
            [
                "\uc8fc\uc18c,\uc704\ub3c4\uc88c\ud45c,\uc5ed\ub4f1\uae09,\uad00\ub828\ub178\uc120,\uc5f4\ucc28\uc815\ucc28\ud69f\uc218,\uc5ed\uc774\ub984,\uc18c\uc18d\uc9c0\uc0ac,\uc5ed\uc5f0\ud601,\uacbd\ub3c4\uc88c\ud45c",
                f"{SEOUL} {JUNGRANG_GU} {SINNAE_DONG},37.6154,1,\uacbd\ucd98\uc120,10,\uc2e0\ub0b4\uc5ed,\uc11c\uc6b8\ubcf8\ubd80,,127.1104",
            ]
        ),
        encoding="utf-8-sig",
    )
    (data_dir / _nfd("\uc804\uad6d\ub3c4\uc2dc\uacf5\uc6d0\uc815\ubcf4\ud45c\uc900\ub370\uc774\ud130-20260609.xls")).write_text(
        "\n".join(
            [
                "\uad00\ub9ac\ubc88\ud638,\uacf5\uc6d0\uba85,\uacf5\uc6d0\uad6c\ubd84,\uc18c\uc7ac\uc9c0\uc9c0\ubc88\uc8fc\uc18c,\uc18c\uc7ac\uc9c0\ub3c4\ub85c\uba85\uc8fc\uc18c,\uacf5\uc6d0\uba74\uc801,\uc704\ub3c4,\uacbd\ub3c4,\ub370\uc774\ud130\uae30\uc900\uc77c\uc790",
                f"P1,\uc2e0\ub0b4\uadfc\ub9b0\uacf5\uc6d0,\uadfc\ub9b0\uacf5\uc6d0,{SEOUL} {JUNGRANG_GU} {SINNAE_DONG},,12000,37.6155,127.1105,2026-06-09",
            ]
        ),
        encoding="utf-8-sig",
    )
    (data_dir / _nfd("\ud55c\uad6d\uad50\uc721\uc2dc\uc124\uc548\uc804\uc6d0_\ucd08\uc911\ub4f1\ud559\uad50\uc704\uce58_20260320.csv")).write_text(
        "\n".join(
            [
                "\ud559\uad50ID,\ud559\uad50\uba85,\ud559\uad50\uae09\uad6c\ubd84,\uc124\ub9bd\uc77c\uc790,\uc124\ub9bd\ud615\ud0dc,\ubcf8\uad50\ubd84\uad50\uad6c\ubd84,\uc6b4\uc601\uc0c1\ud0dc,\uc18c\uc7ac\uc9c0\uc9c0\ubc88\uc8fc\uc18c,\uc18c\uc7ac\uc9c0\ub3c4\ub85c\uba85\uc8fc\uc18c,\uc2dc\ub3c4\uad50\uc721\uccad\ucf54\ub4dc,\uc2dc\ub3c4\uad50\uc721\uccad\uba85,\uad50\uc721\uc9c0\uc6d0\uccad\ucf54\ub4dc,\uad50\uc721\uc9c0\uc6d0\uccad\uba85,\uc0dd\uc131\uc77c\uc790,\ubcc0\uacbd\uc77c\uc790,\uc704\ub3c4,\uacbd\ub3c4,\ub370\uc774\ud130\uae30\uc900\uc77c\uc790",
                f"S1,\uc2e0\ub0b4\ucd08\ub4f1\ud559\uad50,\ucd08\ub4f1\ud559\uad50,2000-01-01,\uacf5\ub9bd,\ubcf8\uad50,\uc6b4\uc601,{SEOUL} {JUNGRANG_GU} {SINNAE_DONG},{SEOUL} {JUNGRANG_GU} {SINNAE_DONG},1,\uc11c\uc6b8,1,\ub3d9\ubd80,2026-01-01,2026-01-01,37.6152,127.1102,2026-03-20",
            ]
        ),
        encoding="utf-8-sig",
    )


def _nfd(value: str) -> str:
    return unicodedata.normalize("NFD", value)
