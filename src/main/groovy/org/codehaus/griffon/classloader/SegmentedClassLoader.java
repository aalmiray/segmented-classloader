/*
 * Copyright 2012 the original author or authors.
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
package org.codehaus.griffon.classloader;

import org.apache.commons.lang.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Andres Almiray
 */
public class SegmentedClassLoader extends SecureClassLoader {
    private static final Logger LOG = LoggerFactory.getLogger(SegmentedClassLoader.class);
    private final Map<String, ClasspathModule> MODULES = new ConcurrentHashMap<String, ClasspathModule>();

    public SegmentedClassLoader() {
        super(ClassLoader.getSystemClassLoader());
    }

    public SegmentedClassLoader(ClassLoader classLoader) {
        super(classLoader);
    }

    public void addClasspathModule(ClasspathModuleConfiguration configuration) {
        addClasspathModule(configuration, true);
    }

    public void addClasspathModule(ClasspathModuleConfiguration configuration, boolean replace) {
        if (configuration == null) {
            throw new IllegalArgumentException("Cannot add an empty classpath module!");
        }

        String moduleName = configuration.getName();
        ClasspathModule other = MODULES.get(moduleName);
        if (other == null) {
            MODULES.put(moduleName, createModule(configuration));
            if (LOG.isInfoEnabled()) {
                LOG.info("Added classpath module " + moduleName + " to " + this);
            }
            if (LOG.isTraceEnabled()) {
                LOG.trace(this + " now contains the following URLS: " + Arrays.toString(configuration.getUrls()));
            }
        } else if (other.getConfiguration() != configuration) {
            if (!replace) {
                MODULES.remove(moduleName);
            }
            MODULES.put(moduleName, createModule(configuration));
            if (LOG.isInfoEnabled()) {
                if (replace) {
                    LOG.info("Replaced classpath module " + moduleName + " in " + this);
                } else {
                    LOG.info("Added classpath module " + moduleName + " to " + this);
                }
            }
            if (LOG.isTraceEnabled()) {
                LOG.trace(this + " no contains the following URLS: " + Arrays.toString(configuration.getUrls()));
            }
        } else {
            if (LOG.isInfoEnabled()) {
                LOG.info(this + " already has a classpath module with name " + moduleName + " containing the same URLs. Classpath has been left unchanged.");
            }
        }
    }

    public void removeClasspathModule(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Cannot remove a nameless module!");
        }

