import math
import pickle
import unicodedata
import zipfile
from pathlib import Path

import pytest

from app.schemas.apartment import ApartmentFeatures
from app.services.valuation_data_repository import LocalValuationDataRepository
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
    assert row["log_car_intercity_bus_terminal_minutes"] == pytest.approx(math.log1p(12.5))
    assert row["log_car_airport_minutes"] == pytest.approx(math.log1p(45.0))
    assert row["log_car_rail_station_minutes"] == pytest.approx(math.log1p(18.0))
    assert row["log_car_general_hospital_minutes"] == pytest.approx(math.log1p(9.0))
    assert row["log_transit_intercity_bus_terminal_minutes"] == pytest.approx(math.log1p(25.0))
    assert row["log_transit_airport_minutes"] == pytest.approx(math.log1p(60.0))
    assert row["log_transit_rail_station_minutes"] == pytest.approx(math.log1p(30.0))
    assert row["log_transit_general_hospital_minutes"] == pytest.approx(math.log1p(15.0))
    assert row["log_nearest_hospital_distance_m"] > 0
    assert row["log_nearest_pharmacy_distance_m"] > 0
    assert row["district_target_log_price_smooth"] == 20.4
    assert row["district_target_log_price_delta"] == pytest.approx(0.4)
    assert row["district_target_count_log1p"] == pytest.approx(math.log1p(400))
    assert row["legal_dong_target_log_price_smooth"] == 20.7
    assert row["legal_dong_target_log_price_delta"] == pytest.approx(0.7)
    assert row["legal_dong_target_count_log1p"] == pytest.approx(math.log1p(80))


def test_repository_uses_aggressive_kapt_matching_in_service_path(tmp_path: Path) -> None:
    repository = ValuationModelRepository(tmp_path / "artifacts", data_dir=tmp_path / "data")

    assert repository.data_repository.accept_remaining_matches is True


def test_repository_uses_user_top_floor_for_relative_floor_when_no_complex_match(tmp_path: Path) -> None:
    row = LocalValuationDataRepository(tmp_path / "data").features_for(
        ApartmentFeatures(
            sido=SEOUL,
            sigungu=JUNGRANG_GU,
            legalDong=SINNAE_DONG,
            propertyName="미래 신규아파트",
            exclusiveAreaM2=84.95,
            floor=15,
            topFloor=30,
            builtYear=2026,
            dealYear=2026,
            dealMonth=6,
        ),
        "seoul",
    )

    assert row["estimated_max_floor"] == 30
    assert row["max_floor_source"] == "user_input"
    assert row["floors_below_estimated_top"] == 15
    assert row["relative_floor"] == pytest.approx(0.5)
    assert row["relative_floor_bin"] == "relative_floor_25_50"


