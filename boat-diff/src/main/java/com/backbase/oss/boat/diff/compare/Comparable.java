package com.backbase.oss.boat.diff.compare;

public interface Comparable<T> {

    boolean compare(T left, T right);
}
