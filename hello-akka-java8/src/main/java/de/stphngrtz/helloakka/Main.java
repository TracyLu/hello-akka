package de.stphngrtz.helloakka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {

    /**
     * https://github.com/typesafehub/activator-hello-akka-java8
     */
    public static void main(String[] args) {
        try {

            ActorSystem system = ActorSystem.create("MyGreetingsActorSystem");
            ActorRef greetActor = system.actorOf(GreetActor.props(), "greetActor");

            Inbox inbox = Inbox.create(system);

            greetActor.tell(new GreetActor.WhoToGreet("stephan"), ActorRef.noSender());
            inbox.send(greetActor, new GreetActor.Greet());
            GreetActor.Greeting greeting1 = ((GreetActor.Greeting) inbox.receive(Duration.create(5, TimeUnit.SECONDS)));
            System.out.println(greeting1.message);

            greetActor.tell(new GreetActor.WhoToGreet("akka"), ActorRef.noSender());
            inbox.send(greetActor, new GreetActor.Greet());
            GreetActor.Greeting greeting2 = ((GreetActor.Greeting) inbox.receive(Duration.create(5, TimeUnit.SECONDS)));
            System.out.println(greeting2.message);

            system.shutdown();
            system.awaitTermination();


        } catch (TimeoutException e) {
            System.err.println("Timeout! :( " + e.getMessage());
            e.printStackTrace();
        }
    }
}
