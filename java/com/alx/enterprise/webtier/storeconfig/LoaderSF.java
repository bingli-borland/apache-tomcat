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

import com.alx.enterprise.webtier.Loader;
import com.alx.enterprise.webtier.loader.ParallelWebappClassLoader;
import com.alx.enterprise.webtier.loader.WebappLoader;
import com.alx.enterprise.logging.internal.Log;
import com.alx.enterprise.logging.internal.LogFactory;

/**
 * Store Loader Element.
 */
public class LoaderSF extends StoreFactoryBase {

    /**
     * Default constructor.
     */
    public LoaderSF() {
    }

    private static final Log log = LogFactory.getLog(LoaderSF.class);

    @Override
    public void store(PrintWriter aWriter, int indent, Object aElement) throws Exception {
        if (aElement instanceof Loader) {
            Loader loader = (Loader) aElement;
            StoreDescription elementDesc = getRegistry().findDescription(aElement.getClass());
            if (elementDesc != null) {
                if (!isDefaultLoader(loader)) {
                    if (log.isTraceEnabled()) {
                        log.trace(sm.getString("factory.storeTag", elementDesc.getTag(), aElement));
                    }
                    getStoreAppender().printIndent(aWriter, indent + 2);
                    getStoreAppender().printTag(aWriter, indent + 2, loader, elementDesc);
                }
            } else {
                log.warn(sm.getString("factory.storeNoDescriptor", aElement.getClass()));
            }
        } else {
            super.store(aWriter, indent, aElement);
        }
    }

    /**
     * Is this an instance of the default <code>Loader</code> configuration, with all-default properties?
     *
     * @param loader Loader to be tested
     *
     * @return <code>true</code> if this is an instance of the default loader
     */
    protected boolean isDefaultLoader(Loader loader) {

        if (!(loader instanceof WebappLoader)) {
            return false;
        }
        WebappLoader wloader = (WebappLoader) loader;
        return (!wloader.getDelegate()) && wloader.getJakartaConverter() == null &&
                wloader.getLoaderClass().equals(ParallelWebappClassLoader.class.getName());
    }
}
