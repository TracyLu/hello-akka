package de.stphngrtz.helloakka;

import java.io.Serializable;

public class Event implements Serializable {

    private final String uuid;
    private final String data;

    public Event(String uuid, String data) {
        this.uuid = uuid;
        this.data = data;
    }

    public String getUuid() {
        return uuid;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Event{" +
                "uuid='" + uuid + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}
