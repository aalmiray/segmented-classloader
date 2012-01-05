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

/**
 * @author Andres Almiray
 */
public class ClasspathModule {
    private final ClasspathModuleConfiguration configuration;
    private final ClassLoader classLoader;
    private boolean active = true;

    public ClasspathModule(ClasspathModuleConfiguration configuration, ClassLoader classLoader) {
        this.configuration = configuration;
        this.classLoader = classLoader;
    }

    public String getName() {
        return getConfiguration().getName();
    }

    public ClasspathModuleConfiguration getConfiguration() {
        return configuration;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
