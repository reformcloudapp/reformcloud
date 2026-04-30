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
package app.reformcloud.node.commands;

import app.reformcloud.wrappers.ProcessWrapper;
import org.jetbrains.annotations.NotNull;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.command.Command;
import app.reformcloud.command.CommandSender;
import app.reformcloud.group.main.MainGroup;
import app.reformcloud.group.process.ProcessGroup;
import app.reformcloud.group.template.Template;
import app.reformcloud.group.template.backend.TemplateBackend;
import app.reformcloud.group.template.version.Version;
import app.reformcloud.group.template.version.Versions;
import app.reformcloud.language.TranslationHolder;
import app.reformcloud.node.template.TemplateBackendManager;
import app.reformcloud.process.ProcessInformation;
import app.reformcloud.process.ProcessState;
import app.reformcloud.shared.StringUtil;
import app.reformcloud.shared.parser.Parsers;
import app.reformcloud.utility.MoreCollections;

import java.util.*;
import java.util.stream.Collectors;

public final class CommandGroup implements Command {

    public void describeCommandToSender(@NotNull CommandSender source) {
        source.sendMessages((
                "group <list>                                   | Shows all registered main and process groups\n" +
                        "group <sub | main> <name> [info]               | Shows information about a specific group\n" +
                        "group <sub | main> <name> [delete]             | Deletes the specified process group\n" +
                        "group <sub | main> <name> [stop]               | Stops either all non-prepared processes of the group or all sub groups of the main group which are not prepared\n" +
                        "group <sub | main> <name> [kill]               | Stops either all processes of the group or all sub groups of the main group\n" +
                        " \n" +
                        "group <sub> <name> [edit]                      | Edits the specified group\n" +
                        " --maintenance=[maintenance]                   | Enables or disables the maintenance mode\n" +
                        " --static=[static]                             | Enables or disables the deleting of the process after the stop\n" +
                        " --lobby=[lobby]                               | Sets if the group can be used as lobby\n" +
                        " --max-players=[max]                           | Sets the max player count for the process\n" +
                        " --min-process-count=[min]                     | Sets the min process count for the process\n" +
                        " --max-process-count=[max]                     | Sets the max process count for the process\n" +
                        " --always-prepared-process-count=[count]       | Sets the count of processes which should always be prepared\n" +
                        " --max-memory=[default/memory]                 | Sets the max memory of the template (format: <template-name>/<max-memory>)\n" +
                        " --startup-pickers=[Node1;Node2]               | Sets the startup pickers for the group\n" +
                        " --add-startup-pickers=[Node1;Node2]           | Adds the specified startup pickers to the group\n" +
                        " --remove-startup-pickers=[Node1;Node2]        | Removes the specified startup pickers from the group\n" +
                        " --clear-startup-pickers=true                  | Clears the startup pickers\n" +
                        " --templates=[default/FILE/WATERFALL;...]      | Sets the templates of the group (format: <name>/<backend>/<version>)\n" +
                        " --add-templates=[default/FILE/WATERFALL;...]  | Adds the specified templates to the group (format: <name>/<backend>/<version>)\n" +
                        " --remove-templates=[default;global]           | Removes the specified templates from the group\n" +
                        " --clear-templates=true                        | Clears the templates of the group\n" +
                        " \n" +
                        "group <main> <name> [edit]                     | Edits the specified main group\n" +
                        " --sub-groups=[Group1;Group2]                  | Sets the sub groups of the main group\n" +
                        " --add-sub-groups=[Group1;Group2]              | Adds the sub groups to the main group\n" +
                        " --remove-sub-groups=[Group1;Group2]           | Removes the sub groups from the main group\n" +
                        " --clear-sub-groups=true                       | Clears the sub groups of the main group"
        ).split("\n"));
    }

    @Override
    public void process(@NotNull CommandSender sender, String[] strings, @NotNull String commandLine) {
        if (strings.length == 1 && strings[0].equalsIgnoreCase("list")) {
            this.listGroupsToSender(sender);
            return;
        }

        if (strings.length <= 2) {
            this.describeCommandToSender(sender);
            return;
        }

        Properties properties = StringUtil.parseProperties(strings, 2);
        if (strings[0].equalsIgnoreCase("sub")) {
            this.handleSubGroupRequest(sender, strings, properties);
            return;
        }

        if (strings[0].equalsIgnoreCase("main")) {
            this.handleMainGroupRequest(sender, strings, properties);
            return;
        }

        this.describeCommandToSender(sender);
    }

