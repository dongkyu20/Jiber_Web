@echo off
setlocal

call "%~dp0import-public-data.bat" --live --limit 0 --months 12 --regions "SEOUL,BUSAN" --api-types RENT --page-size 1000 %*
exit /b %ERRORLEVEL%
