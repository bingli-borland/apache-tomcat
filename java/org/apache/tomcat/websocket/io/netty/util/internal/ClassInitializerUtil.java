/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomcat.websocket.io.netty.util.internal;

/**
 * Utility which ensures that classes are loaded by the {@link ClassLoader}.
 */
public final class ClassInitializerUtil {

    private ClassInitializerUtil() { }

    /**
     * Preload the given classes and so ensure the {@link ClassLoader} has these loaded after this method call.
     *
     * @param loadingClass      the {@link Class} that wants to load the classes.
     * @param classes           the classes to load.
     */
    public static void tryLoadClasses(Class<?> loadingClass, Class<?>... classes) {
        ClassLoader loader = PlatformDependent.getClassLoader(loadingClass);
        for (Class<?> clazz: classes) {
            tryLoadClass(loader, clazz.getName());
        }
    }

    private static void tryLoadClass(ClassLoader classLoader, String className) {
        try {
            // Load the class and also ensure we init it which means its linked etc.
            Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException ignore) {
            // Ignore
        } catch (SecurityException ignore) {
            // Ignore
        }
    }
}
