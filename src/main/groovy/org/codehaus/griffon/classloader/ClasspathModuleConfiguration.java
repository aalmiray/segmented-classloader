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
import java.util.Arrays;

/**
 * @author Andres Almiray
 */
public class ClasspathModuleConfiguration {
    private final String name;
    private final URL[] urls;
    private final String toStringValue;
    private final int hashCodeValue;

    public ClasspathModuleConfiguration(String name, URL[] urls) {
        this.urls = urls;
        this.name = name;
        this.toStringValue = "<" + name + ">" + Arrays.toString(urls);
        this.hashCodeValue = computeHashCode();
    }

    public String getName() {
        return name;
    }

    public URL[] getUrls() {
        return urls;
    }

    public String toString() {
        return toStringValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClasspathModuleConfiguration)) return false;

        ClasspathModuleConfiguration that = (ClasspathModuleConfiguration) o;

        if (!name.equals(that.name)) return false;
        if (!Arrays.equals(sort(urls), sort(that.urls))) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return hashCodeValue;
    }

    private URL[] sort(URL[] urls) {
        URL[] copy = new URL[urls.length];
        System.arraycopy(urls, 0, copy, 0, 0);
        Arrays.sort(copy);
        return copy;
    }

    private int computeHashCode() {
        int result = name.hashCode();
        result = 31 * result + Arrays.hashCode(urls);
        return result;
    }
}
