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
package com.netflix.iep.admin;

/**
 * Represents an error response that should be sent to the user.
 */
public class ErrorMessage {

  private final int status;
  private final String msg;

  /**
   * Create a new instance.
   *
   * @param status
   *     HTTP status code to send to the user.
   * @param msg
   *     Message to add into the payload explaining the problem.
   */
  public ErrorMessage(int status, String msg) {
    this.status = status;
    this.msg = msg;
  }

  /**
   * Create a new instance based on an exception. The status code will be 500.
   *
   * @param t
   *     Throwable to use for constructing the response message.
   */
  public ErrorMessage(Throwable t) {
    this(500, t);
  }

  /**
   * Create a new instance.
   *
   * @param status
   *     HTTP status code to send to the user.
   * @param t
   *     Throwable to use for constructing the response message.
   */
  public ErrorMessage(int status, Throwable t) {
    this(status, t.getClass().getSimpleName() + ": " + t.getMessage());
  }

  /** HTTP status code. */
  public int getStatus() {
    return status;
  }

  /** Message to add into the payload explaining the problem. */
  public String getMessage() {
    return msg;
  }
}
