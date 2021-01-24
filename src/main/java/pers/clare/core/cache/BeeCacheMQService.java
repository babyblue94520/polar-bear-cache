package pers.clare.core.cache;


import pers.clare.core.mq.GenericReceiveListener;

public interface BeeCacheMQService {

    String send(String body);

    GenericReceiveListener<String> addListener(GenericReceiveListener<String> listener);
}
