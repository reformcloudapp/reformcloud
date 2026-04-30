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
import org.jetbrains.annotations.Nullable;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.event.EventManager;
import app.reformcloud.node.concurrent.AsyncCatcher;
import app.reformcloud.node.event.scheduler.SchedulerFullHeartBeatPermanentTaskExecuteEvent;
import app.reformcloud.node.event.scheduler.SchedulerHeartBeatTaskExecuteEvent;
import app.reformcloud.task.Task;
import app.reformcloud.task.defaults.DefaultTask;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TickedTaskScheduler {

    private final Queue<TickedTaskSchedulerTask<?>> queue = new ConcurrentLinkedQueue<>();
    private final Collection<Runnable> permanentTasks = new CopyOnWriteArrayList<>();

    private volatile boolean closed = false;

    public <T> @NotNull Task<T> queue(@NotNull Callable<T> callable) {
        var task = new DefaultTask<T>();
        queue.add(new TickedTaskSchedulerTask<>(task, callable, -1));
        return task;
    }

    public <T> @NotNull Task<T> queue(@NotNull Callable<T> callable, int delay) {
        var task = new DefaultTask<T>();
        var target = CloudTickWorker.CURRENT_TICK.get() + delay;
        queue.add(new TickedTaskSchedulerTask<>(task, callable, target));
        return task;
    }

    public @NotNull Task<Void> queue(@NotNull Runnable runnable) {
        return queue(() -> {
            runnable.run();
            return null;
        });
    }

    public @NotNull Task<Void> queue(@NotNull Runnable runnable, int delay) {
        return queue(() -> {
            runnable.run();
            return null;
        }, delay);
    }

    public void addPermanentTask(@NotNull Runnable runnable) {
        permanentTasks.add(runnable);
    }

    public void close() {
        closed = true;
    }

    void heartBeat() {
        if (closed) return;

        AsyncCatcher.ensureMainThread("scheduler heart beat");

        var next = pollReadyTask();
        if (next != null) {
            try {
                ExecutorAPI.getInstance()
                        .getServiceRegistry()
                        .getProviderUnchecked(EventManager.class)
                        .callEvent(new SchedulerHeartBeatTaskExecuteEvent(next));

                next.call();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    void fullHeartBeat() {
        if (closed) return;

        AsyncCatcher.ensureMainThread("scheduler full heart beat");

        for (var task : permanentTasks) {
            try {
                ExecutorAPI.getInstance()
                        .getServiceRegistry()
                        .getProviderUnchecked(EventManager.class)
                        .callEvent(new SchedulerFullHeartBeatPermanentTaskExecuteEvent(task));

                task.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private @Nullable TickedTaskSchedulerTask<?> pollReadyTask() {
        var currentTick = CloudTickWorker.CURRENT_TICK.get();

        for (var task : queue) {
            if (task.getTargetTick() < 0 || task.getTargetTick() == currentTick) {
                queue.remove(task);
                return task;
            }
        }
        return null;
    }

    public static final class TickedTaskSchedulerTask<T> {

        private final Task<T> task;
        private final Callable<T> callable;
        final long targetTick;

        public TickedTaskSchedulerTask(Task<T> task, Callable<T> callable, long targetTick) {
            this.task = task;
            this.callable = callable;
            this.targetTick = targetTick;
        }

        public long getTargetTick() {
            return targetTick;
        }

        public void call() {
            try {
                task.complete(callable.call());
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}