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
package app.reformcloud.proxy.plugin;

import org.jetbrains.annotations.NotNull;
import app.reformcloud.embedded.Embedded;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.network.packet.PacketProvider;
import app.reformcloud.proxy.ProxyConfigurationHandler;
import app.reformcloud.proxy.application.network.PacketRequestConfig;
import app.reformcloud.proxy.application.network.PacketRequestConfigResult;
import app.reformcloud.proxy.network.PacketProxyConfigUpdate;

public final class PluginConfigHandler {

    private PluginConfigHandler() {
        throw new UnsupportedOperationException();
    }

    public static void request(@NotNull Runnable then) {
        ProxyConfigurationHandler.setup();

        ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(PacketProvider.class).registerPacket(PacketRequestConfigResult.class);
        ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(PacketProvider.class).registerPacket(PacketProxyConfigUpdate.class);

        Embedded.getInstance().sendSyncQuery(new PacketRequestConfig()).ifPresent(result -> {
            if (result instanceof PacketRequestConfigResult) {
                ProxyConfigurationHandler.getInstance().handleProxyConfigUpdate(((PacketRequestConfigResult) result).getProxyConfiguration());
                then.run();
            }
        });
    }
}
