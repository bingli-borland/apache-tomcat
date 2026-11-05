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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.alx.enterprise.webtier.Cluster;
import com.alx.enterprise.webtier.Container;
import com.alx.enterprise.webtier.LifecycleListener;
import com.alx.enterprise.webtier.Realm;
import com.alx.enterprise.webtier.Valve;
import com.alx.enterprise.webtier.core.StandardEngine;

/**
 * Store server.xml Element Engine
 */
public class StandardEngineSF extends StoreFactoryBase {

    private static final Class<?> clusterValveClass;
    static {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("com.alx.enterprise.webtier.ha.ClusterValve");
        } catch (ClassNotFoundException e) {
            // Expected when clustering JARs are not present
        }
        clusterValveClass = clazz;
    }

    /**
     * Constructs a new StandardEngineSF instance for storing Engine elements in server.xml.
     */
    public StandardEngineSF() {
    }

    /**
     * Store the specified Engine properties.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void storeChildren(PrintWriter aWriter, int indent, Object aEngine, StoreDescription parentDesc)
            throws Exception {
        if (aEngine instanceof StandardEngine engine) {
            // Store nested <Listener> elements
            LifecycleListener[] listeners = engine.findLifecycleListeners();
            storeElementArray(aWriter, indent, listeners);

            // Store nested <Realm> element
            Realm realm = engine.getRealm();
            if (realm != null) {
                storeElement(aWriter, indent, realm);

            }

            // Store nested <Valve> elements
            Valve[] valves = engine.getPipeline().getValves();
            if (valves != null && valves.length > 0) {
                List<Valve> engineValves = new ArrayList<>();
                for (Valve valve : valves) {
                    if (clusterValveClass == null || !clusterValveClass.isInstance(valve)) {
                        engineValves.add(valve);
                    }
                }
                storeElementArray(aWriter, indent, engineValves.toArray());
            }

            // store all <Cluster> elements
            Cluster cluster = engine.getCluster();
            if (cluster != null) {
                storeElement(aWriter, indent, cluster);
            }

            // store all <Host> elements
            Container[] children = engine.findChildren();
            storeElementArray(aWriter, indent, children);
        }
    }
}
