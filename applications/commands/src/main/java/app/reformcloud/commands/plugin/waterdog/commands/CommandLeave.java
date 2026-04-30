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
package app.reformcloud.commands.plugin.waterdog.commands;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.command.CommandSettings;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import app.reformcloud.embedded.Embedded;
import app.reformcloud.embedded.controller.ProxyServerController;
import app.reformcloud.embedded.plugin.waterdog.fallback.WaterDogFallbackExtraFilter;
import app.reformcloud.embedded.shared.SharedPlayerFallbackFilter;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.process.ProcessInformation;

import java.util.Optional;

public class CommandLeave extends Command {

    public CommandLeave(String name, String[] aliases) {
        super(name, CommandSettings.builder().setAliases(aliases).build());
    }

    @Override
    public boolean onExecute(CommandSender commandSender, String s, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer player)) {
            return false;
        }

        if (player.getServerInfo() == null) {
            return true;
        }

        if (ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(ProxyServerController.class).getCachedLobbyServers().stream().anyMatch(
                e -> e.getName().equals(player.getServerInfo().getServerName()))
        ) {
            player.sendMessage(Embedded.getInstance().getIngameMessages().format(Embedded.getInstance().getIngameMessages().getAlreadyConnectedToHub()));
            return true;
        }

        final Optional<ProcessInformation> fallback = SharedPlayerFallbackFilter.filterFallback(
                player.getUniqueId(),
                ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(ProxyServerController.class).getCachedLobbyServers(),
                player::hasPermission,
                WaterDogFallbackExtraFilter.INSTANCE,
                player.getServerInfo().getServerName()
        );
        if (fallback.isPresent()) {
            final ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(fallback.get().getName());
            if (serverInfo == null) {
                player.sendMessage(Embedded.getInstance().getIngameMessages().format(Embedded.getInstance().getIngameMessages().getNoHubServerAvailable()));
                return true;
            }

            player.sendMessage(Embedded.getInstance().getIngameMessages().format(Embedded.getInstance().getIngameMessages().getConnectingToHub(), fallback.get().getName()));
            player.connect(serverInfo);
        } else {
            player.sendMessage(Embedded.getInstance().getIngameMessages().format(Embedded.getInstance().getIngameMessages().getNoHubServerAvailable()));
        }
        return true;
    }
}
