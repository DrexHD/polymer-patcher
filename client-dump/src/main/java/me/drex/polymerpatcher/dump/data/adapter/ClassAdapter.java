package me.drex.polymerpatcher.dump.data.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Type;

public class ClassAdapter implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {
    private static final String STORAGE_NAMESPACE = "intermediary";

    @Override
    public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
        String className = FabricLoader.getInstance().getMappingResolver().unmapClassName(STORAGE_NAMESPACE, src.getName());
        return new JsonPrimitive(className);
    }

    @Override
    public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {

        try {
            String className = FabricLoader.getInstance().getMappingResolver().mapClassName(STORAGE_NAMESPACE, json.getAsString());
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
