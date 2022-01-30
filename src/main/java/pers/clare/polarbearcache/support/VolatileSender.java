package pers.clare.polarbearcache.support;

class VolatileSender<T> {
    private final T sender;
    private final long validTime;

    VolatileSender(T sender, long validTime) {
        this.sender = sender;
        this.validTime = validTime;
    }

    public T getSender() {
        return sender;
    }

    public long getValidTime() {
        return validTime;
    }
}
