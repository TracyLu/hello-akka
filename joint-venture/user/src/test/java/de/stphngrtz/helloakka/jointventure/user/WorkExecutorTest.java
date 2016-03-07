package de.stphngrtz.helloakka.jointventure.user;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Objects;
import java.util.stream.Collectors;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class WorkExecutorTest {

    private static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    private MongoDatabase db;
    private MongoCollection<Document> users;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        db = mock(MongoDatabase.class);
        users = mock(MongoCollection.class);
        when(db.getCollection("users")).thenReturn(users);
    }

    @Test
    public void UserCreated_Events_werden_korrekt_verarbeitet() throws Exception {
        new JavaTestKit(system) {{
            ActorRef workExecutor = system.actorOf(WorkExecutor.props(db));
            Event.UserCreated userCreatedEvent = new Event.UserCreated("Hans");
            workExecutor.tell(new WorkExecutor.Protocol.Work<>(userCreatedEvent), getRef());

            expectMsgEquals(duration("1 second"), new Worker.Protocol.WorkIsDone<>(userCreatedEvent));
            verify(users).insertOne(argThat(equalsIgnoringUuid(User.fromEvent(userCreatedEvent).toDocument())));
        }};
    }

    /**
     * Matcher zum Vergleichen von {@link Document}, allerdings ohne Berücksichtigung des "uuid" Feldes
     */
    private static Matcher<Document> equalsIgnoringUuid(Document document) {
        return new TypeSafeMatcher<Document>() {
            @Override
            protected boolean matchesSafely(Document item) {
                return Objects.equals(
                        document.entrySet().stream()
                                .filter(e -> !Objects.equals("uuid", e.getKey()))
                                .collect(Collectors.toSet()),
                        item.entrySet().stream()
                                .filter(e1 -> !Objects.equals("uuid", e1.getKey()))
                                .collect(Collectors.toSet())
                );
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(document);
            }
        };
    }
}
