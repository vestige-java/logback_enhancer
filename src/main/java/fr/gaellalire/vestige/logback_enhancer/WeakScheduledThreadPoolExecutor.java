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

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import fr.gaellalire.vestige.core.weak.WeakCallable;
import fr.gaellalire.vestige.core.weak.WeakRunnable;

/**
 * @author Gael Lalire
 */
public class WeakScheduledThreadPoolExecutor extends WeakThreadPoolExecutor implements ScheduledExecutorService {

    private ScheduledExecutorService delegate;

    public WeakScheduledThreadPoolExecutor(final ScheduledExecutorService delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    public static <T> ScheduledFuture<T> getHandlingScheduleFuture(final ScheduledFuture<T> future, final Runnable runnable) {
        return new ScheduledFuture<T>() {

            @SuppressWarnings("unused")
            final Runnable runnableHandler = runnable;

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

            @Override
            public long getDelay(final TimeUnit unit) {
                return future.getDelay(unit);
            }

            @Override
            public int compareTo(final Delayed o) {
                return future.compareTo(o);
            }
        };
    }

    public static <T> ScheduledFuture<T> getHandlingScheduleFuture(final ScheduledFuture<T> future, final Callable<?> callable) {
        return new ScheduledFuture<T>() {

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

            @Override
            public long getDelay(final TimeUnit unit) {
                return future.getDelay(unit);
            }

            @Override
            public int compareTo(final Delayed o) {
                return future.compareTo(o);
            }
        };
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
        return getHandlingScheduleFuture(delegate.scheduleWithFixedDelay(new WeakRunnable(command), initialDelay, delay, unit), command);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {
        return getHandlingScheduleFuture(delegate.scheduleAtFixedRate(new WeakRunnable(command), initialDelay, period, unit), command);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        return getHandlingScheduleFuture(delegate.schedule(new WeakCallable<V>(callable), delay, unit), callable);
    }

    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        return getHandlingScheduleFuture(delegate.schedule(new WeakRunnable(command), delay, unit), command);
    }

}
