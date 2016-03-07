package de.stphngrtz.helloakka.jointventure.user;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public abstract class Event implements Serializable {
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");

    protected UUID eventUuid = UUID.randomUUID();
    protected Date eventDate = new Date();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(eventUuid, event.eventUuid) &&
                Objects.equals(eventDate, event.eventDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventUuid, eventDate);
    }

    public static class UserCreated extends Event {
        public final String name;

        public UserCreated(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserCreated that = (UserCreated) o;
            return Objects.equals(name, that.name)
                    && Objects.equals(eventUuid, that.eventUuid)
                    && Objects.equals(eventDate, that.eventDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, eventUuid, eventDate);
        }

        @Override
        public String toString() {
            return "UserCreated{" +
                    "name='" + name + '\'' +
                    ", eventUuid=" + eventUuid +
                    ", eventDate=" + FORMAT.format(eventDate) +
                    '}';
        }
    }

    public static class UserUpdated extends Event {
        public final UUID uuid;
        public final String name;

        public UserUpdated(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserUpdated that = (UserUpdated) o;
            return Objects.equals(uuid, that.uuid)
                    && Objects.equals(name, that.name)
                    && Objects.equals(eventUuid, that.eventUuid)
                    && Objects.equals(eventDate, that.eventDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid, name, eventUuid, eventDate);
        }

        @Override
        public String toString() {
            return "UserUpdated{" +
                    "uuid='" + uuid + '\'' +
                    ", name='" + name + '\'' +
                    ", eventUuid=" + eventUuid +
                    ", eventDate=" + FORMAT.format(eventDate) +
                    '}';
        }
    }

    public static class UserDeleted extends Event {
        public final UUID uuid;

        public UserDeleted(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserDeleted that = (UserDeleted) o;
            return Objects.equals(uuid, that.uuid)
                    && Objects.equals(eventUuid, that.eventUuid)
                    && Objects.equals(eventDate, that.eventDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid, eventUuid, eventDate);
        }

        @Override
        public String toString() {
            return "UserDeleted{" +
                    "uuid='" + uuid + '\'' +
                    ", eventUuid=" + eventUuid +
                    ", eventDate=" + FORMAT.format(eventDate) +
                    '}';
        }
    }
}
