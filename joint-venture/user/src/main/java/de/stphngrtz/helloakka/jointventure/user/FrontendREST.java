package de.stphngrtz.helloakka.jointventure.user;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.google.gson.Gson;
import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static spark.Spark.*;

public class FrontendREST extends AbstractActor {

    public static Props props(ActorRef processor, MongoDatabase db) {
        return Props.create(FrontendREST.class, processor, db);
    }

    private static final int OK = 200;
    private static final int BAD_REQUEST = 400;
    private static final String JSON = "application/json";

    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    public FrontendREST(ActorRef processor, MongoDatabase db) {
        receive(ReceiveBuilder.matchAny(message -> log.debug(message.toString())).build());

        MongoCollection<Document> users = db.getCollection("users");
        final Gson gson = new Gson();

        get("/", (request, response) -> {
            response.status(OK);
            response.type(JSON);
            return "Joint Venture!";
        });

        get("/state", (request, response) -> {

            MongoCollection<Document> journal = db.getCollection("akka_persistence_journal");
            System.out.println("Journal:");
            journal.find().forEach((Block) b -> {
                System.out.println(b);
            });

            MongoCollection<Document> snaps = db.getCollection("akka_persistence_snaps");
            System.out.println("Snaps:");
            snaps.find().forEach((Block) b -> {
                System.out.println(b);
            });

            /*
            .309] [JointVenture-ProcessorSystem-akka.actor.default-dispatcher-2] [akka://JointVenture-ProcessorSystem/user/$a] Persistence failure when replaying events for persistenceId [Processor-1]. Last known sequence number [0]
java.lang.IllegalArgumentException:

Can not set final de.stphngrtz.helloakka.jointventure.user.Event field
    de.stphngrtz.helloakka.jointventure.user.EventStore$StorableEvent.event

    to com.google.gson.internal.LinkedTreeMap
	at sun.reflect.UnsafeFieldAccessorImpl.throwSetIllegalArgumentException(UnsafeFieldAccessorImpl.java:167)
	at sun.reflect.UnsafeFieldAccessorImpl.throwSetIllegalArgumentException(UnsafeFieldAccessorImpl.java:171)
	at sun.reflect.UnsafeQualifiedObjectFieldAccessorImpl.set(UnsafeQualifiedObjectFieldAccessorImpl.java:83)
	at java.lang.reflect.Field.set(Field.java:764)
	at com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$1.read(ReflectiveTypeAdapterFactory.java:118)
	at com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$Adapter.read(ReflectiveTypeAdapterFactory.java:216)
	at com.google.gson.Gson.from
             */

            processor.tell(new Processor.Protocol.PrintState(), ActorRef.noSender());
            response.status(OK);
            response.type(JSON);
            return "Joint Venture!";
        });

        post("/user", (request, response) -> {
            User user = gson.fromJson(request.body(), User.class);
            processor.tell(new Processor.Protocol.CreateUser(user.name), ActorRef.noSender());

            response.status(OK);
            response.type(JSON);
            return "";
        });

        get("/user", (request, response) -> {
            List<User> userList = new ArrayList<>();
            users.find().map(User::fromDocument).into(userList);

            response.status(OK);
            response.type(JSON);
            return userList;
        }, gson::toJson);

        get("/user/:uuid", (request, response) -> {
            try {
                UUID uuid = UUID.fromString(request.params(":id"));

                List<User> userList = new ArrayList<>();
                users.find(Filters.eq("uuid", uuid)).map(User::fromDocument).into(userList);
                // TODO noch offene Events anwenden!

                if (userList.isEmpty()) {
                    response.status(BAD_REQUEST);
                    response.type(JSON);
                    return "";
                } else {
                    response.status(OK);
                    response.type(JSON);
                    return userList.get(0);
                }
            } catch (IllegalArgumentException e) {
                response.status(BAD_REQUEST);
                response.type(JSON);
                return "";
            }
        }, gson::toJson);

        post("/user/:uuid", (request, response) -> {
            try {
                UUID uuid = UUID.fromString(request.params(":uuid"));
                User user = gson.fromJson(request.body(), User.class);
                processor.tell(new Processor.Protocol.UpdateUser(uuid, user.name), ActorRef.noSender());
                response.status(OK);
                return "";
            } catch (IllegalArgumentException e) {
                response.status(BAD_REQUEST);
                return "";
            }
        });

        delete("/user/:uuid", (request, response) -> {
            try {
                UUID uuid = UUID.fromString(request.params(":uuid"));
                processor.tell(new Processor.Protocol.DeleteUser(uuid), ActorRef.noSender());
                response.status(OK);
                return "";
            } catch (IllegalArgumentException e) {
                response.status(BAD_REQUEST);
                return "";
            }
        });
    }
}
