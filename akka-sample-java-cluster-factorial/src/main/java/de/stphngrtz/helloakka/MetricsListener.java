package de.stphngrtz.helloakka;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.metrics.ClusterMetricsChanged;
import akka.cluster.metrics.ClusterMetricsExtension;
import akka.cluster.metrics.NodeMetrics;
import akka.cluster.metrics.StandardMetrics;
import akka.japi.pf.ReceiveBuilder;

public class MetricsListener extends AbstractLoggingActor {

    public static Props props() {
        return Props.create(MetricsListener.class);
    }

    private ClusterMetricsExtension extension = ClusterMetricsExtension.get(context().system());

    @Override
    public void preStart() throws Exception {
        extension.subscribe(self());
    }

    @Override
    public void postStop() throws Exception {
        extension.unsubscribe(self());
    }

    public MetricsListener() {
        Cluster cluster = Cluster.get(context().system());

        receive(ReceiveBuilder
                        .match(ClusterMetricsChanged.class, m -> m.getNodeMetrics().forEach(metric -> {
                            if (metric.address().equals(cluster.selfAddress())) {
                                logCpu(metric);
                                logHeap(metric);
                            }
                        }))
                        .match(ClusterEvent.CurrentClusterState.class, m -> {
                        })
                        .build()
        );
    }

    private void logCpu(NodeMetrics metric) {
        StandardMetrics.Cpu cpu = StandardMetrics.extractCpu(metric);
        if (cpu != null && cpu.systemLoadAverage().isDefined())
            log().info("CPU Load: {} ({} processors)", cpu.systemLoadAverage().get(), cpu.processors());
    }

    private void logHeap(NodeMetrics metric) {
        StandardMetrics.HeapMemory heapMemory = StandardMetrics.extractHeapMemory(metric);
        if (heapMemory != null)
            log().info("Used heap: {} MB", ((double) heapMemory.used()) / 1024 / 1024);
    }
}
