package com.netflix.iep.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import com.netflix.config.ConfigurationManager;

public class ScopedPropertiesLoader {
  private static Logger LOGGER = LoggerFactory.getLogger(ScopedPropertiesLoader.class);

  private static final String PROP_PROPERTIES_FILE = "netflix.iep.config.propertiesFile";
  private static final String DEF_PROPERTIES_FILE = "application.scoped.properties";

  public static void load() {
    String[] propFiles = System.getProperty(PROP_PROPERTIES_FILE, DEF_PROPERTIES_FILE).split(",");

    List<URL> propUrls = new ArrayList<URL>();
    for (int i = 0; i < propFiles.length; i++) {
      String propFile = propFiles[i];
      try {
        propUrls.add((Resources.getResource(propFile)));
      }
      catch (IllegalArgumentException e) {
        LOGGER.debug("ignoring " + propFile + " - does not exist");
      }
    }

    StringBuilder debug = new StringBuilder("# Generated properties\n");

    for (URL propUrl : propUrls) {
      LOGGER.debug("loading properties from " + propUrl);
      String propData;
      try {
        propData = Resources.toString(propUrl, Charsets.UTF_8);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      String propString = ConfigFile.toPropertiesString(System.getenv(), propData);
      debug.append(propString).append("\n");

      Properties props = ConfigFile.loadProperties(System.getenv(), propData);
      LOGGER.debug("loading properties: " + props);
      ConfigurationManager.loadProperties(props);
      LOGGER.info("loaded properties file " + propUrl);
    }

    LOGGER.debug("properties debug: " + debug);
    LOGGER.info("properties applied to ConfigurationManager");
  }
}
