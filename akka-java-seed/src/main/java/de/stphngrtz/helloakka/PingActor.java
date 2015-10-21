package de.stphngrtz.helloakka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class PingActor extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public static Props props() {
        return Props.create(PingActor.class);
    }

    public static class Initialize {
    }

    public static class PingMessage {
        private final String text;

        public PingMessage(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    private int counter = 0;
    private final ActorRef pongActor = getContext().actorOf(PongActor.props(), "pongActor");

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof Initialize) {
            log.info("initialize");
            pongActor.tell(new PingMessage("ping"), getSelf());
        }
        else if (message instanceof PongActor.PongMessage) {
            PongActor.PongMessage pongMessage = (PongActor.PongMessage) message;
            log.info("got pong message: {}", pongMessage.getText());

            counter++;
            if (counter == 3) {
                getContext().system().shutdown();
            }
            else {
                getSender().tell(new PingMessage("ping"), getSelf());
            }
        }
        else {
            log.info("unhandeled: {}", message);
            unhandled(message);
        }
    }
}
