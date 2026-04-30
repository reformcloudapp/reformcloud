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
package app.reformcloud.node.argument;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import app.reformcloud.shared.StringUtil;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class DefaultArgumentParser implements ArgumentParser {

    private final Map<String, String> arguments;

    public DefaultArgumentParser(@NonNls String[] args) {
        this.arguments = StringUtil.parseArguments(args, 0);
    }

    @Override
    public boolean has(@NotNull String key) {
        return this.arguments.containsKey(key);
    }

    @Override
    public @NotNull Optional<String> getArgumentRaw(@NotNull String key) {
        return Optional.ofNullable(this.arguments.get(key));
    }

    @Override
    public boolean getBoolean(@NotNull String key) {
        return this.parse(key, Boolean::parseBoolean, false);
    }

    @Override
    public int getInt(@NotNull String key) {
        return this.parse(key, Integer::parseInt, 0);
    }

    @Override
    public long getLong(@NotNull String key) {
        return this.parse(key, Long::parseLong, 0L);
    }

    @Override
    public float getFloat(@NotNull String key) {
        return this.parse(key, Float::parseFloat, 0F);
    }

    @Override
    public double getDouble(@NotNull String key) {
        return this.parse(key, Double::parseDouble, 0D);
    }

    @Override
    public @NotNull <T> Optional<T> get(@NotNull String key, @NotNull Function<String, T> mapper, @Nullable T defaultValue) {
        String value = this.arguments.get(key);
        if (value == null) {
            return Optional.ofNullable(defaultValue);
        }

        try {
            return Optional.ofNullable(mapper.apply(value));
        } catch (IllegalArgumentException | ArithmeticException e) {
            return Optional.ofNullable(defaultValue);
        }
    }

    private <T> T parse(@NotNull String key, @NotNull Function<String, T> mapper, @NotNull T defaultValue) {
        String value = this.arguments.get(key);
        if (value == null) return defaultValue;

        try {
            T result = mapper.apply(value);
            return result != null ? result : defaultValue;
        } catch (IllegalArgumentException | ArithmeticException e) {
            return defaultValue;
        }
    }
}