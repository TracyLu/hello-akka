package de.stphngrtz.helloakka.fsm;

import akka.actor.AbstractFSM;
import akka.actor.ActorRef;
import akka.actor.Props;

public class Chopstick extends AbstractFSM<Chopstick.ChopstickStates, Chopstick.TakenBy> {

    public static Props props() {
        return Props.create(Chopstick.class);
    }

    /**
     * Some states the chopstick can be in
     */
    public enum ChopstickStates {
        Available,
        Taken
    }

    /**
     * Some state container for the chopstick
     */
    public static final class TakenBy {
        public final ActorRef hakker;

        public TakenBy(ActorRef hakker) {
            this.hakker = hakker;
        }
    }

    {
        // A chopstick begins its existence as available and taken by no one
        startWith(ChopstickStates.Available, new TakenBy(context().system().deadLetters()));

        // When a chopstick is available, it can be taken by a some hakker
        when(ChopstickStates.Available,
                matchEventEquals(Protocol.Take,
                        (take, data) -> goTo(ChopstickStates.Taken).using(new TakenBy(sender())).replying(new Protocol.Taken(self()))
                )
        );

        // When a chopstick is taken by a hakker it will refuse to be taken by other hakkers,
        // but the owning hakker can put it back.
        when(ChopstickStates.Taken,
                matchEventEquals(Protocol.Take,
                        (take, data) -> stay().replying(new Protocol.Busy(self()))
                ).event(
                        (event, data) -> (event == Protocol.Put) && (data.hakker == sender()),
                        (event, data) -> goTo(ChopstickStates.Available)
                                .using(new TakenBy(context().system().deadLetters()))
                )
        );

        // Initialize the chopstick
        initialize();
    }
}
