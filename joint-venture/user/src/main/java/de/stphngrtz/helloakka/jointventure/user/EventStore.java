package de.stphngrtz.helloakka.jointventure.user;

import scala.concurrent.duration.Deadline;
import scala.concurrent.duration.FiniteDuration;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class EventStore implements Serializable {

    protected final FiniteDuration processingTimeout;

    protected final Deque<Event> pendingEvents;
    protected final Map<Event, Deadline> processingEvents;
    protected final List<Event> processedEvents;

    /**
     * Öffentlicher Konstruktor zum Erzeugen des EventStore
     */
    public EventStore(FiniteDuration processingTimeout) {
        this.pendingEvents = new ArrayDeque<>();
        this.processingEvents = new LinkedHashMap<>();
        this.processedEvents = new ArrayList<>();
        this.processingTimeout = processingTimeout;
    }

    /**
     * Privater Konstruktor zum Verändern des Zustands, siehe {@link #with(StorableEvent)}
     */
    protected EventStore(Deque<Event> pendingEvents, Map<Event, Deadline> processingEvents, List<Event> processedEvents, FiniteDuration processingTimeout) {
        this.pendingEvents = pendingEvents;
        this.processingEvents = processingEvents;
        this.processedEvents = processedEvents;
        this.processingTimeout = processingTimeout;
    }

    public EventStore with(PendingEvent pendingEvent) {
        Deque<Event> temporaryPendingEvents = new ArrayDeque<>(pendingEvents);
        temporaryPendingEvents.add(pendingEvent.event);

        return new EventStore(temporaryPendingEvents, new LinkedHashMap<>(processingEvents), new ArrayList<>(processedEvents), processingTimeout);
    }

    public EventStore with(ProcessingEvent processingEvent) {
        Deque<Event> temporaryPendingEvents = new ArrayDeque<>(pendingEvents);
        temporaryPendingEvents.remove(processingEvent.event);

        Map<Event, Deadline> temporaryProcessingEvents = new LinkedHashMap<>(processingEvents);
        temporaryProcessingEvents.put(processingEvent.event, Deadline.apply(processingTimeout));

        return new EventStore(temporaryPendingEvents, temporaryProcessingEvents, new ArrayList<>(processedEvents), processingTimeout);
    }

    public EventStore with(ProcessedEvent processedEvent) {
        Map<Event, Deadline> temporaryProcessingEvents = new LinkedHashMap<>(processingEvents);
        temporaryProcessingEvents.remove(processedEvent.event);

        List<Event> temporaryProcessedEvents = new ArrayList<>(processedEvents);
        temporaryProcessedEvents.add(processedEvent.event);

        return new EventStore(new ArrayDeque<>(pendingEvents), temporaryProcessingEvents, temporaryProcessedEvents, processingTimeout);
    }

    public EventStore with(OverdueEvent overdueEvent) {
        Map<Event, Deadline> temporaryProcessingEvents = new LinkedHashMap<>(processingEvents);
        temporaryProcessingEvents.remove(overdueEvent.event);

        Deque<Event> temporaryPendingEvents = new ArrayDeque<>(pendingEvents);
        temporaryPendingEvents.addFirst(overdueEvent.event); // TODO timeout count? damit events, die wiederholt in einen timeout laufen, nicht wieder und wieder an die spitze der queue landen?

        return new EventStore(temporaryPendingEvents, temporaryProcessingEvents, new ArrayList<>(processedEvents), processingTimeout);
    }

    public EventStore with(FailedEvent failedEvent) {
        Map<Event, Deadline> temporaryProcessingEvents = new LinkedHashMap<>(processingEvents);
        temporaryProcessingEvents.remove(failedEvent.event);

        Deque<Event> temporaryPendingEvents = new ArrayDeque<>(pendingEvents);
        temporaryPendingEvents.addFirst(failedEvent.event); // TODO failed count? damit events, die wiederholt fehlschlagen, nicht wieder und wieder an die spitze der queue landen?

        return new EventStore(temporaryPendingEvents, temporaryProcessingEvents, new ArrayList<>(processedEvents), processingTimeout);
    }

    public <T extends StorableEvent> EventStore with(T event) {
        if (event instanceof PendingEvent)
            return with(((PendingEvent) event));
        if (event instanceof ProcessingEvent)
            return with(((ProcessingEvent) event));
        if (event instanceof ProcessedEvent)
            return with(((ProcessedEvent) event));
        if (event instanceof OverdueEvent)
            return with(((OverdueEvent) event));
        if (event instanceof FailedEvent)
            return with(((FailedEvent) event));

        throw new IllegalStateException();
    }

    public EventStore copy() {
        return new EventStore(new ArrayDeque<>(pendingEvents), new LinkedHashMap<>(processingEvents), new ArrayList<>(processedEvents), processingTimeout);
    }

    public boolean hasPendingEvents() {
        return !pendingEvents.isEmpty();
    }

    public Optional<Event> nextPendingEvent() {
        return Optional.ofNullable(pendingEvents.peekFirst());
    }

    public List<Event> getOverdueEvents() {
        LinkedHashMap<Event, Deadline> temporaryProcessingEvents = new LinkedHashMap<>(processingEvents);
        return temporaryProcessingEvents.entrySet().stream().filter(e -> e.getValue().isOverdue()).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        Deque<Event> temporaryPendingEvents = new ArrayDeque<>(pendingEvents);
        Map<Event, Deadline> temporaryProcessingEvents = new LinkedHashMap<>(processingEvents);
        List<Event> temporaryProcessedEvents = new ArrayList<>(processedEvents);

        int limit = 5; // TODO richtige reihenfolge? pending: die 5 nächsten/ältesten, processing und processed die 5 jüngsten

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("EventStore with %d pending events, %d processing events, %d processed events.", temporaryPendingEvents.size(), temporaryProcessingEvents.size(), temporaryProcessedEvents.size()));
        append("Pending", temporaryPendingEvents, limit, sb);
        append("Processing", temporaryProcessingEvents.keySet(), limit, sb);
        append("Processed", temporaryProcessedEvents, limit, sb);
        return sb.toString();
    }

    private static void append(String section, Collection<Event> events, int limit, StringBuilder sb) {
        if (!events.isEmpty()) {
            sb.append(String.format("%n%s:%n", section));
            events.stream().limit(limit).forEach(pendingEvent -> sb.append("  ").append(pendingEvent).append(String.format("%n")));
            if (events.size() > limit)
                sb.append("...").append(String.format("%n"));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventStore that = (EventStore) o;
        return Objects.equals(processingTimeout, that.processingTimeout) &&
                // Hinweis: ArrayDeque überschreibt weder .equals() noch .hashCode(), daher hier Umwandlung zu ArrayList
                Objects.equals(new ArrayList<>(pendingEvents), new ArrayList<>(that.pendingEvents)) &&
                Objects.equals(processingEvents, that.processingEvents) &&
                Objects.equals(processedEvents, that.processedEvents);
    }

    @Override
    public int hashCode() {
        // Hinweis: ArrayDeque überschreibt weder .equals() noch .hashCode(), daher hier Umwandlung zu ArrayList
        return Objects.hash(processingTimeout, new ArrayList<>(pendingEvents), processingEvents, processedEvents);
    }

    /**
     * Abstrakter Wrapper für Events, zur Unterscheidung beim Recover
     */
    public static abstract class StorableEvent<T extends Event> {
        public final T event;

        public StorableEvent(T event) {
            this.event = event;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StorableEvent<?> that = (StorableEvent<?>) o;
            return Objects.equals(event, that.event);
        }

        @Override
        public int hashCode() {
            return Objects.hash(event);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "event=" + event +
                    '}';
        }
    }

    /**
     * Events, die nach {@link EventStore#pendingEvents} wandern
     */
    public static class PendingEvent<T extends Event> extends StorableEvent<T> {
        public PendingEvent(T event) {
            super(event);
        }
    }

    /**
     * Events, die nach {@link EventStore#processingEvents} wandern
     */
    public static class ProcessingEvent<T extends Event> extends StorableEvent<T> {
        public ProcessingEvent(T event) {
            super(event);
        }
    }

    /**
     * Events, die nach {@link EventStore#processedEvents} wandern
     */
    public static class ProcessedEvent<T extends Event> extends StorableEvent<T> {
        public ProcessedEvent(T event) {
            super(event);
        }
    }

    /**
     * Events, dessen Deadline erreicht ist und die somit von {@link #processingEvents} wieder zurück nach {@link #pendingEvents} wandern
     */
    public static class OverdueEvent<T extends Event> extends StorableEvent<T> {
        public OverdueEvent(T event) {
            super(event);
        }
    }

    /**
     * Events, dessen Verarbeitung fehlgeschlagen ist und die somit von {@link #processingEvents} wieder zurück nach {@link #pendingEvents} wandern
     */
    public static class FailedEvent<T extends Event> extends StorableEvent<T> {
        public FailedEvent(T event) {
            super(event);
        }
    }
}
