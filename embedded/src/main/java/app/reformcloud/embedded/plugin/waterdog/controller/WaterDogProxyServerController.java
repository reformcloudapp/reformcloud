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
package app.reformcloud.embedded.plugin.waterdog.controller;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.utils.config.ServerEntry;
import dev.waterdog.waterdogpe.utils.config.ServerList;
import org.jetbrains.annotations.NotNull;
import app.reformcloud.embedded.controller.SharedProxyServerController;
import app.reformcloud.process.ProcessInformation;

import java.util.ArrayList;

public class WaterDogProxyServerController extends SharedProxyServerController {

    public WaterDogProxyServerController() {
        ProxyServer.getInstance().getConfiguration().getPriorities().clear();
        ProxyServer.getInstance().getConfiguration().getForcedHosts().clear();

        final ServerList serverList = ProxyServer.getInstance().getConfiguration().getServerList();
        for (ServerEntry value : new ArrayList<>(serverList.values())) {
            serverList.remove(value.getServerName());
        }
    }

    @Override
    protected void registerServer(@NotNull ProcessInformation information) {
        ProxyServer.getInstance().registerServerInfo(this.constructServerInfo(information));
    }

    @Override
    protected void registerServerWhenUnknown(@NotNull ProcessInformation information) {
        if (ProxyServer.getInstance().getServerInfo(information.getName()) == null) {
            this.registerServer(information);
        }
    }

    @Override
    protected void unregisterServer(@NotNull ProcessInformation information) {
        ProxyServer.getInstance().removeServerInfo(information.getName());
    }

    @NotNull
    private ServerInfo constructServerInfo(@NotNull ProcessInformation information) {
        return new BedrockServerInfo(information.getName(), information.getHost().toInetSocketAddress(), null);
    }
}
