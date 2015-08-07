package com.netflix.iep.karyon3;

import com.netflix.archaius.annotations.Configuration;
import com.netflix.archaius.annotations.DefaultValue;

@Configuration(prefix = "netflix.iep.karyon3")
interface AdminConfig {
  @DefaultValue("/resources")
  String uiLocation();
}
