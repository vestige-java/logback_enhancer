/*
 * This file is part of Vestige.
 *
 * Vestige is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Vestige is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vestige.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.gaellalire.vestige.logback_enhancer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import fr.gaellalire.vestige.core.weak.WeakCallable;
import fr.gaellalire.vestige.core.weak.WeakRunnable;

/**
 * @author Gael Lalire
 */
public class WeakThreadPoolExecutor implements ExecutorService {

    private ExecutorService delegate;

    public WeakThreadPoolExecutor(final ExecutorService delegate) {
        this.delegate = delegate;
    }

    public static <T> List<Future<T>> getHandlingFutures(final List<Future<T>> futures, final Collection<? extends Callable<T>> tasks) {
        List<Future<T>> futureList = new ArrayList<Future<T>>(futures.size());
        final Iterator<? extends Callable<T>> taskIterator = tasks.iterator();
        for (Future<T> future : futures) {
            futureList.add(getHandlingFuture(future, taskIterator.next()));
        }
        return futureList;
    }

    public static <T> Future<T> getHandlingFuture(final Future<T> future, final Callable<?> callable) {
        return new Future<T>() {

            @SuppressWarnings("unused")
            final Callable<?> callableHandler = callable;

            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return future.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return future.isCancelled();
            }

            @Override
            public boolean isDone() {
                return future.isDone();
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                return future.get();
            }

            @Override
            public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return future.get(timeout, unit);
            }
        };
    }

    public static <T> Future<T> getHandlingFuture(final Future<T> future, final Runnable task) {
        return new Future<T>() {

            @SuppressWarnings("unused")
            final Runnable taskHandler = task;

            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return future.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return future.isCancelled();
            }

            @Override
            public boolean isDone() {
                return future.isDone();
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                return future.get();
            }

            @Override
            public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return future.get(timeout, unit);
            }
        };
    }

    public static <T> List<Callable<T>> getTaskList(final Collection<? extends Callable<T>> tasks) {
        List<Callable<T>> taskList = new ArrayList<Callable<T>>(tasks.size());
        for (Callable<T> c : tasks) {
            taskList.add(new WeakCallable<T>(c));

        }
        return taskList;
    }

    @Override
    public void execute(final Runnable command) {
        delegate.execute(new WeakRunnable(command));
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return getHandlingFuture(delegate.submit(new WeakRunnable(task), result), task);
    }

    @Override
    public Future<?> submit(final Runnable task) {
        return getHandlingFuture(delegate.submit(new WeakRunnable(task)), task);
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return getHandlingFuture(delegate.submit(new WeakCallable<T>(task)), task);
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(getTaskList(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(getTaskList(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
        return getHandlingFutures(delegate.invokeAll(getTaskList(tasks), timeout, unit), tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return getHandlingFutures(delegate.invokeAll(getTaskList(tasks)), tasks);
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

}
