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
package app.reformcloud.embedded.plugin.waterdog.reconnect;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.connection.handler.IJoinHandler;
import dev.waterdog.waterdogpe.network.connection.handler.IReconnectHandler;
import dev.waterdog.waterdogpe.network.connection.handler.ReconnectReason;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import app.reformcloud.embedded.controller.ProxyServerController;
import app.reformcloud.embedded.plugin.waterdog.fallback.WaterDogFallbackExtraFilter;
import app.reformcloud.embedded.shared.SharedPlayerFallbackFilter;
import app.reformcloud.ExecutorAPI;

public final class ReformCloudConnectionHandler implements IReconnectHandler, IJoinHandler {

    public static ReformCloudConnectionHandler INSTANCE = new ReformCloudConnectionHandler();

    private ReformCloudConnectionHandler() {
    }

    @Override
    public ServerInfo getFallbackServer(ProxiedPlayer player, ServerInfo oldServer, ReconnectReason reason, String kickMessage) {
        return this.determineServer(player);
    }

    @Override
    public ServerInfo determineServer(ProxiedPlayer proxiedPlayer) {
        return SharedPlayerFallbackFilter.filterFallback(
                proxiedPlayer.getUniqueId(),
                ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(ProxyServerController.class).getCachedLobbyServers(),
                proxiedPlayer::hasPermission,
                WaterDogFallbackExtraFilter.INSTANCE,
                proxiedPlayer.getServerInfo() == null ? null : proxiedPlayer.getServerInfo().getServerName()
        ).map(info -> ProxyServer.getInstance().getServerInfo(info.getName())).orElse(null);
    }
}
