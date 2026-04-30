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
package app.reformcloud.node.logger;

import org.jetbrains.annotations.NotNull;
import org.jline.reader.LineReader;
import app.reformcloud.event.EventManager;
import app.reformcloud.node.NodeExecutor;
import app.reformcloud.node.event.logger.LogRecordProcessEvent;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.*;

public class CloudLogger extends Logger {

    private final RecordDispatcher dispatcher = new RecordDispatcher(this);

    public CloudLogger(@NotNull LineReader lineReader) {
        super("CloudLogger", null);
        setLevel(Level.ALL);

        var logPath = Path.of(System.getProperty("reformcloud.console-log-file", "logs/cloud.log"));
        var logDir = logPath.getParent();

        try {
            if (logDir != null) {
                Files.createDirectories(logDir);
            }

            var fileHandler = createFileHandler(logPath);
            addHandler(fileHandler);

            var colouredWriter = createColouredWriter(lineReader);
            addHandler(colouredWriter);

        } catch (IOException ex) {
            System.err.println("Unable to prepare logger!");
            ex.printStackTrace();
        }

        System.setOut(createRedirectStream(Level.INFO));
        System.setErr(createRedirectStream(Level.SEVERE));

        dispatcher.start();
    }

    private Handler createFileHandler(Path logPath) throws IOException {
        var fileHandler = new FileHandler(logPath.toString(), 1 << 24, 8, true);
        fileHandler.setFormatter(new DefaultFormatter(false));
        fileHandler.setEncoding(StandardCharsets.UTF_8.name());
        return fileHandler;
    }

    private Handler createColouredWriter(LineReader lineReader) throws IOException {
        var colouredWriter = new ColouredWriter(lineReader);
        var level = Level.parse(System.getProperty("reformcloud.console-log-level", "ALL"));
        colouredWriter.setLevel(level);

        boolean disableColours = Boolean.getBoolean("reformcloud.disable.colours");
        colouredWriter.setFormatter(new DefaultFormatter(!disableColours));

        colouredWriter.setEncoding(StandardCharsets.UTF_8.name());
        return colouredWriter;
    }

    private PrintStream createRedirectStream(Level level) {
        return new PrintStream(new LoggingOutputStream(this, level), true, StandardCharsets.UTF_8);
    }

    @Override
    public void log(LogRecord record) {
        dispatcher.queue(record);
    }

    protected void flushRecord(@NotNull LogRecord record) {
        if (preProcessRecord(record)) {
            super.log(record);
        }
    }

    public void close() throws InterruptedException {
        dispatcher.interrupt();
        dispatcher.join();
    }

    /**
     * Pre process the given log record and checks if the provided record should be logged
     * into the console or not.
     *
     * @param logRecord The log record to check
     * @return {@code true} if the record should be printed, {@code false} otherwise
     */
    protected boolean preProcessRecord(@NotNull LogRecord logRecord) {
        return !NodeExecutor.getInstance().getServiceRegistry().getProviderUnchecked(EventManager.class)
                .callEvent(new LogRecordProcessEvent(logRecord))
                .isCanceled();
    }
}
