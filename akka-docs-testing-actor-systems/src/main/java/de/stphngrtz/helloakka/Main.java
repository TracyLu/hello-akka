package de.stphngrtz.helloakka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class Main {

    /**
     * http://doc.akka.io/docs/akka/2.4.0/java/testing.html
     */
    public static void main(String[] args) throws Exception {
        ActorSystem system = ActorSystem.create("simpleSystem");
        ActorRef simpleActor1 = system.actorOf(SimpleActor.props(), "simpleActor1");
        ActorRef simpleActor2 = system.actorOf(SimpleActor.props(), "simpleActor2");

        Future<Object> ask1 = Patterns.ask(simpleActor1, simpleActor2, new Timeout(5, TimeUnit.SECONDS));
        Object result1 = Await.result(ask1, Duration.create(5, TimeUnit.SECONDS));
        System.out.println(result1);

        Future<Object> ask2 = Patterns.ask(simpleActor1, "hello", new Timeout(5, TimeUnit.SECONDS));
        Object result2 = Await.result(ask2, Duration.create(5, TimeUnit.SECONDS));
        System.out.println(result2);

        system.terminate();
    }
}
