package pers.clare.polarbeartest.vo;


public class SimpleUser {
    private Long id;
    private String name;
    private Long time;

    public SimpleUser(Long id, String name, Long time) {
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