    @Override
    public @NotNull List<String> suggest(@NotNull CommandSender sender, String[] args, int index, @NotNull String line) {
        List<String> result = new ArrayList<>();

        if (index == 0) {
            result.addAll(Arrays.asList("list", "sub", "main"));
            return result;
        }

        if (index == 1) {
            if ("sub".equalsIgnoreCase(args[0])) {
                result.addAll(ExecutorAPI.getInstance().getProcessGroupProvider().getProcessGroupNames());
            } else if ("main".equalsIgnoreCase(args[0])) {
                result.addAll(ExecutorAPI.getInstance().getMainGroupProvider().getMainGroupNames());
            }
            return result;
        }

        if (index == 2) {
            result.addAll(Arrays.asList("stop", "kill", "info", "delete", "edit"));
            return result;
        }

        if (index >= 3) {
            if ("edit".equalsIgnoreCase(args[2]) && "sub".equalsIgnoreCase(args[0])) {
                result.addAll(Arrays.asList(
                        "--maintenance=false",
                        "--static=false",
                        "--max-players=512",
                        "--min-process-count=1",
                        "--max-process-count=-1",
                        "--always-prepared-process-count=1",
                        "--start-port=25565",
                        "--max-memory=512",
                        "--startup-pickers=",
                        "--add-startup-pickers=",
                        "--remove-startup-pickers=",
                        "--clear-startup-pickers=true",
                        "--lobby=true",
                        "--templates=default/FILE/NUKKIT",
                        "--add-templates=default/FILE/NUKKIT",
                        "--remove-templates=default",
                        "--clear-templates=true"
                ));
            } else if ("edit".equalsIgnoreCase(args[2]) && "main".equalsIgnoreCase(args[0])) {
                result.addAll(Arrays.asList(
                        "--sub-groups=",
                        "--add-sub-groups=",
                        "--remove-sub-groups=",
                        "--clear-sub-groups=true"
                ));
            }
        }

        return result;
    }

