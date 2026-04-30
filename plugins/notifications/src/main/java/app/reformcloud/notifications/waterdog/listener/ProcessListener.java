package app.reformcloud.notifications.waterdog.listener;

import dev.waterdog.waterdogpe.ProxyServer;
import app.reformcloud.embedded.Embedded;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.event.events.process.ProcessRegisterEvent;
import app.reformcloud.event.events.process.ProcessUnregisterEvent;
import app.reformcloud.event.events.process.ProcessUpdateEvent;
import app.reformcloud.event.handler.Listener;
import app.reformcloud.process.ProcessInformation;
import app.reformcloud.process.ProcessState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProcessListener {

    private final Map<UUID, ProcessInformation> registered = new ConcurrentHashMap<>();

    public ProcessListener() {
        for (ProcessInformation process : ExecutorAPI.getInstance().getProcessProvider().getProcesses()) {
            this.registered.put(process.getId().getUniqueId(), process);
        }
    }

    @Listener
    public void handle(final ProcessRegisterEvent event) {
        this.publishNotification(
                Embedded.getInstance().getIngameMessages().getProcessRegistered(),
                event.getProcessInformation().getName()
        );
    }

    @Listener
    public void handle(final ProcessUnregisterEvent event) {
        if (!this.registered.containsKey(event.getProcessInformation().getId().getUniqueId())) {
            return;
        }

        this.publishNotification(
                Embedded.getInstance().getIngameMessages().getProcessStopped(),
                event.getProcessInformation().getName()
        );
        this.registered.remove(event.getProcessInformation().getId().getUniqueId());
    }

    @Listener
    public void handle(final ProcessUpdateEvent event) {
        ProcessInformation old = this.registered.put(event.getProcessInformation().getId().getUniqueId(), event.getProcessInformation());
        ProcessState state = event.getProcessInformation().getCurrentState();
        if (old != null) {
            if (!old.getCurrentState().isOnline() && event.getProcessInformation().getCurrentState().isOnline()) {
                this.publishNotification(
                        Embedded.getInstance().getIngameMessages().getProcessConnected(),
                        event.getProcessInformation().getName()
                );
            } else if (old.getCurrentState() != ProcessState.STARTED && event.getProcessInformation().getCurrentState() == ProcessState.STARTED) {
                this.publishNotification(
                        Embedded.getInstance().getIngameMessages().getProcessStarted(),
                        event.getProcessInformation().getName()
                );
            } else if (state != old.getCurrentState() && (state == ProcessState.RESTARTING || state == ProcessState.PAUSED)) {
                this.publishNotification(
                        Embedded.getInstance().getIngameMessages().getProcessStopped(),
                        event.getProcessInformation().getName()
                );
            }

            return;
        }

        if (state.isStartedOrOnline()) {
            this.publishNotification(
                    Embedded.getInstance().getIngameMessages().getProcessStarted(),
                    event.getProcessInformation().getName()
            );
        } else if (state == ProcessState.RESTARTING || state == ProcessState.PAUSED) {
            this.publishNotification(
                    Embedded.getInstance().getIngameMessages().getProcessStopped(),
                    event.getProcessInformation().getName()
            );
        }
    }

    private void publishNotification(String message, Object... replacements) {
        String replacedMessage = Embedded.getInstance().getIngameMessages().format(message, replacements);
        ProxyServer.getInstance().getPlayers().values()
                .stream()
                .filter(e -> e.hasPermission("reformcloud.notify"))
                .forEach(player -> player.sendMessage(replacedMessage));
    }
}