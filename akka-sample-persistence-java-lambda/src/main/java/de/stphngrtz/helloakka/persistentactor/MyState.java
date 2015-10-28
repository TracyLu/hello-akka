package de.stphngrtz.helloakka.persistentactor;

import java.io.Serializable;
import java.util.ArrayList;

public class MyState implements Serializable {
    private final ArrayList<String> events;

    public MyState() {
        this(new ArrayList<>());
    }

    public MyState(ArrayList<String> events) {
        this.events = events;
    }

    public MyState copy() {
        return new MyState(events);
    }

    public MyState update(Protocol.Evt evt) {
        ArrayList<String> updatedEvents = new ArrayList<>(this.events);
        updatedEvents.add(evt.getData());
        return new MyState(updatedEvents);
    }

    @Override
    public String toString() {
        return events.toString();
    }

    public int size() {
        return events.size();
    }
}
