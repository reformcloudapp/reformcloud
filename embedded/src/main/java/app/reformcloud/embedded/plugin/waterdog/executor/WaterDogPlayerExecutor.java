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
package app.reformcloud.embedded.plugin.waterdog.executor;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;
import app.reformcloud.embedded.executor.PlayerAPIExecutor;

import java.time.Duration;
import java.util.UUID;

public class WaterDogPlayerExecutor extends PlayerAPIExecutor {

    @Override
    public void executeSendMessage(@NotNull UUID player, @NotNull String message) {
        final ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(player);
        if (proxiedPlayer != null) {
            proxiedPlayer.sendMessage(message);
        }
    }

    @Override
    public void executeKickPlayer(@NotNull UUID player, @NotNull String message) {
        final ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(player);
        if (proxiedPlayer != null) {
            proxiedPlayer.disconnect(message);
        }
    }

    @Override
    public void executePlaySound(@NotNull UUID player, @NotNull String sound, float f1, float f2) {
    }

    @Override
    public void executeSendTitle(@NotNull UUID player, @NotNull String title, @NotNull String subtitle, Duration fadeIn, Duration stay, Duration fadeOut) {
        final ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(player);
        if (proxiedPlayer != null) {
            proxiedPlayer.sendTitle(
                    title,
                    subtitle,
                    fadeIn == null ? 20 : (int) fadeIn.toMillis() / 50,
                    stay == null ? 50 : (int) stay.toMillis() / 50,
                    fadeOut == null ? 20 : (int) fadeOut.toMillis() / 50
            );
        }
    }

    @Override
    public void executeConnect(@NotNull UUID player, @NotNull String server) {
        final ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(player);
        final ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(server);
        if (proxiedPlayer != null && serverInfo != null) {
            proxiedPlayer.connect(serverInfo);
        }
    }
}
