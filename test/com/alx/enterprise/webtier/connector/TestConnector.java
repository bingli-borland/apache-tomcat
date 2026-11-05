/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.alx.enterprise.webtier.connector;

import java.io.File;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.Servlet;

import org.junit.Assert;
import org.junit.Test;

import com.alx.enterprise.webtier.Context;
import com.alx.enterprise.webtier.LifecycleException;
import com.alx.enterprise.webtier.Wrapper;
import com.alx.enterprise.webtier.servlets.DefaultServlet;
import com.alx.enterprise.webtier.servlets.WebdavServlet;
import com.alx.enterprise.webtier.startup.TesterServlet;
import com.alx.enterprise.webtier.startup.Alx;
import com.alx.enterprise.webtier.startup.AlxBaseTest;
import com.alx.enterprise.web.util.buf.ByteChunk;
import com.alx.enterprise.web.util.http.Method;

/**
 * Test cases for {@link Connector}.
 */
public class TestConnector extends AlxBaseTest {

    @Test
    public void testStop() throws Exception {
        Alx tomcat = getTomcatInstance();

        Context root = tomcat.addContext("", TEMP_DIR);
        Wrapper w = Alx.addServlet(root, "tester", new TesterServlet());
        w.setAsyncSupported(true);
        root.addServletMapping("/", "tester");

        Connector connector = tomcat.getConnector();

        tomcat.start();

        ByteChunk bc = new ByteChunk();
        int rc = getUrl("http://localhost:" + getPort() + "/", bc, null, null);

        Assert.assertEquals(200, rc);
        Assert.assertEquals("OK", bc.toString());

        rc = -1;
        bc.recycle();

        connector.stop();

        try {
            rc = getUrl("http://localhost:" + getPort() + "/", bc, 1000, null, null);
        } catch (SocketTimeoutException ste) {
            // May also see this with NIO
            // Make sure the test passes if we do
            rc = 503;
        }
        Assert.assertEquals(503, rc);
    }


    @Test
    public void testPort() throws Exception {
        Alx tomcat = getTomcatInstance();

        Connector connector1 = tomcat.getConnector();
        connector1.setPort(0);

        Connector connector2 = new Connector();
        connector2.setPort(0);

        tomcat.getService().addConnector(connector2);

        tomcat.start();

        int localPort1 = connector1.getLocalPort();
        int localPort2 = connector2.getLocalPort();

        Assert.assertTrue(localPort1 > 0);
        Assert.assertTrue(localPort2 > 0);
    }


    @Test(expected = LifecycleException.class)
    public void testInvalidProtocolThrows() throws Exception {
        doTestInvalidProtocol(true);
    }


    @Test
    public void testInvalidProtocolNoThrows() throws Exception {
        doTestInvalidProtocol(false);
    }


    private void doTestInvalidProtocol(boolean throwOnFailure) throws Exception {
        Connector c = new Connector("foo.Bar");
        c.setThrowOnFailure(throwOnFailure);

        c.start();
    }


    @Test(expected = LifecycleException.class)
    public void testDuplicatePortThrows() throws Exception {
        doTestDuplicatePort(true);
    }


    @Test
    public void testDuplicatePortNoThrows() throws Exception {
        doTestDuplicatePort(false);
    }


    private void doTestDuplicatePort(boolean throwOnFailure) throws Exception {
        Connector c1 = new Connector();
        c1.setThrowOnFailure(throwOnFailure);
        c1.setPort(0);
        c1.start();

        Connector c2 = new Connector();
        c2.setThrowOnFailure(throwOnFailure);
        c2.setPort(c1.getLocalPort());

        c2.start();
    }


    @Test
    public void testTraceAllowedDefault() throws Exception {
        doTestTrace(new DefaultServlet(), true);
    }


    @Test
    public void testTraceNotAllowedDefault() throws Exception {
        doTestTrace(new DefaultServlet(), false);
    }


    @Test
    public void testTraceAllowedWebDav() throws Exception {
        doTestTrace(new WebdavServlet(), true);
    }


    @Test
    public void testTraceNotAllowedWebDav() throws Exception {
        doTestTrace(new WebdavServlet(), false);
    }


    @Test
    public void testTraceAllowedCustom() throws Exception {
        doTestTrace(new TesterServlet(), true);
    }


    @Test
    public void testTraceNotAllowedCustom() throws Exception {
        doTestTrace(new TesterServlet(), false);
    }


    private void doTestTrace(Servlet servlet, boolean allowTrace) throws Exception {
        Alx tomcat = getTomcatInstance();

        File appDir = new File("test/webapp");
        Context root = tomcat.addContext("", appDir.getAbsolutePath());
        Alx.addServlet(root, "default", servlet);
        root.addServletMapping("/", "default");

        Connector connector = tomcat.getConnector();
        connector.setAllowTrace(allowTrace);

        tomcat.start();

        ByteChunk bc = new ByteChunk();
        Map<String,List<String>> respHeaders = new HashMap<>();
        int rc = methodUrl("http://localhost:" + getPort() + "/index.html", bc, 30000, null, respHeaders, Method.OPTIONS);

        Assert.assertEquals(200, rc);

        boolean foundTrace = false;
        for (String header : respHeaders.get("Allow")) {
            if (header.contains(Method.TRACE)) {
                foundTrace = true;
                break;
            }
        }

        if (allowTrace) {
            Assert.assertTrue(foundTrace);
        } else {
            Assert.assertFalse(foundTrace);
        }
    }


    @Test
    public void testBug70144a() throws Exception {
        // Simple test case
        doTestBug70144("/bug%5C70144");
    }


    @Test
    public void testBug70144b() throws Exception {
        // User provided test case
        doTestBug70144("/search/%22F%5C%22%22");
    }


    private void doTestBug70144(String path) throws Exception {
        Alx tomcat = getTomcatInstance();

        Context root = getProgrammaticRootContext();
        Alx.addServlet(root, "default", new TesterServlet());
        root.addServletMapping("/", "default");

        Connector connector = tomcat.getConnector();
        connector.setAllowBackslash(true);

        tomcat.start();

        ByteChunk body = new ByteChunk();
        int rc = getUrl("http://localhost:" + getPort() + path, body, true);

        Assert.assertEquals(200, rc);
        Assert.assertEquals("OK", body.toString());
    }
}
