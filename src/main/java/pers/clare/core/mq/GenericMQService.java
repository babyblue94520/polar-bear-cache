package pers.clare.core.mq;

public interface GenericMQService<T> {

    public T send(T body);

    public GenericReceiveListener<T> addListener(GenericReceiveListener<T> listener);
}
