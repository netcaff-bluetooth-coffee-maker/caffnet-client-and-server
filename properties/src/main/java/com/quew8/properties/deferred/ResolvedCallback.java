package com.quew8.properties.deferred;

/**
 * @author Quew8
 */
public interface ResolvedCallback<T> {
    void resolved(T data);
}