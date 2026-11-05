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
package com.alx.enterprise.webtier.startup;

import com.alx.enterprise.webtier.core.StandardContext;
import com.alx.enterprise.web.util.digester.Digester;
import com.alx.enterprise.web.util.digester.Rule;
import com.alx.enterprise.web.util.digester.RuleSet;
import org.xml.sax.Attributes;

/**
 * <strong>RuleSet</strong> for processing the contents of a Context definition element.
 */
public class ContextRuleSet implements RuleSet {

    // ----------------------------------------------------- Instance Variables

    /**
     * The matching pattern prefix to use for recognizing our elements.
     */
    protected final String prefix;


    /**
     * Should the context be created.
     */
    protected final boolean create;


    // ------------------------------------------------------------ Constructor

    /**
     * Construct an instance of this <code>RuleSet</code> with the default matching pattern prefix.
     * The context instance will be created by the digester.
     */
    public ContextRuleSet() {
        this("");
    }


    /**
     * Construct an instance of this <code>RuleSet</code> with the specified matching pattern prefix.
     * The context instance will be created by the digester.
     *
     * @param prefix Prefix for matching pattern rules (including the trailing slash character)
     */
    public ContextRuleSet(String prefix) {
        this(prefix, true);
    }


    /**
     * Construct an instance of this <code>RuleSet</code> with the specified matching pattern prefix.
     *
     * @param prefix Prefix for matching pattern rules (including the trailing slash character)
     * @param create <code>true</code> if the main context instance should be created
     */
    public ContextRuleSet(String prefix, boolean create) {
        this.prefix = prefix;
        this.create = create;
    }


    // --------------------------------------------------------- Public Methods

