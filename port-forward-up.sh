#!/bin/bash

docker run --rm --net gluetun-back-net \
--name gluetun-helper -h gluetun-helper \
-e GLUETUN_URL=http://gluetun:8000 \
-e QBITTORRENT_URL=http://gluetun:8090 \
-e QBITTORRENT_USERNAME=admin \
-e QBITTORRENT_PASSWORD=adminadmin \
-e LOG_LEVEL=INFO \
ghcr.io/mavdbussche/gluetun-helper:${TAG}