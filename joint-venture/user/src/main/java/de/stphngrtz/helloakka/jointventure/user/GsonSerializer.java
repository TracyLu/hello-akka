package de.stphngrtz.helloakka.jointventure.user;

import akka.serialization.JSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonSerializer extends JSerializer {

    private static final Gson gson = new GsonBuilder()
            // TODO DateFormat & TypeAdapter, siehe GsonTest
            .serializeNulls()
            .create();

    @Override
    public int identifier() {
        return 12062010;
    }

    @Override
    public boolean includeManifest() {
        return true;
    }

    @Override
    public Object fromBinaryJava(byte[] bytes, Class<?> manifest) {
        return gson.fromJson(new String(bytes), manifest);
    }

    @Override
    public byte[] toBinary(Object o) {
        return gson.toJson(o).getBytes();
    }
}
