package pers.clare.polarbeartest.service;

import org.springframework.beans.factory.InitializingBean;
import pers.clare.polarbeartest.vo.SimpleUser;

public abstract class AbstractSimpleUserService implements InitializingBean {

    public SimpleUser find(Long id) {
        if (id == null) return null;
        return new SimpleUser(id, "SimpleUser", System.currentTimeMillis());
    }

}
