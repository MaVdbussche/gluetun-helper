package com.barassolutions.util;

import static com.barassolutions.Main.logger;

public class Environment {

  public static String getEnvOrDefault(String name, String defaultValue) {
    String result = System.getenv(name);
    if (result!=null) {
      return result;
    } else {
      logger.debug("Environment value " + name + " was not defined. The default value will be used, but you should probably adjust your configuration.");
      return defaultValue;
    }
  }
}
