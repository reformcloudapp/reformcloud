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
import org.jline.jansi.AnsiConsole;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import app.reformcloud.console.Console;
import app.reformcloud.node.logger.ConsoleColour;
import app.reformcloud.task.Task;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

public final class DefaultNodeConsole implements Console {

    private static final String USER = System.getProperty("user.name", "unknown");
    private static final String VERSION = System.getProperty("reformcloud.runner.version", "dev");

    private final Terminal terminal;
    private final LineReaderImpl lineReader;
    private final boolean colorSupport;
    private final ConsoleReadThread consoleReadThread;

    private String prompt = System.getProperty(
            "reformcloud.console-prompt-pattern",
            "[&crc&7-&b{0}&7@&b{1} &f~&r]$ "
    );

    public DefaultNodeConsole() {

        this.colorSupport = !Boolean.getBoolean("reformcloud.disable.colours")
                && tryInstallAnsi();

        System.setProperty("reformcloud.disable.colours", Boolean.toString(!colorSupport));

        setPrompt(prompt);
        this.prompt = MessageFormat.format(this.prompt, USER, VERSION);

        try {
            this.terminal = TerminalBuilder.builder()
                    .system(true)
                    .provider("jansi")
                    .encoding(StandardCharsets.UTF_8)
                    .build();

            this.lineReader = (LineReaderImpl) LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(new DefaultNodeCommandCompleter())
                    .variable(LineReader.BELL_STYLE, "off")
                    .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                    .build();

        } catch (IOException ex) {
            System.err.println("Unable to create terminal or line reader");
            throw new RuntimeException(ex);
        }

        this.consoleReadThread = new ConsoleReadThread(this);
        this.consoleReadThread.start();
    }

    @Override
    public @NotNull Task<String> readString() {
        return consoleReadThread.getCurrentTask();
    }

    @Override
    public @NotNull String getPrompt() {
        return prompt;
    }

    @Override
    public void setPrompt(@NotNull String newPrompt) {
        this.prompt = colorSupport
                ? ConsoleColour.toColouredString('&', newPrompt)
                : ConsoleColour.stripColor('&', newPrompt);

        if (lineReader != null) {
            lineReader.setPrompt(this.prompt);
        }
    }

    @Override
    public void addHistoryEntry(@NotNull String entry) {
        lineReader.getHistory().add(entry);
    }

    @Override
    public void clearHistory() {
        try {
            lineReader.getHistory().purge();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void clearScreen() {
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();
    }

    @Override
    public void close() throws Exception {
        consoleReadThread.interrupt();
        terminal.flush();
        terminal.close();
        AnsiConsole.systemUninstall();
    }

    public @NotNull LineReader getLineReader() {
        return lineReader;
    }

    private boolean tryInstallAnsi() {
        try {
            System.setProperty("library.jansi.version", "ReformCloud");
            AnsiConsole.systemInstall();
            return true;
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Ansi support is disabled, running in an unsupported environment");
            return false;
        }
    }
}