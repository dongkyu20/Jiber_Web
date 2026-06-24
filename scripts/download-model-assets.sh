#!/usr/bin/env sh
set -eu

DOCUMENTS_DIR="${MODEL_DOCUMENTS_DIR:-/model-server/documents}"
ARTIFACTS_DIR="${MODEL_ARTIFACTS_DIR:-/model-server/artifacts}"
DATA_DIR="${MODEL_DATA_DIR:-/model-server/data}"
FORCE_DOWNLOAD="${MODEL_ASSET_FORCE_DOWNLOAD:-false}"

mkdir -p "${DOCUMENTS_DIR}" "${ARTIFACTS_DIR}" "${DATA_DIR}"

has_value() {
  [ -n "${1:-}" ]
}

download_url() {
  url="$1"
  output="$2"
  curl --fail --location --silent --show-error --output "${output}" "${url}"
}

download_gdrive() {
  file_id="$1"
  output="$2"
  cookies="$(mktemp)"
  response="$(mktemp)"

  curl --fail --location --silent --show-error \
    --cookie-jar "${cookies}" \
    --output "${response}" \
    "https://drive.google.com/uc?export=download&id=${file_id}"

  confirm="$(awk '/download_warning/ {print $7; exit}' "${cookies}" || true)"
  if [ -n "${confirm}" ]; then
    curl --fail --location --silent --show-error \
      --cookie "${cookies}" \
      --output "${output}" \
      "https://drive.google.com/uc?export=download&confirm=${confirm}&id=${file_id}"
    rm -f "${response}" "${cookies}"
    return
  fi

  mv "${response}" "${output}"
  rm -f "${cookies}"
}

download_source() {
  label="$1"
  url="$2"
  gdrive_id="$3"
  output="$4"

  if [ "${FORCE_DOWNLOAD}" != "true" ] && [ -s "${output}" ]; then
    echo "[model-assets] SKIP ${label}: ${output} already exists"
    return
  fi

  if has_value "${url}"; then
    echo "[model-assets] Downloading ${label} from URL"
    download_url "${url}" "${output}"
    return
  fi

  if has_value "${gdrive_id}"; then
    echo "[model-assets] Downloading ${label} from Google Drive"
    download_gdrive "${gdrive_id}" "${output}"
    return
  fi

  echo "[model-assets] SKIP ${label}: no URL or Google Drive file id configured"
}

extract_archive() {
  archive="$1"
  destination="$2"
  archive_type="${3:-auto}"

  case "${archive_type}" in
    zip)
      unzip -oq "${archive}" -d "${destination}"
      ;;
    tar|tar.gz|tgz)
      tar -xf "${archive}" -C "${destination}"
      ;;
    auto)
      case "${archive}" in
        *.zip) unzip -oq "${archive}" -d "${destination}" ;;
        *.tar|*.tar.gz|*.tgz) tar -xf "${archive}" -C "${destination}" ;;
        *) echo "[model-assets] No archive extraction for ${archive}" ;;
      esac
      ;;
    none)
      ;;
    *)
      echo "[model-assets] Unknown archive type: ${archive_type}" >&2
      exit 2
      ;;
  esac
}

tmp_dir="$(mktemp -d)"
trap 'rm -rf "${tmp_dir}"' EXIT

documents_archive="${tmp_dir}/documents.${MODEL_DOCUMENTS_ARCHIVE_TYPE:-zip}"
download_source \
  "documents" \
  "${MODEL_DOCUMENTS_URL:-}" \
  "${MODEL_DOCUMENTS_GDRIVE_ID:-}" \
  "${documents_archive}"
if [ -s "${documents_archive}" ]; then
  extract_archive "${documents_archive}" "${DOCUMENTS_DIR}" "${MODEL_DOCUMENTS_ARCHIVE_TYPE:-zip}"
fi

price_model_file="${ARTIFACTS_DIR}/${MODEL_PRICE_MODEL_FILENAME:-price_model.pkl}"
download_source \
  "price model" \
  "${MODEL_PRICE_MODEL_URL:-}" \
  "${MODEL_PRICE_MODEL_GDRIVE_ID:-}" \
  "${price_model_file}"

dataset_file="${DATA_DIR}/${MODEL_DATASET_FILENAME:-dataset.csv}"
download_source \
  "dataset" \
  "${MODEL_DATASET_URL:-}" \
  "${MODEL_DATASET_GDRIVE_ID:-}" \
  "${dataset_file}"
if [ -s "${dataset_file}" ]; then
  extract_archive "${dataset_file}" "${DATA_DIR}" "${MODEL_DATASET_ARCHIVE_TYPE:-auto}"
fi

echo "[model-assets] Done"
