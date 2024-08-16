package com.barassolutions.util;

public class Logger {

  protected LogLevel activeLogLevel;

  public Logger() {
    this.activeLogLevel = LogLevel.ALL;
  }

  // Convention on Logging levels taken from https://logging.apache.org/log4j/2.x/log4j-api/apidocs/index.html
  public enum LogLevel {
    OFF(""), //No logging whatsoever
    FATAL("[FATAL] : "), //Little logging
    ERROR("[ERROR] : "),
    WARN("[WARN] : "),
    INFO("[INFO] : "),
    DEBUG("[DEBUG] : "), //Used for tests
    TRACE("[TRACE] : "),
    ALL(""); //Gimme the logs. All of them.

    private final String image;

    LogLevel(String image) {
      this.image = image;
    }
  }

  public void setLogLevel(LogLevel level) {
    this.activeLogLevel = level;
  }

  public void fatal(String message, Object... args) {
    if (activeLogLevel.ordinal() >= LogLevel.FATAL.ordinal()) {
      System.out.printf(LogLevel.FATAL.image + message, args);
      System.out.println();
    }
  }

  public void error(String message, Object... args) {
    if (activeLogLevel.ordinal() >= LogLevel.ERROR.ordinal()) {
      System.out.printf(LogLevel.ERROR.image + message, args);
      System.out.println();
    }
  }

  public void warn(String message, Object... args) {
    if (activeLogLevel.ordinal() >= LogLevel.WARN.ordinal()) {
      System.out.printf(LogLevel.WARN.image + message, args);
      System.out.println();
    }
  }

  public void info(String message, Object... args) {
    if (activeLogLevel.ordinal() >= LogLevel.INFO.ordinal()) {
      System.out.printf(LogLevel.INFO.image + message, args);
      System.out.println();
    }
  }

  public void debug(String message, Object... args) {
    if (activeLogLevel.ordinal() >= LogLevel.DEBUG.ordinal()) {
      System.out.printf(LogLevel.DEBUG.image + message, args);
      System.out.println();
    }
  }

  public void trace(String message, Object... args) {
    if (activeLogLevel.ordinal() >= LogLevel.TRACE.ordinal()) {
      System.out.printf(LogLevel.TRACE.image + message, args);
      System.out.println();
    }
  }
}
