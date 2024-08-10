#!/bin/sh
cd ../website || exit
yarn install
yarn run build
cd ..
cp -r website/dist backend/src/main/resources/website