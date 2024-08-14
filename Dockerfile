# When adapting the following line, make sure to also change the Gradle Wrapper version
# (referenced in ./gradle/wrapper/graddle-wrapper.properties), so that the development environment
# (using this Gradle Wrapper version) remains aligned with the production Dockerfile (using vanilla Gradle)
FROM gradle:8.7-alpine AS build
MAINTAINER MaVdbussche

RUN ["gradle", "init"]
COPY ./app /app

WORKDIR /app

RUN ["gradle", "jar"]

FROM azul/zulu-openjdk-alpine:21-latest
MAINTAINER MaVdbussche

ENV GLUETUN_URL="http://localhost:8000"
ENV QBITTORRENT_URL="http://localhost:8090"
ENV QBITTORRENT_USERNAME="admin"
ENV QBITTORRENT_PASSWORD="adminadmin"
ENV UPDATE_WINDOW_SECONDS="5"
ENV LOG_LEVEL="INFO"

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