    @Override
    public void addRuleInstances(Digester digester) {

        if (create) {
            digester.addObjectCreate(prefix + "Context", "com.alx.enterprise.webtier.core.StandardContext", "className");
            digester.addSetProperties(prefix + "Context");
            digester.addRule(prefix + "Context", new SetDeployedFromServerXmlRule());
        } else {
            digester.addSetProperties(prefix + "Context", new String[] { "path", "docBase" });
        }

        if (create) {
            digester.addRule(prefix + "Context",
                    new LifecycleListenerRule("com.alx.enterprise.webtier.startup.ContextConfig", "configClass"));
            digester.addSetNext(prefix + "Context", "addChild", "com.alx.enterprise.webtier.Container");
        }

        digester.addObjectCreate(prefix + "Context/Listener", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(prefix + "Context/Listener");
        digester.addSetNext(prefix + "Context/Listener", "addLifecycleListener",
                "com.alx.enterprise.webtier.LifecycleListener");

        digester.addObjectCreate(prefix + "Context/Loader", "com.alx.enterprise.webtier.loader.WebappLoader", "className");
        digester.addSetProperties(prefix + "Context/Loader");
        digester.addSetNext(prefix + "Context/Loader", "setLoader", "com.alx.enterprise.webtier.Loader");

        digester.addObjectCreate(prefix + "Context/Manager", "com.alx.enterprise.webtier.session.StandardManager",
                "className");
        digester.addSetProperties(prefix + "Context/Manager");
        digester.addSetNext(prefix + "Context/Manager", "setManager", "com.alx.enterprise.webtier.Manager");

        digester.addObjectCreate(prefix + "Context/Manager/Store", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(prefix + "Context/Manager/Store");
        digester.addSetNext(prefix + "Context/Manager/Store", "setStore", "com.alx.enterprise.webtier.Store");

        digester.addObjectCreate(prefix + "Context/Manager/SessionIdGenerator",
                "com.alx.enterprise.webtier.util.StandardSessionIdGenerator", "className");
        digester.addSetProperties(prefix + "Context/Manager/SessionIdGenerator");
        digester.addSetNext(prefix + "Context/Manager/SessionIdGenerator", "setSessionIdGenerator",
                "com.alx.enterprise.webtier.SessionIdGenerator");

        digester.addObjectCreate(prefix + "Context/Parameter",
                "com.alx.enterprise.web.util.descriptor.web.ApplicationParameter");
        digester.addSetProperties(prefix + "Context/Parameter");
        digester.addSetNext(prefix + "Context/Parameter", "addApplicationParameter",
                "com.alx.enterprise.web.util.descriptor.web.ApplicationParameter");

        digester.addRuleSet(new RealmRuleSet(prefix + "Context/"));

        digester.addObjectCreate(prefix + "Context/Resources", "com.alx.enterprise.webtier.webresources.StandardRoot",
                "className");
        digester.addSetProperties(prefix + "Context/Resources");
        digester.addSetNext(prefix + "Context/Resources", "setResources", "com.alx.enterprise.webtier.WebResourceRoot");

        digester.addObjectCreate(prefix + "Context/Resources/CacheStrategy", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(prefix + "Context/Resources/CacheStrategy");
        digester.addSetNext(prefix + "Context/Resources/CacheStrategy", "setCacheStrategy",
                "com.alx.enterprise.webtier.WebResourceRoot$CacheStrategy");

        digester.addObjectCreate(prefix + "Context/Resources/PreResources", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(prefix + "Context/Resources/PreResources");
        digester.addSetNext(prefix + "Context/Resources/PreResources", "addPreResources",
                "com.alx.enterprise.webtier.WebResourceSet");

        digester.addObjectCreate(prefix + "Context/Resources/JarResources", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(prefix + "Context/Resources/JarResources");
        digester.addSetNext(prefix + "Context/Resources/JarResources", "addJarResources",
                "com.alx.enterprise.webtier.WebResourceSet");

        digester.addObjectCreate(prefix + "Context/Resources/PostResources", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(prefix + "Context/Resources/PostResources");
        digester.addSetNext(prefix + "Context/Resources/PostResources", "addPostResources",
                "com.alx.enterprise.webtier.WebResourceSet");


        digester.addObjectCreate(prefix + "Context/ResourceLink",
                "com.alx.enterprise.web.util.descriptor.web.ContextResourceLink");
        digester.addSetProperties(prefix + "Context/ResourceLink");
        digester.addRule(prefix + "Context/ResourceLink",
                new SetNextNamingRule("addResourceLink", "com.alx.enterprise.web.util.descriptor.web.ContextResourceLink"));

        digester.addObjectCreate(prefix + "Context/Valve", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(prefix + "Context/Valve");
        digester.addSetNext(prefix + "Context/Valve", "addValve", "com.alx.enterprise.webtier.Valve");

        digester.addCallMethod(prefix + "Context/WatchedResource", "addWatchedResource", 0);

        digester.addCallMethod(prefix + "Context/WrapperLifecycle", "addWrapperLifecycle", 0);

        digester.addCallMethod(prefix + "Context/WrapperListener", "addWrapperListener", 0);

        digester.addObjectCreate(prefix + "Context/JarScanner", "com.alx.enterprise.web.util.scan.StandardJarScanner",
                "className");
        digester.addSetProperties(prefix + "Context/JarScanner");
        digester.addSetNext(prefix + "Context/JarScanner", "setJarScanner", "com.alx.enterprise.web.JarScanner");

        digester.addObjectCreate(prefix + "Context/JarScanner/JarScanFilter",
                "com.alx.enterprise.web.util.scan.StandardJarScanFilter", "className");
        digester.addSetProperties(prefix + "Context/JarScanner/JarScanFilter");
        digester.addSetNext(prefix + "Context/JarScanner/JarScanFilter", "setJarScanFilter",
                "com.alx.enterprise.web.JarScanFilter");

        digester.addObjectCreate(prefix + "Context/CookieProcessor",
                "com.alx.enterprise.web.util.http.Rfc6265CookieProcessor", "className");
        digester.addSetProperties(prefix + "Context/CookieProcessor");
        digester.addSetNext(prefix + "Context/CookieProcessor", "setCookieProcessor",
                "com.alx.enterprise.web.util.http.CookieProcessor");
    }
}


/**
 * A Rule that calls <code>setDeployedFromServerXml(true)</code> for the top object on the stack when a Context element
 * is created, which only occurs for Context elements defined in server.xml. The flag allows storeconfig to detect a
 * Context that is defined inline in server.xml so it is not moved to a separate configuration file when the
 * configuration is stored.
 */
final class SetDeployedFromServerXmlRule extends Rule {

    SetDeployedFromServerXmlRule() {
        // NO-OP
    }

    @Override
    public void begin(String namespace, String name, Attributes attributes) throws Exception {
        if (digester.peek() instanceof StandardContext) {
            StandardContext context = (StandardContext) digester.peek();
            context.setDeployedFromServerXml(true);
            if (digester.getLogger().isTraceEnabled()) {
                digester.getLogger().trace("Calling StandardContext.setDeployedFromServerXml(true)");
            }

            StringBuilder code = digester.getGeneratedCode();
            if (code != null) {
                code.append(System.lineSeparator());
                code.append(digester.toVariableName(context)).append(".setDeployedFromServerXml(true);");
                code.append(System.lineSeparator());
            }
        }
    }

}
