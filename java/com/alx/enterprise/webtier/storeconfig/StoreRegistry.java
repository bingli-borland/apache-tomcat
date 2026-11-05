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
package com.alx.enterprise.webtier.storeconfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.directory.DirContext;

import com.alx.enterprise.webtier.CredentialHandler;
import com.alx.enterprise.webtier.LifecycleListener;
import com.alx.enterprise.webtier.Manager;
import com.alx.enterprise.webtier.Realm;
import com.alx.enterprise.webtier.Valve;
import com.alx.enterprise.webtier.WebResourceRoot;
import com.alx.enterprise.webtier.WebResourceSet;
import com.alx.enterprise.web.crane.UpgradeProtocol;
import com.alx.enterprise.logging.internal.Log;
import com.alx.enterprise.logging.internal.LogFactory;
import com.alx.enterprise.web.util.http.CookieProcessor;
import com.alx.enterprise.web.util.res.StringManager;

/**
 * Central StoreRegistry for all server.xml elements
 */
public class StoreRegistry {
    /**
     * Constructs a new StoreRegistry with default settings.
     */
    public StoreRegistry() {
    }

    private static final Log log = LogFactory.getLog(StoreRegistry.class);
    private static final StringManager sm = StringManager.getManager(StoreRegistry.class);

    private final Map<String,StoreDescription> descriptors = new HashMap<>();

    private String encoding = "UTF-8";

    private String name;

    private String version;

    // Access Information
    // Lazily initialized to gracefully handle optional features like clustering
    private static volatile Class<?>[] interfaces = null;

    /**
     * Initialize the interfaces array with all available classes.
     * Uses dynamic loading for optional classes (e.g., clustering) to avoid
     * ClassNotFoundException when those JARs are not present. This approach
     * is consistent with how Catalina.addClusterRuleSet() handles clustering.
     */
    private static Class<?>[] getInterfaces() {
        if (interfaces == null) {
            synchronized (StoreRegistry.class) {
                if (interfaces == null) {
                    // Required interfaces - always present
                    List<Class<?>> list = new ArrayList<>();
                    list.add(Realm.class);
                    list.add(Manager.class);
                    list.add(DirContext.class);
                    list.add(LifecycleListener.class);
                    list.add(Valve.class);
                    list.add(WebResourceRoot.class);
                    list.add(WebResourceSet.class);
                    list.add(CredentialHandler.class);
                    list.add(UpgradeProtocol.class);
                    list.add(CookieProcessor.class);

                    // Optional clustering interfaces - load dynamically to support
                    // deployments where clustering JARs may not be present
                    tryAddClass(list, "com.alx.enterprise.webtier.ha.CatalinaCluster");
                    tryAddClass(list, "com.alx.enterprise.webtier.tribes.ChannelSender");
                    tryAddClass(list, "com.alx.enterprise.webtier.tribes.ChannelReceiver");
                    tryAddClass(list, "com.alx.enterprise.webtier.tribes.Channel");
                    tryAddClass(list, "com.alx.enterprise.webtier.tribes.MembershipService");
                    tryAddClass(list, "com.alx.enterprise.webtier.ha.ClusterDeployer");
                    tryAddClass(list, "com.alx.enterprise.webtier.ha.ClusterListener");
                    tryAddClass(list, "com.alx.enterprise.webtier.tribes.MessageListener");
                    tryAddClass(list, "com.alx.enterprise.webtier.tribes.transport.DataSender");
                    tryAddClass(list, "com.alx.enterprise.webtier.tribes.ChannelInterceptor");
                    tryAddClass(list, "com.alx.enterprise.webtier.tribes.Member");

                    interfaces = list.toArray(new Class<?>[0]);

                    if (log.isDebugEnabled()) {
                        log.debug(sm.getString("registry.interfacesLoaded", Integer.valueOf(interfaces.length)));
                    }
                }
            }
        }
        return interfaces;
    }

