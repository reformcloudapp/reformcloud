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
package app.reformcloud.embedded.process;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import app.reformcloud.embedded.Embedded;
import app.reformcloud.embedded.cache.ProcessCache;
import app.reformcloud.group.template.version.Version;
import app.reformcloud.process.ProcessInformation;
import app.reformcloud.process.builder.ProcessBuilder;
import app.reformcloud.protocol.node.*;
import app.reformcloud.provider.ProcessProvider;
import app.reformcloud.registry.service.ServiceRegistry;
import app.reformcloud.wrappers.ProcessWrapper;

import java.util.*;

public class DefaultEmbeddedProcessProvider implements ProcessProvider {

    private final ProcessCache processCache;

    public DefaultEmbeddedProcessProvider() {
        this.processCache = ServiceRegistry.getUnchecked(ProcessCache.class);
    }

    @NotNull
    @Override
    public Optional<ProcessWrapper> getProcessByName(@NotNull String name) {
        return this.processCache.provide(name).map(DefaultEmbeddedProcessWrapper::new);
    }

    @NotNull
    @Override
    public Optional<ProcessWrapper> getProcessByUniqueId(@NotNull UUID uniqueId) {
        return this.processCache.provide(uniqueId).map(DefaultEmbeddedProcessWrapper::new);
    }

    @NotNull
    @Override
    public ProcessBuilder createProcess() {
        return new DefaultEmbeddedProcessBuilder();
    }

    @NotNull
    @Override
    public @UnmodifiableView Collection<ProcessInformation> getProcesses() {
        return Embedded.getInstance().sendSyncQuery(new ApiToNodeGetProcessInformationObjects())
                .map(result -> {
                    if (result instanceof ApiToNodeGetProcessInformationObjectsResult) {
                        return ((ApiToNodeGetProcessInformationObjectsResult) result).getProcessInformation();
                    }

                    return new ArrayList<ProcessInformation>();
                }).orElseGet(Collections::emptyList);
    }

    @NotNull
    @Override
    public @UnmodifiableView Collection<ProcessInformation> getProcessesByProcessGroup(@NotNull String processGroup) {
        return Embedded.getInstance().sendSyncQuery(new ApiToNodeGetProcessInformationObjectsByProcessGroup(processGroup))
                .map(result -> {
                    if (result instanceof ApiToNodeGetProcessInformationObjectsResult) {
                        return ((ApiToNodeGetProcessInformationObjectsResult) result).getProcessInformation();
                    }

                    return new ArrayList<ProcessInformation>();
                }).orElseGet(Collections::emptyList);
    }

    @NotNull
    @Override
    public @UnmodifiableView Collection<ProcessInformation> getProcessesByMainGroup(@NotNull String mainGroup) {
        return Embedded.getInstance().sendSyncQuery(new ApiToNodeGetProcessInformationObjectsByMainGroup(mainGroup))
                .map(result -> {
                    if (result instanceof ApiToNodeGetProcessInformationObjectsResult) {
                        return ((ApiToNodeGetProcessInformationObjectsResult) result).getProcessInformation();
                    }

                    return new ArrayList<ProcessInformation>();
                }).orElseGet(Collections::emptyList);
    }

    @NotNull
    @Override
    public @UnmodifiableView Collection<ProcessInformation> getProcessesByVersion(@NotNull Version version) {
        return Embedded.getInstance().sendSyncQuery(new ApiToNodeGetProcessInformationObjectsByVersion(version))
                .map(result -> {
                    if (result instanceof ApiToNodeGetProcessInformationObjectsResult) {
                        return ((ApiToNodeGetProcessInformationObjectsResult) result).getProcessInformation();
                    }

                    return new ArrayList<ProcessInformation>();
                }).orElseGet(Collections::emptyList);
    }

    @NotNull
    @Override
    public @UnmodifiableView Collection<UUID> getProcessUniqueIds() {
        return Embedded.getInstance().sendSyncQuery(new ApiToNodeGetProcessUniqueIds())
                .map(result -> {
                    if (result instanceof ApiToNodeGetProcessUniqueIdsResult) {
                        return ((ApiToNodeGetProcessUniqueIdsResult) result).getUniqueIds();
                    }

                    return new ArrayList<UUID>();
                }).orElseGet(Collections::emptyList);
    }

    @Override
    public long getProcessCount() {
        return Embedded.getInstance().sendSyncQuery(new ApiToNodeGetProcessCount())
                .map(result -> {
                    if (result instanceof ApiToNodeGetProcessCountResult) {
                        return ((ApiToNodeGetProcessCountResult) result).getResult();
                    }

                    return 0L;
                }).orElseGet(() -> 0L);
    }

    @Override
    public long getProcessCount(@NotNull String processGroup) {
        return Embedded.getInstance().sendSyncQuery(new ApiToNodeGetProcessCountByProcessGroup(processGroup))
                .map(result -> {
                    if (result instanceof ApiToNodeGetProcessCountResult) {
                        return ((ApiToNodeGetProcessCountResult) result).getResult();
                    }

                    return 0L;
                }).orElseGet(() -> 0L);
    }

    @Override
    public void updateProcessInformation(@NotNull ProcessInformation processInformation) {
        Embedded.getInstance().sendPacket(new ApiToNodeUpdateProcessInformation(processInformation));
    }
}
