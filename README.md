# Gluetun Helper
A small script to update [qBittorrent](https://github.com/qbittorrent/qBittorrent)'s _listening port_ automatically based on the VPN forwarded port in [Gluetun](https://github.com/qdm12/gluetun), compatible with Gluetun v4.

[![License](https://img.shields.io/github/license/MaVdbussche/gluetun-helper?label=License)](https://github.com/MaVdbussche/gluetun-helper/blob/master/LICENSE)

[![Release](https://img.shields.io/github/v/release/MaVdbussche/gluetun-helper?label=Release)](https://github.com/MaVdbussche/gluetun-helper/releases)

[![Open issues](https://img.shields.io/github/issues/MaVdbussche/gluetun-helper?label=Issues)](https://github.com/MaVdbussche/gluetun-helper/issues)
[![Open PRs](https://img.shields.io/github/issues-pr/MaVdbussche/gluetun-helper?label=Pull%20requests)](https://github.com/MaVdbussche/gluetun-helper/pulls)

[![Build status](https://github.com/MaVdbussche/gluetun-helper/actions/workflows/docker-image.yml/badge.svg?label=Build%20status)](https://github.com/MaVdbussche/gluetun-helper/actions/workflows/docker-image.yml)
[![Container registry build](https://github.com/MaVdbussche/gluetun-helper/actions/workflows/docker-publish.yml/badge.svg?label=Container%20registry%20build)](https://github.com/MaVdbussche/gluetun-helper/actions/workflows/docker-publish.yml)


## Table of Contents
- [Background](#background)
- [How to use](#how-to-use)
- [Configuration](#configuration)
- [Docker network config](#docker-network-config)
- [Set up the development environment](#set-up-the-development-environment)
- [Contributing](#contributing)
- [License](#license)

## Background
When I installed Gluetun on my home server as a way to tunnel my torrent clients' traffic through my VPN provider,
I quickly encountered the issue that the [forwarded port](https://github.com/qdm12/gluetun-wiki/blob/main/setup/advanced/vpn-port-forwarding.md) 
wasn't persisted across Gluetun restarts, or would sometimes even change suddenly. 
This would mean I could not reliably define the listening port in my torrent clients, and speeds would be affected.

I am of course not the first person to face this issue, and you will find on the internet many other scripts that do a similar job.
However, I decided to create my own solution for several reasons :
- Many of the solutions I found use the file `/tmp/gluetun/forwarded_port` to get the port currently forwarded by Gluetun.<br/>
  However, this solution will be deprecated in the v4.0.0 release of Gluetun.<br/>
  This script makes instead use of the more robust [control server](https://github.com/qdm12/gluetun-wiki/blob/main/setup/advanced/control-server.md#openvpn);
- I wanted to build something myself;
- I plan on expanding this program in the future to support other torrent clients

## How to use
This script is packaged as a Docker container. The reason for this are multiple :
- This simplifies the distribution and makes this program available to all, regardless of the platform they use;
- Since Gluetun is, to my knowledge, only distributed as a Docker container, you will already be using Docker if you have the need for this repository;
- I wanted to gain more experience with Docker images and their build pipeline

To run this program, simply pull the image `ghcr.io/mavdbussche/gluetun-helper:${TAG}`, 
where `${TAG}` is the image version tag (see 'Packages' on the right for a list of available tags).<br/>
You can also consult the `docker-compose.yml` file at the root of this repository for an example configuration if you use Docker Compose.

## Configuration
To use this container, you will need to define some environment variables that are specific to your setup.
Some defaults are provided, but they will most likely not work for you !

| Variable                | Mandatory | Default value           | Description                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|-------------------------|-----------|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GLUETUN_URL`           | *         | `http://localhost:8000` | Full URL to your Gluetun container's [control server](https://github.com/qdm12/gluetun-wiki/blob/main/setup/advanced/control-server.md).<br/>If Gluetun is accessible on the LAN, specify the IP address and the exposed port (for example, `http://1.2.3.4:1234`).<br/>If this program accesses Gluetun through a bridge Docker network (recommended), use the hostname form with the internal port (always 8000) (for example, `http://gluetun:8000`) |
| `QBITTORRENT_URL`       | *         | `http://localhost:8080` |                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| `QBITTORRENT_USERNAME`  |           | `admin`                 | Credentials to connect to the qBittorrent Web UI. Default is the default value defined in the qBittorrent project.                                                                                                                                                                                                                                                                                                                                      |
| `QBITTORRENT_PASSWORD`  |           | `adminadmin`            | Credentials to connect to the qBittorrent Web UI. Default is the default value defined in the qBittorrent project.                                                                                                                                                                                                                                                                                                                                      |
| `UPDATE_WINDOW_SECONDS` |           | `45`                    | Refresh window (in seconds, no unit) to periodically send the updated port to the download client.<br/>Default (45s) is the refresh period for Gluetun with the Wireguard protocol.<br/>The program enforces a minimum of 5s to avoid unnecessary load.                                                                                                                                                                                                 |
| `LOG_LEVEL`             |           | `INFO`                  | Possible values : `OFF`, `FATAL`, `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`, `ALL`                                                                                                                                                                                                                                                                                                                                                                      |

## Docker network config
It is recommended to run this container on a Docker bridge network shared with Gluetun. 
While qBittorrent is probably running *through* Gluetun if you need this script in the first place, that is not recommended/necessary for `gluetun-helper`. 
I propose two approaches for your `docker-compose.yml` file :
### Approach A (Docker networks) :
```
services:
  gluetun:
     [...]
     hostname: gluetun
     ports: N/A # No exposed ports necessary if Docker networks are used
     networks: [ "some-network" ] # Shared with gluetun-helper
  qbittorrent:
     [...]
     environment:
       -WEBUI_PORT=8080
     network_mode: "container:gluetun" # This container access the network through Gluetun's Docker namespace
     ports: N/A
     networks: N/A # All traffic goes through Gluetun; nothing can/should contact this container directly
     depends_on:
       gluetun:
         condition: service_healthy
         restart: true
  gluetun-helper:
     [...]
     environment:
       - GLUETUN_URL=http://gluetun:8000
       - QBITTORRENT_URL=http://gluetun:8080 # All qBittorrent traffic goes through Gluetun => this is the de facto address of qBittorrent Web UI.
       - QBITTORRENT_USERNAME=XXXXX
       - QBITTORRENT_PASSWORD=XXXXXXXXXXXX
     ports: N/A # This container doesn't need to expose any port to function
     networks: [ "some-network" ] # Shared with Gluetun
     depends_on:
       gluetun:
         condition: service_healthy
       qbittorrent:
         condition: service_started
```
### Approach B (open ports) :
```
services:
  gluetun:
     [...]
     ports:
       - "1.2.3.4:8000:8000" # Gluetun control server runs on 8000
       - "1.2.3.4:8080:8080" # qBittorrent's web UI
  qbittorrent:
     [...]
     environment:
       -WEBUI_PORT=8080
     network_mode: "container:gluetun" # This container access the network through Gluetun's Docker namespace
     ports: N/A
     depends_on:
       gluetun:
         condition: service_healthy
         restart: true
  gluetun-helper:
     [...]
     environment:
       - GLUETUN_URL=http://1.2.3.4:8000
       - QBITTORRENT_URL=http://1.2.3.4:8080 # All qBittorrent traffic goes through Gluetun => this is the de facto adress of qBittorrent Web UI.
       - QBITTORRENT_USERNAME=XXXXX
       - QBITTORRENT_PASSWORD=XXXXXXXXXXXX
     ports: [] # This container doesn't need to expose any port to function
     depends_on:
       gluetun:
         condition: service_healthy
       qbittorrent:
         condition: service_started
```

## Set up the development environment
This project is built with Gradle. A distribution of Gradle Wrapper is included with this repository. Clone this repository, then execute the following commands to build and run the code locally :
```
./gradlew init # Prepare the environment
./gradlew jar # To build an executable JAR
java -jar app/buid/libs/${VERSION}.jar # Where VERSION is the name of the JAR from the previous step
```
You could also simply build the image directly using the Dockerfile included :
```
docker build . -t gluetun-helper:gluetun-helper
docker run gluetun-helper:gluetun-helper
```

## Contributing
If you feel like you found a bug in the program, please raise an issue on GitHub.<br/>You can also use issues to request new features.

## License
This program is licensed under the [Apache License, Version 2.0](https://github.com/MaVdbussche/gluetun-helper/blob/master/LICENSE)
This program uses the following dependencies:
- [OkHttp](https://square.github.io/okhttp/), licensed under the [Apache License, Version 2.0](https://square.github.io/okhttp/#license)
- [Gson](https://github.com/google/gson), licensed under the [Apache License, Version 2.0](https://github.com/google/gson/blob/main/LICENSE)
