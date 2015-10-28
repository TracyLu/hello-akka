package de.stphngrtz.helloakka.persistentactor;

import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotOffer;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

import static java.util.Arrays.asList;

public class MyPersistentActor extends AbstractPersistentActor {

    public static Props props() {
        return Props.create(MyPersistentActor.class);
    }

    private MyState myState = new MyState();

    @Override
    public PartialFunction<Object, BoxedUnit> receiveRecover() {
        return ReceiveBuilder
                .match(Protocol.Evt.class, m -> myState = myState.update(m))
                .match(SnapshotOffer.class, m -> myState = (MyState) m.snapshot())
                .build();
    }

    @Override
    public PartialFunction<Object, BoxedUnit> receiveCommand() {
        return ReceiveBuilder
                .match(Protocol.Cmd.class, m -> {
                    String data = m.getData();
                    Protocol.Evt evt1 = new Protocol.Evt(data + "_" + myState.size());
                    Protocol.Evt evt2 = new Protocol.Evt(data + "_" + (myState.size() + 1));
                    persistAll(asList(evt1, evt2), (Protocol.Evt evt) -> {
                        myState = myState.update(evt);
                        if (evt.equals(evt2))
                            context().system().eventStream().publish(evt);
                    });
                })
                .match(String.class, s -> s.equals("snap"), s -> saveSnapshot(myState.copy()))
                .match(String.class, s -> s.equals("print"), s -> System.out.println(myState))
                .build();
    }

    @Override
    public String persistenceId() {
        return "sample-id-1";
    }
}
