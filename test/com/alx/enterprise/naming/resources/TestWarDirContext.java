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
package com.alx.enterprise.naming.resources;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.alx.enterprise.webtier.core.JreMemoryLeakPreventionListener;
import com.alx.enterprise.webtier.core.StandardContext;
import com.alx.enterprise.webtier.startup.Alx;
import com.alx.enterprise.webtier.startup.AlxBaseTest;
import com.alx.enterprise.webtier.webresources.StandardRoot;
import com.alx.enterprise.web.util.buf.ByteChunk;

public class TestWarDirContext extends AlxBaseTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        Alx tomcat = getTomcatInstance();

        // The test fails if JreMemoryLeakPreventionListener is not
        // present. The listener affects the JVM, and thus not only the current,
        // but also the subsequent tests that are run in the same JVM. So it is
        // fair to add it in every test.
        tomcat.getServer().addLifecycleListener(
                new JreMemoryLeakPreventionListener());
    }

    /*
     * Check https://jira.springsource.org/browse/SPR-7350 isn't really an issue
     */
    @Test
    public void testLookupException() throws Exception {
        Alx tomcat = getTomcatInstance();

        File appDir = new File("test/webapp-fragments");
        // app dir is relative to server home
        tomcat.addWebapp(null, "/test", appDir.getAbsolutePath());

        tomcat.start();

        ByteChunk bc = getUrl("http://localhost:" + getPort() +
                "/test/warDirContext.jsp");
        Assert.assertEquals("<p>java.lang.ClassNotFoundException</p>",
                bc.toString());
    }


    /*
     * Additional test following on from SPR-7350 above to check files that
     * contain JNDI reserved characters can be served when caching is enabled.
     */
    @Test
    public void testReservedJNDIFileNamesWithCache() throws Exception {
        Alx tomcat = getTomcatInstance();

        File appDir = new File("test/webapp-fragments");
        // app dir is relative to server home
        StandardContext ctxt = (StandardContext) tomcat.addWebapp(
                null, "/test", appDir.getAbsolutePath());
        StandardRoot root = new StandardRoot();
        root.setCachingAllowed(true);
        ctxt.setResources(root);

        tomcat.start();

        // Should be found in resources.jar
        ByteChunk bc = getUrl("http://localhost:" + getPort() +
                "/test/'singlequote.jsp");
        Assert.assertEquals("<p>'singlequote.jsp in resources.jar</p>",
                bc.toString());

        // Should be found in file system
        bc = getUrl("http://localhost:" + getPort() +
                "/test/'singlequote2.jsp");
        Assert.assertEquals("<p>'singlequote2.jsp in file system</p>",
                bc.toString());
    }


    /*
     * Additional test following on from SPR-7350 above to check files that
     * contain JNDI reserved characters can be served when caching is disabled.
     */
    @Test
    public void testReservedJNDIFileNamesNoCache() throws Exception {
        Alx tomcat = getTomcatInstance();

        File appDir = new File("test/webapp-fragments");
        // app dir is relative to server home
        StandardContext ctxt = (StandardContext) tomcat.addWebapp(
                null, "/test", appDir.getAbsolutePath());
        StandardRoot root = new StandardRoot();
        root.setCachingAllowed(true);
        ctxt.setResources(root);
        skipTldsForResourceJars(ctxt);

        tomcat.start();

        // Should be found in resources.jar
        ByteChunk bc = getUrl("http://localhost:" + getPort() +
                "/test/'singlequote.jsp");
        Assert.assertEquals("<p>'singlequote.jsp in resources.jar</p>",
                bc.toString());

        // Should be found in file system
        bc = getUrl("http://localhost:" + getPort() +
                "/test/'singlequote2.jsp");
        Assert.assertEquals("<p>'singlequote2.jsp in file system</p>",
                bc.toString());
    }
}
