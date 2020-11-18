package pers.clare.demo.config;


import pers.clare.core.cache.BeeCacheMQService;
import pers.clare.core.mq.AbstractGenericMQService;
import pers.clare.core.mq.GenericReceiveListener;
import pers.clare.core.mq.MQService;

public class BeeCacheMQServiceImpl extends AbstractGenericMQService<String> implements BeeCacheMQService {

    public BeeCacheMQServiceImpl(
            MQService mqService
            , String topic
    ) {
        super(mqService, topic);
    }
}
