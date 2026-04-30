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
package app.reformcloud.proxy.application;

import app.reformcloud.configuration.JsonConfiguration;
import app.reformcloud.proxy.ProxyConfiguration;
import app.reformcloud.proxy.config.MotdConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class ConfigHelper {

    private static ProxyConfiguration proxyConfiguration;

    private ConfigHelper() {
        throw new UnsupportedOperationException();
    }

    public static void init(Path configFile) {
        if (Files.notExists(configFile)) {
            JsonConfiguration.newJsonConfiguration()
                    .add("config", new ProxyConfiguration(
                            Arrays.asList(
                                    new MotdConfiguration(
                                            "§b§lReform§f§lCloud §8» §7Server Network",
                                            "§b§lN§f§lews §8» §7§lWe are §a§lonline§7§l!",
                                            2
                                    ), new MotdConfiguration(
                                            "§b§lReform§f§lCloud §8» §7Check out §b§lGit§f§lHub",
                                            "§b§lN§f§lews §8» §7§lWe are §a§lonline§7§l!",
                                            2
                                    )
                            ), Arrays.asList(
                            new MotdConfiguration(
                                    "§b§lReform§f§lCloud §8» §7Server Network",
                                    "§b§lN§f§lews §8» §7§lWe are in §c§lmaintenance§7§l!",
                                    2
                            ), new MotdConfiguration(
                                    "§b§lReform§f§lCloud §8» §7Check out §b§lGit§f§lHub",
                                    "§b§lN§f§lews §8» §7§lWe are in §c§lmaintenance§7§l!",
                                    2
                            )
                    ))).write(configFile);
        }

        proxyConfiguration = JsonConfiguration.newJsonConfiguration(configFile).get("config", ProxyConfiguration.class);
    }

    public static ProxyConfiguration getProxyConfiguration() {
        return proxyConfiguration;
    }
}
