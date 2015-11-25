package com.netflix.iep.guice;

import com.google.inject.AbstractModule;

import java.lang.reflect.Constructor;

/**
 * Base class for modules that provides some additional helper methods.
 */
public abstract class BaseModule extends AbstractModule {

  /**
   * Returns the only constructor for class {code}cls{code}. If the class has more than one
   * constructor, then an IllegalArgumentException will be thrown. This is typically used
   * for creating a quick constructor binding on a class that doesn't have an explicit
   * Inject annotation.
   */
  protected <T> Constructor<T> getConstructor(Class<? extends T> cls) {
    Constructor<?>[] constructors = cls.getDeclaredConstructors();
    if (constructors.length != 1) {
      final String msg = "a single constructor is required, class " +
          cls.getName() + " has " + constructors.length + " constructors";
      throw new IllegalArgumentException(msg);
    }
    return (Constructor<T>) constructors[0];
  }
}
