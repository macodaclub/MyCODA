@ECHO OFF
CD ../website || EXIT
CALL yarn install
CALL yarn run build
CD ..
XCOPY  website\dist backend\src\main\resources\website /E /Y /I