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
package com.netflix.iep.launcher;

public class Settings {
  private static final String PREFIX = "netflix.iep.launcher.";
  public static final String MAIN_CLASS = PREFIX + "mainClass";
  public static final String WORKING_DIR = PREFIX + "workingDir";
  public static final String CLEAN_WORKING_DIR = PREFIX + "cleanWorkingDir";
  public static final String LOGGING_ENABLED = PREFIX + "loggingEnabled";
}
