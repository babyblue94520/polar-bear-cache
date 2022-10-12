package pers.clare.polarbeartest.service;

import pers.clare.polarbeartest.vo.ReloadUser;
import pers.clare.polarbeartest.vo.User;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractUserService {

    protected ReloadUser reloadUser;

    public void setReloadUser(ReloadUser reloadUser) {
        this.reloadUser = reloadUser;
    }

    public List<User> findAll() {
        return new ArrayList<>();
    }

    public User find(Long id) {
        if (id == null) return null;
        return new User(id, "User", System.currentTimeMillis());
    }

    public ReloadUser findReload(Long id) {
        if (id == null) return null;
        return new ReloadUser(id, "ReloadUser", System.currentTimeMillis());
    }

    public User findSync(Long id) {
        if (id == 1) return null;
        return new User(id, "UserSync", System.currentTimeMillis());
    }

    public User findNull(Long id) {
        return null;
    }

    public User insert() {
        return new User(System.currentTimeMillis(), "User", System.currentTimeMillis());
    }

    public User update(Long id) {
        if (id == null) return null;
        return new User(id, "User", System.currentTimeMillis());
    }

    public User updatePut(Long id) {
        if (id == null) return null;
        return new User(id, "User", System.currentTimeMillis());
    }

    public User updatePutResultKey(Long id){
        return updatePut(id);
    }

    abstract public User findCaching(Long id);

    abstract public User updateCachingPut(Long id);

    abstract public void clear();
}
