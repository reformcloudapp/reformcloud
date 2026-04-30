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
package app.reformcloud.node.console;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.command.CommandManager;
import app.reformcloud.language.TranslationHolder;
import app.reformcloud.shared.command.sources.ConsoleCommandSender;
import app.reformcloud.task.Task;
import app.reformcloud.task.defaults.DefaultTask;

import java.util.Arrays;

public final class ConsoleReadThread extends Thread {

    private final DefaultNodeConsole console;
    private Task<String> currentTask;

    ConsoleReadThread(@NotNull DefaultNodeConsole console) {
        super("ReformCloud console read thread");
        this.console = console;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            var line = readLine();
            if (line == null) {
                break;
            }

            if (line.isBlank()) {
                continue;
            }

            if (currentTask != null) {
                currentTask.complete(line);
                currentTask = null;
                continue;
            }

            Arrays.stream(line.split(" && "))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(this::dispatchLine);
        }
    }

    @Nullable
    private String readLine() {
        try {
            return console.getLineReader().readLine(console.getPrompt());
        } catch (EndOfFileException ignored) {
            return null;
        } catch (UserInterruptException e) {
            interrupt();
            return null;
        }
    }

    private void dispatchLine(@NotNull String line) {
        var commandManager = ExecutorAPI.getInstance()
                .getServiceRegistry()
                .getProviderUnchecked(CommandManager.class);

        if (!commandManager.process(line, ConsoleCommandSender.INSTANCE)) {
            System.out.println(TranslationHolder.translate("command-help-use"));
        }
    }

    @NotNull
    public Task<String> getCurrentTask() {
        if (currentTask == null) {
            currentTask = new DefaultTask<>();
        }
        return currentTask;
    }
}
