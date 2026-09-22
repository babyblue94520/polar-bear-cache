package pers.clare.polarbearcache.event;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class EventDataCodec {
    private static final String encodedPrefix = "~pb64~";

    private EventDataCodec() {
    }

    public static String encode(String value) {
        if (value == null) return "";
        if (!value.startsWith(encodedPrefix) && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        return encodedPrefix + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String value) {
        if (value == null || !value.startsWith(encodedPrefix)) return value;
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(value.substring(encodedPrefix.length()));
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return value;
        }
    }
}
