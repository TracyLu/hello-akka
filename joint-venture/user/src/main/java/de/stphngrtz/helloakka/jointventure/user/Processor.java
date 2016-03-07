package de.stphngrtz.helloakka.jointventure.user;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotOffer;
import scala.PartialFunction;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.BoxedUnit;

import java.util.*;

public class Processor extends AbstractPersistentActor {

    /**
     * Messages, auf die dieser Actor reagieren kann
     */
    public static class Protocol {

        /**
         * Löst das Speichern eines Snapshots aus
         */
        public static class SaveSnapshot {
        }

        /**
         * Schreibt den aktuellen Zustand des {@link #eventStore} ins Log
         */
        public static class PrintState {
        }

        /**
         * Veranlasst das Erstellen eines neuen Users
         */
        public static class CreateUser {
            public final String name;

            public CreateUser(String name) {
                this.name = name;
            }
        }

        /**
         * Veranlasst das Aktualisieren eines Users
         */
        public static class UpdateUser {
            public final UUID uuid;
            public final String name;

            public UpdateUser(UUID uuid, String name) {
                this.uuid = uuid;
                this.name = name;
            }
        }

        /**
         * Veranlasst das Löschen eines Users
         */
        public static class DeleteUser {
            public final UUID uuid;

            public DeleteUser(UUID uuid) {
                this.uuid = uuid;
            }
        }

        /**
         * Registriert einen Worker beim Processor
         */
        public static class RegisterWorker {
            public final UUID workerId;

            public RegisterWorker(UUID workerId) {
                this.workerId = workerId;
            }
        }

        /**
         * Veranlasst die Übergabe eines noch offenen Events an einen Worker
         */
        public static class CallingForWork {
            public final UUID workerId;

            public CallingForWork(UUID workerId) {
                this.workerId = workerId;
            }
        }

        /**
         * Setzt den Status eines Events auf verarbeitet
         */
        public static class WorkIsDone<T extends Event> {
            public final UUID workerId;
            public final T event;

            public WorkIsDone(UUID workerId, T event) {
                this.workerId = workerId;
                this.event = event;
            }
        }

        /**
         * Setzt den Status eines Events zurück
         */
        public static class WorkFailed<T extends Event> {
            public final UUID workerId;
            public final T event;

            public WorkFailed(UUID workerId, T event) {
                this.workerId = workerId;
                this.event = event;
            }
        }

        /**
         * Veranlasst ein Aufräumen der Worker sowie Events (bzgl. deren Deadline)
         */
        public static class Cleanup {
        }

        /**
         * Informiert alle Idle-Worker über zu erledigende Arbeit
         */
        public static class Notify {
        }
    }

    public static Props props(FiniteDuration eventProcessingTimeout, FiniteDuration workerStateTimeout, FiniteDuration cleanupTaskInterval, FiniteDuration notifyTaskInterval, FiniteDuration saveSnapshotTaskInterval) {
        return Props.create(Processor.class, eventProcessingTimeout, workerStateTimeout, cleanupTaskInterval, notifyTaskInterval, saveSnapshotTaskInterval);
    }

    private final LoggingAdapter log = Logging.getLogger(context().system(), this);
    private final Map<UUID, WorkerState> workerWithState = new HashMap<>();

    private final Cancellable cleanupTask;
    private final Cancellable notifyTask;
    private final Cancellable saveSnapshotTask;

    private final FiniteDuration workerStateTimeout;

    /**
     * Der {@link EventStore} ist das Herz des Processors. Registrierte Worker erhalten hieraus ihre zu verarbeitenden Events.
     */
    private EventStore eventStore;

