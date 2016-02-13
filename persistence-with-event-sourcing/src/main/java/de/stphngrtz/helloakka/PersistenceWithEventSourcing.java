package de.stphngrtz.helloakka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class PersistenceWithEventSourcing {

    /**
     * https://github.com/royrusso/akka-java-examples#akka-persistence-with-eventsourcing
     */
    public static void main(String[] args) throws Exception {
        ActorSystem actorSystem = ActorSystem.create("server");

        ActorRef eventHandler = actorSystem.actorOf(EventHandler.props());
        actorSystem.eventStream().subscribe(eventHandler, Event.class);

        Thread.sleep(5000);

        ActorRef baseProcessor = actorSystem.actorOf(Processor.props());

        baseProcessor.tell(new Command("1"), ActorRef.noSender());
        baseProcessor.tell(new Command("2"), ActorRef.noSender());
        baseProcessor.tell(new Command("3"), ActorRef.noSender());
        baseProcessor.tell("snapshot", ActorRef.noSender());
        baseProcessor.tell(new Command("4"), ActorRef.noSender());
        baseProcessor.tell(new Command("5"), ActorRef.noSender());
        baseProcessor.tell("printstate", ActorRef.noSender());

        Thread.sleep(5000);
        actorSystem.shutdown();
    }
}
