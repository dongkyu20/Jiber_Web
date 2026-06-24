#!/usr/bin/env python3
"""Download and extract MySQL dump archives from a public Google Drive folder."""

from __future__ import annotations

import os
import re
import shutil
import sys
import urllib.request
import zipfile
from dataclasses import dataclass
from pathlib import Path


DEFAULT_DB_DUMP_DRIVE_URL = (
    "https://drive.google.com/drive/folders/"
    "1G5HRQa7K-mPk8o5qguqqv2QaPGoFlGmJ?usp=drive_link"
)


@dataclass(frozen=True)
class DriveFile:
    id: str
    path: str


def main() -> int:
    if _env_bool("DB_DUMP_SKIP_DOWNLOAD", default=False):
        print("DB_DUMP_SKIP_DOWNLOAD=true; skipping database dump download.")
        return 0

    dump_dir = Path(_env("DB_DUMP_DIR", "db/dumps"))
    extract_dir = Path(_env("DB_DUMP_EXTRACT_DIR", str(dump_dir / "extracted")))
    complete_marker = extract_dir / ".jiber-db-dump-download-complete"
    force = _env_bool("DB_DUMP_FORCE_DOWNLOAD", default=False)

    if complete_marker.is_file() and not force:
        print(f"Database dump already extracted at {extract_dir}; skipping.")
        return 0

    if force:
        _clear_directory(dump_dir)

    dump_dir.mkdir(parents=True, exist_ok=True)
    extract_dir.mkdir(parents=True, exist_ok=True)

    files = _list_drive_files(_env("DB_DUMP_DRIVE_URL", DEFAULT_DB_DUMP_DRIVE_URL))
    zip_files = [file for file in files if file.path.lower().endswith(".zip")]
    if not zip_files:
        raise RuntimeError("No .zip database dump files found in Google Drive folder.")

    selected = _select_dump_files(zip_files)
    print("Selected database dump archive(s):")
    for file in selected:
        print(f"- {file.path}")
        archive_path = dump_dir / Path(file.path).name
        if not archive_path.is_file() or force:
            _download_file(file.id, archive_path)
        _extract_zip(archive_path, extract_dir)

    complete_marker.write_text("ok\n", encoding="utf-8")
    print("Database dump download/extract completed.")
    return 0


def _list_drive_files(url: str) -> list[DriveFile]:
    from gdown.download_folder import download_folder

    files = download_folder(
        url=url,
        output="/tmp/jiber-db-dump-list",
        remaining_ok=True,
        skip_download=True,
        quiet=True,
    )
    if files is None:
        raise RuntimeError(f"Could not list Google Drive folder: {url}")
    return [DriveFile(id=file.id, path=file.path) for file in files]


def _select_dump_files(files: list[DriveFile]) -> list[DriveFile]:
    selection = _env("DB_DUMP_SELECTION", "latest").lower()
    sorted_files = sorted(files, key=lambda file: file.path)
    if selection == "all":
        return sorted_files
    if selection == "latest":
        return [sorted_files[-1]]
    raise RuntimeError("DB_DUMP_SELECTION must be 'latest' or 'all'.")


def _download_file(file_id: str, output_path: Path) -> None:
    url = (
        "https://drive.usercontent.google.com/download"
        f"?id={file_id}&export=download&confirm=t"
    )
    request = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with urllib.request.urlopen(request, timeout=180) as response:
        content_type = response.headers.get("content-type", "")
        with output_path.open("wb") as file:
            shutil.copyfileobj(response, file)

    if output_path.stat().st_size == 0:
        output_path.unlink(missing_ok=True)
        raise RuntimeError(f"Downloaded empty response for Google Drive file {file_id}.")
    if "text/html" in content_type.lower():
        output_path.unlink(missing_ok=True)
        raise RuntimeError(f"Google Drive returned an HTML page for file {file_id}.")


def _extract_zip(archive_path: Path, extract_dir: Path) -> None:
    if not zipfile.is_zipfile(archive_path):
        raise RuntimeError(f"Downloaded database dump is not a zip archive: {archive_path}")
    with zipfile.ZipFile(archive_path) as archive:
        archive.extractall(extract_dir)


def _clear_directory(path: Path) -> None:
    if not path.exists():
        return
    for child in path.iterdir():
        if child.is_dir():
            shutil.rmtree(child)
        else:
            child.unlink()


def _env(name: str, default: str) -> str:
    return os.getenv(name, default).strip() or default


def _env_bool(name: str, default: bool) -> bool:
    value = os.getenv(name)
    if value is None or not value.strip():
        return default
    return value.strip().lower() in {"1", "true", "yes", "y", "on"}


if __name__ == "__main__":
    raise SystemExit(main())
