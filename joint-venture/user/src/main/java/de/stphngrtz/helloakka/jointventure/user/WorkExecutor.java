package de.stphngrtz.helloakka.jointventure.user;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

public class WorkExecutor extends AbstractActor {

    public static class Protocol {

        public static class Work<T extends Event> {
            public final T event;

            public Work(T event) {
                this.event = event;
            }
        }
    }

    public static Props props(MongoDatabase db) {
        return Props.create(WorkExecutor.class, db);
    }

    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    public WorkExecutor(MongoDatabase db) {
        MongoCollection<Document> users = db.getCollection("users");

        receive(ReceiveBuilder
                .match(Protocol.Work.class, command -> command.event instanceof Event.UserCreated, command -> {
                    log.debug("Command: {}, Event: {}", command.getClass().getSimpleName(), command.event);

                    Event.UserCreated userCreated = (Event.UserCreated) command.event;
                    User user = User.fromEvent(userCreated);

                    users.insertOne(user.toDocument());
                    sender().tell(new Worker.Protocol.WorkIsDone<>(userCreated), self());
                })
                .match(Protocol.Work.class, command -> command.event instanceof Event.UserUpdated, command -> {
                    log.debug("Command: {}, Event: {}", command.getClass().getSimpleName(), command.event);

                    Event.UserUpdated userUpdated = (Event.UserUpdated) command.event;
                    Document d = users.find(Filters.eq("uuid", userUpdated.uuid)).first();
                    User user = (d == null) ? User.fromEvent(userUpdated) : User.fromDocument(d).with(userUpdated);

                    users.replaceOne(Filters.eq("uuid", userUpdated.uuid), user.toDocument());
                    sender().tell(new Worker.Protocol.WorkIsDone<>(userUpdated), self());
                })
                .match(Protocol.Work.class, command -> command.event instanceof Event.UserDeleted, command -> {
                    log.debug("Command: {}, Event: {}", command.getClass().getSimpleName(), command.event);

                    Event.UserDeleted userDeleted = (Event.UserDeleted) command.event;

                    users.deleteOne(Filters.eq("uuid", userDeleted.uuid));
                    sender().tell(new Worker.Protocol.WorkIsDone<>(userDeleted), self());
                })
                .build()
        );
    }
}
