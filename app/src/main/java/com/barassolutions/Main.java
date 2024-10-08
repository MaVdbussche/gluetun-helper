package com.barassolutions;

import com.barassolutions.util.Environment;
import com.barassolutions.util.Logger;
import com.barassolutions.util.Logger.LogLevel;
import java.io.IOException;
import com.google.gson.*;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Main {

  public static final Logger logger = new Logger();
  private static OkHttpClient client = new OkHttpClient();

  private static String SID;

  private static final String GLUETUN_URL = Environment.getEnvOrDefault("GLUETUN_URL", null, true);
  private static final String QBITTORRENT_URL = Environment.getEnvOrDefault("QBITTORRENT_URL", null, true);
  private static final String QBITTORRENT_USERNAME = Environment.getEnvOrDefault("QBITTORRENT_USERNAME", "admin", false);
  private static final String QBITTORRENT_PASSWORD = Environment.getEnvOrDefault("QBITTORRENT_PASSWORD", "adminadmin", false);
  private static final String UPDATE_WINDOW_SECONDS = Environment.getEnvOrDefault("UPDATE_WINDOW_SECONDS", "45", false);

  public static void main(String[] args) {
    logger.setLogLevel(LogLevel.valueOf(Environment.getEnvOrDefault("LOG_LEVEL", "INFO", false)));

    Runnable runnable = Main::updatePort;

    // Investigate new Java Virtual threads ?
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    int window = Math.max(Integer.parseInt(UPDATE_WINDOW_SECONDS), 5);
    logger.debug("The script will be executed every " + window + " seconds.");
    executor.scheduleAtFixedRate(runnable, 0, window, TimeUnit.SECONDS);
  }

  private static void updatePort() {
    int port = getCurrentPortFromGluetun();
    if (port < 0) {
      return;
    }

    if (!logInqBittorrent()) {
      return;
    }

    if (!defineIncomingPort(port)) {
      logger.info("Attempting to logout from qBittorrent");
      logOutQBittorrent(); //Best effort
      return;
    }

    if (!logOutQBittorrent()) {
      return;
    }

    logger.info("Program completed successfully. Exiting.");
  }

  private static boolean logInqBittorrent() {
    final String url = QBITTORRENT_URL + "/api/v2/auth/login";
    logger.info("Logging in qBittorrent at " + url);

    String sBody = "username="+QBITTORRENT_USERNAME+"&password="+QBITTORRENT_PASSWORD;
    logger.trace("Body : " + sBody); //TODO warn in the doc that this exposes the password in logs !!

    Request request = new Request.Builder()
        .url(url)
        .post(RequestBody.create(sBody,
            MediaType.get("application/x-www-form-urlencoded")))
        .build();

    try (Response response = client.newCall(request).execute()) {
      logger.debug("Response code : " + response.code());
      if (!response.isSuccessful()) {
        logger.error("Error. Stopping here.");
        response.close();
        return false;
      }

      String cookieHeader = response.headers().get("Set-Cookie");
      if (cookieHeader != null) {
        SID = Arrays.stream(cookieHeader.split(";")).filter(e -> e.startsWith("SID=")).findFirst()
            .orElseThrow();
        logger.trace("Our auth cookie is "
            + SID); //TODO warn in the doc that this exposes sensitive data in logs !!
      }

    } catch (NoSuchElementException noSuchElementException) {
      logger.error("No authentication cookie returned by qBittorrent. Stopping here.");
      return false;
    } catch (IOException ioException) {
      logger.error("Error logging in qBittorrent. Stopping here.");
      return false;
    }

    // From now one every request will be authenticated
    client = client.newBuilder()
        .addInterceptor(chain -> {
          final Request original = chain.request();
          final Request authorized = original.newBuilder().addHeader("Cookie", SID)
              .build();
          return chain.proceed(authorized);
        })
        .build();

    logger.debug("Client is authenticated.");
    return true;
  }

  private static boolean logOutQBittorrent() {
    final String url = QBITTORRENT_URL + "/api/v2/auth/logout";
    logger.info("Logging out of qBittorrent at " + url);

    Request request = new Request.Builder()
        .url(url)
        .post(RequestBody.create("", MediaType.get("application/x-www-form-urlencoded")))
        .build();

    try (Response response = client.newCall(request).execute()) {
      logger.debug("Response code : " + response.code());
      if (!response.isSuccessful()) {
        logger.error("Error. Stopping here."); // Is it really a big deal ?
        response.close();
        return false;
      }
    } catch (IOException ioException) {
      logger.error("Error logging out of qBittorrent. Stopping here.");
      return false;
    }

    logger.debug("Client is logged out.");
    return true;
  }

  private static int getCurrentPortFromGluetun() {
    final String url = GLUETUN_URL + "/v1/openvpn/portforwarded"; //Actually not only for OpenVPN
    logger.info("Getting the current forwarded port from Gluetun at " + url);

    Request requestForPort = new Request.Builder()
        .url(url)
        .get()
        .build();

    try (Response response = client.newCall(requestForPort).execute()) {
      logger.debug("Response code : " + response.code());
      if (!response.isSuccessful()) {
        logger.error("Error. Stopping here.");
        response.close();
        return -1;
      }

      if (response.body() != null) {
        JsonElement port = JsonParser.parseString(response.body().string()).getAsJsonObject()
            .get("port");

        if (port == null) {
          logger.error("No port found in response. Stopping here.");
          response.close();
          return -1;
        } else if (port.getAsInt() == 0) {
          logger.error("No port currently forwarded. Check the Gluetun logs, there is probably something wrong with your configuration or your VPN provider. Stopping here.");
          response.close();
          return -1;
        }
        logger.info("Forwarded port : " + port);

        return port.getAsInt();
      } else {
        logger.error("No response received from Gluetun. Stopping here.");
        response.close();
        return -1;
      }
    } catch (IOException ioException) {
      logger.error("Error getting current port from qBittorrent. Stopping here.");
      return -1;
    }
  }

  private static boolean defineIncomingPort(int port) {
    final String url = QBITTORRENT_URL + "/api/v2/app/setPreferences";
    logger.info("Updating qBittorrent settings");

    JsonObject bodyJson = new JsonObject();
    bodyJson.addProperty("listen_port", port);
    logger.trace("Body : json=" + bodyJson);

    Request setPortRequest = new Request.Builder()
        .url(url)
        .post(RequestBody.create("json=" + bodyJson,
            MediaType.get("application/x-www-form-urlencoded")))
        .build();

    try (Response response = client.newCall(setPortRequest).execute()) {
      logger.debug("Response code : " + response.code());
      if (!response.isSuccessful()) {
        logger.error("Error. Stopping here.");
        response.close();
        return false;
      }
    } catch (IOException ioException) {
      logger.error("Error updating qBittorrent incoming port. Stopping here.");
      return false;
    }

    logger.debug("Settings successfully updated.");
    return true;
  }
}