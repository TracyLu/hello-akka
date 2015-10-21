package de.stphngrtz.helloakka.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.singleton.ClusterSingletonProxy;
import akka.cluster.singleton.ClusterSingletonProxySettings;
import akka.dispatch.Mapper;
import akka.dispatch.Recover;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import akka.util.Timeout;
import de.stphngrtz.helloakka.MessageProtocol;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;

import java.util.concurrent.TimeUnit;

public class FrontendActor extends AbstractActor {

    public static Props props() {
        return Props.create(FrontendActor.class);
    }

    public FrontendActor() {

        ActorRef masterProxy = context().actorOf(
                ClusterSingletonProxy.props(
                        "/user/master",
                        ClusterSingletonProxySettings.create(context().system()).withRole("master")
                ),
                "masterProxy"
        );

        ExecutionContextExecutor dispatcher = context().system().dispatcher();

        receive(ReceiveBuilder
                        .matchAny(message -> {
                            Future<Object> res = Patterns.ask(masterProxy, message, new Timeout(5, TimeUnit.SECONDS))
                                    .map(new Mapper<Object, Object>() {
                                        @Override
                                        public Object apply(Object parameter) {
                                            if (parameter instanceof MessageProtocol.Ack)
                                                return new MessageProtocol.Ok();
                                            else
                                                return super.apply(parameter);
                                        }
                                    }, dispatcher)
                                    .recover(new Recover<Object>() {
                                        @Override
                                        public Object recover(Throwable failure) throws Throwable {
                                            return new MessageProtocol.Nok();
                                        }
                                    }, dispatcher);

                            Patterns.pipe(res, dispatcher).to(sender());
                        })
                        .build()
        );
    }
}
