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
package app.reformcloud.node.factory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.configuration.JsonConfiguration;
import app.reformcloud.group.process.ProcessGroup;
import app.reformcloud.group.template.Template;
import app.reformcloud.language.TranslationHolder;
import app.reformcloud.network.address.NetworkAddress;
import app.reformcloud.node.NodeExecutor;
import app.reformcloud.node.NodeInformation;
import app.reformcloud.node.cluster.ClusterManager;
import app.reformcloud.node.process.DefaultNodeProcessProvider;
import app.reformcloud.process.ProcessInformation;
import app.reformcloud.process.ProcessState;
import app.reformcloud.shared.process.DefaultIdentity;
import app.reformcloud.shared.process.DefaultProcessInformation;
import app.reformcloud.shared.process.DefaultProcessRuntimeInformation;
import app.reformcloud.task.Task;
import app.reformcloud.wrappers.NodeProcessWrapper;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DefaultProcessFactory implements ProcessFactory {

    private final DefaultNodeProcessProvider defaultNodeProcessProvider;

    public DefaultProcessFactory(DefaultNodeProcessProvider defaultNodeProcessProvider) {
        this.defaultNodeProcessProvider = defaultNodeProcessProvider;
    }

    @Override
    public @NotNull Task<ProcessInformation> buildProcessInformation(@NotNull ProcessFactoryConfiguration configuration) {
        return NodeExecutor.getInstance().getTaskScheduler().queue(() -> {
            Template template = configuration.getTemplate() == null ? this.nextTemplate(configuration.getProcessGroup()) : configuration.getTemplate();
            if (template == null) {
                System.err.println(TranslationHolder.translate("process-unable-to-find-template", configuration.getProcessGroup().getName()));
                return null;
            }

            NodeInformation nodeInformation = this.getNode(configuration.getNode()).orElseGet(() -> this.getBestNode(configuration.getProcessGroup()));
            if (nodeInformation == null) {
                System.err.println(TranslationHolder.translate("process-unable-to-find-node", configuration.getProcessGroup().getName()));
                return null;
            }

            int id = this.nextId(configuration.getProcessGroup().getName(), configuration.getId() <= 0 ? 1 : configuration.getId());
            UUID processUniqueId = this.preventCollision(configuration.getProcessUniqueId());

            ProcessInformation processInformation = new DefaultProcessInformation(
                    configuration.getExtra() == null ? JsonConfiguration.newJsonConfiguration() : configuration.getExtra(),
                    new DefaultIdentity(
                            configuration.getProcessGroup().getName() + template.getServerNameSplitter() + id,
                            configuration.getDisplayName() != null ? configuration.getDisplayName() : configuration.getProcessGroup().getName()
                                    + (configuration.getProcessGroup().showIdInName() ? template.getServerNameSplitter() + id : ""),
                            processUniqueId,
                            id,
                            nodeInformation.getName(),
                            nodeInformation.getUniqueId()
                    ),
                    NetworkAddress.address(
                            nodeInformation.getProcessStartHost().getHost(),
                            this.nextPort(template.getVersion().getDefaultStartPort())
                    ),
                    template,
                    configuration.getProcessGroup(),
                    DefaultProcessRuntimeInformation.EMPTY,
                    ConcurrentHashMap.newKeySet(),
                    ProcessState.CREATED,
                    configuration.getInitialState(),
                    configuration.getInclusions()
            );
            if (configuration.getMessageOfTheDay() != null) {
                processInformation.add("motd", configuration.getMessageOfTheDay());
            }

            this.defaultNodeProcessProvider.registerProcess(processInformation);
            ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(ClusterManager.class).publishProcessRegister(processInformation);

            return processInformation;
        });
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @NotNull
    @Override
    public String getName() {
        return DefaultProcessFactory.class.getName();
    }

    private @NotNull Optional<NodeInformation> getNode(@Nullable String nodeName) {
        if (nodeName == null) {
            return Optional.empty();
        }

        return ExecutorAPI.getInstance().getNodeInformationProvider().getNodeInformation(nodeName).map(NodeProcessWrapper::getNodeInformation);
    }

    private @Nullable NodeInformation getBestNode(@NotNull ProcessGroup processGroup) {
        var config = processGroup.getStartupConfiguration();
        var allowed = config.getStartingNodes();

        return ExecutorAPI.getInstance()
                .getNodeInformationProvider()
                .getNodes()
                .stream()
                .filter(node -> allowed.isEmpty() || allowed.contains(node.getName()))
                .min((a, b) -> {
                    int mem = Long.compare(a.getUsedMemory(), b.getUsedMemory());
                    if (mem != 0) return mem;

                    var cpuA = a.getProcessRuntimeInformation().getCpuUsageSystem();
                    var cpuB = b.getProcessRuntimeInformation().getCpuUsageSystem();

                    if (cpuA == -1 || cpuB == -1) return -1; // a ist besser
                    return Double.compare(cpuA, cpuB);
                })
                .orElse(null);
    }

    private int nextId(@NotNull String groupName, int beginId) {
        var provider = ExecutorAPI.getInstance().getProcessProvider();

        var ids = provider.getProcessesByProcessGroup(groupName)
                .stream()
                .map(process -> process.getId().getId())
                .collect(Collectors.toSet());

        while (ids.contains(beginId)) {
            beginId++;
        }

        return beginId;
    }

    private int nextPort(int start) {
        var provider = ExecutorAPI.getInstance().getProcessProvider();

        var ports = provider.getProcesses()
                .stream()
                .map(process -> process.getHost().getPort())
                .collect(Collectors.toSet());

        int port = start;
        while (ports.contains(port)) {
            port++;
        }

        return port;
    }

    private @Nullable Template nextTemplate(@NotNull ProcessGroup processGroup) {
        return processGroup.getTemplates()
                .stream()
                .filter(template -> !template.isGlobal())
                .max(Comparator.comparingInt(Template::getPriority))
                .orElse(null);
    }

    private @NotNull UUID preventCollision(@NotNull UUID current) {
        var provider = ExecutorAPI.getInstance().getProcessProvider();

        do {
            var exists = provider.getProcessByUniqueId(current).isPresent();
            if (!exists) break;
            current = UUID.randomUUID();
        } while (true);

        return current;
    }
}
