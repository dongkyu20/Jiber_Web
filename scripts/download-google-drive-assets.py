#!/usr/bin/env python3
"""Download model/data artifacts from public Google Drive links.

This script is intentionally small and runtime-oriented: Docker Compose installs
gdown in a one-shot asset container, then calls this script to populate volumes.
"""

from __future__ import annotations

import os
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import zipfile
from dataclasses import dataclass
from pathlib import Path


DEFAULT_VALUATION_DATA_URL = (
    "https://drive.google.com/drive/folders/"
    "1jdnBblDrBYiAWf1ZsXCz89tnLaTf4CiY?usp=drive_link"
)
DEFAULT_VALUATION_MODEL_URL = (
    "https://drive.google.com/file/d/"
    "19mv0MTkN03YA2cUUQqVGS6zhvar9dmf9/view?usp=drive_link"
)
DEFAULT_XAI_URL = (
    "https://drive.google.com/drive/folders/"
    "1KMv9Lep73c6DLpTSYuVGwpaSW7YetdLP?usp=drive_link"
)


@dataclass(frozen=True)
class DriveAsset:
    name: str
    url: str
    target_dir: Path
    kind: str


def main() -> int:
    if _env_bool("MODEL_ASSETS_SKIP_DOWNLOAD", default=False):
        print("MODEL_ASSETS_SKIP_DOWNLOAD=true; skipping Google Drive asset download.")
        return 0

    force = _env_bool("MODEL_ASSETS_FORCE_DOWNLOAD", default=False)
    allow_partial = _env_bool("MODEL_ASSETS_ALLOW_PARTIAL_DOWNLOAD", default=False)
    assets = []
    if not _env_bool("MODEL_ASSETS_SKIP_DATASET", default=False):
        assets.append(
            DriveAsset(
                name="valuation dataset",
                url=_env("VALUATION_DATA_DRIVE_URL", DEFAULT_VALUATION_DATA_URL),
                target_dir=Path(_env("VALUATION_DATA_DIR", "data/valuation")),
                kind="folder",
            )
        )
    if not _env_bool("MODEL_ASSETS_SKIP_MODEL", default=False):
        assets.append(
            DriveAsset(
                name="valuation model",
                url=_env("VALUATION_MODEL_DRIVE_URL", DEFAULT_VALUATION_MODEL_URL),
                target_dir=Path(_env("VALUATION_ARTIFACTS_DIR", "model-server/artifacts/valuation")),
                kind="zip",
            )
        )
    if not _env_bool("MODEL_ASSETS_SKIP_XAI", default=False):
        assets.append(
            DriveAsset(
                name="xai artifacts",
                url=_env("XAI_DRIVE_URL", DEFAULT_XAI_URL),
                target_dir=Path(_env("XAI_ARTIFACTS_DIR", "model-server/artifacts/xai")),
                kind="folder",
            )
        )

    _require_gdown()
    failures: list[str] = []
    for asset in assets:
        try:
            _download_asset(asset, force=force, allow_partial=allow_partial)
        except Exception as exc:
            failures.append(f"{asset.name}: {exc}")
            if not allow_partial:
                raise
            print(f"{asset.name}: download failed; continuing because partial downloads are allowed.")

    if failures:
        print("Google Drive asset download completed with partial failures:")
        for failure in failures:
            print(f"- {failure}")
        return 2
    print("Google Drive asset download check completed.")
    return 0


def _download_asset(asset: DriveAsset, force: bool, allow_partial: bool) -> None:
    asset.target_dir.mkdir(parents=True, exist_ok=True)
    complete_marker = asset.target_dir / ".jiber-download-complete"
    if complete_marker.is_file() and not force:
        print(f"{asset.name}: already populated at {asset.target_dir}; skipping.")
        return

    if force:
        _clear_directory(asset.target_dir)

    print(f"{asset.name}: downloading to {asset.target_dir}.")
    if asset.kind == "folder":
        _download_folder(asset.url, asset.target_dir, allow_partial=allow_partial)
        _write_complete_marker(complete_marker)
        return

    if asset.kind == "zip":
        with tempfile.TemporaryDirectory(prefix="jiber-drive-") as temp_dir:
            zip_path = Path(temp_dir) / "asset.zip"
            _download_file(_file_id_from_url(asset.url), zip_path)
            if not zipfile.is_zipfile(zip_path):
                raise RuntimeError(f"{asset.name}: downloaded file is not a zip archive.")
            with zipfile.ZipFile(zip_path) as archive:
                archive.extractall(asset.target_dir)
        _write_complete_marker(complete_marker)
        return

    raise RuntimeError(f"{asset.name}: unsupported asset kind {asset.kind!r}.")


def _require_gdown() -> None:
    try:
        import gdown  # noqa: F401
    except ImportError as exc:
        raise RuntimeError(
            "gdown is required. In Docker Compose it is installed automatically; "
            "for local use run `python -m pip install gdown`."
        ) from exc


def _download_folder(url: str, target_dir: Path, allow_partial: bool) -> None:
    from gdown.download_folder import download_folder

    files = download_folder(
        url=url,
        output=str(target_dir),
        remaining_ok=True,
        skip_download=True,
        resume=True,
    )
    if files is None:
        raise RuntimeError(f"Could not list Google Drive folder: {url}")

    failures: list[str] = []
    for file_to_download in files:
        local_path = Path(file_to_download.local_path)
        if local_path.is_file():
            print(f"Skipping already downloaded file {local_path}")
            continue

        local_path.parent.mkdir(parents=True, exist_ok=True)
        try:
            _download_file(file_to_download.id, local_path)
            result = str(local_path)
        except Exception as exc:
            result = None
            print(f"Failed to download {file_to_download.path}: {exc}", file=sys.stderr)
        if result is None:
            failures.append(f"{file_to_download.path} ({file_to_download.id})")
            if not allow_partial:
                break

    if failures:
        message = "Failed to download Google Drive folder files: " + ", ".join(failures)
        raise RuntimeError(message)


def _download_file(file_id: str, output_path: Path) -> None:
    url = (
        "https://drive.usercontent.google.com/download"
        f"?id={file_id}&export=download&confirm=t"
    )
    request = urllib.request.Request(
        url,
        headers={"User-Agent": "Mozilla/5.0"},
    )
    with urllib.request.urlopen(request, timeout=120) as response:
        content_type = response.headers.get("content-type", "")
        output_path.parent.mkdir(parents=True, exist_ok=True)
        with output_path.open("wb") as file:
            shutil.copyfileobj(response, file)

    if output_path.stat().st_size == 0:
        output_path.unlink(missing_ok=True)
        raise RuntimeError(f"Downloaded empty response for Google Drive file {file_id}.")
    if "text/html" in content_type.lower():
        output_path.unlink(missing_ok=True)
        raise RuntimeError(f"Google Drive returned an HTML page for file {file_id}.")


def _file_id_from_url(url: str) -> str:
    patterns = (
        r"/file/d/([^/]+)",
        r"[?&]id=([^&]+)",
    )
    for pattern in patterns:
        match = re.search(pattern, url)
        if match:
            return match.group(1)
    raise RuntimeError(f"Could not extract Google Drive file id from URL: {url}")


def _run(command: list[str]) -> None:
    subprocess.run(command, check=True)


def _is_populated(path: Path) -> bool:
    return path.is_dir() and any(path.iterdir())


def _write_complete_marker(path: Path) -> None:
    path.write_text("ok\n", encoding="utf-8")


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
