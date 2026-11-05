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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.alx.enterprise.web.util.digester.Digester;
import com.alx.enterprise.web.util.file.ConfigFileLoader;
import com.alx.enterprise.web.util.file.ConfigurationSource.Resource;

/**
 * <b>XML Format </b>
 *
 * <pre>
 * {@code
 *       <Registry name="" encoding="UTF-8" >
 *         <Description
 *             tag="Server"
 *             standard="true"
 *             default="true"
 *             tagClass="com.alx.enterprise.webtier.core.StandardServer"
 *             storeFactoryClass="com.alx.enterprise.webtier.storeconfig.StandardServerSF">
 *           <TransientAttribute>domain</TransientAttribute>
 *           <TransientChild>com.alx.enterprise.webtier.core.StandardContext</TransientChild>
 *         </Description>
 *   ...
 *       </Registry>
 * }
 * </pre>
 *
 * Convention:
 * <ul>
 * <li>Factories at subpackage <i>com.alx.enterprise.webtier.storeconfig.xxxSF</i>.</li>
 * <li>Element name are the unique Class name</li>
 * <li>SF for StoreFactory</li>
 * <li>standard implementation is true</li>
 * </ul>
 */
public class StoreLoader {

    /**
     * Constructs a new StoreLoader for parsing store registry XML descriptors.
     */
    public StoreLoader() {
    }

    /**
     * The <code>Digester</code> instance used to parse registry descriptors.
     */
    protected static final Digester digester = createDigester();

    private StoreRegistry registry;

    private URL registryResource;

    /**
     * Returns the StoreRegistry instance loaded from the configuration.
     *
     * @return the StoreRegistry instance
     */
    public StoreRegistry getRegistry() {
        return registry;
    }

    /**
     * Sets the StoreRegistry instance to use.
     *
     * @param registry the StoreRegistry instance
     */
    public void setRegistry(StoreRegistry registry) {
        this.registry = registry;
    }

    /**
     * Create and configure the Digester we will be using for setup store registry.
     *
     * @return the XML digester that will be used to parse the configuration
     */
    protected static Digester createDigester() {
        // Initialize the digester
        Digester digester = new Digester();
        digester.setValidating(false);
        digester.setClassLoader(StoreRegistry.class.getClassLoader());

        // Configure the actions we will be using
        digester.addObjectCreate("Registry", "com.alx.enterprise.webtier.storeconfig.StoreRegistry", "className");
        digester.addSetProperties("Registry");
        digester.addObjectCreate("Registry/Description", "com.alx.enterprise.webtier.storeconfig.StoreDescription",
                "className");
        digester.addSetProperties("Registry/Description");
        digester.addRule("Registry/Description",
                new StoreFactoryRule("com.alx.enterprise.webtier.storeconfig.StoreFactoryBase", "storeFactoryClass",
                        "com.alx.enterprise.webtier.storeconfig.StoreAppender", "storeAppenderClass"));
        digester.addSetNext("Registry/Description", "registerDescription",
                "com.alx.enterprise.webtier.storeconfig.StoreDescription");
        digester.addCallMethod("Registry/Description/TransientAttribute", "addTransientAttribute", 0);
        digester.addCallMethod("Registry/Description/TransientChild", "addTransientChild", 0);

        return digester;

    }

    /**
     * Load registry configuration.
     *
     * @param path Path to the configuration file, may be null to use the default name server-registry.xml
     *
     * @throws Exception when the configuration file isn't found or a parse error occurs
     */
    public void load(String path) throws Exception {
        try (Resource resource = (path == null) ? ConfigFileLoader.getSource().getConfResource("server-registry.xml") :
                ConfigFileLoader.getSource().getResource(path); InputStream is = resource.getInputStream()) {
            registryResource = resource.getURI().toURL();
            synchronized (digester) {
                registry = (StoreRegistry) digester.parse(is);
            }
        } catch (IOException ioe) {
            // Try default classloader location
            try (InputStream is =
                    StoreLoader.class.getResourceAsStream("/com/alx/enterprise/webtier/storeconfig/server-registry.xml")) {
                if (is != null) {
                    registryResource =
                            StoreLoader.class.getResource("/com/alx/enterprise/webtier/storeconfig/server-registry.xml");
                    synchronized (digester) {
                        registry = (StoreRegistry) digester.parse(is);
                    }
                } else {
                    throw ioe;
                }
            }
        }
    }

    /**
     * Returns the URL of the registry resource that was loaded.
     *
     * @return the registry resource URL
     */
    public URL getRegistryResource() {
        return registryResource;
    }
}