    private void handleSubGroupRequest(CommandSender source, String[] args, Properties props) {
        var optionalGroup = ExecutorAPI.getInstance()
                .getProcessGroupProvider()
                .getProcessGroup(args[1]);

        if (optionalGroup.isEmpty()) {
            source.sendMessage(TranslationHolder.translate("command-group-sub-group-not-exists", args[1]));
            return;
        }

        var group = optionalGroup.get();
        var processProvider = ExecutorAPI.getInstance().getProcessProvider();

        if (args.length == 3) {
            var action = args[2].toLowerCase();

            switch (action) {
                case "stop" -> {
                    var processes = processProvider.getProcessesByProcessGroup(group.getName())
                            .stream()
                            .filter(p -> !p.getCurrentState().equals(ProcessState.PREPARED))
                            .toList();

                    source.sendMessage(TranslationHolder.translate("command-group-stopping-all-not-prepared", group.getName()));
                    processes.forEach(p -> processProvider.getProcessByUniqueId(p.getId().getUniqueId())
                            .ifPresent(w -> w.setRuntimeStateAsync(ProcessState.STOPPED)));
                    return;
                }
                case "kill" -> {
                    var processes = processProvider.getProcessesByProcessGroup(group.getName());
                    source.sendMessage(TranslationHolder.translate("command-group-stopping-all", group.getName()));
                    processes.forEach(p -> processProvider.getProcessByUniqueId(p.getId().getUniqueId())
                            .ifPresent(w -> w.setRuntimeStateAsync(ProcessState.STOPPED)));
                    return;
                }
                case "info" -> {
                    describeProcessGroupToSender(source, group);
                    return;
                }
                case "delete" -> {
                    ExecutorAPI.getInstance().getProcessGroupProvider().deleteProcessGroup(group.getName());

                    var processes = processProvider.getProcessesByProcessGroup(group.getName());
                    processes.forEach(p -> processProvider.getProcessByUniqueId(p.getId().getUniqueId())
                            .ifPresent(w -> w.setRuntimeStateAsync(ProcessState.STOPPED)));

                    source.sendMessage(TranslationHolder.translate("command-group-sub-delete", group.getName()));
                    return;
                }
            }

        }

        if (args.length < 4 || !args[2].equalsIgnoreCase("edit")) {
            describeCommandToSender(source);
            return;
        }

        if (props.containsKey("maintenance")) {
            var v = Parsers.BOOLEAN.parse(props.getProperty("maintenance"));
            if (v == null) {
                source.sendMessage(TranslationHolder.translate("command-required-boolean", props.getProperty("maintenance")));
                return;
            }
            group.getPlayerAccessConfiguration().setMaintenance(v);
            source.sendMessage(TranslationHolder.translate("command-group-edit", "maintenance", v));
        }

        if (props.containsKey("static")) {
            var v = Parsers.BOOLEAN.parse(props.getProperty("static"));
            if (v == null) {
                source.sendMessage(TranslationHolder.translate("command-required-boolean", props.getProperty("static")));
                return;
            }
            group.setCreatesStaticProcesses(v);
            source.sendMessage(TranslationHolder.translate("command-group-edit", "static", v));
        }

        if (props.containsKey("lobby")) {
            var v = Parsers.BOOLEAN.parse(props.getProperty("lobby"));
            if (v == null) {
                source.sendMessage(TranslationHolder.translate("command-required-boolean", props.getProperty("lobby")));
                return;
            }
            group.setLobbyGroup(v);
            source.sendMessage(TranslationHolder.translate("command-group-edit", "lobby", v));
        }

        if (props.containsKey("max-players")) {
            var v = Parsers.INT.parse(props.getProperty("max-players"));
            if (v == null || v <= 0) {
                source.sendMessage(TranslationHolder.translate("command-integer-failed", 0, props.getProperty("max-players")));
                return;
            }
            group.getPlayerAccessConfiguration().setMaxPlayers(v);
            source.sendMessage(TranslationHolder.translate("command-group-edit", "max-players", v));
        }

        if (props.containsKey("max-memory")) {
            var split = props.getProperty("max-memory").split("/");
            if (split.length == 2) {
                var amount = Parsers.INT.parse(split[1]);
                if (amount == null || amount <= 50) {
                    source.sendMessage(TranslationHolder.translate("command-integer-failed", 50, split[1]));
                    return;
                }
                group.getTemplate(split[0]).ifPresent(t -> {
                    t.getRuntimeConfiguration().setMaximumJvmMemoryAllocation(amount);
                    source.sendMessage(TranslationHolder.translate("command-group-edit", "max-memory", split[0] + "/" + amount));
                });
            }
        }

        if (props.containsKey("min-process-count")) {
            var v = Parsers.INT.parse(props.getProperty("min-process-count"));
            if (v == null || v < 0) {
                source.sendMessage(TranslationHolder.translate("command-integer-failed", -1, props.getProperty("min-process-count")));
                return;
            }
            group.getStartupConfiguration().setAlwaysOnlineProcessAmount(v);
            source.sendMessage(TranslationHolder.translate("command-group-edit", "min-process-count", v));
        }

        if (props.containsKey("max-process-count")) {
            var v = Parsers.INT.parse(props.getProperty("max-process-count"));
            if (v == null || v <= -2) {
                source.sendMessage(TranslationHolder.translate("command-integer-failed", -2, props.getProperty("max-process-count")));
                return;
            }
            group.getStartupConfiguration().setMaximumProcessAmount(v);
            source.sendMessage(TranslationHolder.translate("command-group-edit", "max-process-count", v));
        }

        if (props.containsKey("always-prepared-process-count")) {
            var v = Parsers.INT.parse(props.getProperty("always-prepared-process-count"));
            if (v == null || v < 0) {
                source.sendMessage(TranslationHolder.translate("command-integer-failed", -1, props.getProperty("always-prepared-process-count")));
                return;
            }
            group.getStartupConfiguration().setAlwaysPreparedProcessAmount(v);
            source.sendMessage(TranslationHolder.translate("command-group-edit", "always-prepared-process-count", v));
        }

        if (props.containsKey("startup-pickers")) {
            var list = parseStrings(props.getProperty("startup-pickers"));
            group.getStartupConfiguration().setStartingNodes(list);
            source.sendMessage(TranslationHolder.translate("command-group-edit", "startup-pickers", String.join(", ", list)));
        }

        if (props.containsKey("add-startup-pickers")) {
            var list = parseStrings(props.getProperty("add-startup-pickers"));
            list.addAll(group.getStartupConfiguration().getStartingNodes());
            group.getStartupConfiguration().setStartingNodes(list);
            source.sendMessage(TranslationHolder.translate("command-group-edit", "startup-pickers", String.join(", ", list)));
        }

        if (props.containsKey("remove-startup-pickers")) {
            var list = group.getStartupConfiguration().getStartingNodes();
            list.removeAll(parseStrings(props.getProperty("remove-startup-pickers")));
            group.getStartupConfiguration().setStartingNodes(list);
            source.sendMessage(TranslationHolder.translate("command-group-edit", "startup-pickers", String.join(", ", list)));
        }

        if (props.containsKey("clear-startup-pickers")) {
            var v = Parsers.BOOLEAN.parse(props.getProperty("clear-startup-pickers"));
            if (v == null) {
                source.sendMessage(TranslationHolder.translate("command-required-boolean", props.getProperty("clear-startup-pickers")));
                return;
            }
            if (v) {
                group.getStartupConfiguration().setStartingNodes(new ArrayList<>());
                source.sendMessage(TranslationHolder.translate("command-group-edit", "use-specific-start-picker", "false"));
            }
        }

        if (props.containsKey("templates")) {
            var templates = parseTemplates(parseStrings(props.getProperty("templates")), source, group);
            if (!templates.isEmpty()) {
                group.setTemplates(templates);
                source.sendMessage(TranslationHolder.translate("command-group-edit", "templates",
                        templates.stream().map(Template::getName).collect(Collectors.joining(", "))));
            }
        }

        if (props.containsKey("add-templates")) {
            var newTemplates = parseTemplates(parseStrings(props.getProperty("add-templates")), source, group);
            if (!newTemplates.isEmpty()) {
                newTemplates.addAll(group.getTemplates());
                group.setTemplates(newTemplates);
                source.sendMessage(TranslationHolder.translate("command-group-edit", "add-templates",
                        newTemplates.stream().map(Template::getName).collect(Collectors.joining(", "))));
            }
        }

        if (props.containsKey("remove-templates")) {
            var toRemoveNames = parseStrings(props.getProperty("remove-templates"));
            var toRemove = group.getTemplates()
                    .stream()
                    .filter(t -> toRemoveNames.contains(t.getName()))
                    .toList();

            toRemove.forEach(group::removeTemplate);
            source.sendMessage(TranslationHolder.translate("command-group-edit", "remove-templates",
                    toRemove.stream().map(Template::getName).collect(Collectors.joining(", "))));
        }

        if (props.containsKey("clear-templates")) {
            var v = Parsers.BOOLEAN.parse(props.getProperty("clear-templates"));
            if (v == null) {
                source.sendMessage(TranslationHolder.translate("command-required-boolean", props.getProperty("clear-templates")));
                return;
            }
            if (v) {
                group.removeAllTemplates();
                source.sendMessage(TranslationHolder.translate("command-group-edit", "templates", "clear"));
            }
        }

        ExecutorAPI.getInstance().getProcessGroupProvider().updateProcessGroup(group);

        processProvider.getProcessesByProcessGroup(group.getName())
                .forEach(p -> System.out.println(
                        TranslationHolder.translate("command-group-edited-running-process", p.getName())
                ));
    }

