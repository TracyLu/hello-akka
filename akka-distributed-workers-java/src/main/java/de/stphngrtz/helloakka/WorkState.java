package de.stphngrtz.helloakka;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class WorkState {

    private final ConcurrentLinkedDeque<MessageProtocol.Work> pendingWork;
    private final Map<String, MessageProtocol.Work> workInProgress;
    private final Set<String> doneWorkIds;
    private final Set<String> acceptedWorkIds;

    public WorkState() {
        pendingWork = new ConcurrentLinkedDeque<>();
        workInProgress = new HashMap<>();
        doneWorkIds = new HashSet<>();
        acceptedWorkIds = new HashSet<>();
    }

    public WorkState(WorkState workState, WorkAccepted event) {
        ConcurrentLinkedDeque<MessageProtocol.Work> temporaryPendingWork = new ConcurrentLinkedDeque<>(workState.pendingWork);
        Set<String> temporaryAcceptedWorkIds = new HashSet<>(workState.acceptedWorkIds);

        temporaryPendingWork.add(event.work);
        temporaryAcceptedWorkIds.add(event.work.workId);

        pendingWork = temporaryPendingWork;
        workInProgress = new HashMap<>(workState.workInProgress);
        doneWorkIds = new HashSet<>(workState.doneWorkIds);
        acceptedWorkIds = temporaryAcceptedWorkIds;
    }

    public WorkState(WorkState workState, WorkStarted event) {
        ConcurrentLinkedDeque<MessageProtocol.Work> temporaryPendingWork = new ConcurrentLinkedDeque<>(workState.pendingWork);
        Map<String, MessageProtocol.Work> temporaryWorkInProgress = new HashMap<>(workState.workInProgress);

        MessageProtocol.Work work = temporaryPendingWork.removeFirst();
        if (!Objects.equals(work.workId, event.workId))
            throw new IllegalArgumentException();

        temporaryWorkInProgress.put(work.workId, work);

        pendingWork = temporaryPendingWork;
        workInProgress = temporaryWorkInProgress;
        doneWorkIds = new HashSet<>(workState.doneWorkIds);
        acceptedWorkIds = new HashSet<>(workState.acceptedWorkIds);
    }

    public WorkState(WorkState workState, WorkCompleted event) {
        Map<String, MessageProtocol.Work> temporaryWorkInProgress = new HashMap<>(workState.workInProgress);
        Set<String> temporaryDoneWorkIds = new HashSet<>(workState.doneWorkIds);

        temporaryWorkInProgress.remove(event.workId);
        temporaryDoneWorkIds.add(event.workId);

        pendingWork = new ConcurrentLinkedDeque<>(workState.pendingWork);
        workInProgress = temporaryWorkInProgress;
        doneWorkIds = temporaryDoneWorkIds;
        acceptedWorkIds = new HashSet<>(workState.acceptedWorkIds);
    }

    public WorkState(WorkState workState, WorkerFailed event) {
        ConcurrentLinkedDeque<MessageProtocol.Work> temporaryPendingWork = new ConcurrentLinkedDeque<>(workState.pendingWork);
        Map<String, MessageProtocol.Work> temporaryWorkInProgress = new HashMap<>(workState.workInProgress);

        MessageProtocol.Work work = temporaryWorkInProgress.remove(event.workId);
        temporaryPendingWork.addLast(work);

        pendingWork = temporaryPendingWork;
        workInProgress = temporaryWorkInProgress;
        doneWorkIds = new HashSet<>(workState.doneWorkIds);
        acceptedWorkIds = new HashSet<>(workState.acceptedWorkIds);
    }

    public WorkState(WorkState workState, WorkerTimedOut event) {
        this(workState, new WorkerFailed(event.workId));
    }

    public WorkState updated(WorkStateEvent event) {
        if (event instanceof WorkAccepted) {
            return new WorkState(this, (WorkAccepted) event);
        }
        else if (event instanceof WorkStarted) {
            return new WorkState(this, (WorkStarted) event);
        }
        else if (event instanceof WorkCompleted) {
            return new WorkState(this, (WorkCompleted) event);
        }
        else if (event instanceof WorkerFailed) {
            return new WorkState(this, (WorkerFailed) event);
        }
        else if (event instanceof WorkerTimedOut) {
            return new WorkState(this, (WorkerTimedOut) event);

        }

        else
            throw new IllegalArgumentException();
    }

    public boolean hasWork() {
        return !pendingWork.isEmpty();
    }

    public MessageProtocol.Work nextWork() {
        return pendingWork.getFirst();
    }

    public boolean isDone(String workId) {
        return doneWorkIds.contains(workId);
    }

    public boolean isInProgress(String workId) {
        return workInProgress.containsKey(workId);
    }

    public boolean isAccepted(String workId) {
        return acceptedWorkIds.contains(workId);
    }

    public interface WorkStateEvent {}

    public static final class WorkAccepted implements WorkStateEvent {
        public final MessageProtocol.Work work;

        public WorkAccepted(MessageProtocol.Work work) {
            this.work = work;
        }

        @Override
        public String toString() {
            return "WorkAccepted{work=" + work + "}";
        }
    }

    public static final class WorkStarted implements WorkStateEvent {
        public final String workId;

        public WorkStarted(String workId) {
            this.workId = workId;
        }

        @Override
        public String toString() {
            return "WorkStarted{workId='" + workId + "'}";
        }
    }

    public static final class WorkCompleted implements WorkStateEvent {
        public final String workId;

        public WorkCompleted(String workId) {
            this.workId = workId;
        }

        @Override
        public String toString() {
            return "WorkCompleted{workId='" + workId + "'}";
        }
    }

    public static final class WorkerFailed implements WorkStateEvent {
        public final String workId;

        public WorkerFailed(String workId) {
            this.workId = workId;
        }

        @Override
        public String toString() {
            return "WorkerFailed{workId='" + workId + "'}";
        }
    }

    public static final class WorkerTimedOut implements WorkStateEvent {
        public final String workId;

        public WorkerTimedOut(String workId) {
            this.workId = workId;
        }

        @Override
        public String toString() {
            return "WorkerTimedOut{workId='" + workId + "'}";
        }
    }
}
