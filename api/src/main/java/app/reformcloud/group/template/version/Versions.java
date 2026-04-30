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
package app.reformcloud.group.template.version;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import app.reformcloud.group.template.ProcessConfigurators;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public final class Versions {
    private static final Map<String, Version> VERSIONS = new ConcurrentHashMap<>();

    // ----
    // SERVERS
    // ----
    public static final Version NUKKITX = server("NUKKITX", ProcessConfigurators.NUKKIT, "https://repo.opencollab.dev/api/maven/latest/file/maven-snapshots/cn/nukkit/nukkit/1.0-SNAPSHOT?extension=jar");
    // ----
    // PROXIES
    // ----
    public static final Version WATERDOGPE = proxy("WATERDOGPE", ProcessConfigurators.WATERDOG, "https://github.com/WaterdogPE/WaterdogPE/releases/download/latest/Waterdog.jar");

    private Versions() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public static Optional<Version> getByName(@NotNull String name) {
        return Optional.ofNullable(VERSIONS.get(name.toUpperCase()));
    }

    @NotNull
    @Unmodifiable
    public static Map<String, Version> getKnownVersions() {
        return Collections.unmodifiableMap(VERSIONS);
    }

    @NotNull
    public static String formatVersion(@NotNull Version version) {
        final String lower = version.getName().toLowerCase(Locale.ROOT);
        return lower.replace('_', '-') + ".jar";
    }

    @SuppressWarnings("SameParameterValue")
    private static Version server(String versionName, String configurator, String downloadUrl) {
        return version(versionName, configurator, downloadUrl, VersionType.SERVER, 41000, true);
    }

    @SuppressWarnings("SameParameterValue")
    private static Version proxy(String versionName, String configurator, String downloadUrl) {
        return version(versionName, configurator, downloadUrl, VersionType.PROXY, 19132, true);
    }

    @SuppressWarnings("SameParameterValue")
    private static Version version(String versionName, String configurator, String downloadUrl, VersionType versionType, int defaultStartPort, boolean nativeTransportSupported) {
        Version version = Version.version(versionName, downloadUrl, VersionInstaller.DOWNLOADING, configurator, versionType, defaultStartPort, nativeTransportSupported, null);
        VERSIONS.put(versionName.toUpperCase(), version);
        return version;
    }
}
