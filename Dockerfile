# syntax=docker/dockerfile:1

FROM node:20.12.2 AS build-website

ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG http_proxy
ARG https_proxy

WORKDIR /website
COPY website ./

RUN yarn config set registry https://registry.npmjs.org/
RUN yarn install --network-timeout 600000 --ignore-optional
RUN yarn run build


FROM eclipse-temurin:11-jdk AS build-backend

ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG http_proxy
ARG https_proxy

WORKDIR /backend
COPY backend ./
COPY --from=build-website /website/dist ./src/main/resources/website
RUN ./gradlew buildFatJar


FROM eclipse-temurin:11-jre AS run-backend

EXPOSE 80/tcp 443/tcp

RUN mkdir /backend
COPY --from=build-backend /backend/build/libs/*.jar /backend/backend.jar

ENTRYPOINT ["java","-jar","/backend/backend.jar"]