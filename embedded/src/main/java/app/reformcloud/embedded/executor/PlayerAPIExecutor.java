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
package app.reformcloud.embedded.executor;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public abstract class PlayerAPIExecutor {

    private static PlayerAPIExecutor instance;

    public static PlayerAPIExecutor getInstance() {
        return instance;
    }

    public static void setInstance(PlayerAPIExecutor instance) {
        PlayerAPIExecutor.instance = Objects.requireNonNull(instance);
    }


    public abstract void executeSendMessage(@NotNull UUID player, @NotNull String message);

    public abstract void executeKickPlayer(@NotNull UUID player, @NotNull String message);

    public abstract void executePlaySound(@NotNull UUID player, @NotNull String sound, float f1, float f2);

    public abstract void executeSendTitle(@NotNull UUID player, @NotNull String title, @NotNull String subtitle, Duration fadeIn, Duration stay, Duration fadeOut);

    public abstract void executeConnect(@NotNull UUID player, @NotNull String server);
}
