package de.stphngrtz.helloakka.fsm;

import akka.actor.AbstractFSM;
import akka.actor.ActorRef;
import akka.actor.FSM;
import akka.actor.Props;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class Hakker extends AbstractFSM<Hakker.HakkerStates, Hakker.TakenChopsticks> {

    public static Props props(ActorRef left, ActorRef right) {
        return Props.create(Hakker.class, left, right);
    }

    /**
     * Some fsm hakker states
     */
    public enum HakkerStates {
        Waiting,
        Thinking,
        Hungry,
        WaitForOtherChopstick,
        FirstChopstickDenied,
        Eating
    }

    /**
     * Some state container to keep track of which chopsticks we have
     */
    public static final class TakenChopsticks {
        public final ActorRef left;
        public final ActorRef right;

        public TakenChopsticks(ActorRef left, ActorRef right) {
            this.left = left;
            this.right = right;
        }
    }

    private ActorRef left;
    private ActorRef right;

    public Hakker(ActorRef left, ActorRef right) {
        this.left = left;
        this.right = right;
    }

    {
        startWith(HakkerStates.Waiting, new TakenChopsticks(null, null));

        when(HakkerStates.Waiting,
                matchEventEquals(Protocol.Think, (think, data) -> {
                    System.out.println(self().path().name() + " starts to think");
                    return startThinking(Duration.create(5, TimeUnit.SECONDS));
                })
        );

        // When a hakker is thinking it can become hungry and try to pick up its chopsticks and eat
        when(HakkerStates.Thinking,
                matchEventEquals(StateTimeout(), (event, data) -> {
                    left.tell(Protocol.Take, self());
                    right.tell(Protocol.Take, self());
                    return goTo(HakkerStates.Hungry);
                })
        );

        // When a hakker is hungry it tries to pick up its chopsticks and eat.
        // When it picks one up, it goes into wait for the other.
        // If the hakkers first attempt at grabbing a chopstick fails, it starts to wait for the response of the other grab.
        when(HakkerStates.Hungry,
                matchEvent(Protocol.Taken.class,
                        (taken, data) -> taken.chopstick == left,
                        (taken, data) -> goTo(HakkerStates.WaitForOtherChopstick).using(new TakenChopsticks(left, null))
                ).event(Protocol.Taken.class,
                        (taken, data) -> taken.chopstick == right,
                        (taken, data) -> goTo(HakkerStates.WaitForOtherChopstick).using(new TakenChopsticks(null, right))
                ).event(Protocol.Busy.class,
                        (busy, data) -> goTo(HakkerStates.FirstChopstickDenied)
                )
        );

        // When a hakker is waiting for the last chopstick it can either obtain it and start eating, or the other
        // chopstick was busy, and the hakker goes back to think about how he should obtain his chopsticks :-)
        when(HakkerStates.WaitForOtherChopstick,
                matchEvent(Protocol.Taken.class,
                        (taken, data) -> (taken.chopstick == left && data.left == null && data.right != null),
                        (taken, data) -> startEating(left, right)
                ).event(Protocol.Taken.class,
                        (taken, data) -> (taken.chopstick == right && data.left != null && data.right == null),
                        (taken, data) -> startEating(left, right)
                ).event(Protocol.Busy.class,
                        (busy, data) -> {
                            if (data.left != null) left.tell(Protocol.Put, self());
                            if (data.right != null) right.tell(Protocol.Put, self());
                            return startThinking(Duration.create(10, TimeUnit.MILLISECONDS));
                        }
                )
        );

        // When the results of the other grab comes back, he needs to put it back if he got the other one.
        // Then go back and think and try to grab the chopsticks again.
        when(HakkerStates.FirstChopstickDenied,
                matchEvent(Protocol.Taken.class,
                        (taken, data) -> {
                            taken.chopstick.tell(Protocol.Put, self());
                            return startThinking(Duration.create(10, TimeUnit.MILLISECONDS));
                        }
                ).event(Protocol.Busy.class,
                        (busy, data) -> startThinking(Duration.create(10, TimeUnit.MILLISECONDS))
                )
        );

        when(HakkerStates.Eating,
                matchEventEquals(StateTimeout(),
                        (event, data) -> {
                            left.tell(Protocol.Put, self());
                            right.tell(Protocol.Put, self());
                            System.out.println(self().path().name() + " puts down his chopsticks and starts to think");
                            return startThinking(Duration.create(5, TimeUnit.SECONDS));
                        }
                )
        );

        // Initialize the hakker
        initialize();
    }

    private FSM.State<HakkerStates, TakenChopsticks> startEating(ActorRef left, ActorRef right) {
        System.out.println(self().path().name() + " has picked up "+ left.path().name() +" and "+ right.path().name() +" and starts to eat");
        return goTo(HakkerStates.Eating).using(new TakenChopsticks(left, right)).forMax(Duration.create(5, TimeUnit.SECONDS));
    }

    private FSM.State<HakkerStates, TakenChopsticks> startThinking(FiniteDuration finiteDuration) {
        return goTo(HakkerStates.Thinking).using(new TakenChopsticks(null, null)).forMax(finiteDuration);
    }
}
