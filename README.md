# MyCODA Platform

The MaCODA initiative was launched in September 2019 in the context of the MaCODA Workshop, organized by the Lorentz Center at the Leiden Institute of Advanced Computer Science (LIACS), University of Leiden.

This initiative aims to develop and disseminate a community-led research agenda for many-criteria optimization, by promoting discussion and creating a research agenda for a step-change in the understanding of many-criteria optimization.

#### Our key missions include:

- To promote and disseminate the Manycriteria Optimization and Decision Analysis research domain.
- Aggregate and support the MaCODA research community efforts, facilitate knowledge sharing, knowledge transfer and collaboration in academy and between industry and academy.
- MaCODA knowledge domain curation by the means of a community built ontology-based representation and a set of integrated tools for knowledge management.

The MyCODA Platform is presented in Many-Criteria Optimisation and Decision Analysis Book (Chapter 13 – Many-Criteria Optimisation and Decision Analysis Ontology and Knowledge Management, Vitor Basto-Fernandes, Diana Salvador, Iryna Yevseyeva, and Michael Emmerich, https://doi.org/10.1007/978-3-031-25263-1_13).

![](website/public/img/macoda-book.png)

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
3. Edit the configuration of the compose.yaml, replacing any placeholders, represented by double square brackets e.g. [[ Some Secret ]].
4. Run `docker compose up -d`.
