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
package app.reformcloud.proxy.waterdog;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.ProxyPingEvent;
import dev.waterdog.waterdogpe.plugin.Plugin;
import app.reformcloud.event.EventManager;
import app.reformcloud.proxy.plugin.PluginConfigHandler;
import app.reformcloud.proxy.waterdog.listener.WaterDogListener;
import app.reformcloud.proxy.waterdog.listener.WaterDogProxyConfigurationHandlerSetupListener;
import app.reformcloud.registry.service.ServiceRegistry;

public class WaterDogPlugin extends Plugin {

    @Override
    public void onEnable() {
        ServiceRegistry.getUnchecked(EventManager.class).registerListener(new WaterDogProxyConfigurationHandlerSetupListener());
        PluginConfigHandler.request(() -> ProxyServer.getInstance().getEventManager().subscribe(ProxyPingEvent.class, WaterDogListener.forProxyPing()));
    }
}