    private void handleMainGroupRequest(CommandSender source, String[] strings, Properties properties) {
        Optional<MainGroup> mainGroup = ExecutorAPI.getInstance().getMainGroupProvider().getMainGroup(strings[1]);
        if (mainGroup.isEmpty()) {
            source.sendMessage(TranslationHolder.translate("command-group-main-group-not-exists", strings[1]));
            return;
        }

        if (strings.length == 3 && strings[2].equalsIgnoreCase("info")) {
            this.describeMainGroupToSender(source, mainGroup.get());
            return;
        }

        if (strings.length == 3 && strings[2].equalsIgnoreCase("delete")) {
            ExecutorAPI.getInstance().getMainGroupProvider().deleteMainGroup(mainGroup.get().getName());
            source.sendMessage(TranslationHolder.translate("command-group-main-delete", mainGroup.get().getName()));
            return;
        }

        if (strings.length == 3 && strings[2].equalsIgnoreCase("stop")) {
            for (String subGroup : mainGroup.get().getSubGroups()) {
                Collection<ProcessInformation> running = ExecutorAPI.getInstance()
                        .getProcessProvider()
                        .getProcessesByProcessGroup(subGroup)
                        .stream()
                        .filter(e -> !e.getCurrentState().equals(ProcessState.PREPARED))
                        .toList();

                source.sendMessage(TranslationHolder.translate("command-group-stopping-all-not-prepared", subGroup));

                for (ProcessInformation information : running) {
                    Optional<ProcessWrapper> wrapper = ExecutorAPI.getInstance()
                            .getProcessProvider()
                            .getProcessByUniqueId(information.getId().getUniqueId());
                    wrapper.ifPresent(processWrapper -> processWrapper.setRuntimeStateAsync(ProcessState.STOPPED));
                }
            }
            return;
        }

        if (strings.length == 3 && strings[2].equalsIgnoreCase("kill")) {
            for (String subGroup : mainGroup.get().getSubGroups()) {
                Collection<ProcessInformation> running = ExecutorAPI.getInstance()
                        .getProcessProvider()
                        .getProcessesByProcessGroup(subGroup);

                for (ProcessInformation information : running) {
                    Optional<ProcessWrapper> wrapper = ExecutorAPI.getInstance()
                            .getProcessProvider()
                            .getProcessByUniqueId(information.getId().getUniqueId());
                    wrapper.ifPresent(processWrapper -> processWrapper.setRuntimeStateAsync(ProcessState.STOPPED));
                }
            }
            return;
        }

        if (strings.length >= 4 && strings[2].equalsIgnoreCase("edit")) {
            if (properties.containsKey("sub-groups")) {
                List<String> groups = this.parseStrings(properties.getProperty("sub-groups"));
                mainGroup.get().setSubGroups(groups);
                source.sendMessage(TranslationHolder.translate("command-group-edit", "sub-groups", String.join(", ", groups)));
            }

            if (properties.containsKey("add-sub-groups")) {
                List<String> groups = this.parseStrings(properties.getProperty("add-sub-groups"));
                MoreCollections.allOf(mainGroup.get().getSubGroups(), groups::contains).forEach(groups::remove);
                groups.forEach(mainGroup.get()::addSubGroup);
                source.sendMessage(TranslationHolder.translate("command-group-edit", "sub-groups", String.join(", ", mainGroup.get().getSubGroups())));
            }

            if (properties.containsKey("remove-sub-groups")) {
                List<String> groups = this.parseStrings(properties.getProperty("remove-sub-groups"));
                groups.forEach(mainGroup.get()::removeSubGroup);
                source.sendMessage(TranslationHolder.translate("command-group-edit", "sub-groups-remove", String.join(", ", groups)));
            }

            if (properties.containsKey("clear-sub-groups")) {
                Boolean clear = Parsers.BOOLEAN.parse(properties.getProperty("clear-sub-groups"));
                if (clear == null) {
                    source.sendMessage(TranslationHolder.translate("command-required-boolean", properties.getProperty("clear-sub-groups")));
                    return;
                }

                if (clear) {
                    mainGroup.get().removeAllSubGroups();
                    source.sendMessage(TranslationHolder.translate("command-group-edit", "sub-groups", "clear"));
                }
            }

            ExecutorAPI.getInstance().getMainGroupProvider().updateMainGroup(mainGroup.get());
            return;
        }

        this.describeCommandToSender(source);
    }

