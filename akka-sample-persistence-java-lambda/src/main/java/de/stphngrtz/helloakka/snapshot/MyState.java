package de.stphngrtz.helloakka.snapshot;

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

    public MyState update(String event) {
        ArrayList<String> updatedEvents = new ArrayList<>(this.events);
        updatedEvents.add(event);
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
