package de.stphngrtz.helloakka;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Main {

    /**
     * http://doc.akka.io/docs/akka/2.4.0/java/cluster-usage.html
     * https://github.com/akka/akka/tree/master/akka-samples/akka-sample-cluster-java
     */
    public static void main(String[] args) {
        if (args.length == 0)
            startup("2551", "2552", "2553");
        else
            startup(args);
    }

    private static void startup(String... ports) {
        for (String port : ports) {
            Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).withFallback(ConfigFactory.load());
            ActorSystem system = ActorSystem.create("ClusterSystem", config);
            system.actorOf(SimpleClusterListener.props(), "cluster-listener");
        }
    }
}
