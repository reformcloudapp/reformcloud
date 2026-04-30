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
package app.reformcloud.embedded.player;

import org.jetbrains.annotations.NotNull;
import app.reformcloud.embedded.Embedded;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.process.ProcessInformation;
import app.reformcloud.protocol.node.ApiToNodeConnectPlayerToPlayer;
import app.reformcloud.protocol.node.ApiToNodeGetCurrentPlayerProcessUniqueIds;
import app.reformcloud.protocol.node.ApiToNodeGetCurrentPlayerProcessUniqueIdsResult;
import app.reformcloud.protocol.shared.*;
import app.reformcloud.shared.collect.Entry2;
import app.reformcloud.task.Task;
import app.reformcloud.wrappers.PlayerWrapper;
import app.reformcloud.wrappers.ProcessWrapper;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class DefaultEmbeddedPlayerWrapper implements PlayerWrapper {

    private final UUID playerUniqueId;

    DefaultEmbeddedPlayerWrapper(UUID playerUniqueId) {
        this.playerUniqueId = playerUniqueId;
    }

    @NotNull
    private Optional<Entry2<UUID, UUID>> getPlayerProcess() {
        return Embedded.getInstance().sendSyncQuery(new ApiToNodeGetCurrentPlayerProcessUniqueIds(this.playerUniqueId))
                .map(result -> {
                    if (result instanceof ApiToNodeGetCurrentPlayerProcessUniqueIdsResult) {
                        return Optional.ofNullable(((ApiToNodeGetCurrentPlayerProcessUniqueIdsResult) result).getResult());
                    }

                    return Optional.<Entry2<UUID, UUID>>empty();
                }).orElseGet(Optional::empty);
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
        Embedded.getInstance().sendPacket(new PacketSendPlayerMessage(this.playerUniqueId, message));
    }

    @Override
    public void disconnect(@NotNull String kickReason) {
        Embedded.getInstance().sendPacket(new PacketDisconnectPlayer(this.playerUniqueId, kickReason));
    }

    @Override
    public void sendTitle(@NotNull String title, @NotNull String subtitle, Duration fadeIn, Duration stay, Duration fadeOut) {
        Embedded.getInstance().sendPacket(new PacketSendPlayerTitle(
                this.playerUniqueId,
                title,
                subtitle,
                fadeIn == null ? 0 : fadeIn.toMillis(),
                stay == null ? 0 : stay.toMillis(),
                fadeOut == null ? 0 : fadeOut.toMillis()
        ));
    }

    @Override
    public void connect(@NotNull String server) {
        Embedded.getInstance().sendPacket(new PacketConnectPlayerToServer(this.playerUniqueId, server));
    }

    @Override
    public void connect(@NotNull UUID otherPlayer) {
        Embedded.getInstance().sendPacket(new ApiToNodeConnectPlayerToPlayer(this.playerUniqueId, otherPlayer));
    }
}