    /**
     * Try to load a class by name and add it to the list if successful.
     * Logs at TRACE level if the class is not available.
     */
    private static void tryAddClass(List<Class<?>> list, String className) {
        try {
            Class<?> clazz = Class.forName(className, false, StoreRegistry.class.getClassLoader());
            list.add(clazz);
            if (log.isTraceEnabled()) {
                log.trace(sm.getString("registry.optionalClassLoaded", className));
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            if (log.isTraceEnabled()) {
                log.trace(sm.getString("registry.optionalClassNotFound", className));
            }
        }
    }

    /**
     * Returns the name of this registry.
     *
     * @return the registry name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this registry.
     *
     * @param name the registry name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the version of this registry.
     *
     * @return the registry version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of this registry.
     *
     * @param version the registry version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Find a description for id. Handle interface search when no direct match found.
     *
     * @param id The class name
     *
     * @return the description
     */
    public StoreDescription findDescription(String id) {
        if (log.isTraceEnabled()) {
            log.trace("search descriptor " + id);
        }
        StoreDescription desc = descriptors.get(id);
        if (desc == null) {
            Class<?> aClass = null;
            try {
                aClass = Class.forName(id, true, this.getClass().getClassLoader());
            } catch (ClassNotFoundException e) {
                log.error(sm.getString("registry.loadClassFailed", id), e);
            }
            if (aClass != null) {
                desc = descriptors.get(aClass.getName());
                Class<?>[] availableInterfaces = getInterfaces();
                for (int i = 0; desc == null && i < availableInterfaces.length; i++) {
                    if (availableInterfaces[i].isAssignableFrom(aClass)) {
                        desc = descriptors.get(availableInterfaces[i].getName());
                    }
                }
            }
        }
        if (log.isDebugEnabled()) {
            if (desc != null) {
                log.trace("find descriptor " + id + "#" + desc.getTag() + "#" + desc.getStoreFactoryClass());
            } else {
                log.debug(sm.getString("registry.noDescriptor", id));
            }
        }
        return desc;
    }

    /**
     * Find Description by class.
     *
     * @param aClass The class
     *
     * @return the description
     */
    public StoreDescription findDescription(Class<?> aClass) {
        return findDescription(aClass.getName());
    }

    /**
     * Find factory from class name.
     *
     * @param aClassName The class name
     *
     * @return the factory
     */
    public IStoreFactory findStoreFactory(String aClassName) {
        StoreDescription desc = findDescription(aClassName);
        if (desc != null) {
            return desc.getStoreFactory();
        } else {
            return null;
        }

    }

    /**
     * Find factory from class.
     *
     * @param aClass The class
     *
     * @return the factory
     */
    public IStoreFactory findStoreFactory(Class<?> aClass) {
        return findStoreFactory(aClass.getName());
    }

    /**
     * Register a new description.
     *
     * @param desc New description
     */
    public void registerDescription(StoreDescription desc) {
        String key = desc.getId();
        if (key == null || key.isEmpty()) {
            key = desc.getTagClass();
        }
        descriptors.put(key, desc);
        if (log.isTraceEnabled()) {
            log.trace("register store descriptor " + key + "#" + desc.getTag() + "#" + desc.getTagClass());
        }
    }

    /**
     * Unregister a description.
     *
     * @param desc The description
     *
     * @return the description, or <code>null</code> if it was not registered
     */
    public StoreDescription unregisterDescription(StoreDescription desc) {
        String key = desc.getId();
        if (key == null || key.isEmpty()) {
            key = desc.getTagClass();
        }
        return descriptors.remove(key);
    }

    // Attributes

    /**
     * Returns the character encoding used when writing configuration files.
     *
     * @return the character encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Set the encoding to use when writing the configuration files.
     *
     * @param string The encoding
     */
    public void setEncoding(String string) {
        encoding = string;
    }

}
