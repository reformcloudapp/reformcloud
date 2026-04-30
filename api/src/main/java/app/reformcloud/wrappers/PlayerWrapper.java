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
package app.reformcloud.wrappers;

import org.jetbrains.annotations.NotNull;
import app.reformcloud.process.ProcessInformation;
import app.reformcloud.task.Task;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * A wrapper for a connected player
 */
public interface PlayerWrapper {

    /**
     * Get the proxy process the player is currently on
     *
     * @return An optional process information which is present if the player is currently connected to a proxy
     */
    @NotNull Task<Optional<ProcessInformation>> getConnectedProxy();

    /**
     * Get the server process the player is currently on
     *
     * @return An optional process information which is present if the player is currently connected to a server
     */
    @NotNull Task<Optional<ProcessInformation>> getConnectedServer();

    /**
     * Get the proxy unique id the player is currently on
     *
     * @return An optional unique id which is present if the player is currently connected to a proxy
     */
    @NotNull Optional<UUID> getConnectedProxyUniqueId();

    /**
     * Get the server unique id the player is currently on
     *
     * @return An optional unique id which is present if the player is currently connected to a server
     */
    @NotNull Optional<UUID> getConnectedServerUniqueId();

    /**
     * Sends a message to the current player using the proxy the player is currently connected to.
     * <p>If the player is not connected this method has no effect.</p>
     *
     * @param message The message which should get sent to the player
     */
    void sendMessage(@NotNull String message);

    /**
     * Disconnects the player from his connected proxy.
     * <p>If the player is not connected this method has no effect.</p>
     *
     * @param kickReason The reason for the disconnect
     */
    void disconnect(@NotNull String kickReason);

    /**
     * Sends a title to the player using it's currently connected proxy.
     * <p>If the player is not connected this method has no effect.</p>
     *
     * @param title The title to send
     */
    void sendTitle(@NotNull String title, @NotNull String subtitle, Duration fadeIn, Duration stay, Duration fadeOut);

    /**
     * Connects / transfers the player to the specified server, gracefully closing the current one
     * using the player's currently connected proxy.
     * <p>If the player is not connected this method has no effect.</p>
     *
     * @param server The name of the server to connect to
     */
    void connect(@NotNull String server);

    /**
     * Connects / transfers the player to the specified player's server, gracefully closing the current one
     * using the player's currently connected proxy.
     * <p>If either this or the target player is not connected this method has no effect.</p>
     *
     * @param otherPlayer The player to connect to
     */
    void connect(@NotNull UUID otherPlayer);
}
