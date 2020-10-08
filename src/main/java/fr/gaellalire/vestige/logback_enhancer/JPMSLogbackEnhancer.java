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

import java.lang.ModuleLayer.Controller;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import fr.gaellalire.vestige.core.JPMSVestige;
import fr.gaellalire.vestige.core.VestigeCoreContext;
import fr.gaellalire.vestige.core.function.Function;

/**
 * @author Gael Lalire
 */
public class JPMSLogbackEnhancer extends LogbackEnhancer {

    private Controller controller;

    public JPMSLogbackEnhancer(final Class<?> mainClass, final VestigeCoreContext vestigeCoreContext, final Function<Thread, Void, RuntimeException> addShutdownHook,
            final Function<Thread, Void, RuntimeException> removeShutdownHook, final List<? extends ClassLoader> privilegedClassloaders, final Controller controller,
            final String[] dargs) {
        super(mainClass, vestigeCoreContext, addShutdownHook, removeShutdownHook, privilegedClassloaders, dargs);
        this.controller = controller;
    }

    public void runEnhancedMain() throws Exception {
        try {
            Method method = getMainClass().getMethod("vestigeEnhancedCoreMain", VestigeCoreContext.class, Function.class, Function.class, List.class, Controller.class,
                    String[].class);
            method.invoke(null, new Object[] {getVestigeCoreContext(), getAddShutdownHook(), getRemoveShutdownHook(), getPrivilegedClassloaders(), controller, getDargs()});
        } catch (NoSuchMethodException e) {
            super.runEnhancedMain();
        }
    }

    public void runMain() throws Exception {
        JPMSVestige.runMain(null, getMainClass(), controller, getVestigeCoreContext(), getDargs());
    }

    public static void vestigeEnhancedCoreMain(final VestigeCoreContext vestigeCoreContext, final Function<Thread, Void, RuntimeException> addShutdownHook,
            final Function<Thread, Void, RuntimeException> removeShutdownHook, final List<? extends ClassLoader> privilegedClassloaders, final Controller controller,
            final String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Expecting at least 1 arg : mainModule[/mainClass]");
        }
        String mainModule = args[0];
        String mainClass = null;
        int indexOf = mainModule.indexOf('/');
        if (indexOf != -1) {
            mainClass = mainModule.substring(indexOf + 1);
            mainModule = mainModule.substring(0, indexOf);
        }

        String[] dargs = new String[args.length - 1];
        System.arraycopy(args, 1, dargs, 0, dargs.length);
        Optional<Module> findModule;
        if (controller == null) {
            findModule = ModuleLayer.boot().findModule(mainModule);
        } else {
            findModule = controller.layer().findModule(mainModule);
        }
        if (!findModule.isPresent()) {
            throw new IllegalArgumentException("Module " + mainModule + " cannot be found");
        }
        Module module = findModule.get();
        controller.addReads(JPMSLogbackEnhancer.class.getModule(), module);
        if (mainClass == null) {
            Optional<String> mainClassOptional = module.getDescriptor().mainClass();
            if (!mainClassOptional.isPresent()) {
                throw new IllegalArgumentException("Module " + mainModule + " has no main class");
            }
            mainClass = mainClassOptional.get();
        }

        new JPMSLogbackEnhancer(module.getClassLoader().loadClass(mainClass), vestigeCoreContext, addShutdownHook, removeShutdownHook, privilegedClassloaders, controller, dargs)
                .enhance();
    }

    public static void vestigeCoreMain(final Controller controller, final VestigeCoreContext vestigeCoreContext, final String[] args) throws Exception {
        vestigeEnhancedCoreMain(vestigeCoreContext, null, null, null, controller, args);
    }

    public static void vestigeCoreMain(final VestigeCoreContext vestigeCoreContext, final String[] args) throws Exception {
        vestigeCoreMain(null, vestigeCoreContext, args);
    }

    public static void main(final String[] args) throws Exception {
        VestigeCoreContext vestigeCoreContext = VestigeCoreContext.buildDefaultInstance();
        URL.setURLStreamHandlerFactory(vestigeCoreContext.getStreamHandlerFactory());
        vestigeCoreMain(vestigeCoreContext, args);
    }

}
