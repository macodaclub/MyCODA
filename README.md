# MyCODA

## Pre-Requisites

- [Install Docker](https://docs.docker.com/engine/install/)

## Run Project Locally using Docker Compose

### Build and run

```sh
docker compose -f ./compose-dev.yaml up
```

Or run configuration [Docker: dev](.run/Docker_%20dev.run.xml), if using IntelliJ IDEA.

## Deploy Docker Image to Docker Hub

### Set up Docker BuildKit builder for multi-platform builds (only needed the first time)

```sh
docker buildx create --name mybuilder --bootstrap --use

# if already defined, but not in use:
docker buildx use mybuilder
```

### Build and push Docker Image to Docker Hub

```sh
docker buildx build --platform linux/amd64,linux/arm64 -t tiagonuneslx/mycoda:latest --push .
```

Or run configuration [Docker: Build and Upload Image](.run/Docker_%20Build%20and%20Upload%20Image.run.xml), if using IntelliJ IDEA.

## Run Docker Image in the server

1. Copy compose.yaml to the server.
2. Configure SSL certificate, and generate a JKS file.
3. Edit the configuration of the compose.yaml, replacing any placeholders (XXXXXX).
4. Run `docker compose up -d`.