    private void describeProcessGroupToSender(CommandSender source, ProcessGroup group) {
        StringBuilder builder = new StringBuilder();

        builder.append(" > Name        - ").append(group.getName()).append("\n");
        builder.append(" > Lobby       - ").append(group.isLobbyGroup() ? "&ayes&r" : "&cno&r").append("\n");
        builder.append(" > Max-Players - ").append(group.getPlayerAccessConfiguration().getMaxPlayers()).append("\n");
        builder.append(" > Maintenance - ").append(group.getPlayerAccessConfiguration().isMaintenance() ? "&ayes&r" : "&cno&r").append("\n");
        builder.append(" > Min-Online  - ").append(group.getStartupConfiguration().getAlwaysOnlineProcessAmount()).append("\n");
        builder.append(" > Max-Online  - ").append(group.getStartupConfiguration().getMaximumProcessAmount()).append("\n");

        builder.append(" ").append("\n");
        builder.append(" > Templates (").append(group.getTemplates().size()).append(")");

        for (Template template : group.getTemplates()) {
            builder.append("\n");
            builder.append("  > Name       - ").append(template.getName()).append("\n");
            builder.append("  > Version    - ").append(template.getVersion().getName()).append("\n");
            builder.append("  > Backend    - ").append(template.getBackend()).append("\n");
            builder.append("  > Priority   - ").append(template.getPriority()).append("\n");
            builder.append("  > Max-Memory - ").append(template.getRuntimeConfiguration().getMaximumJvmMemoryAllocation()).append("MB\n");
            builder.append("  > Global     - ").append(template.isGlobal() ? "&ayes&r" : "&cno&r").append("\n");
            builder.append("  > Start-Port - ").append(template.getVersion().getDefaultStartPort()).append("\n");
            builder.append(" ");
        }

        source.sendMessages(builder.toString().split("\n"));
    }

