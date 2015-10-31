package de.stphngrtz.helloakka;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import scala.concurrent.Future;

import java.math.BigInteger;

public class FactorialBackend extends AbstractLoggingActor {

    public static Props props() {
        return Props.create(FactorialBackend.class);
    }

    public FactorialBackend() {
        receive(ReceiveBuilder
                        .match(Integer.class, n -> {
                            Future<Protocol.FactorialResult> result = Futures.future(() -> factorial(n), context().dispatcher())
                                    //.map(factorial -> new Protocol.FactorialResult(n, factorial), context().dispatcher());
                                    .map(new Mapper<BigInteger, Protocol.FactorialResult>() {
                                        @Override
                                        public Protocol.FactorialResult apply(BigInteger parameter) {
                                            return new Protocol.FactorialResult(n, parameter);
                                        }
                                    }, context().dispatcher());

                            Patterns.pipe(result, context().dispatcher()).to(sender());
                        })
                        .build()
        );
    }

    private BigInteger factorial(Integer n) {
        BigInteger acc = BigInteger.ONE;
        for (int i = 1; i <= n; i++) {
            acc = acc.multiply(BigInteger.valueOf(i));
        }
        return acc;
    }
}
