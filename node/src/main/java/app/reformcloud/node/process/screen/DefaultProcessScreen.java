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
package app.reformcloud.node.process.screen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.language.TranslationHolder;
import app.reformcloud.network.channel.manager.ChannelManager;
import app.reformcloud.node.NodeExecutor;
import app.reformcloud.node.process.DefaultNodeLocalProcessWrapper;
import app.reformcloud.node.protocol.NodeToNodeProcessScreenLines;
import app.reformcloud.process.ProcessInformation;
import org.jline.utils.InputStreamReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DefaultProcessScreen implements ProcessScreen {

    private static final int MAX_CACHE_SIZE = Integer.getInteger("app.reformcloud.screen-cache-max-size", 256);

    private final Queue<String> cachedLogLines = new ConcurrentLinkedQueue<>();
    private final Collection<String> listeningNodes = new CopyOnWriteArrayList<>();

    private final byte[] readBuffer = new byte[1024];
    private final StringBuffer stringBuffer = new StringBuffer();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final DefaultNodeLocalProcessWrapper processWrapper;

    public DefaultProcessScreen(DefaultNodeLocalProcessWrapper processWrapper) {
        this.processWrapper = processWrapper;
    }

    @Override
    public @NotNull ProcessInformation getTargetProcess() {
        return this.processWrapper.getProcessInformation();
    }

    @Override
    public @NotNull Queue<String> getCachedLogLines() {
        return this.cachedLogLines;
    }

    @Override
    public @NotNull @UnmodifiableView Collection<String> getListeningNodes() {
        return Collections.unmodifiableCollection(this.listeningNodes);
    }

    @Override
    public void addListeningNode(@NotNull String name) {
        this.listeningNodes.add(name);
        this.printLines(name, this.cachedLogLines);
    }

    @Override
    public void removeListeningNode(@NotNull String name) {
        this.listeningNodes.remove(name);
    }

    @Override
    public void tick() {
        lock.writeLock().lock();
        try {
            tick0();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void tick0() {
        this.processWrapper.getProcess().ifPresent(process -> {
            Collection<String> lines = this.readInputStream(process.getInputStream());
            this.printLines(lines);
            lines = this.readInputStream(process.getErrorStream());
            this.printLines(lines);
        });
    }

    private @NotNull Collection<String> readInputStream(@NotNull InputStream inputStream) {
        Collection<String> lines = new ArrayList<>();

        try {
            int length;
            while (inputStream.available() > 0 &&
                    (length = inputStream.read(readBuffer, 0, readBuffer.length)) != -1) {

                stringBuffer.append(new String(readBuffer, 0, length, StandardCharsets.UTF_8));
            }

            String collected = stringBuffer.toString();

            if (!collected.contains("\n") && !collected.contains("\r")) {
                return lines;
            }

            for (String part : collected.split("\r")) {
                for (String line : part.split("\n")) {
                    if (!line.trim().isEmpty()) {
                        cache(line);
                        lines.add(line);
                    }
                }
            }

            stringBuffer.setLength(0);
        } catch (IOException exception) {
            if (exception.getMessage() == null || !exception.getMessage().equals("Stream closed")) {
                exception.printStackTrace();
            }
        }
        return lines;
    }


    private void cache(@NotNull String text) {
        synchronized (this.cachedLogLines) {
            if (this.cachedLogLines.size() >= MAX_CACHE_SIZE) {
                this.cachedLogLines.poll();
            }
            this.cachedLogLines.add(text);
        }
    }

    private void printLines(@NotNull Collection<String> lines) {
        for (String listeningNode : this.listeningNodes) {
            this.printLines(listeningNode, lines);
        }
    }

    private void printLines(@NotNull String nodeName, @NotNull Collection<String> lines) {
        if (NodeExecutor.getInstance().isOwnIdentity(nodeName)) {
            for (String line : lines) {
                System.out.println(TranslationHolder.translate(
                        "screen-line-added",
                        this.processWrapper.getProcessInformation().getName(),
                        NodeExecutor.getInstance().getCurrentNodeInformation().getName(),
                        line
                ));
            }
        } else {
            ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(ChannelManager.class)
                    .getChannel(nodeName)
                    .ifPresent(channel -> channel.sendPacket(new NodeToNodeProcessScreenLines(
                            this.processWrapper.getProcessInformation().getName(),
                            NodeExecutor.getInstance().getSelfName(),
                            lines
                    )));
        }
    }
}
