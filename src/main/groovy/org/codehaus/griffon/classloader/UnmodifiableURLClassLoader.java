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

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Andres Almiray
 */
public class UnmodifiableURLClassLoader extends URLClassLoader {
    private final String name;

    public UnmodifiableURLClassLoader(String name, URL[] urls, ClassLoader classLoader) {
        super(urls, classLoader);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    protected void addURL(URL url) {
        throw new UnsupportedOperationException("Classloader <" + name + "> is unmodifiable!");
    }
}
