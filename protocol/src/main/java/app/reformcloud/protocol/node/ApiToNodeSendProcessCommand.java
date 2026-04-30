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
package app.reformcloud.protocol.node;

import org.jetbrains.annotations.NotNull;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.network.PacketIds;
import app.reformcloud.network.channel.NetworkChannel;
import app.reformcloud.network.channel.listener.ChannelListener;
import app.reformcloud.network.data.ProtocolBuffer;
import app.reformcloud.process.ProcessInformation;
import app.reformcloud.protocol.ProtocolPacket;

import java.util.UUID;

public class ApiToNodeSendProcessCommand extends ProtocolPacket {

    private UUID processUniqueId;
    private String commandLine;

    public ApiToNodeSendProcessCommand() {
    }

    public ApiToNodeSendProcessCommand(ProcessInformation information, String commandLine) {
        this.processUniqueId = information.getId().getUniqueId();
        this.commandLine = commandLine;
    }

    @Override
    public int getId() {
        return PacketIds.EMBEDDED_BUS + 84;
    }

    @Override
    public void handlePacketReceive(@NotNull ChannelListener reader, @NotNull NetworkChannel channel) {
        ExecutorAPI.getInstance().getProcessProvider().getProcessByUniqueId(this.processUniqueId).ifPresent(wrapper -> wrapper.sendCommand(this.commandLine));
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeUniqueId(this.processUniqueId);
        buffer.writeString(this.commandLine);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.processUniqueId = buffer.readUniqueId();
        this.commandLine = buffer.readString();
    }
}
