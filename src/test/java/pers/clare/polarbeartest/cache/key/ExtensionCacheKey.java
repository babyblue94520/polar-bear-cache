package pers.clare.polarbeartest.cache.key;

public interface ExtensionCacheKey {
    String Prefix = "e_";
    String User = Prefix + "user";
    String UserSync = Prefix + "user_sync";
    String AllUser = Prefix + "all_user";
    String SimpleUser = Prefix + "simple_user";
    String ReloadUser = Prefix + "reload_user";
}
