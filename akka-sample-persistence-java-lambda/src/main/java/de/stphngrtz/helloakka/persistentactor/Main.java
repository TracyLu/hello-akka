package de.stphngrtz.helloakka.persistentactor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class Main {

    /**
     * https://github.com/akka/akka/tree/master/akka-samples/akka-sample-persistence-java-lambda
     */
    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("example");
        ActorRef persistentActor = system.actorOf(MyPersistentActor.props(), "my-persistent-actor");
        persistentActor.tell(new Protocol.Cmd("foo"), ActorRef.noSender());
        persistentActor.tell(new Protocol.Cmd("baz"), ActorRef.noSender());
        persistentActor.tell(new Protocol.Cmd("bar"), ActorRef.noSender());
        persistentActor.tell("snap", ActorRef.noSender());
        persistentActor.tell(new Protocol.Cmd("buzz"), ActorRef.noSender());
        persistentActor.tell("print", ActorRef.noSender());

        Thread.sleep(1000);
        system.terminate();

        // 1st: [foo_0, foo_1, baz_2, baz_3, bar_4, bar_5, buzz_6, buzz_7]
        // 2nd: [foo_0, foo_1, baz_2, baz_3, bar_4, bar_5, buzz_6, buzz_7, foo_8, foo_9, baz_10, baz_11, bar_12, bar_13, buzz_14, buzz_15]
        // 3rd: [foo_0, foo_1, baz_2, baz_3, bar_4, bar_5, buzz_6, buzz_7, foo_8, foo_9, baz_10, baz_11, bar_12, bar_13, buzz_14, buzz_15, foo_16, foo_17, baz_18, baz_19, bar_20, bar_21, buzz_22, buzz_23]
    }
}
