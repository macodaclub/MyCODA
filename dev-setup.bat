@ECHO OFF
CD website || EXIT
CALL yarn install
CALL yarn run build
CD ..\frontend || EXIT
CALL yarn install
CALL yarn run build  --base=/frontend
CD ..
XCOPY  website\dist backend\src\main\resources\website /E /Y /I
XCOPY  frontend\dist backend\src\main\resources\frontend /E /Y /I