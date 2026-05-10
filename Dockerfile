# syntax=docker/dockerfile:1
FROM node:20.12.2 AS build-website
WORKDIR /website
COPY website ./
RUN yarn install
RUN yarn run build

FROM eclipse-temurin:11-jdk AS build-backend
WORKDIR /backend
COPY backend ./
COPY --from=build-website /website/dist ./src/main/resources/website
RUN ./gradlew buildFatJar

FROM eclipse-temurin:11-jre AS run-backend
EXPOSE 80/tcp 443/tcp
RUN mkdir /backend
COPY --from=build-backend /backend/build/libs/*.jar /backend/backend.jar
ENTRYPOINT ["java","-jar","/backend/backend.jar"]