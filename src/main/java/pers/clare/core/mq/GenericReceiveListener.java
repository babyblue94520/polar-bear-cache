package pers.clare.core.mq;

@FunctionalInterface
public interface GenericReceiveListener<T> {
    void accept(String origin, T data);
}
