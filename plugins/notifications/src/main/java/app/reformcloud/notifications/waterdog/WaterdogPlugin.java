package app.reformcloud.notifications.waterdog;

import dev.waterdog.waterdogpe.plugin.Plugin;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.event.EventManager;
import app.reformcloud.notifications.waterdog.listener.ProcessListener;

public class WaterdogPlugin extends Plugin {

    private ProcessListener listener;

    @Override
    public void onEnable() {
        this.listener = new ProcessListener();
        ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(EventManager.class).registerListener(this.listener);
    }

    @Override
    public void onDisable() {
        ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(EventManager.class).unregisterListener(this.listener);
    }
}
