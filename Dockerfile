# When adapting the following line, make sure to also change the Gradle Wrapper version
# (referenced in /gradle/wrapper/graddle-wrapper.properties), so that the development environment
# (using this Gradle Wrapper version) remains aligned with the production Dockerfile (using vanilla Gradle)
FROM gradle:8.7-alpine AS build

RUN ["gradle", "init"]
COPY ./app /app

WORKDIR /app

RUN ["gradle", "jar"]

FROM azul/zulu-openjdk-alpine:21-latest

LABEL org.opencontainers.image.authors="MaVdbussche (Barasingha)"
LABEL org.opencontainers.image.url="https://github.com/MaVdbussche/gluetun-helper"
LABEL org.opencontainers.image.documentation="https://github.com/MaVdbussche/gluetun-helper"
LABEL org.opencontainers.image.source="https://github.com/MaVdbussche/gluetun-helper"
LABEL org.opencontainers.image.vendor="Barassolutions"
LABEL org.opencontainers.image.licenses="Apache-2.0"
LABEL org.opencontainers.image.title="Gluetun Helper"
LABEL org.opencontainers.image.description="A small script to update open ports in your torrent client(s), sabed on the port forwarded in Gluetun/your VPN provider."

ENV GLUETUN_URL="http://localhost:8000"
ENV QBITTORRENT_URL="http://localhost:8090"
ENV QBITTORRENT_USERNAME="admin"
ENV QBITTORRENT_PASSWORD="adminadmin"
ENV LOG_LEVEL="INFO"

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