        if (MODULES.containsKey(name)) {
            MODULES.remove(name);
            if (LOG.isInfoEnabled()) {
                LOG.info("Removed classpath module " + name + " from " + this);
            }
        }
    }

    public boolean activateModule(String name) {
        boolean success = toggleModuleState(name, true);
        if (success && LOG.isInfoEnabled()) {
            LOG.info("Module <" + MODULES.get(name) + "> has been activated.");
        }
        return success;
    }

    public boolean deactivateModule(String name) {
        boolean success = toggleModuleState(name, false);
        if (success && LOG.isInfoEnabled()) {
            LOG.info("Module <" + MODULES.get(name) + "> has been deactivated.");
        }
        return success;
    }

    public String classpath() {
        StringBuilder buffer = new StringBuilder();
        synchronized (MODULES) {
            for (ClasspathModule module : MODULES.values()) {
                if (module.isActive()) {
                    buffer.append(module.getConfiguration())
                            .append(";");
                }
            }
        }
        return buffer.toString();
    }

    private boolean toggleModuleState(String name, boolean newState) {
        if (name == null) {
            throw new IllegalArgumentException("Cannot " + (newState ? "activate" : "deactivate") + " a nameless module!");
        }

        ClasspathModule module = MODULES.get(name);
        if (module != null) {
            boolean currentState = module.isActive();
            module.setActive(newState);
            return currentState != newState;
        }

        return false;
    }

    private ClasspathModule createModule(ClasspathModuleConfiguration configuration) {
        return new ClasspathModule(configuration, newClassLoader(configuration));
    }

    private ClassLoader newClassLoader(ClasspathModuleConfiguration configuration) {
        return new UnmodifiableURLClassLoader(configuration.getName(), configuration.getUrls(), getParent());
    }

    private <V> V withClassLoaders(final ClassLoaderClosure<V> action) throws ClassNotFoundException {
        synchronized (MODULES) {
            for (ClasspathModule module : MODULES.values()) {
                if (module.isActive()) {
                    if (action.execute(module.getClassLoader())) {
                        break;
                    }
                }
            }
        }

        action.onCompletion();

        return action.getValue();
    }

    private <V> V withClassLoadersNoExceptions(final ClassLoaderClosure<V> action) {
        try {
            return withClassLoaders(action);
        } catch (ClassNotFoundException e) {
            // ignore
        }
        return null;
    }

    private static interface ClassLoaderClosure<V> {
        boolean execute(ClassLoader classLoader);

        V getValue();

        void onCompletion() throws ClassNotFoundException;
    }

    private static abstract class AbstractClassLoaderClosure<V> implements ClassLoaderClosure<V> {
        protected V value;

        public V getValue() {
            return value;
        }

        public void onCompletion() throws ClassNotFoundException {
            // empty
        }
    }

    @Override
    protected Class<?> loadClass(final String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = null;

        synchronized (this) {
            clazz = findLoadedClass(name);
        }

        if (clazz == null) {
            clazz = withClassLoaders(new AbstractClassLoaderClosure<Class<?>>() {
                public boolean execute(ClassLoader classLoader) {
                    try {
                        value = classLoader.loadClass(name);
                    } catch (ClassNotFoundException cnfe) {
                        // continue
                    }

                    return value != null;
                }

                public void onCompletion() throws ClassNotFoundException {
                    if (value == null) {
                        throw new ClassNotFoundException(name + " not found by " + this);
                    }
                }
            });
        }

        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        return withClassLoaders(new AbstractClassLoaderClosure<Class<?>>() {
            public boolean execute(ClassLoader classLoader) {
                value = invokeMethod(classLoader, "findClass", name, Class.class);
                return value != null;
            }

            public void onCompletion() throws ClassNotFoundException {
                if (value == null) {
                    throw new ClassNotFoundException(name + " not found by " + this);
                }
            }
        });
    }

    @Override
    public URL getResource(final String name) {
        return withClassLoadersNoExceptions(new AbstractClassLoaderClosure<URL>() {
            public boolean execute(ClassLoader classLoader) {
                value = classLoader.getResource(name);
                return value != null;
            }
        });
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        Enumeration<URL> resources = withClassLoadersNoExceptions(new AbstractClassLoaderClosure<Enumeration<URL>>() {
            public boolean execute(ClassLoader classLoader) {
                try {
                    value = classLoader.getResources(name);
                } catch (IOException ioe) {
                    // ignore
                }
                return value != null;
            }
        });

        if (resources == null) {
            throw new IOException("Resources for " + name + " not found by " + this);
        }

        return resources;
    }

    @Override
    protected URL findResource(final String name) {
        return withClassLoadersNoExceptions(new AbstractClassLoaderClosure<URL>() {
            public boolean execute(ClassLoader classLoader) {
                value = invokeMethod(classLoader, "findResource", name, URL.class);
                return value != null;
            }
        });
    }

    @Override
    protected Enumeration<URL> findResources(final String name) throws IOException {
        Enumeration<URL> resources = withClassLoadersNoExceptions(new AbstractClassLoaderClosure<Enumeration<URL>>() {
            public boolean execute(ClassLoader classLoader) {
                value = invokeMethod(classLoader, "findResources", name, Enumeration.class);
                return value != null;
            }
        });

        if (resources == null) {
            throw new IOException("Resources for " + name + " not found by " + this);
        }

        return resources;
    }

    @Override
    public InputStream getResourceAsStream(final String name) {
        return withClassLoadersNoExceptions(new AbstractClassLoaderClosure<InputStream>() {
            public boolean execute(ClassLoader classLoader) {
                value = classLoader.getResourceAsStream(name);
                return value != null;
            }
        });
    }

    private <V> V invokeMethod(Object object, String methodName, Object arg, Class<V> returnType) {
        try {
            Object result = MethodUtils.invokeExactMethod(object, methodName, arg);
            returnType.cast(result);
        } catch (NoSuchMethodException e) {
            // ignore
        } catch (IllegalAccessException e) {
            // ignore ?
        } catch (InvocationTargetException e) {
            // ignore ?
        }
        return null;
    }
}