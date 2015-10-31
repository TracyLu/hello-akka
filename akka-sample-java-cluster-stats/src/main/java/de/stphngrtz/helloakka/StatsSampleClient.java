package de.stphngrtz.helloakka;

import akka.actor.*;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.MemberStatus;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class StatsSampleClient extends AbstractLoggingActor {

    public static Props props(String servicePath) {
        return Props.create(StatsSampleClient.class, servicePath);
    }

    private Cluster cluster = Cluster.get(context().system());
    private Cancellable tickTask;
    private Set<Address> nodes = new HashSet<>();

    public StatsSampleClient(String servicePath) {
        tickTask = context().system().scheduler().schedule(
                Duration.create(2, TimeUnit.SECONDS),
                Duration.create(2, TimeUnit.SECONDS),
                self(),
                "tick",
                context().dispatcher(),
                self()
        );

        receive(ReceiveBuilder
                        .matchEquals("tick", s -> !nodes.isEmpty(), s -> {
                            Address node = new ArrayList<>(nodes).get(ThreadLocalRandom.current().nextInt(nodes.size()));
                            ActorSelection service = context().actorSelection(node + servicePath);
                            service.tell(new Protocol.StatsJob("this is the text that will be analyzed"), self());
                        })
                        .match(Protocol.StatsResult.class, m -> System.out.println("meanWordLength: " + m.meanWordLength))
                        .match(Protocol.JobFailed.class, m -> System.out.println("failed: " + m.reason))
                        .match(ClusterEvent.CurrentClusterState.class, m -> {
                            nodes.clear();
                            m.getMembers().forEach(member -> {
                                if (member.hasRole("compute") && member.status().equals(MemberStatus.up())) {
                                    nodes.add(member.address());
                                }
                            });
                        })
                        .match(ClusterEvent.MemberUp.class, m -> nodes.add(m.member().address()))
                        .match(ClusterEvent.MemberEvent.class, m -> nodes.remove(m.member().address()))
                        .match(ClusterEvent.UnreachableMember.class, m -> nodes.remove(m.member().address()))
                        .match(ClusterEvent.ReachableMember.class, m -> {
                            if (m.member().status().equals(MemberStatus.up())) {
                                nodes.add(m.member().address());
                            }
                        })
                        .build()
        );
    }

    @Override
    public void preStart() throws Exception {
        cluster.subscribe(self(), ClusterEvent.MemberEvent.class, ClusterEvent.ReachabilityEvent.class);
    }

    @Override
    public void postStop() throws Exception {
        cluster.unsubscribe(self());
        tickTask.cancel();
    }
}
