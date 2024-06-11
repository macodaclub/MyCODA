# syntax=docker/dockerfile:1
FROM node:20.12.2 AS build-website
WORKDIR /website
COPY website ./
RUN yarn install
RUN yarn run build

FROM node:20.12.2 AS build-frontend
WORKDIR /frontend
COPY frontend ./
RUN yarn install
RUN yarn run build --base=/frontend

FROM openjdk:11 as build-backend
WORKDIR /backend
COPY backend ./
COPY --from=build-website /website/dist ./src/main/resources/website
COPY --from=build-frontend /frontend/dist ./src/main/resources/frontend
RUN ./gradlew buildFatJar

FROM openjdk:11 as run-backend
EXPOSE 8081:8081
RUN mkdir /backend
COPY --from=build-backend /backend/build/libs/*.jar /backend/backend.jar
ENTRYPOINT ["java","-jar","/backend/backend.jar"]