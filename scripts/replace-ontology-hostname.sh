#!/bin/sh
cd ../backend/src/main/resources/static || exit
sed -i '' 's|http://localhost|https://mycoda.ddns.net|g' MaCODA.owl
sed -i '' 's|http://localhost|https://mycoda.ddns.net|g' MaCODA-labeled.owl