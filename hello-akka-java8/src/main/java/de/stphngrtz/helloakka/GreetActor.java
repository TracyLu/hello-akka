package de.stphngrtz.helloakka;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;

public class GreetActor extends AbstractActor {

    public static Props props() {
        return Props.create(GreetActor.class);
    }

    public static class WhoToGreet {
        public final String who;

        public WhoToGreet(String who) {
            this.who = who;
        }
    }

    public static class Greet {
    }

    public static class Greeting {
        public final String message;

        public Greeting(String message) {
            this.message = message;
        }
    }

    private String greeting;

    public GreetActor() {
        receive(ReceiveBuilder
                        .match(WhoToGreet.class, message -> greeting = "hello " + message.who)
                        .match(Greet.class, message -> sender().tell(new Greeting(greeting), self()))
                        .build()
        );
    }
}
