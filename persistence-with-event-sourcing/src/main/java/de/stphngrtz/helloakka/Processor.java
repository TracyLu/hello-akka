package de.stphngrtz.helloakka;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotOffer;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

import java.util.UUID;

public class Processor extends AbstractPersistentActor { // sadly, there is no AbstractPersistentLoggingActor :(

    public static Props props() {
        return Props.create(Processor.class);
    }

    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    private ProcessorState processorState = new ProcessorState();

    @Override
    public PartialFunction<Object, BoxedUnit> receiveRecover() {
        return ReceiveBuilder
                .match(Event.class, event -> processorState = processorState.with(event))
                .match(SnapshotOffer.class, snapshotOffer -> processorState = (ProcessorState) snapshotOffer.snapshot())
                .build();
    }

    @Override
    public PartialFunction<Object, BoxedUnit> receiveCommand() {
        return ReceiveBuilder
                .match(Command.class, command -> {
                    Event event = new Event(UUID.randomUUID().toString(), command.getData());
                    persist(event, persistedEvent -> {
                        processorState = processorState.with(persistedEvent);
                        broadcast(persistedEvent);
                    });
                })
                .matchEquals("snapshot", s -> saveSnapshot(processorState.copy()))
                .matchEquals("printstate", s -> log.info(processorState.toString()))
                .build();
    }

    private void broadcast(Object event) {
        context().system().eventStream().publish(event);
    }

    @Override
    public String persistenceId() {
        return this.getClass().getSimpleName() + "-1";
    }
}
