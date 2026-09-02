package me.drex.polymerpatcher.dump.data.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.util.SystemProperties;

import java.lang.reflect.Type;

public class ClassAdapter implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {
    private static final boolean DISABLE_ENVIRONMENT_STRIP = SystemProperties.isSet(SystemProperties.DISABLE_ENVIRONMENT_STRIP);
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
        } catch (RuntimeException e) {
            // fabric-loader throws RuntimeException in FabricTransformer.java when loading classes with CLIENT
            // environment on the server
            // Cannot load class {} in environment type SERVER
            if (!DISABLE_ENVIRONMENT_STRIP) {
                throw new RuntimeException("You need to disable environment stripping, please disable environment stripping by adding \"-Dfabric.disableEnvironmentStrip=1\" to your jvm arguments like this: \"java -jar -Dfabric.disableEnvironmentStrip=1 server.jar\"");
            }
            throw e;
        }
    }
}
