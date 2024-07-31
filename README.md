# MyCODA

## How to run the project locally

### Pre-Requisites

- [Install Node.js](https://nodejs.org/en/download/package-manager) (Recommended: v20.14.0)
- [Install yarn](https://classic.yarnpkg.com/lang/en/docs/install)
- [Install Docker](https://docs.docker.com/engine/install/)

### Build and run docker image
```shell
docker build -t mycoda .
docker run -p 8081 mycoda
```