    private void describeMainGroupToSender(CommandSender source, MainGroup mainGroup) {
        String prefix = " > Sub-Groups (" + mainGroup.getSubGroups().size() + ")";
        String s = " > Name " + mainGroup.getName() + "\n" + prefix + " - " + String.join(", ", mainGroup.getSubGroups()) + "\n";
        source.sendMessages(s.split("\n"));
    }

    private void listGroupsToSender(CommandSender source) {
        StringBuilder builder = new StringBuilder();

        final Collection<MainGroup> mainGroups = ExecutorAPI.getInstance().getMainGroupProvider().getMainGroups();
        final Collection<ProcessGroup> processGroups = ExecutorAPI.getInstance().getProcessGroupProvider().getProcessGroups();

        builder.append(" Main-Groups (").append(mainGroups.size()).append(")");
        for (MainGroup mainGroup : mainGroups) {
            builder.append("\n");
            builder.append("  > Name       - ").append(mainGroup.getName()).append("\n");
            builder.append("  > Sub-Groups - ").append(String.join(", ", mainGroup.getSubGroups())).append("\n");
            builder.append(" ");
        }

        builder.append(mainGroups.isEmpty() ? "\n" : "").append(" \n");

        builder.append(" Process-Groups (").append(processGroups.size()).append(")");
        for (ProcessGroup processGroup : processGroups) {
            builder.append("\n");
            builder.append(" > Name            - ").append(processGroup.getName()).append("\n");
            builder.append(" > Min-Processes   - ").append(processGroup.getStartupConfiguration().getAlwaysOnlineProcessAmount()).append("\n");
            builder.append(" > Max-Processes   - ").append(processGroup.getStartupConfiguration().getMaximumProcessAmount()).append("\n");
            builder.append(" > Startup-Pickers - ").append(processGroup.getStartupConfiguration().getStartingNodes().isEmpty()
                    ? "all" : String.join(", ", processGroup.getStartupConfiguration().getStartingNodes())
            ).append("\n");
            builder.append(" ");
        }

        source.sendMessages(builder.toString().split("\n"));
    }

    private List<String> parseStrings(String s) {
        List<String> out = new ArrayList<>();
        if (s.contains(";")) {
            String[] split = s.split(";");
            for (String s1 : split) {
                if (out.contains(s1)) {
                    continue;
                }

                out.add(s1);
            }
        } else {
            out.add(s);
        }

        return out;
    }

    private List<Template> parseTemplates(Collection<String> collection, CommandSender source, ProcessGroup processGroup) {
        List<Template> newTemplates = new ArrayList<>();
        for (String template : collection) {
            String[] templateConfig = template.split("/");
            if (templateConfig.length != 3) {
                source.sendMessage(TranslationHolder.translate("command-group-template-format-error", template));
                continue;
            }

            if (processGroup.getTemplate(templateConfig[0]).isPresent()) {
                source.sendMessage(TranslationHolder.translate("command-group-template-already-exists", templateConfig[0]));
                continue;
            }

            Optional<TemplateBackend> backend = TemplateBackendManager.get(templateConfig[1]);
            if (backend.isEmpty()) {
                source.sendMessage(TranslationHolder.translate("command-group-template-backend-invalid", templateConfig[1]));
                continue;
            }

            Version version = Versions.getByName(templateConfig[2]).orElse(null);
            if (version == null) {
                source.sendMessage(TranslationHolder.translate("command-group-template-version-not-found", templateConfig[2]));
                continue;
            }

            newTemplates.add(Template.builder(templateConfig[0], version).backend(backend.get().getName()).build());
        }

        return newTemplates;
    }
}
