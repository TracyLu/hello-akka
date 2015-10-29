package de.stphngrtz.helloakka.become;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

/**
 * A Chopstick is an actor, it can be taken, and put back
 */
public class Chopstick extends AbstractActor {

    public static Props props() {
        return Props.create(Chopstick.class);
    }

    PartialFunction<Object, BoxedUnit> takenBy(ActorRef hakker) {
        return ReceiveBuilder
                .match(Protocol.Take.class, t -> t.hakker.tell(new Protocol.Busy(self()), self()))
                .match(Protocol.Put.class, p -> p.hakker == hakker, p -> context().become(available))
                .build();
    }

    PartialFunction<Object, BoxedUnit> available = ReceiveBuilder
            .match(Protocol.Take.class, t -> {
                context().become(takenBy(t.hakker));
                t.hakker.tell(new Protocol.Taken(self()), self());
            })
            .build();

    public Chopstick() {
        receive(available);
    }
}
