package pers.clare.polarbearcache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "polar-bear-cache")
public class PolarBearCacheProperties {

    /** Cache alive duration. */
    private Duration duration;

    /** Extend the effective time of each use. */
    private boolean extension = false;

    private String topic = "polar.bear.cache";

    private long effectiveTime = 0;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Duration getDuration() {
        return duration;
    }

    public PolarBearCacheProperties setDuration(Duration duration) {
        this.duration = duration;
        this.effectiveTime = duration.toMillis();
        return this;
    }

    public boolean isExtension() {
        return extension;
    }

    public PolarBearCacheProperties setExtension(boolean extension) {
        this.extension = extension;
        return this;
    }

    public long getEffectiveTime() {
        return effectiveTime;
    }
}
