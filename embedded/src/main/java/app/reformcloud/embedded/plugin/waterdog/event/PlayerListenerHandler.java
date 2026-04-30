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
package app.reformcloud.embedded.plugin.waterdog.event;

import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectedEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import org.jetbrains.annotations.NotNull;
import app.reformcloud.embedded.Embedded;
import app.reformcloud.embedded.controller.ProxyServerController;
import app.reformcloud.embedded.plugin.waterdog.WaterDogExecutor;
import app.reformcloud.embedded.shared.SharedDisconnectHandler;
import app.reformcloud.embedded.shared.SharedJoinAllowChecker;
import app.reformcloud.registry.service.ServiceRegistry;
import app.reformcloud.shared.collect.Entry2;

import java.util.function.Consumer;

public final class PlayerListenerHandler {

    private PlayerListenerHandler() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public static Consumer<PlayerLoginEvent> handlerForLogin() {
        return event -> {
            if (!Embedded.getInstance().isReady()) {
                event.setCancelReason(Embedded.getInstance().getIngameMessages().format(Embedded.getInstance().getIngameMessages().getProcessNotReadyToAcceptPlayersMessage()));
                event.setCancelled(true);
                return;
            }

            Entry2<Boolean, String> checked = SharedJoinAllowChecker.checkIfConnectAllowed(
                    event.getPlayer()::hasPermission,
                    WaterDogExecutor.getInstance().getIngameMessages(),
                    ServiceRegistry.getUnchecked(ProxyServerController.class).getCachedProxies(),
                    event.getPlayer().getUniqueId(),
                    event.getPlayer().getName()
            );
            if (!checked.getFirst() && checked.getSecond() != null) {
                event.setCancelReason(checked.getSecond());
                event.setCancelled(true);
            }
        };
    }

    @NotNull
    public static Consumer<PlayerDisconnectedEvent> handlerForDisconnect() {
        return event -> SharedDisconnectHandler.handleDisconnect(event.getPlayer().getUniqueId());
    }
}
