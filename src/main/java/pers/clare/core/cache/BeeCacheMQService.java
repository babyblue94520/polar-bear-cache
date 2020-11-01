package pers.clare.core.cache;


import pers.clare.core.mq.GenericReceiveListener;

public interface BeeCacheMQService{

    public String send(String body);

    public GenericReceiveListener<String> addListener(GenericReceiveListener<String> listener);
}