    /**
     * Initialisiert den {@link EventStore} sowie diverse Tasks
     */
    public Processor(FiniteDuration eventProcessingTimeout, FiniteDuration workerStateTimeout, FiniteDuration cleanupTaskInterval, FiniteDuration notifyTaskInterval, FiniteDuration saveSnapshotTaskInterval) {
        this.eventStore = new EventStore(eventProcessingTimeout); // Hinweis: Worker-Timeout sollte kürzer gewählt werden!
        this.workerStateTimeout = workerStateTimeout;

        this.cleanupTask = context().system().scheduler().schedule(
                cleanupTaskInterval,
                cleanupTaskInterval,
                self(),
                new Protocol.Cleanup(),
                context().dispatcher(),
                self()
        );
        this.notifyTask = context().system().scheduler().schedule(
                notifyTaskInterval,
                notifyTaskInterval,
                self(),
                new Protocol.Notify(),
                context().dispatcher(),
                self()
        );
        this.saveSnapshotTask = context().system().scheduler().schedule(
                saveSnapshotTaskInterval,
                saveSnapshotTaskInterval,
                self(),
                new Protocol.SaveSnapshot(),
                context().dispatcher(),
                self()
        );

        log.debug("Processor initialized");
    }

    @Override
    public PartialFunction<Object, BoxedUnit> receiveRecover() {
        return ReceiveBuilder
                .match(EventStore.StorableEvent.class, event -> {
                    log.debug("Recover: {}", event);
                    eventStore = eventStore.with(event);
                })
                .match(SnapshotOffer.class, offer -> {
                    log.debug("Recover: {}", offer);
                    eventStore = (EventStore) offer.snapshot();
                })
                .build();
    }

