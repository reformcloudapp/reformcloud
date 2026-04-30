/*
 * This file is part of reformcloud, licensed under the MIT License (MIT).
 *
 * Copyright (c) ReformCloud <https://github.com/reformcloudapp>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package app.reformcloud.embedded.plugin.waterdog;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.EventPriority;
import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectedEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;
import app.reformcloud.embedded.Embedded;
import app.reformcloud.embedded.controller.ProcessEventHandler;
import app.reformcloud.embedded.controller.ProxyServerController;
import app.reformcloud.embedded.executor.PlayerAPIExecutor;
import app.reformcloud.embedded.plugin.waterdog.controller.WaterDogProxyServerController;
import app.reformcloud.embedded.plugin.waterdog.event.PlayerListenerHandler;
import app.reformcloud.embedded.plugin.waterdog.executor.WaterDogPlayerExecutor;
import app.reformcloud.embedded.plugin.waterdog.reconnect.ReformCloudConnectionHandler;
import app.reformcloud.embedded.shared.SharedInvalidPlayerFixer;
import app.reformcloud.ExecutorType;
import app.reformcloud.event.EventManager;
import app.reformcloud.process.ProcessInformation;
import app.reformcloud.shared.process.DefaultPlayer;

public final class WaterDogExecutor extends Embedded {

    public WaterDogExecutor() {
        super.type = ExecutorType.API;

        PlayerAPIExecutor.setInstance(new WaterDogPlayerExecutor());

        ProxyServer.getInstance().getEventManager().subscribe(PlayerLoginEvent.class, PlayerListenerHandler.handlerForLogin(), EventPriority.HIGH);
        ProxyServer.getInstance().getEventManager().subscribe(PlayerDisconnectedEvent.class, PlayerListenerHandler.handlerForDisconnect(), EventPriority.LOWEST);

        ProxyServer.getInstance().setJoinHandler(ReformCloudConnectionHandler.INSTANCE);
        ProxyServer.getInstance().setReconnectHandler(ReformCloudConnectionHandler.INSTANCE);

        super.getServiceRegistry().setProvider(ProxyServerController.class, new WaterDogProxyServerController(), true);
        super.getServiceRegistry().getProviderUnchecked(EventManager.class).registerListener(new ProcessEventHandler());

        this.fixInvalidPlayers();
    }

    @Override
    public int getPlayerCount() {
        return ProxyServer.getInstance().getPlayers().size();
    }

    @Override
    protected int getMaxPlayersOfEnvironment() {
        return ProxyServer.getInstance().getConfiguration().getMaxPlayerCount();
    }

    @Override
    protected void updatePlayersOfEnvironment(@NotNull ProcessInformation information) {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers().values()) {
            if (information.getPlayerByUniqueId(player.getUniqueId()).isEmpty()) {
                information.getPlayers().add(new DefaultPlayer(player.getUniqueId(), player.getName(), System.currentTimeMillis()));
            }
        }
    }

    private void fixInvalidPlayers() {
        SharedInvalidPlayerFixer.start(
                uuid -> ProxyServer.getInstance().getPlayer(uuid) != null,
                () -> ProxyServer.getInstance().getPlayers().size()
        );
    }
}
