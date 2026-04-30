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
package app.reformcloud.embedded.group;

import org.jetbrains.annotations.NotNull;
import app.reformcloud.embedded.Embedded;
import app.reformcloud.group.process.ProcessGroup;
import app.reformcloud.network.packet.Packet;
import app.reformcloud.protocol.node.ApiToNodeCreateProcessGroup;
import app.reformcloud.protocol.node.ApiToNodeCreateProcessGroupResult;
import app.reformcloud.shared.group.DefaultProcessGroupBuilder;
import app.reformcloud.task.Task;

import java.util.Optional;

class DefaultEmbeddedProcessGroupBuilder extends DefaultProcessGroupBuilder {

    @NotNull
    @Override
    public Task<ProcessGroup> createPermanently() {
        return Task.supply(() -> {
            Optional<Packet> packet = Embedded.getInstance().sendSyncQuery(new ApiToNodeCreateProcessGroup(
                    super.name, super.staticGroup, super.lobby, super.showId, super.templates, super.playerAccessConfiguration, super.startupConfiguration
            ));
            if (!packet.isPresent() || !(packet.get() instanceof ApiToNodeCreateProcessGroupResult)) {
                return null;
            }

            return ((ApiToNodeCreateProcessGroupResult) packet.get()).getProcessGroup();
        });
    }
}
