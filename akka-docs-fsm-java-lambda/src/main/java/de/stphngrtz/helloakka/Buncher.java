package de.stphngrtz.helloakka;

import akka.actor.AbstractLoggingFSM;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.UnitMatch;
import scala.concurrent.duration.Duration;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Buncher extends AbstractLoggingFSM<Buncher.State, Buncher.Data> {

    public static Props props() {
        return Props.create(Buncher.class);
    }

    public enum State {
        Idle,
        Active
    }

    public interface Data {
    }

    enum Uninitialized implements Data {
        Uninitialized
    }

    public final class Todo implements Data {
        public final ActorRef target;
        public final List<Object> queue;

        public Todo(ActorRef target, List<Object> queue) {
            this.target = target;
            this.queue = queue;
        }

        public Todo copy(LinkedList<Object> queue) {
            return new Todo(target, queue);
        }

        public Todo withElement(Object object) {
            LinkedList<Object> newQueue = new LinkedList<>(queue);
            newQueue.add(object);
            return new Todo(target, newQueue);
        }
    }

    {
        startWith(State.Idle, Uninitialized.Uninitialized);

        when(State.Idle,
                matchEvent(Protocol.SetTarget.class,
                        Uninitialized.class,
                        (setTarget, uninitialized) -> stay().using(new Todo(setTarget.target, new LinkedList<>()))
                )
        );
        when(State.Active, Duration.create(1, TimeUnit.SECONDS),
                matchEvent(Arrays.asList(Protocol.Flush.class, StateTimeout()),
                        Todo.class,
                        (event, todo) -> goTo(State.Idle).using(todo.copy(new LinkedList<>()))
                )
        );

        whenUnhandled(
                matchEvent(Protocol.Queue.class,
                        Todo.class,
                        (queue, todo) -> goTo(State.Active).using(todo.withElement(queue.object))
                ).anyEvent(
                        (event, state) -> {
                            log().warning("received unhandled request {} in state {}/{}", event, stateName(), state);
                            return stay();
                        }
                )
        );

        onTransition(
                matchState(State.Active, State.Idle, () -> {
                    UnitMatch<Data> m = UnitMatch.create(
                            matchData(Todo.class, todo -> todo.target.tell(new Protocol.Batch(todo.queue), self()))
                    );
                    m.match(stateData());
                }).state(State.Idle, State.Active, () -> {
                    log().info("Idle -> Active");
                })
        );

        initialize();
    }
}
