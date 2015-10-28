package de.stphngrtz.helloakka.persistentactorfailure;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("example");
        ActorRef persistentFailureActor = system.actorOf(MyPersistentFailureActor.props(), "my-persistent-failure-actor");

        persistentFailureActor.tell("a", ActorRef.noSender());
        persistentFailureActor.tell("print", ActorRef.noSender());
        persistentFailureActor.tell("boom", ActorRef.noSender());
        persistentFailureActor.tell("print", ActorRef.noSender());
        persistentFailureActor.tell("b", ActorRef.noSender());
        persistentFailureActor.tell("print", ActorRef.noSender());
        persistentFailureActor.tell("c", ActorRef.noSender());
        persistentFailureActor.tell("print", ActorRef.noSender());

        Thread.sleep(1000);
        system.terminate();

        // 1st: [a] -> [a, b] -> [a, b, c]
        // 2nd: [a, b, c, a] -> [a, b, c, a, b] -> [a, b, c, a, b, c]
    }
}
