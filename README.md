# Gluetun Helper


A small script to update [qBittorrent](https://github.com/qbittorrent/qBittorrent)'s _listening port_ automatically based on the VPN forwarded port in [Gluetun](https://github.com/qdm12/gluetun), compatible with Gluetun v4.

## Table of Contents
- [Background](#background)
- [How to use](#how-to-use)

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

To run this program, simply pull the image `ghcr.io/MaVdbussche/gluetun-helper:${TAG}`, 
where `${TAG}` is the image version tag (see 'Packages' on the right for a list of available tags).

You can also consult the `docker-compose.yml` file at the root of this repository for an example configuration if you use Docker Compose.

## Configuration
To use this container, you will need to define some environment variables that are specific to your setup.
Some defaults are provided, but they will most likely not work for you and a warning will be logged in the console if the fallback default value has been used.

| Variable               | Mandatory | Default value           | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|------------------------|-----------|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GLUETUN_URL`          | *         | `http://localhost:8000` | This is the full URL to your Gluetun container's [control server](https://github.com/qdm12/gluetun-wiki/blob/main/setup/advanced/control-server.md).<br/>If Gluetun is accessible on the LAN, specify the IP address and the exposed port (for example, `http://1.2.3.4:1234`).<br/>If this program accesses Gluetun through a bridge Docker network (recommended), use the hostname form with the internal port (always 8000) (for example, `http://gluetun:8000`) |
| `QBITTORRENT_URL`      | *         | `http://localhost:8090` |                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `QBITTORRENT_USERNAME` | *         | `admin`                 | These are the credentials to connect to the qBittorrent Web UI.                                                                                                                                                                                                                                                                                                                                                                                                     |
| `QBITTORRENT_PASSWORD` | *         | `adminadmin`            | These are the credentials to connect to the qBittorrent Web UI.                                                                                                                                                                                                                                                                                                                                                                                                     |
| `LOGLEVEL`             |           | `INFO`                  | Possible values : `OFF`, `FATAL`, `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`, `ALL`                                                                                                                                                                                                                                                                                                                                                                                  |

## Docker network config
It is recommended to run this container on a Docker bridge network shared with Gluetun. 
While qBittorrent is probably running *through* Gluetun if you need this script, it is not recommended/necessary to run `gluetun-helper` _through_ Gluetun. 
In other words, your `docker-compose.yml` file should look like :
```
services:
  gluetun:
     [...]
     ports: [] # No exposed ports necessary if Docker networks are used
     networks: [ "some-network" ]
  qbittorrent:
     [...]
     network_mode: "container:gluetun" # This container access the network through Gluetun's Docker namesapce
  gluetun-helper:
     [...]
     ports: [] # This container doesn't need to expose any port to function
     networks: [ "some-network" ]
```
or :
```
services:
  gluetun:
     [...]
     ports:
       - "1.2.3.4:8000:8000" # Gluetun control server runs on 8000
       - "1.2.3.4:8090:8090" # qBittorrent's web UI
  qbittorrent:
     [...]
     network_mode: "container:gluetun" # This container access the network through Gluetun's Docker namesapce
  gluetun-helper:
     [...]
     environment:
      - GLUETUN_URL: "http://1.2.3.4:8000"
      - QBITTORRENT_URL: "http://1.2.3.4:8090"
      - QBITTORRENT_USERNAME: "MyUSername"
      - QBITTORRENT_PASSWORD: "MyVeryLongAndComplexPassword"
      - LOGLEVEL: "INFO"
     ports: [] # This container doesn't need to expose any port to function
```