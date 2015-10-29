package de.stphngrtz.helloakka.view;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class Main {

    /**
     * https://github.com/akka/akka/tree/master/akka-samples/akka-sample-persistence-java-lambda
     */
    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("example");
        ActorRef persistentActor = system.actorOf(MyPersistentActor.props(), "my-persistent-actor");
        ActorRef view = system.actorOf(MyPersistentView.props(), "my-view");

        system.scheduler().schedule(
                Duration.Zero(),
                Duration.create(2, TimeUnit.SECONDS),
                persistentActor,
                "scheduled",
                system.dispatcher(),
                ActorRef.noSender()
        );

        system.scheduler().schedule(
                Duration.Zero(),
                Duration.create(5, TimeUnit.SECONDS),
                view,
                "snap",
                system.dispatcher(),
                ActorRef.noSender()
        );

        Thread.sleep(10000);
        system.terminate();
    }
}
