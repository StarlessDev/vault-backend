package dev.starless.hosting.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import dev.starless.hosting.webserver.Response;

import java.lang.reflect.Type;

public class ResponseAdapter implements JsonSerializer<Response> {

    @Override
    public JsonElement serialize(Response src,
                                 Type typeOfSrc,
                                 JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("status", src.outcome().name().toLowerCase());
        if (src.outcome().equals(Response.Outcome.SUCCESS)) {
            src.values().forEach((key, obj) -> {
                jsonObject.add(key, context.serialize(obj));
            });
        } else {
            jsonObject.addProperty("message", src.errorMessage());
        }
        return jsonObject;
    }
}