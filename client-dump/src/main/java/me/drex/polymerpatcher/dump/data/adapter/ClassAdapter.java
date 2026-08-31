package me.drex.polymerpatcher.dump.data.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class ClassAdapter implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

    @Override
    public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getName());
    }

    @Override
    public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {

        try {
            return Class.forName(json.getAsString());
        } catch (ClassNotFoundException e) {
            return null;
//            throw new JsonParseException("Failed to load class: " + json.getAsString(), e);
        }
    }
}
