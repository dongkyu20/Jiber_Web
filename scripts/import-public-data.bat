@echo off
setlocal

set "ROOT_DIR=%~dp0.."
for %%I in ("%ROOT_DIR%") do set "ROOT_DIR=%%~fI"

set "DRY_RUN=true"
set "LIMIT=100"
set "MONTHS=12"
set "REGIONS=SEOUL,BUSAN"
set "API_TYPES=SALE,RENT"
set "PAGE_SIZE=1000"

:parse_args
if "%~1"=="" goto run
if /I "%~1"=="--live" (
  set "DRY_RUN=false"
  shift
  goto parse_args
)
if /I "%~1"=="--dry-run" (
  set "DRY_RUN=true"
  shift
  goto parse_args
)
if /I "%~1"=="--limit" (
  if "%~2"=="" goto usage
  set "LIMIT=%~2"
  shift
  shift
  goto parse_args
)
if /I "%~1"=="--months" (
  if "%~2"=="" goto usage
  set "MONTHS=%~2"
  shift
  shift
  goto parse_args
)
if /I "%~1"=="--regions" (
  if "%~2"=="" goto usage
  set "REGIONS=%~2"
  shift
  shift
  goto parse_args
)
if /I "%~1"=="--api-types" (
  if "%~2"=="" goto usage
  set "API_TYPES=%~2"
  shift
  shift
  goto parse_args
)
if /I "%~1"=="--page-size" (
  if "%~2"=="" goto usage
  set "PAGE_SIZE=%~2"
  shift
  shift
  goto parse_args
)
goto usage

:run
echo Jiber public data import
echo - Scope: Seoul/Busan by default. API types can include SALE, RENT, OFFICETEL_SALE, OFFICETEL_RENT, VILLA_SALE, VILLA_RENT.
echo - Dry run: %DRY_RUN%
echo - Limit: %LIMIT%
echo - Months: %MONTHS%
echo - Regions: %REGIONS%
echo - API types: %API_TYPES%
echo - Page size: %PAGE_SIZE%
echo.
echo No API keys are printed. Keep PUBLIC_DATA_SERVICE_KEY and KAKAO_REST_API_KEY in .env only.

set "BACKEND_DIR=%ROOT_DIR%\backend"
set "MVN_CMD="
if exist "%BACKEND_DIR%\mvnw.cmd" set "MVN_CMD=%BACKEND_DIR%\mvnw.cmd"
if not defined MVN_CMD (
  where mvn.cmd >nul 2>nul
  if not errorlevel 1 set "MVN_CMD=mvn.cmd"
)
if not defined MVN_CMD (
  if exist "%ROOT_DIR%\mysql-data\maven\apache-maven-3.9.9\bin\mvn.cmd" (
    set "MVN_CMD=%ROOT_DIR%\mysql-data\maven\apache-maven-3.9.9\bin\mvn.cmd"
  )
)

if defined MVN_CMD (
  cd /d "%BACKEND_DIR%" || exit /b 1
  call "%MVN_CMD%" spring-boot:run -Dspring-boot.run.arguments="--spring.main.web-application-type=none --jiber.public-data.enabled=true --jiber.public-data.dry-run=%DRY_RUN% --jiber.public-data.limit=%LIMIT% --jiber.public-data.import-months=%MONTHS% --jiber.public-data.target-regions=%REGIONS% --jiber.public-data.api-types=%API_TYPES% --jiber.public-data.page-size=%PAGE_SIZE%"
  exit /b
)

set "JAR_PATH=%BACKEND_DIR%\target\jiber-backend-0.0.1-SNAPSHOT.jar"
if not exist "%JAR_PATH%" (
  echo Maven was not found and %JAR_PATH% does not exist. Build the backend first. 1>&2
  exit /b 1
)

java -jar "%JAR_PATH%" --spring.main.web-application-type=none --jiber.public-data.enabled=true --jiber.public-data.dry-run=%DRY_RUN% --jiber.public-data.limit=%LIMIT% --jiber.public-data.import-months=%MONTHS% --jiber.public-data.target-regions=%REGIONS% --jiber.public-data.api-types=%API_TYPES% --jiber.public-data.page-size=%PAGE_SIZE%
exit /b %ERRORLEVEL%

:usage
echo Usage: scripts\import-public-data.bat [--dry-run^|--live] [--limit N] [--months N] [--regions "SEOUL,BUSAN"] [--api-types "SALE,RENT"] [--page-size N] 1>&2
exit /b 2
