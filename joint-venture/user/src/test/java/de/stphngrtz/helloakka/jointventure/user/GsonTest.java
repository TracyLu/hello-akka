package de.stphngrtz.helloakka.jointventure.user;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import scala.concurrent.duration.Duration;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class GsonTest {

    private static final Gson gson = new GsonBuilder()
            .serializeNulls()
            .setDateFormat("dd.MM.yyyy HH:mm:ss.SSS")
            .registerTypeAdapter(Event.class, new EventSerializer())
            .registerTypeAdapter(Event.class, new EventDeserializer())
            .create();

    @Test
    public void Serialisieren_und_Deserialisieren_des_UserCreated_Events() throws Exception {
        Event.UserCreated userCreatedEvent = new Event.UserCreated("Dummy");
        assertThat(gson.fromJson(gson.toJson(userCreatedEvent), Event.UserCreated.class), equalTo(userCreatedEvent));
    }

    @Test
    public void Serialisieren_und_Deserialisieren_des_UserUpdated_Events() throws Exception {
        Event.UserUpdated userUpdatedEvent = new Event.UserUpdated(UUID.randomUUID(), "Dummy");
        assertThat(gson.fromJson(gson.toJson(userUpdatedEvent), Event.UserUpdated.class), equalTo(userUpdatedEvent));
    }

    @Test
    public void Serialisieren_und_Deserialisieren_des_UserDeleted_Events() throws Exception {
        Event.UserDeleted userDeletedEvent = new Event.UserDeleted(UUID.randomUUID());
        assertThat(gson.fromJson(gson.toJson(userDeletedEvent), Event.UserDeleted.class), equalTo(userDeletedEvent));
    }

    private static class DummyEvent extends Event {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DummyEvent that = (DummyEvent) o;
            return Objects.equals(eventUuid, that.eventUuid) && Objects.equals(eventDate, that.eventDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventUuid, eventDate);
        }

        @Override
        public String toString() {
            return "DummyEvent{" + "eventUuid=" + eventUuid + ", eventDate=" + eventDate + '}';
        }
    }

    @Test
    public void Serialisieren_und_Deserialisieren_des_PendingEvent_Wrappers() throws Exception {
        Type type = new TypeToken<EventStore.PendingEvent<DummyEvent>>() {
        }.getType();

        EventStore.PendingEvent<DummyEvent> pendingDummyEvent = new EventStore.PendingEvent<>(new DummyEvent());
        assertThat(gson.fromJson(gson.toJson(pendingDummyEvent, type), type), equalTo(pendingDummyEvent));
    }

    @Test
    public void Serialisieren_und_Deserialisieren_des_ProcessingEvent_Wrappers() throws Exception {
        Type type = new TypeToken<EventStore.ProcessingEvent<DummyEvent>>() {
        }.getType();

        EventStore.ProcessingEvent<DummyEvent> processingDummyEvent = new EventStore.ProcessingEvent<>(new DummyEvent());
        assertThat(gson.fromJson(gson.toJson(processingDummyEvent, type), type), equalTo(processingDummyEvent));
    }

    @Test
    public void Serialisieren_und_Deserialisieren_des_ProcessedEvent_Wrappers() throws Exception {
        Type type = new TypeToken<EventStore.ProcessedEvent<DummyEvent>>() {
        }.getType();

        EventStore.ProcessedEvent<DummyEvent> processedDummyEvent = new EventStore.ProcessedEvent<>(new DummyEvent());
        assertThat(gson.fromJson(gson.toJson(processedDummyEvent, type), type), equalTo(processedDummyEvent));
    }

    @Test
    public void Serialisieren_und_Deserialisieren_des_OverdueEvent_Wrappers() throws Exception {
        Type type = new TypeToken<EventStore.OverdueEvent<DummyEvent>>() {
        }.getType();

        EventStore.OverdueEvent<DummyEvent> overdueDummyEvent = new EventStore.OverdueEvent<>(new DummyEvent());
        assertThat(gson.fromJson(gson.toJson(overdueDummyEvent, type), type), equalTo(overdueDummyEvent));
    }

    @Test
    public void Serialisieren_und_Deserialisieren_des_FailedEvent_Wrappers() throws Exception {
        Type type = new TypeToken<EventStore.FailedEvent<DummyEvent>>() {
        }.getType();

        EventStore.FailedEvent<DummyEvent> failedDummyEvent = new EventStore.FailedEvent<>(new DummyEvent());
        assertThat(gson.fromJson(gson.toJson(failedDummyEvent, type), type), equalTo(failedDummyEvent));
    }

    @Test
    public void Serialisieren_und_Deserialisieren_des_EventStore() throws Exception {
        EventStore eventStore = new EventStore(Duration.create(10, TimeUnit.SECONDS));
        EventStore.PendingEvent<Event.UserCreated> event = new EventStore.PendingEvent<>(new Event.UserCreated("hans"));
        eventStore = eventStore.with(event);

        System.out.println(gson.toJson(eventStore));
        assertThat(gson.fromJson(gson.toJson(eventStore), EventStore.class), equalTo(eventStore));
    }

    private static class EventSerializer implements JsonSerializer<Event> {

        @Override
        public JsonElement serialize(Event src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("e", gson.toJson(src));
            jsonObject.addProperty("t", src.getClass().getName());
            return jsonObject;
        }
    }

    private static class EventDeserializer implements JsonDeserializer<Event> {

        @Override
        public Event deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            String e = jsonObject.get("e").getAsString();
            String t = jsonObject.get("t").getAsString();

            try {
                return (Event) gson.fromJson(e, Class.forName(t));
            } catch (ClassNotFoundException e1) {
                throw new RuntimeException(e1);
            }
        }
    }
}
