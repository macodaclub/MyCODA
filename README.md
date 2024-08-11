# MyCODA

## Pre-Requisites

- [Install Docker](https://docs.docker.com/engine/install/)

## Run Project Locally using Docker

### Build and run docker image

```sh
docker build -t mycoda .
docker run -p 80:80 mycoda
```

## Deploy Docker Image to Docker Hub

### Set up Docker BuildKit builder for multi-platform builds (only needed the first time)

```sh
docker buildx create --name mybuilder --bootstrap --use
```

### Build and push Docker Image to Docker Hub

```sh
docker buildx build --platform linux/amd64,linux/arm64 -t tiagonuneslx/mycoda:latest --push .
```

Or run configuration [Docker: Build and Upload Image](.run/Docker_%20Build%20and%20Upload%20Image.run.xml), if using IntelliJ IDEA.

## Run Docker Image in the server

After setting up SSL Certificate using Let's Encrypt, and converting to JKS, run:

```sh
docker pull tiagonuneslx/mycoda:latest
docker run --name mycoda \
  --detach \
  --restart=always \
  -p 80:80 -p 443:443 \
  -v /etc/letsencrypt/live/mycoda.ddns.net/keystore.jks:/backend/keystore.jks \
  -e SSL_KEY_ALIAS=XXXXXX \
  -e SSL_KEYSTORE_PASSWORD=XXXXXX \
  -e SSL_PRIVATE_KEY_PASSWORD=XXXXXX \
  -e GITHUB_OAUTH=XXXXXX \
  tiagonuneslx/mycoda \
  -sslPort=443 \
  -sslKeyStore=/backend/keystore.jks
```