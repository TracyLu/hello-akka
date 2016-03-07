package de.stphngrtz.helloakka.jointventure.user;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class JointVenture {

    private static final String db_url = Optional.ofNullable(System.getenv("DB_PORT_27017_TCP_ADDR")).orElse("192.168.99.100");
    private static final String db_port = Optional.ofNullable(System.getenv("DB_PORT_27017_TCP_PORT")).orElse("27017");

    public static void main(String[] args) throws Exception {
        // TODO MongoClientOptions.builder()
        MongoDatabase db = new MongoClient(db_url, Integer.valueOf(db_port)).getDatabase("joint-venture");

        // TODO abhängig von den Parametern werden folgende "Teile" der Anwendung gestartet.
        // TODO wenn keine Parameter angegeben sind, dann "default" Setup starten (zB. für Entwicklung)

        ActorRef processor = startProcessor();

        startWorker(processor, db);
        startWorker(processor, db);
        startWorker(processor, db);

        startFrontendREST(processor, db);
        startFrontendRabbitMQ();
    }

    private static ActorRef startProcessor() {
        FiniteDuration eventProcessingTimeout = Duration.create(2, TimeUnit.MINUTES);
        FiniteDuration workerStateTimeout = Duration.create(1, TimeUnit.MINUTES);
        FiniteDuration cleanupTaskInterval = Duration.create(1, TimeUnit.MINUTES);
        FiniteDuration notifyTaskInterval = Duration.create(30, TimeUnit.SECONDS);
        FiniteDuration saveSnapshotTaskInterval = Duration.create(5, TimeUnit.MINUTES);

        ActorSystem system = ActorSystem.create("JointVenture-ProcessorSystem");
        return system.actorOf(Processor.props(eventProcessingTimeout, workerStateTimeout, cleanupTaskInterval, notifyTaskInterval, saveSnapshotTaskInterval));
    }

    private static ActorRef startWorker(ActorRef processor, MongoDatabase db) {
        FiniteDuration intervalForRegistration = Duration.create(1, TimeUnit.MINUTES);
        FiniteDuration timeoutForAcknowledgement = Duration.create(15, TimeUnit.SECONDS);

        ActorSystem system = ActorSystem.create("JointVenture-WorkerSystem");
        return system.actorOf(Worker.props(processor, db, intervalForRegistration, timeoutForAcknowledgement));
    }

    /**
     * REST-Frontend
     */
    private static ActorRef startFrontendREST(ActorRef processor, MongoDatabase db) {
        ActorSystem system = ActorSystem.create("JointVenture-FrontendSystem");
        return system.actorOf(FrontendREST.props(processor, db));
    }

    /**
     * RabbitMQ-Frontend
     */
    private static void startFrontendRabbitMQ() {
        // TODO fehlt noch...
    }

    /*
    private static void mongoDbExperimente() {
        MongoDatabase db = MongoClients
                .create("mongodb://192.168.99.100:27017")
                .getDatabase("joint-venture");

        SingleResultCallback<Void> finishedCallback = (Void findResult, Throwable findException) -> System.out.println("finished!");

        MongoCollection<Document> users = db.getCollection("users");
        users.drop((Void dropResult, Throwable dropException) -> {
            if (dropException != null) {
                dropException.printStackTrace();
                return;
            }
            users.insertMany(Arrays.asList(
                    new Document()
                            .append("name", "Testuser 1")
                            .append("email", "test1@mail.com")
                            .append("address", new Document()
                                    .append("plz", "52134")
                                    .append("city", "Herzogenrath")
                                    .append("street", "Verratichnicht 1")
                            )
                            .append("events", Arrays.asList(1L, 2L, 3L)),
                    new Document()
                            .append("name", "Testuser 2")
                            .append("email", "test2@mail.com")
                            .append("address", new Document()
                                    .append("plz", "52072")
                                    .append("city", "Aachen")
                                    .append("street", "Verratichnicht 2")
                            )
                            .append("events", Arrays.asList(4L, 5L))
            ), (Void insertResult, Throwable insertException) -> {
                if (insertException != null) {
                    insertException.printStackTrace();
                    return;
                }
                System.out.println("Inserted!");
                FindIterable<Document> allUsers = users.find();

                allUsers.forEach(System.out::println, finishedCallback);
                FindIterable<Document> filteredUsers = users.find(Filters.eq("address.plz", "52134"));
                filteredUsers.forEach(System.out::println, finishedCallback);

                users.updateMany(
                        Filters.regex("email", "test1@.*"),
                        new Document()
                                .append("$set", new Document("address.street", "Verratichwirklichnicht 1"))
                                .append("$currentDate", new Document("lastModified", true)),
                        (UpdateResult updateResult, Throwable updateException) -> {
                            if (updateException != null) {
                                updateException.printStackTrace();
                                return;
                            }
                            System.out.println("Updated! " + updateResult);

                            FindIterable<Document> moreFilteredUsers = users.find(Filters.eq("address.plz", "52134"));
                            moreFilteredUsers.forEach(System.out::println, finishedCallback);

                            users.deleteMany(
                                    Filters.and(Filters.eq("address.plz", "52072")), // Filters.elemMatch("events", Filters.gt("", "4L"))
                                    (DeleteResult deleteResult, Throwable deleteException) -> {
                                        if (deleteException != null) {
                                            deleteException.printStackTrace();
                                            return;
                                        }
                                        System.out.println("Deleted! " + deleteResult);

                                        users.createIndex(new Document("email", 1), new IndexOptions().unique(true), (String s, Throwable indexException) -> {
                                            if (indexException != null) {
                                                indexException.printStackTrace();
                                                return;
                                            }
                                            System.out.println("Indexed! " + s);
                                        });
                                        users.createIndex(new Document("address.plz", 1).append("address.city", 1), (String s, Throwable indexException) -> {
                                            if (indexException != null) {
                                                indexException.printStackTrace();
                                                return;
                                            }
                                            System.out.println("Indexed! " + s);
                                        });
                                    });
                        });
            });
        });
    }
    */
}