def test_repository_uses_national_subway_address_file_for_subway_features(tmp_path: Path) -> None:
    data_dir = tmp_path / "data"
    data_dir.mkdir()
    (data_dir / _nfd("국가철도공단_서울경기도_지하철_주소데이터_20250630.csv")).write_text(
        "\n".join(
            [
                "철도운영기관명,선명,역명,지번주소,도로명주소",
                f"코레일,테스트선,신내,{SEOUL} {JUNGRANG_GU} {SINNAE_DONG} 1,{SEOUL} {JUNGRANG_GU} 신내역로 1",
            ]
        ),
        encoding="cp949",
    )
    (data_dir / _nfd("국가철도공단_철도역 정보_20250711.csv")).write_text(
        "\n".join(
            [
                "주소,위도좌표,역등급,관련노선,열차정차횟수,역이름,소속지사,역연혁,경도좌표",
                f"{SEOUL} {JUNGRANG_GU} 신내역로 1,37.6154,1,테스트선,전동열차 정차,신내역,서울본부,,127.1104",
            ]
        ),
        encoding="cp949",
    )

    row = LocalValuationDataRepository(data_dir).features_for(
        ApartmentFeatures(
            sido=SEOUL,
            sigungu=JUNGRANG_GU,
            legalDong=SINNAE_DONG,
            propertyName=SINNAE_COMPLEX,
            latitude=37.6151579,
            longitude=127.1101263,
            exclusiveAreaM2=84.95,
            floor=15,
            builtYear=2010,
            dealYear=2026,
            dealMonth=6,
        ),
        "seoul",
    )

    assert row["nearest_subway_distance_m_missing"] == 0
    assert row["subway_count_radius_bin"] == "count_1_2"
    assert row["log_nearest_subway_distance_m"] > 0


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
    (data_dir / "subway_station_locations.csv").write_text(
        "\n".join(
            [
                "city_code,station_name,line_name,latitude,longitude",
                "seoul,\uc2e0\ub0b4\uc5ed,\uacbd\ucd98\uc120,37.6154,127.1104",
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
    _write_access_time_xlsx(data_dir / _nfd("01_\ud3c9\uade0\uc811\uadfc\uc2dc\uac04_2023.xlsx"))
    (data_dir / "\uac74\uac15_\ubcd1\uc6d0_\uc11c\uc6b8\ud2b9\ubcc4\uc2dc.csv").write_text(
        "\n".join(
            [
                "\uad00\ub9ac\ubc88\ud638,\uc0ac\uc5c5\uc7a5\uba85,\uc601\uc5c5\uc0c1\ud0dc\uba85,\uc0c1\uc138\uc601\uc5c5\uc0c1\ud0dc\uba85,\ub3c4\ub85c\uba85\uc8fc\uc18c,\uc88c\ud45c\uc815\ubcf4(X),\uc88c\ud45c\uc815\ubcf4(Y)",
                f"H1,\uc2e0\ub0b4\ubcd1\uc6d0,\uc601\uc5c5/\uc815\uc0c1,\uc601\uc5c5\uc911,{SEOUL} {JUNGRANG_GU} {SINNAE_DONG},127.1107,37.6157",
            ]
        ),
        encoding="cp949",
    )
    (data_dir / "\uac74\uac15_\uc758\uc6d0_\uc11c\uc6b8\ud2b9\ubcc4\uc2dc.csv").write_text(
        "\n".join(
            [
                "\uad00\ub9ac\ubc88\ud638,\uc0ac\uc5c5\uc7a5\uba85,\uc601\uc5c5\uc0c1\ud0dc\uba85,\uc0c1\uc138\uc601\uc5c5\uc0c1\ud0dc\uba85,\ub3c4\ub85c\uba85\uc8fc\uc18c,\uc88c\ud45c\uc815\ubcf4(X),\uc88c\ud45c\uc815\ubcf4(Y)",
                f"C1,\uc2e0\ub0b4\uc758\uc6d0,\ud3d0\uc5c5,\ud3d0\uc5c5,{SEOUL} {JUNGRANG_GU} {SINNAE_DONG},127.1101,37.6151",
            ]
        ),
        encoding="cp949",
    )
    (data_dir / "\uac74\uac15_\uc57d\uad6d_\uc11c\uc6b8\ud2b9\ubcc4\uc2dc.csv").write_text(
        "\n".join(
            [
                "\uad00\ub9ac\ubc88\ud638,\uc0ac\uc5c5\uc7a5\uba85,\uc601\uc5c5\uc0c1\ud0dc\uba85,\uc0c1\uc138\uc601\uc5c5\uc0c1\ud0dc\uba85,\ub3c4\ub85c\uba85\uc8fc\uc18c,\uc88c\ud45c\uc815\ubcf4(X),\uc88c\ud45c\uc815\ubcf4(Y)",
                f"P1,\uc2e0\ub0b4\uc57d\uad6d,\uc601\uc5c5/\uc815\uc0c1,\uc601\uc5c5\uc911,{SEOUL} {JUNGRANG_GU} {SINNAE_DONG},127.1108,37.6158",
            ]
        ),
        encoding="cp949",
    )


def _nfd(value: str) -> str:
    return unicodedata.normalize("NFD", value)


def _write_access_time_xlsx(path: Path) -> None:
    headers = [
        "Year",
        "HDCD",
        "Region",
        "HDCD_Lev",
        "Faci_CD",
        "Time_Zone",
        "Mode",
        "HDCD_SD_NM",
        "HDCD_SGG_NM",
        "HDCD_EMD_NM",
        "Region_NM",
        "Faci_CA",
        "Faci_NM",
        "Time_Zone_NM",
        "Mode_NM",
        "\ud3c9\uade0\uc811\uadfc\uc2dc\uac04(\ubd84)",
    ]
    metric_rows = [
        ("\ubc84\uc2a4\ud130\ubbf8\ub110", "\uc2b9\uc6a9\ucc28", 12.5),
        ("\uacf5\ud56d", "\uc2b9\uc6a9\ucc28", 45.0),
        ("\ucca0\ub3c4\uc5ed", "\uc2b9\uc6a9\ucc28", 18.0),
        ("\uc885\ud569\ubcd1\uc6d0", "\uc2b9\uc6a9\ucc28", 9.0),
        ("\ubc84\uc2a4\ud130\ubbf8\ub110", "\ub300\uc911\uad50\ud1b5/\ub3c4\ubcf4", 25.0),
        ("\uacf5\ud56d", "\ub300\uc911\uad50\ud1b5/\ub3c4\ubcf4", 60.0),
        ("\ucca0\ub3c4\uc5ed", "\ub300\uc911\uad50\ud1b5/\ub3c4\ubcf4", 30.0),
        ("\uc885\ud569\ubcd1\uc6d0", "\ub300\uc911\uad50\ud1b5/\ub3c4\ubcf4", 15.0),
    ]
    rows = [
        ["2023\ub144 \uae30\uc900 \uad50\ud1b5\uc811\uadfc\uc131\uc9c0\ud45c"],
        [],
        [],
        [],
        headers,
    ]
    for facility, mode, minutes in metric_rows:
        rows.append(
            [
                2023,
                "11260106",
                "11260106",
                4,
                "99",
                "0_AllDay",
                "1",
                SEOUL,
                JUNGRANG_GU,
                SINNAE_DONG,
                SINNAE_DONG,
                "\ud14c\uc2a4\ud2b8",
                facility,
                "\uc77c\ud3c9\uade0(06-20\uc2dc)",
                mode,
                minutes,
            ]
        )
    sheet_xml = _sheet_xml(rows)
    with zipfile.ZipFile(path, "w") as archive:
        archive.writestr(
            "[Content_Types].xml",
            """<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""",
        )
        archive.writestr(
            "_rels/.rels",
            """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""",
        )
        archive.writestr(
            "xl/workbook.xml",
            """<?xml version="1.0" encoding="UTF-8"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="\ud3c9\uade0\uc811\uadfc\uc2dc\uac04" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""",
        )
        archive.writestr(
            "xl/_rels/workbook.xml.rels",
            """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>""",
        )
        archive.writestr("xl/worksheets/sheet1.xml", sheet_xml)


def _sheet_xml(rows: list[list[object]]) -> str:
    row_xml = []
    for row_index, row in enumerate(rows, start=1):
        cell_xml = []
        for column_index, value in enumerate(row, start=1):
            if value is None or value == "":
                continue
            ref = f"{_column_name(column_index)}{row_index}"
            if isinstance(value, (int, float)):
                cell_xml.append(f'<c r="{ref}"><v>{value}</v></c>')
            else:
                escaped = str(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                cell_xml.append(f'<c r="{ref}" t="inlineStr"><is><t>{escaped}</t></is></c>')
        row_xml.append(f'<row r="{row_index}">{"".join(cell_xml)}</row>')
    return (
        '<?xml version="1.0" encoding="UTF-8"?>'
        '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">'
        f'<sheetData>{"".join(row_xml)}</sheetData>'
        '</worksheet>'
    )


def _column_name(index: int) -> str:
    letters = ""
    while index:
        index, remainder = divmod(index - 1, 26)
        letters = chr(65 + remainder) + letters
    return letters
