/*
 * Copyright 2014-2024 Netflix, Inc.
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
package com.netflix.iep.admin;

import java.util.Objects;

public class EndpointMapping {

  private final String path;
  private final Object object;

  public EndpointMapping(String path, Object object) {
    this.path = path;
    this.object = object;
  }

  public String getPath() {
    return path;
  }

  public Object getObject() {
    return object;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EndpointMapping that)) return false;
    return path.equals(that.path) && object.equals(that.object);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, object);
  }
}
