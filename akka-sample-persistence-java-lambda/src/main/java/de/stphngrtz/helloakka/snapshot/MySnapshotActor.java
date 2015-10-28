package de.stphngrtz.helloakka.snapshot;

import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotOffer;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

public class MySnapshotActor extends AbstractPersistentActor {

    public static Props props() {
        return Props.create(MySnapshotActor.class);
    }

    private MyState myState = new MyState();

    @Override
    public PartialFunction<Object, BoxedUnit> receiveRecover() {
        return ReceiveBuilder
                .match(String.class, m -> myState = myState.update(m))
                .match(SnapshotOffer.class, m -> {
                    System.out.println("offered snapshot:" + m);
                    myState = (MyState) m.snapshot();
                })
                .build();
    }

    @Override
    public PartialFunction<Object, BoxedUnit> receiveCommand() {
        return ReceiveBuilder
                .match(String.class, s -> s.equals("print"), s -> System.out.println("current state: " + myState))
                .match(String.class, s -> s.equals("snap"), s -> {
                    saveSnapshot(myState.copy()); // unnecessary copy because myState is not mutable?
                })
                .match(String.class, s -> persist(s, evt -> {
                    myState = myState.update(evt);
                }))
                .build();
    }

    @Override
    public String persistenceId() {
        return "sample-id-3";
    }
}
