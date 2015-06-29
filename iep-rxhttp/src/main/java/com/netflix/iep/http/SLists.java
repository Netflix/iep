package com.netflix.iep.http;

import java.util.Iterator;

final class SLists {

  private static final SList<?> EMPTY = new SList.Nil<>();

  private SLists() {
  }

  @SuppressWarnings("unchecked")
  static <T> SList<T> empty() {
    return (SList<T>) EMPTY;
  }

  static <T> SList<T> create(Iterator<T> iter) {
    SList<T> data = empty();
    while (iter.hasNext()) {
      data = data.prepend(iter.next());
    }
    return data;
  }

  static <T> SList<T> create(Iterable<T> vs) {
    return create(vs.iterator());
  }
}
