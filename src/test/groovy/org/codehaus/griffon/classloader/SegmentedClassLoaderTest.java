package org.codehaus.griffon.classloader;

import junit.framework.TestCase;

import java.net.URL;

public class SegmentedClassLoaderTest extends TestCase {
    private SegmentedClassLoader classLoader = new SegmentedClassLoader();
    private ClasspathModuleConfiguration groovyModule = new ClasspathModuleConfiguration("groovy", new URL[]{SegmentedClassLoaderTest.class.getClassLoader().getResource("groovy-1.8.5.jar")});

    public void testShouldThrowClassNotFoundException() {
        try {
            classLoader.loadClass("groovy.lang.Closure");
            fail("Should have thrown ClassNotFoundException for 'groovy.lang.Closure'");
        } catch (ClassNotFoundException ncfe) {
            // OK!
        }
    }

    public void testShouldFindTheClass() {
        try {
            classLoader.addClasspathModule(groovyModule);
            classLoader.loadClass("groovy.lang.Closure");
        } catch (ClassNotFoundException ncfe) {
            fail("Should have found class 'groovy.lang.Closure");
        }
    }

    public void testShouldFailToFindTheClassIfModuleIsRemoved() {
        testShouldThrowClassNotFoundException();
        testShouldFindTheClass();

        classLoader.removeClasspathModule(groovyModule.getName());

        testShouldThrowClassNotFoundException();
    }

    public void testShouldFailToFindTheClassIfModuleIsDeactivated() {
        testShouldThrowClassNotFoundException();
        testShouldFindTheClass();

        classLoader.deactivateModule(groovyModule.getName());

        testShouldThrowClassNotFoundException();
    }

    public void testShouldFindTheClassIfModuleIsReactivated() {
        testShouldFailToFindTheClassIfModuleIsDeactivated();

        classLoader.activateModule(groovyModule.getName());

        try {
            classLoader.loadClass("groovy.lang.Closure");
        } catch (ClassNotFoundException ncfe) {
            fail("Should have found class 'groovy.lang.Closure");
        }
    }
}
