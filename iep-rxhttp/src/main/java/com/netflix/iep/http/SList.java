package com.netflix.iep.http;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

interface SList<T> extends Iterable<T> {

  SList<T> prepend(T v);

  boolean isEmpty();

  T head();

  SList<T> tail();

  class Nil<V> implements SList<V> {

    @Override public SList<V> prepend(V v) {
      return new Node<>(v, this);
    }

    @Override public boolean isEmpty() {
      return true;
    }

    @Override public V head() {
      throw new NoSuchElementException();
    }

    @Override public SList<V> tail() {
      return null;
    }

    @Override public Iterator<V> iterator() {
      return Collections.emptyIterator();
    }
  }

  class Node<V> implements SList<V> {

    private V item;
    private SList<V> next;

    Node(V item, SList<V> next) {
      this.item = item;
      this.next = next;
    }

    @Override public SList<V> prepend(V v) {
      return new Node<>(v, this);
    }

    @Override public boolean isEmpty() {
      return false;
    }

    @Override public V head() {
      return item;
    }

    @Override public SList<V> tail() {
      return next;
    }

    @Override
    public Iterator<V> iterator() {
      final SList<V> start = this;
      return new Iterator<V>() {
        private SList<V> pos = start;

        @Override public boolean hasNext() {
          return !pos.isEmpty();
        }

        @Override public V next() {
          V v = pos.head();
          pos = pos.tail();
          return v;
        }

        @Override public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }
}
