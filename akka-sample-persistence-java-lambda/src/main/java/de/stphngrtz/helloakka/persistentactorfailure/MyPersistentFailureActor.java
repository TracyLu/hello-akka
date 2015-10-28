package de.stphngrtz.helloakka.persistentactorfailure;

import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.persistence.AbstractPersistentActor;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

import java.util.ArrayList;

public class MyPersistentFailureActor extends AbstractPersistentActor {

    public static Props props() {
        return Props.create(MyPersistentFailureActor.class);
    }

    private ArrayList<String> received = new ArrayList<>();

    @Override
    public PartialFunction<Object, BoxedUnit> receiveRecover() {
        return ReceiveBuilder
                .match(String.class, received::add)
                .build();
    }

    @Override
    public PartialFunction<Object, BoxedUnit> receiveCommand() {
        return ReceiveBuilder
                .match(String.class, m -> m.equals("boom"), s -> {
                    throw new RuntimeException("boom!");
                })
                .match(String.class, m -> m.equals("print"), s -> System.out.println(received))
                .match(String.class, m -> {
                    persist(m, evt -> {
                        received.add(evt);
                    });
                })
                .build();
    }

    @Override
    public String persistenceId() {
        return "sample-id-2";
    }
}
