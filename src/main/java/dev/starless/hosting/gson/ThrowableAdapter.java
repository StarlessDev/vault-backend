package dev.starless.hosting.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class ThrowableAdapter extends TypeAdapter<Throwable> {

    @Override
    public void write(JsonWriter out, Throwable value) throws IOException {
        if (value == null) {
            out.value("Unknown exception");
        } else {
            out.value(value.getMessage());
        }
    }

    @Override
    public Throwable read(JsonReader in) throws IOException {
        return new Exception(in.nextString());
    }
}
