package pers.clare.core.json;

import com.fasterxml.jackson.core.type.TypeReference;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class RewriteTypeReference<T> extends TypeReference<T> {

    protected RewriteTypeReference(Type type) {
        super();
        try {
            Field field = TypeReference.class.getDeclaredField("_type");
            field.setAccessible(true);
            field.set(this,type);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
