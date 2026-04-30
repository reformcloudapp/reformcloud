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
package app.reformcloud.node.tick;

import org.jetbrains.annotations.NotNull;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.event.EventManager;
import app.reformcloud.node.NodeExecutor;
import app.reformcloud.node.event.worker.WorkerFullTickEvent;
import app.reformcloud.node.event.worker.WorkerTickEvent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public final class CloudTickWorker {

    static final int TPS = 20;
    static final AtomicLong CURRENT_TICK = new AtomicLong();
    static final long SEC_IN_NANO = TimeUnit.SECONDS.toNanos(1);

    private static final long TICK_TIME = SEC_IN_NANO / TPS;
    private static final long MAX_CATCHUP_BUFFER = TICK_TIME * TPS * 60L;

    private final TickedTaskScheduler taskScheduler;
    private final Thread mainThread;

    public CloudTickWorker(@NotNull TickedTaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
        this.mainThread = Thread.currentThread();
    }

    public void startTick() {
        long nextTick = System.nanoTime();

        while (NodeExecutor.isRunning()) {
            long now = System.nanoTime();
            long sleepNanos = nextTick - now;

            if (sleepNanos > 0) {
                LockSupport.parkNanos(sleepNanos);
                now = System.nanoTime();
            }

            if (now - nextTick > MAX_CATCHUP_BUFFER) {
                nextTick = now;
            }

            if (CURRENT_TICK.incrementAndGet() % TPS == 0) {
                taskScheduler.fullHeartBeat();
                ExecutorAPI.getInstance().getServiceRegistry()
                        .getProviderUnchecked(EventManager.class)
                        .callEvent(WorkerFullTickEvent.INSTANCE);
            }

            taskScheduler.heartBeat();
            ExecutorAPI.getInstance().getServiceRegistry()
                    .getProviderUnchecked(EventManager.class)
                    .callEvent(WorkerTickEvent.INSTANCE);

            nextTick += TICK_TIME;
        }
    }

    @NotNull
    public Thread getMainThread() {
        return this.mainThread;
    }
}
