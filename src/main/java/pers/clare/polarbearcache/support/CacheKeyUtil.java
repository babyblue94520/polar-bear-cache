package pers.clare.polarbearcache.support;

import java.util.regex.Pattern;

public class CacheKeyUtil {

    public static boolean isRegex(String key) {
        if (key != null && key.length() > 6) {
            char[] cs = key.toCharArray();
            if (cs[0] == 'r' && cs[1] == 'e' && cs[2] == 'g' && cs[3] == 'e' && cs[4] == 'x' && cs[5] == ':')
                return true;
        }
        return false;
    }

    public static Pattern getPattern(String key) {
        if (isRegex(key)) {
            return Pattern.compile(key.substring(6));
        }
        return null;
    }
}
