package de.stphngrtz.helloakka.view;

import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.persistence.AbstractPersistentView;
import akka.persistence.SnapshotOffer;

public class MyPersistentView extends AbstractPersistentView {

    public static Props props() {
        return Props.create(MyPersistentView.class);
    }

    private int numReplicated = 0;

    public MyPersistentView() {
        receive(ReceiveBuilder
                        .match(Object.class, m -> isPersistent(), m -> {
                            numReplicated++;
                            System.out.println("view received " + m + " (nr=" + numReplicated + ")");
                        })
                        .match(SnapshotOffer.class, m -> {
                            numReplicated = (int) m.snapshot();
                            System.out.println("view received snapshot offer " + numReplicated + " (meta=" + m.metadata() + ")");
                        })
                        .match(String.class, m -> m.equals("snap"), m -> {
                            saveSnapshot(numReplicated);
                        })
                        .build()
        );
    }

    @Override
    public String viewId() {
        return "sample-view-id-4";
    }

    @Override
    public String persistenceId() {
        return "sample-id-4";
    }
}
