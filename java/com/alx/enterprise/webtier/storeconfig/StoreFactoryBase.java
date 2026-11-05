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
import java.io.PrintWriter;

import com.alx.enterprise.logging.internal.Log;
import com.alx.enterprise.logging.internal.LogFactory;
import com.alx.enterprise.web.util.res.StringManager;

/**
 * StoreFactory saves special elements. Output was generated with StoreAppenders.
 */
public class StoreFactoryBase implements IStoreFactory {
    /**
     * Constructs a new StoreFactoryBase with default settings.
     */
    public StoreFactoryBase() {
    }

    private static final Log log = LogFactory.getLog(StoreFactoryBase.class);

    private StoreRegistry registry;

    private StoreAppender storeAppender = new StoreAppender();

    /**
     * The string manager for this package.
     */
    protected static final StringManager sm = StringManager.getManager(Constants.Package);

    /**
     * The descriptive information string for this implementation.
     */
    private static final String info = "com.alx.enterprise.webtier.storeconfig.StoreFactoryBase/1.0";

    /**
     * Returns descriptive information about this Factory implementation and the corresponding version number, in the
     * format <code>&lt;description&gt;/&lt;version&gt;</code>.
     *
     * @return descriptive information about this Factory implementation and the corresponding version number, in the
     *             format <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return info;
    }

    @Override
    public StoreAppender getStoreAppender() {
        return storeAppender;
    }

    @Override
    public void setStoreAppender(StoreAppender storeAppender) {
        this.storeAppender = storeAppender;
    }

    @Override
    public void setRegistry(StoreRegistry aRegistry) {
        registry = aRegistry;

    }

    @Override
    public StoreRegistry getRegistry() {

        return registry;
    }

    @Override
    public void storeXMLHead(PrintWriter aWriter) {
        // Store the beginning of this element
        aWriter.print("<?xml version=\"1.0\" encoding=\"");
        aWriter.print(getRegistry().getEncoding());
        aWriter.println("\"?>");
    }

    @Override
    public void store(PrintWriter aWriter, int indent, Object aElement) throws Exception {

        StoreDescription elementDesc = getRegistry().findDescription(aElement.getClass());

        if (elementDesc != null) {
            if (log.isTraceEnabled()) {
                log.trace(sm.getString("factory.storeTag", elementDesc.getTag(), aElement));
            }
            getStoreAppender().printIndent(aWriter, indent + 2);
            if (!elementDesc.isChildren()) {
                getStoreAppender().printTag(aWriter, indent, aElement, elementDesc);
            } else {
                getStoreAppender().printOpenTag(aWriter, indent + 2, aElement, elementDesc);
                storeChildren(aWriter, indent + 2, aElement, elementDesc);
                getStoreAppender().printIndent(aWriter, indent + 2);
                getStoreAppender().printCloseTag(aWriter, elementDesc);
            }
        } else {
            log.warn(sm.getString("factory.storeNoDescriptor", aElement.getClass()));
        }
    }

    /**
     * Must Implement at subclass for custom store children handling.
     *
     * @param aWriter     Current output writer
     * @param indent      Indentation level
     * @param aElement    Current element
     * @param elementDesc The element description
     *
     * @throws Exception Configuration storing error
     */
    public void storeChildren(PrintWriter aWriter, int indent, Object aElement, StoreDescription elementDesc)
            throws Exception {
    }

    /**
     * Store only elements from storeChildren methods that are not a transient child.
     *
     * @param aWriter     Current output writer
     * @param indent      Indentation level
     * @param aTagElement Current element
     *
     * @throws Exception Configuration storing error
     */
    protected void storeElement(PrintWriter aWriter, int indent, Object aTagElement) throws Exception {
        if (aTagElement != null) {
            IStoreFactory elementFactory = getRegistry().findStoreFactory(aTagElement.getClass());

            if (elementFactory != null) {
                StoreDescription desc = getRegistry().findDescription(aTagElement.getClass());
                if (desc != null) {
                    if (!desc.isTransientChild(aTagElement.getClass().getName())) {
                        elementFactory.store(aWriter, indent, aTagElement);
                    }
                } else {
                    log.warn(sm.getString("factory.storeNoDescriptor", aTagElement.getClass()));
                }
            } else {
                log.warn(sm.getString("factory.storeNoDescriptor", aTagElement.getClass()));
            }
        }
    }

    /**
     * Save an array of elements.
     *
     * @param aWriter  Current output writer
     * @param indent   Indentation level
     * @param elements Array of elements
     *
     * @throws Exception Configuration storing error
     */
    protected void storeElementArray(PrintWriter aWriter, int indent, Object[] elements) throws Exception {
        if (elements != null) {
            for (Object element : elements) {
                try {
                    storeElement(aWriter, indent, element);
                } catch (IOException ignore) {
                    // Ignore. Children do not report the error themselves (e.g.
                    // StandardContextSF.storeWithBackup() throws without logging).
                }
            }
        }
    }
}
