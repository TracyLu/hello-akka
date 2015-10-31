package de.stphngrtz.helloakka;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.japi.pf.ReceiveBuilder;

public class SimpleClusterListener extends AbstractLoggingActor {

    public static Props props() {
        return Props.create(SimpleClusterListener.class);
    }

    private Cluster cluster = Cluster.get(context().system());

    @Override
    public void preStart() throws Exception {
        cluster.subscribe(self(), ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);
    }

    @Override
    public void postStop() throws Exception {
        cluster.unsubscribe(self());
    }

    public SimpleClusterListener() {
        receive(ReceiveBuilder
                        .match(ClusterEvent.MemberUp.class, m -> log().info("member is up: {}", m.member()))
                        .match(ClusterEvent.UnreachableMember.class, m -> log().info("member is unreachable: {}", m.member()))
                        .match(ClusterEvent.MemberRemoved.class, m -> log().info("member is removed: {}", m.member()))
                        .build()
        );
    }
}
