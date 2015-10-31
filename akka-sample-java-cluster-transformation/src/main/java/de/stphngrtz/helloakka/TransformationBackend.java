package de.stphngrtz.helloakka;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.japi.pf.ReceiveBuilder;

public class TransformationBackend extends AbstractLoggingActor {

    public static Props props() {
        return Props.create(TransformationBackend.class);
    }

    private final Cluster cluster = Cluster.get(context().system());

    @Override
    public void preStart() throws Exception {
        cluster.subscribe(self(), ClusterEvent.MemberUp.class);
    }

    @Override
    public void postStop() throws Exception {
        cluster.unsubscribe(self());
    }

    public TransformationBackend() {
        receive(ReceiveBuilder
                        .match(Protocol.TransformationJob.class, m -> sender().tell(new Protocol.TransformationResult(m.text.toUpperCase()), self()))
                        .match(ClusterEvent.CurrentClusterState.class, m -> {
                            m.getMembers().forEach(member -> {
                                if (member.status().equals(MemberStatus.up()))
                                    register(member);
                            });
                        })
                        .match(ClusterEvent.MemberUp.class, m -> register(m.member()))
                        .build()
        );
    }

    private void register(Member member) {
        if (member.hasRole("frontend")) {
            context().actorSelection(member.address() + "/user/frontend").tell(Protocol.BackendRegistration, self());
        }
    }
}
