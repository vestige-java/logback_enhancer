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

import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.util.ExecutorServiceFactory;
import ch.qos.logback.core.util.ExecutorServiceUtil;
import fr.gaellalire.vestige.core.Vestige;
import fr.gaellalire.vestige.core.VestigeCoreContext;
import fr.gaellalire.vestige.core.executor.VestigeExecutor;
import fr.gaellalire.vestige.core.executor.VestigeWorker;
import fr.gaellalire.vestige.core.function.Function;
import fr.gaellalire.vestige.core.weak.ExecutorServiceReaperHelper;
import fr.gaellalire.vestige.core.weak.VestigeReaper;
import fr.gaellalire.vestige.core.weak.VestigeWorkerReaperHelper;
import fr.gaellalire.vestige.core.weak.WeakThreadFactory;

/**
 * @author Gael Lalire
 */
public class LogbackEnhancer {

    private Class<?> mainClass;

    private String[] dargs;

    private VestigeCoreContext vestigeCoreContext;

    private List<? extends ClassLoader> privilegedClassloaders;

    private Function<Thread, Void, RuntimeException> addShutdownHook;

    private Function<Thread, Void, RuntimeException> removeShutdownHook;

    public void runEnhancedMain() throws Exception {
        try {
            Method method = mainClass.getMethod("vestigeEnhancedCoreMain", VestigeCoreContext.class, Function.class, Function.class, List.class, String[].class);
            method.invoke(null, new Object[] {vestigeCoreContext, addShutdownHook, removeShutdownHook, privilegedClassloaders, dargs});
        } catch (NoSuchMethodException e) {
            runMain();
        }
    }

    public void runMain() throws Exception {
        Vestige.runMain(null, mainClass, vestigeCoreContext, dargs);
    }

    public LogbackEnhancer(final Class<?> mainClass, final VestigeCoreContext vestigeCoreContext, final Function<Thread, Void, RuntimeException> addShutdownHook,
            final Function<Thread, Void, RuntimeException> removeShutdownHook, final List<? extends ClassLoader> privilegedClassloaders, final String[] dargs) {
        this.mainClass = mainClass;
        this.vestigeCoreContext = vestigeCoreContext;
        this.addShutdownHook = addShutdownHook;
        this.removeShutdownHook = removeShutdownHook;
        this.privilegedClassloaders = privilegedClassloaders;
        this.dargs = dargs;
    }

    public Class<?> getMainClass() {
        return mainClass;
    }

    public String[] getDargs() {
        return dargs;
    }

    public VestigeCoreContext getVestigeCoreContext() {
        return vestigeCoreContext;
    }

    public VestigeExecutor getVestigeExecutor() {
        return vestigeCoreContext.getVestigeExecutor();
    }

    public Function<Thread, Void, RuntimeException> getAddShutdownHook() {
        return addShutdownHook;
    }

    public Function<Thread, Void, RuntimeException> getRemoveShutdownHook() {
        return removeShutdownHook;
    }

    public List<? extends ClassLoader> getPrivilegedClassloaders() {
        return privilegedClassloaders;
    }

    public static void enhance(final VestigeCoreContext vestigeCoreContext) throws Exception {
        final VestigeReaper vestigeReaper = vestigeCoreContext.getVestigeReaper();

        final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        final VestigeWorker vestigeWorker = vestigeCoreContext.getVestigeExecutor().createWorker("logback-worker", true, 0);
        vestigeReaper.addReapable(ExecutorServiceUtil.class, new VestigeWorkerReaperHelper(vestigeWorker));

        final ThreadFactory threadFactory = new ThreadFactory() {

            private final AtomicInteger threadNumber = new AtomicInteger(1);

            public Thread newThread(final Runnable r) {
                Thread thread;
                try {
                    thread = vestigeWorker.createThread(null, r, "logback-" + threadNumber.getAndIncrement(), 0);
                } catch (InterruptedException e) {
                    return null;
                }

                if (!thread.isDaemon()) {
                    thread.setDaemon(true);
                }
                thread.setContextClassLoader(classLoader);
                return thread;
            }
        };

        ExecutorServiceUtil.setFactory(new ExecutorServiceFactory() {

            // prevent GC of threadFactory while this ExecutorServiceFactory is not GC
            @SuppressWarnings("unused")
            private ThreadFactory threadFactoryHandler = threadFactory;

            // prepare the weak reference for ThreadPoolExecutor
            private WeakThreadFactory weakThreadFactory = new WeakThreadFactory(threadFactory);

            @Override
            public ScheduledExecutorService newScheduledExecutorService() {
                final ScheduledThreadPoolExecutor delegate = new ScheduledThreadPoolExecutor(CoreConstants.SCHEDULED_EXECUTOR_POOL_SIZE, weakThreadFactory);
                WeakScheduledThreadPoolExecutor weakScheduledThreadPoolExecutor = new WeakScheduledThreadPoolExecutor(delegate);
                vestigeReaper.addReapable(ExecutorServiceUtil.class, new ExecutorServiceReaperHelper(delegate));
                return weakScheduledThreadPoolExecutor;
            }

            @Override
            public ExecutorService newExecutorService() {
                final ThreadPoolExecutor delegate = new ThreadPoolExecutor(CoreConstants.CORE_POOL_SIZE, CoreConstants.MAX_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
                        new SynchronousQueue<Runnable>(), weakThreadFactory);
                final WeakThreadPoolExecutor weakThreadPoolExecutor = new WeakThreadPoolExecutor(delegate);
                vestigeReaper.addReapable(ExecutorServiceUtil.class, new ExecutorServiceReaperHelper(delegate));
                return weakThreadPoolExecutor;
            }

        });

    }

    public void enhance() throws Exception {
        enhance(vestigeCoreContext);

        if (addShutdownHook != null || removeShutdownHook != null || privilegedClassloaders != null) {
            runEnhancedMain();
        } else {
            runMain();
        }
    }

    public static void vestigeEnhancedCoreMain(final VestigeCoreContext vestigeCoreContext, final Function<Thread, Void, RuntimeException> addShutdownHook,
            final Function<Thread, Void, RuntimeException> removeShutdownHook, final List<? extends ClassLoader> privilegedClassloaders, final String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Expecting at least 1 arg : mainClass");
        }

        String[] dargs = new String[args.length - 1];
        System.arraycopy(args, 1, dargs, 0, dargs.length);

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        new LogbackEnhancer(contextClassLoader.loadClass(args[0]), vestigeCoreContext, addShutdownHook, removeShutdownHook, privilegedClassloaders, dargs).enhance();
    }

    public static void vestigeCoreMain(final VestigeCoreContext vestigeCoreContext, final String[] args) throws Exception {
        vestigeEnhancedCoreMain(vestigeCoreContext, null, null, null, args);
    }

    public static void main(final String[] args) throws Exception {
        final VestigeCoreContext vestigeCoreContext = VestigeCoreContext.buildDefaultInstance();
        URL.setURLStreamHandlerFactory(vestigeCoreContext.getStreamHandlerFactory());
        vestigeCoreMain(vestigeCoreContext, args);
    }

}
