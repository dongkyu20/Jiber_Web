import csv
import math
import re
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from xml.etree import ElementTree

from app.schemas.apartment import ApartmentFeatures


FACILITY_RADIUS_M = 1000.0
EARTH_RADIUS_M = 6_371_000.0
CSV_ENCODINGS = ("utf-8-sig", "cp949", "euc-kr", "utf-8")


@dataclass(frozen=True)
class GeoPoint:
    latitude: float
    longitude: float
    kind: str = ""


class LocalValuationDataRepository:
    def __init__(self, data_dir: str | Path) -> None:
        self.data_dir = Path(data_dir)
        self._complex_floor_records: list[dict[str, Any]] | None = None
        self._academy_records: list[dict[str, Any]] | None = None
        self._kapt_records: list[dict[str, Any]] | None = None
        self._bus_points: list[GeoPoint] | None = None
        self._rail_points: list[GeoPoint] | None = None
        self._school_points: list[GeoPoint] | None = None

    def features_for(self, features: ApartmentFeatures, city_code: str) -> dict[str, Any]:
        row: dict[str, Any] = {}
        floor = _integer(features.floor)

        complex_record = self._find_complex_floor(features, city_code)
        kapt_record = self._find_kapt(features, city_code)
        academy_record = self._find_academy(features, city_code)

        estimated_max_floor = _first_int(
            kapt_record.get("kapt_max_floor") if kapt_record else None,
            complex_record.get("estimated_max_floor") if complex_record else None,
        )
        if estimated_max_floor is not None:
            row["estimated_max_floor"] = estimated_max_floor
            self._add_top_floor_features(row, floor, estimated_max_floor, "estimated")

        kapt_max_floor = _integer(kapt_record.get("kapt_max_floor")) if kapt_record else None
        if kapt_max_floor is not None:
            row["kapt_max_floor"] = kapt_max_floor
            row["kapt_max_floor_missing"] = 0
            self._add_top_floor_features(row, floor, kapt_max_floor, "kapt")
        else:
            row["kapt_max_floor_missing"] = 1
            row["floors_below_kapt_top_bin"] = "missing"
            row["kapt_relative_floor_bin"] = "missing"

        observation_count = _integer(complex_record.get("observation_count")) if complex_record else None
        if observation_count is not None:
            row["recent_transaction_count_bin"] = _count_bin(observation_count)

        household_count = _first_int(
            features.householdCount,
            kapt_record.get("household_count") if kapt_record else None,
            academy_record.get("household_count") if academy_record else None,
        )
        if household_count is not None and household_count > 0:
            row["log_household_count"] = math.log1p(household_count)

        building_count = _first_int(
            kapt_record.get("building_count") if kapt_record else None,
            academy_record.get("building_count") if academy_record else None,
        )
        if household_count and building_count and building_count > 0:
            row["households_per_building"] = household_count / building_count

        total_parking_spaces = _integer(kapt_record.get("total_parking_spaces")) if kapt_record else None
        if total_parking_spaces is not None and total_parking_spaces >= 0:
            row["log_total_parking_spaces"] = math.log1p(total_parking_spaces)
            if household_count and household_count > 0:
                row["parking_spaces_per_household"] = total_parking_spaces / household_count

        if kapt_record and kapt_record.get("has_community_facilities") is not None:
            row["has_community_facilities"] = int(kapt_record["has_community_facilities"])

        academy_count = _integer(academy_record.get("academy_count")) if academy_record else None
        if academy_count is not None:
            row["academy_count_radius_bin"] = _count_bin(academy_count)

        latitude = _number(features.latitude)
        longitude = _number(features.longitude)
        if latitude is not None and longitude is not None:
            self._add_spatial_features(row, latitude, longitude, city_code)

        return row

    def _add_top_floor_features(
        self,
        row: dict[str, Any],
        floor: int | None,
        top_floor: int,
        source: str,
    ) -> None:
        if floor is None or top_floor <= 0:
            return

        floors_below = max(top_floor - floor, 0)
        relative_floor = min(max(floor / top_floor, 0), 1)
        row[f"floors_below_{source}_top"] = floors_below
        row[f"floors_below_{source}_top_bin"] = _below_top_bin(floors_below)
        if source == "estimated":
            row["is_estimated_top_floor"] = 1 if floors_below == 0 else 0
            row["is_near_estimated_top_floor"] = 1 if floors_below <= 2 else 0
            row["relative_floor"] = relative_floor
            row["relative_floor_bin"] = _relative_floor_bin(relative_floor)
        else:
            row["kapt_relative_floor"] = relative_floor
            row["kapt_relative_floor_bin"] = _relative_floor_bin(relative_floor)

    def _add_spatial_features(
        self,
        row: dict[str, Any],
        latitude: float,
        longitude: float,
        city_code: str,
    ) -> None:
        bus_summary = _nearest_summary(self._bus_stop_points(city_code), latitude, longitude)
        if bus_summary:
            row["bus_stop_count_radius_bin"] = _count_bin(bus_summary["count"])
            row["log_nearest_bus_stop_distance_m"] = math.log1p(bus_summary["nearest_m"])

        rail_summary = _nearest_summary(self._rail_station_points(city_code), latitude, longitude)
        if rail_summary:
            row["subway_count_radius_bin"] = _count_bin(rail_summary["count"])
            row["log_nearest_subway_distance_m"] = math.log1p(rail_summary["nearest_m"])

        school_points = self._school_location_points(city_code)
        school_summary = _nearest_summary(school_points, latitude, longitude)
        if school_summary:
            row["school_count_radius_bin"] = _count_bin(school_summary["count"])

        elementary_summary = _nearest_summary(
            [point for point in school_points if "초" in point.kind],
            latitude,
            longitude,
        )
        if elementary_summary:
            row["log_nearest_elementary_school_distance_m"] = math.log1p(elementary_summary["nearest_m"])

        middle_summary = _nearest_summary(
            [point for point in school_points if "중" in point.kind],
            latitude,
            longitude,
        )
        if middle_summary:
            row["log_nearest_middle_school_distance_m"] = math.log1p(middle_summary["nearest_m"])

    def _find_complex_floor(
        self,
        features: ApartmentFeatures,
        city_code: str,
    ) -> dict[str, Any] | None:
        if not _normalize_name(features.propertyName):
            return None
        return self._best_named_record(self._complex_floor_rows(), features, city_code)

    def _find_academy(
        self,
        features: ApartmentFeatures,
        city_code: str,
    ) -> dict[str, Any] | None:
        if not _normalize_name(features.propertyName):
            return None
        return self._best_named_record(self._academy_rows(), features, city_code)

    def _find_kapt(
        self,
        features: ApartmentFeatures,
        city_code: str,
    ) -> dict[str, Any] | None:
        if not _normalize_name(features.propertyName):
            return None
        return self._best_named_record(self._kapt_rows(), features, city_code)

    def _best_named_record(
        self,
        records: list[dict[str, Any]],
        features: ApartmentFeatures,
        city_code: str,
    ) -> dict[str, Any] | None:
        wanted_name = _normalize_name(features.propertyName)
        if not wanted_name:
            return None

        best_record = None
        best_score = 0
        for record in records:
            if record.get("city_code") and record["city_code"] != city_code:
                continue
            score = 0
            if record.get("normalized_name") == wanted_name:
                score += 5
            elif wanted_name in record.get("normalized_name", "") or record.get("normalized_name", "") in wanted_name:
                score += 3
            else:
                continue

            if _same_text(record.get("district"), features.sigungu):
                score += 2
            if _same_text(record.get("legal_dong"), features.legalDong):
                score += 2
            address = str(record.get("address") or "")
            if features.sigungu and features.sigungu in address:
                score += 1
            if features.legalDong and features.legalDong in address:
                score += 1

            if score > best_score:
                best_record = record
                best_score = score

        return best_record

    def _complex_floor_rows(self) -> list[dict[str, Any]]:
        if self._complex_floor_records is not None:
            return self._complex_floor_records

        path = self.data_dir / "seoul_busan_complex_floor_stats_201007_202606_merged.csv"
        rows = []
        for row in _read_csv_dicts(path):
            name = str(row.get("building_name") or "")
            rows.append(
                {
                    "city_code": str(row.get("city_code") or "").strip().lower(),
                    "district": _clean_text(row.get("district")),
                    "legal_dong": _clean_text(row.get("legal_dong")),
                    "name": name,
                    "normalized_name": _normalize_name(name),
                    "estimated_max_floor": _integer(row.get("estimated_max_floor")),
                    "observation_count": _integer(row.get("observation_count")),
                }
            )
        self._complex_floor_records = rows
        return rows

    def _academy_rows(self) -> list[dict[str, Any]]:
        if self._academy_records is not None:
            return self._academy_records

        path = self.data_dir / "아파트단지인근학원교습소2604_csv.csv"
        count_columns = (
            "SSIZE_INSTUT_CNT",
            "MSIZE_INSTUT_CNT",
            "LGZ_INSTUT_CNT",
            "GNRLZ_INSTUT_CNT",
            "ETEX_INSTUT_CNT",
            "FGGG_INSTUT_CNT",
            "AAMAPE_INSTUT_CNT",
            "READRM_CNT",
            "INFO_INSTUT_CNT",
            "SPCEDU_INSTUT_CNT",
            "VCSK_INSTUT_CNT",
            "ETC_INSTUT_CNT",
        )
        records = []
        for row in _read_csv_dicts(path, delimiter="|"):
            name = str(row.get("POTVALE_IFRA_HSMP_NM") or "")
            address = str(row.get("LNNO_ADRES") or "")
            records.append(
                {
                    "city_code": _city_code_from_text(address),
                    "district": _district_from_address(address),
                    "legal_dong": _legal_dong_from_address(address),
                    "address": address,
                    "name": name,
                    "normalized_name": _normalize_name(name),
                    "building_count": _integer(row.get("DONG_CNT")),
                    "household_count": _integer(row.get("NMHSH")),
                    "academy_count": sum(_integer(row.get(column)) or 0 for column in count_columns),
                }
            )
        self._academy_records = records
        return records

    def _kapt_rows(self) -> list[dict[str, Any]]:
        if self._kapt_records is not None:
            return self._kapt_records

        path = self.data_dir / "20260605_단지_기본정보.xlsx"
        records = []
        for row in _read_xlsx_dicts(path, required_headers=("시도", "시군구", "단지명")):
            name = str(row.get("단지명") or "")
            community = str(row.get("부대복리시설") or "").strip()
            records.append(
                {
                    "city_code": _city_code_from_text(str(row.get("시도") or "")),
                    "district": _clean_text(row.get("시군구")),
                    "legal_dong": _clean_text(row.get("동리") or row.get("읍면")),
                    "address": _clean_text(row.get("법정동주소")),
                    "name": name,
                    "normalized_name": _normalize_name(name),
                    "building_count": _integer(row.get("동수")),
                    "household_count": _integer(row.get("세대수")),
                    "total_parking_spaces": _integer(row.get("총주차대수")),
                    "kapt_max_floor": _first_int(row.get("최고층수"), row.get("최고층수(건축물대장상)")),
                    "has_community_facilities": 0 if not community or community in {"없음", "0", "-"} else 1,
                }
            )
        self._kapt_records = records
        return records

    def _bus_stop_points(self, city_code: str) -> list[GeoPoint]:
        if self._bus_points is None:
            path = self.data_dir / "국토교통부_전국 버스정류장 위치정보_20251031.csv"
            self._bus_points = [
                point
                for row in _read_csv_dicts(path)
                if (point := _point_from_row(row, "위도", "경도")) is not None
            ]
        return _filter_city_points(self._bus_points, city_code)

    def _rail_station_points(self, city_code: str) -> list[GeoPoint]:
        if self._rail_points is None:
            path = self.data_dir / "국가철도공단_철도역 정보_20250711.csv"
            self._rail_points = [
                point
                for row in _read_csv_dicts(path)
                if (point := _point_from_row(row, "위도좌표", "경도좌표", kind=str(row.get("주소") or ""))) is not None
            ]
        return _filter_city_points(self._rail_points, city_code)

    def _school_location_points(self, city_code: str) -> list[GeoPoint]:
        if self._school_points is None:
            path = self.data_dir / "한국교육시설안전원_초중등학교위치_20260320.csv"
            self._school_points = [
                point
                for row in _read_csv_dicts(path)
                if (
                    point := _point_from_row(
                        row,
                        "위도",
                        "경도",
                        kind=f"{row.get('소재지지번주소') or ''} {row.get('학교급구분') or ''}",
                    )
                )
                is not None
            ]
        return _filter_city_points(self._school_points, city_code)


