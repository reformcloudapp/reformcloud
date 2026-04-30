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
package app.reformcloud.embedded.plugin.nukkit.executor;

import cn.nukkit.Server;
import cn.nukkit.level.Sound;
import org.jetbrains.annotations.NotNull;
import app.reformcloud.embedded.executor.PlayerAPIExecutor;
import app.reformcloud.enums.EnumUtil;

import java.time.Duration;
import java.util.UUID;

public class NukkitPlayerAPIExecutor extends PlayerAPIExecutor {

    @Override
    public void executeSendMessage(@NotNull UUID player, @NotNull String message) {
        Server.getInstance().getPlayer(player).ifPresent(val -> val.sendMessage(message));
    }

    @Override
    public void executeKickPlayer(@NotNull UUID player, @NotNull String message) {
        Server.getInstance().getPlayer(player).ifPresent(val -> val.sendMessage(message));
    }

    @Override
    public void executePlaySound(@NotNull UUID player, @NotNull String sound, float f1, float f2) {
        Sound nukkitSound = EnumUtil.findEnumFieldByName(Sound.class, sound).orElse(null);
        if (nukkitSound == null) {
            return;
        }

        Server.getInstance().getPlayer(player).ifPresent(val -> val.getLevel().addSound(val.getLocation(), nukkitSound, f1, f2, val));
    }

    @Override
    public void executeSendTitle(@NotNull UUID player, @NotNull String title, @NotNull String subtitle, Duration fadeIn, Duration stay, Duration fadeOut) {
        Server.getInstance().getPlayer(player).ifPresent(val -> val.sendTitle(
                title,
                subtitle,
                fadeIn == null ? 20 : (int) fadeIn.toMillis() / 50,
                stay == null ? 50 : (int) stay.toMillis() / 50,
                fadeOut == null ? 20 : (int) fadeOut.toMillis() / 50
        ));
    }

    @Override
    public void executeConnect(@NotNull UUID player, @NotNull String server) {

    }
}
