/*
 * Copyright 2020 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
