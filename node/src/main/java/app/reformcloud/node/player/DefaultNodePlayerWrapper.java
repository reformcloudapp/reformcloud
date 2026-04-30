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
package app.reformcloud.node.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.network.channel.manager.ChannelManager;
import app.reformcloud.network.packet.Packet;
import app.reformcloud.node.NodeExecutor;
import app.reformcloud.node.protocol.*;
import app.reformcloud.process.ProcessInformation;
import app.reformcloud.protocol.shared.*;
import app.reformcloud.shared.collect.Entry2;
import app.reformcloud.task.Task;
import app.reformcloud.wrappers.PlayerWrapper;
import app.reformcloud.wrappers.ProcessWrapper;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class DefaultNodePlayerWrapper implements PlayerWrapper {

    private final UUID uniqueId;

    DefaultNodePlayerWrapper(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    @NotNull
    private Optional<Entry2<UUID, UUID>> getPlayerProcess() {
        UUID proxy = null;
        UUID server = null;

        for (ProcessInformation process : ExecutorAPI.getInstance().getProcessProvider().getProcesses()) {
            if (process.getPrimaryTemplate().getVersion().getVersionType().isServer()
                    && process.getPlayerByUniqueId(this.uniqueId).isPresent()
                    && server == null
            ) {
                server = process.getId().getUniqueId();
            } else if (process.getPrimaryTemplate().getVersion().getVersionType().isProxy()
                    && process.getPlayerByUniqueId(this.uniqueId).isPresent()
                    && proxy == null
            ) {
                proxy = process.getId().getUniqueId();
            }
        }

        return proxy == null || server == null ? Optional.empty() : Optional.of(new Entry2<>(proxy, server));
    }

    @Override
    public @NotNull Task<Optional<ProcessInformation>> getConnectedProxy() {
        return Task.supply(() -> this.getPlayerProcess()
                .flatMap(duo -> ExecutorAPI.getInstance().getProcessProvider().getProcessByUniqueId(duo.getFirst()))
                .map(ProcessWrapper::getProcessInformation));
    }

    @Override
    public @NotNull Task<Optional<ProcessInformation>> getConnectedServer() {
        return Task.supply(() -> this.getPlayerProcess()
                .flatMap(duo -> ExecutorAPI.getInstance().getProcessProvider().getProcessByUniqueId(duo.getSecond()))
                .map(ProcessWrapper::getProcessInformation));
    }

    @Override
    public @NotNull Optional<UUID> getConnectedProxyUniqueId() {
        return this.getPlayerProcess().map(Entry2::getFirst);
    }

    @Override
    public @NotNull Optional<UUID> getConnectedServerUniqueId() {
        return this.getPlayerProcess().map(Entry2::getSecond);
    }

    @Override
    public void sendMessage(@NotNull String message) {
        final ProcessInformation proxy = this.getPlayerProxy();
        if (proxy != null) {
            if (proxy.getId().getNodeUniqueId().equals(NodeExecutor.getInstance().getNodeConfig().getUniqueID())) {
                this.sendPacketToPlayerProxy(new PacketSendPlayerMessage(this.uniqueId, message));
            } else {
                this.sendPacketToParent(proxy, new NodeToNodeSendPlayerMessage(this.uniqueId, message));
            }
        }
    }

    @Override
    public void disconnect(@NotNull String kickReason) {
        final ProcessInformation proxy = this.getPlayerProxy();
        if (proxy != null) {
            if (proxy.getId().getNodeUniqueId().equals(NodeExecutor.getInstance().getNodeConfig().getUniqueID())) {
                this.sendPacketToPlayerProxy(new PacketDisconnectPlayer(this.uniqueId, kickReason));
            } else {
                this.sendPacketToParent(proxy, new NodeToNodeDisconnectPlayer(this.uniqueId, kickReason));
            }
        }
    }

    @Override
    public void sendTitle(@NotNull String title, @NotNull String subtitle, Duration fadeIn, Duration stay, Duration fadeOut) {
        final ProcessInformation proxy = this.getPlayerProxy();
        if (proxy != null) {
            if (proxy.getId().getUniqueId().equals(NodeExecutor.getInstance().getNodeConfig().getUniqueID())) {
                this.sendPacketToPlayerProxy(new PacketSendPlayerTitle(
                        this.uniqueId,
                        title,
                        subtitle,
                        fadeIn == null ? 0 : fadeIn.toMillis(),
                        stay == null ? 0 : stay.toMillis(),
                        fadeOut == null ? 0 : fadeOut.toMillis()
                ));
            } else {
                this.sendPacketToParent(proxy, new NodeToNodeSendPlayerTitle(
                        this.uniqueId,
                        title,
                        subtitle,
                        fadeIn == null ? 0 : fadeIn.toMillis(),
                        stay == null ? 0 : stay.toMillis(),
                        fadeOut == null ? 0 : fadeOut.toMillis()
                ));
            }
        }
    }

    @Override
    public void connect(@NotNull String server) {
        ProcessInformation proxy = this.getPlayerProxy();
        if (proxy == null) {
            return;
        }

        if (proxy.getId().getNodeUniqueId().equals(NodeExecutor.getInstance().getNodeConfig().getUniqueID())) {
            this.sendPacketToPlayerProxy(new PacketConnectPlayerToServer(this.uniqueId, server));
        } else {
            this.sendPacketToParent(proxy, new NodeToNodeSendPlayerToServer(this.uniqueId, server));
        }
    }

    @Override
    public void connect(@NotNull UUID otherPlayer) {
        for (ProcessInformation process : ExecutorAPI.getInstance().getProcessProvider().getProcesses()) {
            if (process.getPrimaryTemplate().getVersion().getVersionType().isServer()
                    && process.getPlayerByUniqueId(otherPlayer).isPresent()
            ) {
                this.connect(process.getName());
                break;
            }
        }
    }

    private void sendPacketToPlayerProxy(@NotNull Packet packet) {
        ProcessInformation proxy = this.getPlayerProxy();
        if (proxy == null) {
            return;
        }

        ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(ChannelManager.class)
                .getChannel(proxy.getName())
                .ifPresent(channel -> channel.sendPacket(packet));
    }

    private void sendPacketToParent(@NotNull ProcessInformation processInformation, @NotNull Packet packet) {
        ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(ChannelManager.class)
                .getChannel(processInformation.getId().getNodeName())
                .ifPresent(channel -> channel.sendPacket(packet));
    }

    private @Nullable ProcessInformation getPlayerProxy() {
        for (ProcessInformation process : ExecutorAPI.getInstance().getProcessProvider().getProcesses()) {
            if (!process.getPrimaryTemplate().getVersion().getVersionType().isServer()
                    && process.getPlayerByUniqueId(this.uniqueId).isPresent()
            ) {
                return process;
            }
        }

        return null;
    }

    private @Nullable ProcessInformation getPlayerServer() {
        for (ProcessInformation process : ExecutorAPI.getInstance().getProcessProvider().getProcesses()) {
            if (process.getPrimaryTemplate().getVersion().getVersionType().isServer()
                    && process.getPlayerByUniqueId(this.uniqueId).isPresent()
            ) {
                return process;
            }
        }

        return null;
    }
}
