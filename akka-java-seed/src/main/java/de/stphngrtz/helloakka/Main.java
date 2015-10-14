package de.stphngrtz.helloakka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {

    /**
     * https://github.com/typesafehub/activator-akka-java-seed
     */

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("MyPingPongActorSystem");
        ActorRef pingActor = system.actorOf(PingActor.props(), "pingActor");
        pingActor.tell(new PingActor.Initialize(), null);

        system.awaitTermination();
    }
}
