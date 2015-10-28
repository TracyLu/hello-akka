package de.stphngrtz.helloakka.view;

import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.persistence.AbstractPersistentActor;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

public class MyPersistentActor extends AbstractPersistentActor {

    public static Props props() {
        return Props.create(MyPersistentActor.class);
    }

    private int count = 0;

    @Override
    public PartialFunction<Object, BoxedUnit> receiveRecover() {
        return ReceiveBuilder
                .match(String.class, s -> count++)
                .build();
    }

    @Override
    public PartialFunction<Object, BoxedUnit> receiveCommand() {
        return ReceiveBuilder
                .match(String.class, s -> {
                    System.out.println("persistent actor received " + s + " (nr=" + (count + 1) + ")");
                    persist(s+(count+1), evt -> {
                        count++;
                    });
                })
                .build();
    }

    @Override
    public String persistenceId() {
        return "sample-id-4";
    }
}
