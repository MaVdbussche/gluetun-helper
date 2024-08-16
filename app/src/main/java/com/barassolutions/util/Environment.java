package com.barassolutions.util;

import static com.barassolutions.Main.logger;

import org.jetbrains.annotations.NotNull;

public class Environment {

  @NotNull
  public static String getEnvOrDefault(String name, String defaultValue, boolean mandatory) {
    String result = System.getenv(name);
    if (mandatory && result == null) {
      logger.error("Mandatory environment value \"" + name + "\" was not defined.");
      System.exit(1);
      return null;
    } else {
      if (result == null) {
        logger.debug("Environment value \"" + name + "\" was not defined. The default value (\"" + defaultValue + "\") will be used. If necessary, adjust your configuration.");
        return defaultValue;
      } else {
        logger.trace("Environment value \"" + name + "\" was defined with value \"" + result + "\".");
        return result;
      }
    }
  }
}
