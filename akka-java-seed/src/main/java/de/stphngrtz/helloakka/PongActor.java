package de.stphngrtz.helloakka;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class PongActor extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public static Props props() {
        return Props.create(PongActor.class);
    }

    public static class PongMessage {
        private final String text;

        public PongMessage(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof PingActor.PingMessage) {
            PingActor.PingMessage pingMessage = (PingActor.PingMessage) message;
            log.info("got ping message: {}", pingMessage.getText());
            getSender().tell(new PongMessage("pong"), getSelf());
        }
        else {
            log.info("unhandeled: {}", message);
            unhandled(message);
        }
    }
}
