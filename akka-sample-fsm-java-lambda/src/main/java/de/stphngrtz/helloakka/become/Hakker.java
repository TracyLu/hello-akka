package de.stphngrtz.helloakka.become;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import scala.PartialFunction;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.BoxedUnit;

import java.util.concurrent.TimeUnit;

/**
 * A hakker is an awesome dude or dudette who either thinks about hacking or has to eat ;-)
 */
public class Hakker extends AbstractActor {

    public static Props props(ActorRef left, ActorRef right) {
        return Props.create(Hakker.class, left, right);
    }

    private ActorRef left;
    private ActorRef right;

    public Hakker(ActorRef left, ActorRef right) {
        this.left = left;
        this.right = right;

        // All hakkers start in a non-eating state
        receive(ReceiveBuilder
                        .matchEquals(Protocol.Think, m -> {
                            System.out.println(self().path().name() + " starts to think");
                            startThinking(Duration.create(5, TimeUnit.SECONDS));
                        })
                        .build()
        );
    }

    private void startThinking(FiniteDuration finiteDuration) {
        context().become(thinking);
        context().system().scheduler().scheduleOnce(finiteDuration, self(), Protocol.Eat, context().system().dispatcher(), self());
    }

    // When a hakker is eating, he can decide to start to think, then he puts down his chopsticks and starts to think.
    PartialFunction<Object, BoxedUnit> eating = ReceiveBuilder
            .matchEquals(Protocol.Think, t -> {
                left.tell(new Protocol.Put(self()), self());
                right.tell(new Protocol.Put(self()), self());
                System.out.println(self().path().name() + " puts down his chopsticks and starts to think");
                startThinking(Duration.create(5, TimeUnit.SECONDS));
            })
            .build();

    // When a hakker is waiting for the last chopstick it can either obtain it and start eating, or the other chopstick
    // was busy, and the hakker goes back to think about how he should obtain his chopsticks :-)
    PartialFunction<Object, BoxedUnit> waitingFor(ActorRef chopstickToWaitFor, ActorRef otherChopstick) {
        return ReceiveBuilder
                .match(Protocol.Taken.class, t -> t.chopstick == chopstickToWaitFor, t -> {
                    System.out.println(self().path().name() + " has picked up " + left.path().name() + " and " + right.path().name() + " and starts to eat");
                    context().become(eating);
                    context().system().scheduler().scheduleOnce(Duration.create(5, TimeUnit.SECONDS), self(), Protocol.Think, context().system().dispatcher(), self());
                })
                .match(Protocol.Busy.class, t -> {
                    otherChopstick.tell(new Protocol.Put(self()), self());
                    startThinking(Duration.create(10, TimeUnit.MILLISECONDS));
                })
                .build();
    }

    // When the results of the other grab comes back, he needs to put it back if he got the other one.
    // Then go back and think and try to grab the chopsticks again
    PartialFunction<Object, BoxedUnit> deniedAChopstick = ReceiveBuilder
            .match(Protocol.Taken.class, t -> {
                t.chopstick.tell(new Protocol.Put(self()), self());
                startThinking(Duration.create(10, TimeUnit.MILLISECONDS));
            })
            .match(Protocol.Busy.class, t -> {
                startThinking(Duration.create(10, TimeUnit.MILLISECONDS));
            })
            .build();

    // When a hakker is hungry it tries to pick up its chopsticks and eat.
    // When it picks one up, it goes into wait for the other.
    // If the hakkers first attempt at grabbing a chopstick fails, it starts to wait for the response of the other grab.
    PartialFunction<Object, BoxedUnit> hungry = ReceiveBuilder
            .match(Protocol.Taken.class, t -> t.chopstick == left, t -> context().become(waitingFor(right, left)))
            .match(Protocol.Taken.class, t -> t.chopstick == right, t -> context().become(waitingFor(left, right)))
            .match(Protocol.Busy.class, t -> context().become(deniedAChopstick))
            .build();

    // When a hakker is thinking it can become hungry and try to pick up its chopsticks and eat
    PartialFunction<Object, BoxedUnit> thinking = ReceiveBuilder
            .matchEquals(Protocol.Eat, m -> {
                context().become(hungry);
                left.tell(new Protocol.Take(self()), self());
                right.tell(new Protocol.Take(self()), self());
            })
            .build();
}
