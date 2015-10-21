package de.stphngrtz.helloakka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class Main {

    /**
     * https://github.com/typesafehub/activator-akka-supervision
     * http://www.typesafe.com/activator/template/akka-supervision-java-lambda
     */
    public static void main(String[] args) throws Exception {
        ActorSystem system = ActorSystem.create("calculation-system");
        ActorRef arithmeticService = system.actorOf(ArithmeticService.props());

        // (3 + 5) / (2 * (1 + 1))
        Expression expression = new Expression.Divide(
                new Expression.Add(new Expression.Const(3), new Expression.Const(5)),
                new Expression.Multiply(
                        new Expression.Const(2),
                        new Expression.Add(new Expression.Const(1), new Expression.Const(1))
                )
        );

        Timeout timeout = new Timeout(1, TimeUnit.SECONDS);
        Object result = Await.result(Patterns.ask(arithmeticService, new Protocol.Calculate(expression), timeout), timeout.duration());
        System.out.println("got result: " + result);

        Await.ready(system.terminate(), Duration.Inf());
    }
}
