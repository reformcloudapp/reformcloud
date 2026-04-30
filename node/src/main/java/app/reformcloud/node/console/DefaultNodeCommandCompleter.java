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

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.command.CommandContainer;
import app.reformcloud.command.CommandManager;
import app.reformcloud.shared.command.sources.ConsoleCommandSender;

import java.util.Collection;
import java.util.List;

public final class DefaultNodeCommandCompleter implements Completer {

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> list) {
        var commandManager = ExecutorAPI.getInstance()
                .getServiceRegistry()
                .getProviderUnchecked(CommandManager.class);

        var buffer = parsedLine.line();
        var lastSpace = buffer.lastIndexOf(' ');

        if (lastSpace == -1) {
            var candidates = commandManager.getCommands()
                    .stream()
                    .map(CommandContainer::getAliases)
                    .flatMap(Collection::stream)
                    .filter(alias -> buffer.isEmpty() || alias.startsWith(buffer))
                    .sorted()
                    .map(Candidate::new)
                    .toList();

            list.addAll(candidates);
            return;
        }

        var tokens = buffer.split(" ");
        var lastToken = (!buffer.endsWith(" ") && tokens.length > 0)
                ? tokens[tokens.length - 1].trim().toLowerCase()
                : null;

        var suggestions = commandManager.suggest(buffer, ConsoleCommandSender.INSTANCE)
                .stream()
                .filter(s -> lastToken == null || s.toLowerCase().startsWith(lastToken))
                .sorted()
                .map(Candidate::new)
                .toList();

        list.addAll(suggestions);
    }
}
