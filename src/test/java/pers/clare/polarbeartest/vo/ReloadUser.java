package pers.clare.polarbeartest.vo;


public class ReloadUser {
    private Long id;
    private String name;
    private Long time;

    public ReloadUser(Long id, String name, Long time) {
        this.id = id;
        this.name = name;
        this.time = time;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getTime() {
        return time;
    }
}
