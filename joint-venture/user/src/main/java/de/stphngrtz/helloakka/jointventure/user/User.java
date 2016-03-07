package de.stphngrtz.helloakka.jointventure.user;

import org.bson.Document;

import java.util.*;

public class User {

    public final UUID uuid;
    public final String name;
    public final List<UUID> eventUuids;

    public User(UUID uuid, String name, List<UUID> eventUuids) {
        if (uuid == null || name == null || eventUuids == null || eventUuids.isEmpty())
            throw new IllegalStateException();

        this.uuid = uuid;
        this.name = name;
        this.eventUuids = eventUuids;
    }

    public User with(Event.UserUpdated event) {
        List<UUID> eventUuids = new ArrayList<>();
        eventUuids.addAll(this.eventUuids);
        eventUuids.add(event.eventUuid);

        return new User(uuid, Optional.ofNullable(event.name).orElse(name), eventUuids);
    }

    public Document toDocument() {
        return new Document()
                .append("uuid", uuid)
                .append("name", name)
                .append("eventUuids", eventUuids);
    }

    public static User fromEvent(Event.UserCreated event) {
        return new User(UUID.randomUUID(), event.name, Collections.singletonList(event.eventUuid));
    }

    public static User fromEvent(Event.UserUpdated event) {
        return new User(event.uuid, event.name, Collections.singletonList(event.eventUuid));
    }

    @SuppressWarnings("unchecked")
    public static User fromDocument(Document document) {
        return new User((UUID) document.get("uuid"), document.getString("name"), (List<UUID>) document.get("eventUuids"));
    }
}
