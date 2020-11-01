package pers.clare.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pers.clare.core.cache.BeeCacheMQService;
import pers.clare.core.mq.AbstractGenericMQService;
import pers.clare.core.mq.MQService;


@Service
public class BeeCacheMQServiceImpl extends AbstractGenericMQService<String> implements BeeCacheMQService {

    public BeeCacheMQServiceImpl(@Value("${cache.notify.topic:default}") String topic) {
        super(topic);
    }

    @Autowired
    @Override
    public void init(MQService mqService) {
        super.init(mqService);
    }
}
