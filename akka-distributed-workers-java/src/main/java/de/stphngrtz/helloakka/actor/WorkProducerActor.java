package de.stphngrtz.helloakka.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import de.stphngrtz.helloakka.MessageProtocol;
import scala.PartialFunction;
import scala.concurrent.duration.Duration;
import scala.runtime.BoxedUnit;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WorkProducerActor extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public static Props props(ActorRef frontend) {
        return Props.create(WorkProducerActor.class, frontend);
    }

    private PartialFunction<Object, BoxedUnit> producing;
    private PartialFunction<Object, BoxedUnit> waiting;

    private int n=0;
    private MessageProtocol.Work lastWork;

    public WorkProducerActor(ActorRef frontend) {

        producing = ReceiveBuilder
                .match(MessageProtocol.Tick.class, message -> {
                    n++;
                    log.info("Produced work: {}", n);
                    lastWork = new MessageProtocol.Work(UUID.randomUUID().toString(), n);
                    frontend.tell(lastWork, self());
                    context().become(waiting);
                })
                .build();

        waiting = ReceiveBuilder
                .match(MessageProtocol.Ok.class, message -> {
                    context().become(producing);
                    context().system().scheduler().scheduleOnce(
                            Duration.create(5, TimeUnit.SECONDS),
                            self(),
                            new MessageProtocol.Tick(),
                            context().system().dispatcher(),
                            self()
                    );
                })
                .match(MessageProtocol.Nok.class, message -> {
                    log.info("Work not accepted, retry after a while");
                    context().system().scheduler().scheduleOnce(
                            Duration.create(3, TimeUnit.SECONDS),
                            frontend,
                            lastWork,
                            context().system().dispatcher(),
                            self()
                    );
                })
                .build();

        context().become(producing);
    }

    @Override
    public void preStart() throws Exception {
        context().system().scheduler().scheduleOnce(
                Duration.create(5, TimeUnit.SECONDS),
                self(),
                new MessageProtocol.Tick(),
                context().system().dispatcher(),
                self()
        );
    }
}
