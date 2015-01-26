package com.netflix.iep.gov;

/**
 * Simple main for apps that don't need to do anything other than startup governator with a
 * set of modules.
 */
public final class Main {

  private Main() {
  }

  public static void main(String[] args) throws Exception {
    Governator.getInstance().start();
    Governator.addShutdownHook();
  }
}