def _read_csv_dicts(path: Path, delimiter: str = ",") -> list[dict[str, Any]]:
    if not path.is_file():
        return []
    for encoding in CSV_ENCODINGS:
        try:
            with path.open("r", encoding=encoding, newline="") as file:
                return list(csv.DictReader(file, delimiter=delimiter))
        except UnicodeDecodeError:
            continue
    return []


def _read_xlsx_dicts(path: Path, required_headers: tuple[str, ...]) -> list[dict[str, str]]:
    if not path.is_file():
        return []

    with zipfile.ZipFile(path) as archive:
        shared_strings = _xlsx_shared_strings(archive)
        sheet_names = [
            name
            for name in archive.namelist()
            if name.startswith("xl/worksheets/sheet") and name.endswith(".xml")
        ]
        for sheet_name in sheet_names:
            rows = list(_xlsx_rows(archive, sheet_name, shared_strings))
            header_index = next(
                (
                    index
                    for index, row in enumerate(rows)
                    if all(required_header in row for required_header in required_headers)
                ),
                None,
            )
            if header_index is None:
                continue

            headers = rows[header_index]
            return [
                {
                    headers[index]: value
                    for index, value in enumerate(row)
                    if index < len(headers) and headers[index]
                }
                for row in rows[header_index + 1 :]
                if any(value for value in row)
            ]
    return []


