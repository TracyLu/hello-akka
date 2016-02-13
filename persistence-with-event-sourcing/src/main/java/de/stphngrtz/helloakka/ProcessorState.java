package de.stphngrtz.helloakka;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ProcessorState implements Serializable {

    private final List<String> events;

    public ProcessorState(List<String> events) {
        this.events = events;
    }

    public ProcessorState() {
        this.events = new ArrayList<>();
    }

    @Override
    public String toString() {
        return events.toString();
    }

    public ProcessorState with(Event event) {
        List<String> newEvents = new ArrayList<>();
        newEvents.addAll(events);
        newEvents.add(event.toString());

        return new ProcessorState(newEvents);
    }

    public ProcessorState copy() {
        return new ProcessorState(events);
    }
}
