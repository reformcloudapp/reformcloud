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
package app.reformcloud.node.process.configurator.defaults.waterdog;

import org.jetbrains.annotations.NotNull;
import app.reformcloud.group.template.ProcessConfigurators;
import app.reformcloud.node.process.DefaultNodeLocalProcessWrapper;
import app.reformcloud.node.process.configurator.ProcessConfigurator;
import app.reformcloud.node.process.configurator.defaults.ConfiguratorUtils;

public class WaterdogConfigurator implements ProcessConfigurator {

    @Override
    public void configure(@NotNull DefaultNodeLocalProcessWrapper wrapper) {
        ConfiguratorUtils.extractCompiledFile("files/mcpe/waterdog/config.yml", wrapper.getPath().resolve("config.yml"));
        ConfiguratorUtils.rewriteFile(wrapper.getPath().resolve("config.yml"), line -> {
            if (line.trim().startsWith("  host: ")) {
                line = "  host: '" + ConfiguratorUtils.formatHost(wrapper) + "'";
            } else if (line.trim().startsWith("use_login_extras: ")) {
                line = "use_login_extras: true";
            } else if (line.trim().startsWith("  max_players: ") && wrapper.getProcessInformation().getProcessGroup().getPlayerAccessConfiguration().isUsePlayerLimit()) {
                line = "  max_players: " + wrapper.getProcessInformation().getProcessGroup().getPlayerAccessConfiguration().getMaxPlayers();
            }
            return line;
        });
    }

    @Override
    public String getName() {
        return ProcessConfigurators.WATERDOG;
    }
}