def _xlsx_shared_strings(archive: zipfile.ZipFile) -> list[str]:
    if "xl/sharedStrings.xml" not in archive.namelist():
        return []
    namespace = {"a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
    root = ElementTree.fromstring(archive.read("xl/sharedStrings.xml"))
    return [
        "".join(text.text or "" for text in item.findall(".//a:t", namespace))
        for item in root.findall("a:si", namespace)
    ]


def _xlsx_rows(
    archive: zipfile.ZipFile,
    sheet_name: str,
    shared_strings: list[str],
) -> list[list[str]]:
    namespace = {"a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
    root = ElementTree.fromstring(archive.read(sheet_name))
    rows = []
    for row in root.findall("a:sheetData/a:row", namespace):
        values: dict[int, str] = {}
        for cell in row.findall("a:c", namespace):
            value_node = cell.find("a:v", namespace)
            value = "" if value_node is None else value_node.text or ""
            if cell.attrib.get("t") == "s" and value:
                value = shared_strings[int(value)]
            column_index = _xlsx_column_index(cell.attrib.get("r", "A1"))
            values[column_index] = value
        if values:
            max_index = max(values)
            rows.append([values.get(index, "") for index in range(max_index + 1)])
    return rows


def _xlsx_column_index(cell_reference: str) -> int:
    letters = "".join(char for char in cell_reference if char.isalpha())
    index = 0
    for letter in letters:
        index = index * 26 + ord(letter.upper()) - ord("A") + 1
    return max(index - 1, 0)


def _nearest_summary(
    points: list[GeoPoint],
    latitude: float,
    longitude: float,
) -> dict[str, float] | None:
    nearest = None
    count = 0
    for point in points:
        if abs(point.latitude - latitude) > 0.02 or abs(point.longitude - longitude) > 0.02:
            continue
        distance = _haversine_m(latitude, longitude, point.latitude, point.longitude)
        if nearest is None or distance < nearest:
            nearest = distance
        if distance <= FACILITY_RADIUS_M:
            count += 1
    if nearest is None:
        return None
    return {"nearest_m": nearest, "count": count}


def _haversine_m(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    lat1_r = math.radians(lat1)
    lat2_r = math.radians(lat2)
    delta_lat = math.radians(lat2 - lat1)
    delta_lon = math.radians(lon2 - lon1)
    a = math.sin(delta_lat / 2) ** 2 + math.cos(lat1_r) * math.cos(lat2_r) * math.sin(delta_lon / 2) ** 2
    return EARTH_RADIUS_M * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def _point_from_row(
    row: dict[str, Any],
    latitude_key: str,
    longitude_key: str,
    kind: str = "",
) -> GeoPoint | None:
    latitude = _number(row.get(latitude_key))
    longitude = _number(row.get(longitude_key))
    if latitude is None or longitude is None:
        return None
    return GeoPoint(latitude=latitude, longitude=longitude, kind=kind)


def _filter_city_points(points: list[GeoPoint], city_code: str) -> list[GeoPoint]:
    city_label = "서울" if city_code == "seoul" else "부산"
    filtered = [point for point in points if not point.kind or city_label in point.kind]
    return filtered if filtered else points


def _count_bin(count: int) -> str:
    if count <= 0:
        return "count_0"
    if count <= 2:
        return "count_1_2"
    if count <= 5:
        return "count_3_5"
    if count <= 10:
        return "count_6_10"
    if count <= 20:
        return "count_11_20"
    return "count_21_plus"


def _below_top_bin(floors_below: int) -> str:
    if floors_below <= 0:
        return "below_top_0"
    if floors_below <= 2:
        return "below_top_1_2"
    if floors_below <= 5:
        return "below_top_3_5"
    if floors_below <= 10:
        return "below_top_6_10"
    return "below_top_11_plus"


def _relative_floor_bin(relative_floor: float) -> str:
    if relative_floor <= 0.25:
        return "relative_floor_0_25"
    if relative_floor <= 0.5:
        return "relative_floor_25_50"
    if relative_floor < 0.75:
        return "relative_floor_50_75"
    if relative_floor < 1:
        return "relative_floor_75_100"
    return "relative_floor_100"


def _normalize_name(value: str | None) -> str:
    if not value:
        return ""
    return re.sub(r"[^0-9A-Za-z가-힣]", "", value).lower()


def _same_text(left: str | None, right: str | None) -> bool:
    return _clean_text(left) == _clean_text(right)


def _clean_text(value: Any) -> str:
    return str(value or "").strip()


def _city_code_from_text(value: str) -> str:
    if "서울" in value:
        return "seoul"
    if "부산" in value:
        return "busan"
    return ""


def _district_from_address(address: str) -> str:
    for token in address.split():
        if token.endswith("구") or token.endswith("군"):
            return token
    return ""


def _legal_dong_from_address(address: str) -> str:
    for token in address.split():
        if token.endswith("동") or token.endswith("가") or token.endswith("리"):
            return token
    return ""


def _integer(value: Any) -> int | None:
    number = _number(value)
    if number is None:
        return None
    return int(number)


def _first_int(*values: Any) -> int | None:
    for value in values:
        integer = _integer(value)
        if integer is not None:
            return integer
    return None


def _number(value: Any) -> float | None:
    if value is None:
        return None
    if isinstance(value, str):
        value = value.strip().replace(",", "")
        if not value or value.lower() == "nan":
            return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None