    @Override
    public PartialFunction<Object, BoxedUnit> receiveCommand() {
        return ReceiveBuilder
                /**
                 * Fachliche Commands von außen
                 */
                .match(Protocol.CreateUser.class, createUserCommand -> {
                    log.debug("Command: {}", createUserCommand.getClass().getSimpleName());
                    Event.UserCreated userCreatedEvent = new Event.UserCreated(createUserCommand.name);
                    persist(new EventStore.PendingEvent<>(userCreatedEvent), persistedPendingUserCreatedEvent -> {
                        eventStore = eventStore.with(persistedPendingUserCreatedEvent);
                        notifyWorkers();
                    });
                })
                .match(Protocol.UpdateUser.class, updateUserCommand -> {
                    log.debug("Command: {}", updateUserCommand.getClass().getSimpleName());
                    Event.UserUpdated userUpdatedEvent = new Event.UserUpdated(updateUserCommand.uuid, updateUserCommand.name);
                    persist(new EventStore.PendingEvent<>(userUpdatedEvent), persistedPendingUserUpdatedEvent -> {
                        eventStore = eventStore.with(persistedPendingUserUpdatedEvent);
                        notifyWorkers();
                    });
                })
                .match(Protocol.DeleteUser.class, deleteUserCommand -> {
                    log.debug("Command: {}", deleteUserCommand.getClass().getSimpleName());
                    Event.UserDeleted userDeletedEvent = new Event.UserDeleted(deleteUserCommand.uuid);
                    persist(new EventStore.PendingEvent<>(userDeletedEvent), persistedPendingUserDeletedEvent -> {
                        eventStore = eventStore.with(persistedPendingUserDeletedEvent);
                        notifyWorkers();
                    });
                })
                /**
                 * Technische Commands von innen/außen
                 */
                .match(Protocol.SaveSnapshot.class, command -> {
                    log.debug("Command: {}", command.getClass().getSimpleName());
                    saveSnapshot(eventStore.copy());
                })
                .match(Protocol.PrintState.class, command -> {
                    log.debug("Command: {}", command.getClass().getSimpleName());
                    log.info(String.format("Event Store%n%s", eventStore.toString()));

                    StringBuilder sb = new StringBuilder();
                    workerWithState.entrySet().forEach(e -> {
                        sb.append(e.getKey()).append(" ").append(e.getValue()).append(String.format("%n"));
                    });
                    log.info(String.format("Worker & State%n%s", sb.toString()));
                })
                .match(Protocol.Cleanup.class, command -> {
                    log.debug("Command: {}", command.getClass().getSimpleName());
                    new HashSet<>(workerWithState.entrySet()).stream()
                            .filter(e -> e.getValue().isBusy() && e.getValue().isDeadlineOverdue())
                            .forEach(e -> {
                                log.debug("Cleanup found overdue Worker {}", e.getKey());
                                workerWithState.remove(e.getKey());

                                persist(new EventStore.OverdueEvent<>(e.getValue().getEvent().get()), persistedOverdueEvent -> {
                                    eventStore = eventStore.with(persistedOverdueEvent);
                                /*
                                TODO sollte eigentlich erst passieren, wenn persistAll fertig ist. nicht nach jedem einzelnen persist..
                                List<Event> moreOverdueEvents = eventStore.getOverdueEvents();
                                if (!overdueEvents.isEmpty()) {
                                    persistAll(moreOverdueEvents.stream().map(EventStore.OverdueEvent::new).collect(Collectors.toList()), anotherPersistedOverdueEvent -> {
                                        eventStore = eventStore.with(anotherPersistedOverdueEvent);
                                    });
                                }
                                */
                                });
                            });
                })
                .match(Protocol.Notify.class, command -> {
                    log.debug("Command: {}", command.getClass().getSimpleName());
                    if (eventStore.hasPendingEvents()) {
                        notifyWorkers();
                    }
                })
                /**
                 * Fachliche Commands von innen
                 */
                .match(Protocol.RegisterWorker.class, command -> {
                    log.debug("Command: {}", command.getClass().getSimpleName());
                    if (!workerWithState.containsKey(command.workerId)) {
                        workerWithState.put(command.workerId, WorkerState.idle(sender()));
                        log.debug("Worker registered (id:{})", command.workerId);
                        if (eventStore.hasPendingEvents())
                            notifyWorker(sender());
                    }
                })
                .match(Protocol.CallingForWork.class, command -> {
                    log.debug("Command: {}", command.getClass().getSimpleName());
                    Optional<Event> nextPendingEvent = eventStore.nextPendingEvent();
                    if (nextPendingEvent.isPresent()) {
                        persist(new EventStore.ProcessingEvent<>(nextPendingEvent.get()), persistedProcessingEvent -> {
                            eventStore = eventStore.with(persistedProcessingEvent);

                            Event event = persistedProcessingEvent.event;
                            workerWithState.put(command.workerId, WorkerState.busy(sender(), event, workerStateTimeout));
                            sender().tell(new Worker.Protocol.Work<>(event), self());
                        });
                    }
                })
                .match(Protocol.WorkIsDone.class, command -> {
                    log.debug("Command: {}", command.getClass().getSimpleName());
                    persist(new EventStore.ProcessedEvent<>(command.event), persistedProcessedEvent -> {
                        eventStore = eventStore.with(persistedProcessedEvent);

                        workerWithState.put(command.workerId, WorkerState.idle(sender()));
                        sender().tell(new Worker.Protocol.Acknowledged<>(command.event), self());
                    });
                })
                .match(Protocol.WorkFailed.class, command -> {
                    log.debug("Command: {}", command.getClass().getSimpleName());
                    workerWithState.remove(command.workerId);
                    persist(new EventStore.OverdueEvent<>(command.event), persistedOverdueEvent -> {
                        eventStore = eventStore.with(persistedOverdueEvent);
                    });
                })
                .build();
    }

    @Override
    public String persistenceId() {
        return this.getClass().getSimpleName() + "-1";
    }

    @Override
    public void postStop() {
        cleanupTask.cancel();
        notifyTask.cancel();
        saveSnapshotTask.cancel();
        super.postStop();
    }

    private void notifyWorkers() {
        workerWithState.values().stream()
                .filter(WorkerState::isIdle)
                .forEach(workerState -> notifyWorker(workerState.getWorker()));
    }

    private void notifyWorker(ActorRef worker) {
        worker.tell(new Worker.Protocol.CallForWork(), self());
    }
}
