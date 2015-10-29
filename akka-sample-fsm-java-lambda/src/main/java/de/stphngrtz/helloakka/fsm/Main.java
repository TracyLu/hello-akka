package de.stphngrtz.helloakka.fsm;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class Main {

    /**
     * https://github.com/akka/akka/tree/master/akka-samples/akka-sample-fsm-java-lambda
     */
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("example");

        ActorRef chopstick1 = system.actorOf(Chopstick.props(), "Chopstick1");
        ActorRef chopstick2 = system.actorOf(Chopstick.props(), "Chopstick2");
        ActorRef chopstick3 = system.actorOf(Chopstick.props(), "Chopstick3");
        ActorRef chopstick4 = system.actorOf(Chopstick.props(), "Chopstick4");
        ActorRef chopstick5 = system.actorOf(Chopstick.props(), "Chopstick5");

        ActorRef hakker1 = system.actorOf(Hakker.props(chopstick1, chopstick2), "Hakker1");
        ActorRef hakker2 = system.actorOf(Hakker.props(chopstick2, chopstick3), "Hakker2");
        ActorRef hakker3 = system.actorOf(Hakker.props(chopstick3, chopstick4), "Hakker3");
        ActorRef hakker4 = system.actorOf(Hakker.props(chopstick4, chopstick5), "Hakker4");
        ActorRef hakker5 = system.actorOf(Hakker.props(chopstick5, chopstick1), "Hakker5");

        hakker1.tell(Protocol.Think, ActorRef.noSender());
        hakker2.tell(Protocol.Think, ActorRef.noSender());
        hakker3.tell(Protocol.Think, ActorRef.noSender());
        hakker4.tell(Protocol.Think, ActorRef.noSender());
        hakker5.tell(Protocol.Think, ActorRef.noSender());
    }
}
