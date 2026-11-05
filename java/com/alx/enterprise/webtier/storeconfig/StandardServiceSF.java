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

import com.alx.enterprise.webtier.Engine;
import com.alx.enterprise.webtier.Executor;
import com.alx.enterprise.webtier.LifecycleListener;
import com.alx.enterprise.webtier.connector.Connector;
import com.alx.enterprise.webtier.core.StandardService;

/**
 * Store server.xml Element Service and all children
 */
public class StandardServiceSF extends StoreFactoryBase {

    /**
     * Default constructor.
     */
    public StandardServiceSF() {
    }

    /**
     * Store the specified service element children.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void storeChildren(PrintWriter aWriter, int indent, Object aService, StoreDescription parentDesc)
            throws Exception {
        if (aService instanceof StandardService) {
            StandardService service = (StandardService) aService;
            // Store nested <Listener> elements
            LifecycleListener[] listeners = service.findLifecycleListeners();
            storeElementArray(aWriter, indent, listeners);

            // Store nested <Executor> elements
            Executor[] executors = service.findExecutors();
            storeElementArray(aWriter, indent, executors);

            Connector[] connectors = service.findConnectors();
            storeElementArray(aWriter, indent, connectors);

            // Store nested <Engine> element
            Engine container = service.getContainer();
            if (container != null) {
                StoreDescription elementDesc = getRegistry().findDescription(container.getClass());
                if (elementDesc != null) {
                    IStoreFactory factory = elementDesc.getStoreFactory();
                    factory.store(aWriter, indent, container);
                }
            }
        }
    }
}
