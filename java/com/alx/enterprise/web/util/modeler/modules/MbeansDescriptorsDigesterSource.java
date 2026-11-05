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
package com.alx.enterprise.web.util.modeler.modules;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.management.ObjectName;

import com.alx.enterprise.logging.internal.Log;
import com.alx.enterprise.logging.internal.LogFactory;
import com.alx.enterprise.web.util.digester.Digester;
import com.alx.enterprise.web.util.modeler.ManagedBean;
import com.alx.enterprise.web.util.modeler.Registry;

/**
 * Source for loading MBean descriptors from XML using a Digester.
 */
public class MbeansDescriptorsDigesterSource extends ModelerSource {
    /**
     * Default constructor.
     */
    public MbeansDescriptorsDigesterSource() {
    }

    /**
     * The log instance for this class.
     */
    private static final Log log = LogFactory.getLog(MbeansDescriptorsDigesterSource.class);
    private static final Object dLock = new Object();

    private Registry registry;
    private final List<ObjectName> mbeans = new ArrayList<>();
    private static Digester digester = null;

    private static Digester createDigester() {

        Digester digester = new Digester();
        digester.setNamespaceAware(false);
        digester.setValidating(false);
        URL url = Registry.getRegistry(null).getClass()
                .getResource("/com/alx/enterprise/web/util/modeler/mbeans-descriptors.dtd");
        if (url != null) {
            digester.register("-//Apache Software Foundation//DTD Model MBeans Configuration File", url.toString());
        }

        // Configure the parsing rules
        digester.addObjectCreate("mbeans-descriptors/mbean", "com.alx.enterprise.web.util.modeler.ManagedBean");
        digester.addSetProperties("mbeans-descriptors/mbean");
        digester.addSetNext("mbeans-descriptors/mbean", "add", "java.lang.Object");

        digester.addObjectCreate("mbeans-descriptors/mbean/attribute", "com.alx.enterprise.web.util.modeler.AttributeInfo");
        digester.addSetProperties("mbeans-descriptors/mbean/attribute");
        digester.addSetNext("mbeans-descriptors/mbean/attribute", "addAttribute",
                "com.alx.enterprise.web.util.modeler.AttributeInfo");

        digester.addObjectCreate("mbeans-descriptors/mbean/notification",
                "com.alx.enterprise.web.util.modeler.NotificationInfo");
        digester.addSetProperties("mbeans-descriptors/mbean/notification");
        digester.addSetNext("mbeans-descriptors/mbean/notification", "addNotification",
                "com.alx.enterprise.web.util.modeler.NotificationInfo");

        digester.addObjectCreate("mbeans-descriptors/mbean/notification/descriptor/field",
                "com.alx.enterprise.web.util.modeler.FieldInfo");
        digester.addSetProperties("mbeans-descriptors/mbean/notification/descriptor/field");
        digester.addSetNext("mbeans-descriptors/mbean/notification/descriptor/field", "addField",
                "com.alx.enterprise.web.util.modeler.FieldInfo");

        digester.addCallMethod("mbeans-descriptors/mbean/notification/notification-type", "addNotifType", 0);

        digester.addObjectCreate("mbeans-descriptors/mbean/operation", "com.alx.enterprise.web.util.modeler.OperationInfo");
        digester.addSetProperties("mbeans-descriptors/mbean/operation");
        digester.addSetNext("mbeans-descriptors/mbean/operation", "addOperation",
                "com.alx.enterprise.web.util.modeler.OperationInfo");

        digester.addObjectCreate("mbeans-descriptors/mbean/operation/descriptor/field",
                "com.alx.enterprise.web.util.modeler.FieldInfo");
        digester.addSetProperties("mbeans-descriptors/mbean/operation/descriptor/field");
        digester.addSetNext("mbeans-descriptors/mbean/operation/descriptor/field", "addField",
                "com.alx.enterprise.web.util.modeler.FieldInfo");

        digester.addObjectCreate("mbeans-descriptors/mbean/operation/parameter",
                "com.alx.enterprise.web.util.modeler.ParameterInfo");
        digester.addSetProperties("mbeans-descriptors/mbean/operation/parameter");
        digester.addSetNext("mbeans-descriptors/mbean/operation/parameter", "addParameter",
                "com.alx.enterprise.web.util.modeler.ParameterInfo");

        return digester;

    }

    /**
     * Set the registry.
     *
     * @param reg the registry to set
     */
    public void setRegistry(Registry reg) {
        this.registry = reg;
    }


    /**
     * Set the source.
     *
     * @param source the source to set
     */
    public void setSource(Object source) {
        this.source = source;
    }

    @Override
    public List<ObjectName> loadDescriptors(Registry registry, String type, Object source) throws Exception {
        setRegistry(registry);
        setSource(source);
        execute();
        return mbeans;
    }

    /**
     * Execute the descriptor loading.
     *
     * @throws Exception if an error occurs
     */
    public void execute() throws Exception {
        if (registry == null) {
            registry = Registry.getRegistry(null);
        }

        InputStream stream = (InputStream) source;

        List<ManagedBean> loadedMbeans = new ArrayList<>();
        synchronized (dLock) {
            if (digester == null) {
                digester = createDigester();
            }

            // Process the input file to configure our registry
            try {
                // Push our registry object onto the stack
                digester.push(loadedMbeans);
                digester.parse(stream);
            } catch (Exception e) {
                log.error(sm.getString("modules.digesterParseError"), e);
                throw e;
            } finally {
                digester.reset();
            }

        }
        for (ManagedBean loadedMbean : loadedMbeans) {
            registry.addManagedBean(loadedMbean);
        }
    }
}
