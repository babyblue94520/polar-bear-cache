package pers.clare.polarbeartest.service;

import pers.clare.polarbeartest.vo.SimpleUser;
import org.springframework.beans.factory.InitializingBean;

public abstract class AbstractSimpleUserService implements InitializingBean {

    public SimpleUser find(Long id) {
        if (id == null) return null;
        return new SimpleUser(id, "SimpleUser", System.currentTimeMillis());
    }

}
