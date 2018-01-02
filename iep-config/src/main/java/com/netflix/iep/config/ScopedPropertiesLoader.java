/*
 * Copyright 2014-2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.iep.config;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class ScopedPropertiesLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(ScopedPropertiesLoader.class);

  private static final String PROP_PROPERTIES_FILE = "netflix.iep.config.propertiesFile";
  private static final String DEF_PROPERTIES_FILE = "application.scoped.properties";


  public static Properties load() {
    String[] propFiles = System.getProperty(PROP_PROPERTIES_FILE, DEF_PROPERTIES_FILE).split(",");
    return load(propFiles);
  }

  public static Properties load(String[] propFiles) {
    List<URL> propUrls = new ArrayList<>();
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

    Properties finalProps = new Properties();

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
      finalProps.putAll(props);
      LOGGER.info("loaded properties file " + propUrl);
    }

    LOGGER.debug("properties debug: " + debug);
    return finalProps;
  }
}
