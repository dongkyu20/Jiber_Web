#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.env"
  set +a
fi

DRY_RUN="${PUBLIC_DATA_IMPORT_DRY_RUN:-true}"
LIMIT="${PUBLIC_DATA_IMPORT_LIMIT:-100}"
MONTHS="${PUBLIC_DATA_IMPORT_MONTHS:-12}"
REGIONS="${PUBLIC_DATA_TARGET_REGIONS:-SEOUL,BUSAN}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --live)
      DRY_RUN=false
      shift
      ;;
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --limit)
      LIMIT="$2"
      shift 2
      ;;
    --months)
      MONTHS="$2"
      shift 2
      ;;
    --regions)
      REGIONS="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1" >&2
      echo "Usage: scripts/import-public-data.sh [--dry-run|--live] [--limit N] [--months N] [--regions SEOUL,BUSAN]" >&2
      exit 2
      ;;
    esac
done

echo "Jiber public data import"
echo "- Scope: apartments, Seoul/Busan by default, sale/rent public-data endpoints"
echo "- Dry run: ${DRY_RUN}"
echo "- Limit: ${LIMIT}"
echo "- Months: ${MONTHS}"
echo "- Regions: ${REGIONS}"
echo
echo "No API keys are printed. Keep PUBLIC_DATA_SERVICE_KEY and KAKAO_REST_API_KEY in .env only."

if [[ "$DRY_RUN" != "true" ]]; then
  if [[ -z "${PUBLIC_DATA_SERVICE_KEY:-}" || -z "${KAKAO_REST_API_KEY:-}" ]]; then
    echo "Live mode requires PUBLIC_DATA_SERVICE_KEY and KAKAO_REST_API_KEY in .env." >&2
    exit 1
  fi
fi

cd "$ROOT_DIR/backend"
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.main.web-application-type=none --jiber.public-data.enabled=true --jiber.public-data.dry-run=${DRY_RUN} --jiber.public-data.limit=${LIMIT} --jiber.public-data.import-months=${MONTHS} --jiber.public-data.target-regions=${REGIONS}"
