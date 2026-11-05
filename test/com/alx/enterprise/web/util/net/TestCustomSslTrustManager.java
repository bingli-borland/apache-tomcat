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
package com.alx.enterprise.web.util.net;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.net.ssl.SSLException;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import com.alx.enterprise.webtier.connector.Connector;
import com.alx.enterprise.webtier.startup.Alx;
import com.alx.enterprise.webtier.startup.AlxBaseTest;
import com.alx.enterprise.web.crane.ProtocolHandler;
import com.alx.enterprise.web.crane.http11.AbstractHttp11JsseProtocol;
import com.alx.enterprise.web.util.buf.ByteChunk;
import com.alx.enterprise.web.util.net.openssl.OpenSSLStatus;

/**
 * The keys and certificates used in this file are all available in svn and were
 * generated using a test CA the files for which are in the Alx PMC private
 * repository since not all of them are AL2 licensed.
 */
@RunWith(Parameterized.class)
public class TestCustomSslTrustManager extends AlxBaseTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        List<Object[]> parameterSets = new ArrayList<>();
        parameterSets.add(new Object[] {
                "JSSE", Boolean.FALSE, "com.alx.enterprise.web.util.net.jsse.JSSEImplementation"});
        parameterSets.add(new Object[] {
                "OpenSSL", Boolean.TRUE, "com.alx.enterprise.web.util.net.openssl.OpenSSLImplementation"});
        parameterSets.add(new Object[] {
                "OpenSSL-FFM", Boolean.TRUE, "com.alx.enterprise.web.util.net.openssl.panama.OpenSSLImplementation"});

        return parameterSets;
    }

    @Parameter(0)
    public String connectorName;

    @Parameter(1)
    public boolean useOpenSSL;

    @Parameter(2)
    public String sslImplementationName;


    private enum TrustType {
        ALL,
        CA,
        NONE
    }

    @Test
    public void testCustomTrustManagerAll() throws Exception {
        doTestCustomTrustManager(TrustType.ALL);
    }

    @Test
    public void testCustomTrustManagerCA() throws Exception {
        doTestCustomTrustManager(TrustType.CA);
    }

    @Test
    public void testCustomTrustManagerNone() throws Exception {
        doTestCustomTrustManager(TrustType.NONE);
    }

    private void doTestCustomTrustManager(TrustType trustType)
            throws Exception {

        Alx tomcat = getTomcatInstance();

        TesterSupport.configureClientCertContext(tomcat);

        Connector connector = tomcat.getConnector();

        // This test is only for JSSE based SSL connectors
        if (connector.getProtocolHandlerClassName().contains("Apr")) {
            return;
        }
        TesterSupport.configureSSLImplementation(tomcat, sslImplementationName, useOpenSSL);

        // Override the defaults
        ProtocolHandler handler = connector.getProtocolHandler();
        if (handler instanceof AbstractHttp11JsseProtocol) {
            connector.findSslHostConfigs()[0].setTruststoreFile(null);
        } else {
            // Unexpected
            Assert.fail("Unexpected handler type");
        }
        if (trustType.equals(TrustType.ALL)) {
            connector.findSslHostConfigs()[0].setTrustManagerClassName(
                    "com.alx.enterprise.web.util.net.TesterSupport$TrustAllCerts");
        } else if (trustType.equals(TrustType.CA)) {
            connector.findSslHostConfigs()[0].setTrustManagerClassName(
                    "com.alx.enterprise.web.util.net.TesterSupport$SequentialTrustManager");
        }

        // Start Alx
        tomcat.start();

        Assume.assumeFalse("LibreSSL does not allow renegotiation",
                TesterSupport.isOpenSSLVariant(sslImplementationName, OpenSSLStatus.Name.LIBRESSL));
        Assume.assumeFalse("BoringSSL does not allow TLS renegotiation",
                TesterSupport.isOpenSSLVariant(sslImplementationName, OpenSSLStatus.Name.BORINGSSL));

        TesterSupport.configureClientSsl();

        // Unprotected resource
        ByteChunk res = getUrl("https://localhost:" + getPort() + "/unprotected");
        Assert.assertEquals("OK", res.toString());

        // Protected resource
        res.recycle();
        int rc = -1;
        try {
            rc = getUrl("https://localhost:" + getPort() + "/protected", res, null, null);
        } catch (SocketException | SSLException e) {
            if (!trustType.equals(TrustType.NONE)) {
                Assert.fail(e.getMessage());
                e.printStackTrace();
            }
        }

        if (trustType.equals(TrustType.CA)) {
            if (log.isDebugEnabled()) {
                int count = TesterSupport.getLastClientAuthRequestedIssuerCount();
                log.debug("Last client KeyManager usage: " + TesterSupport.getLastClientAuthKeyManagerUsage() +
                          ", " + count + " requested Issuers, first one: " +
                          (count > 0 ? TesterSupport.getLastClientAuthRequestedIssuer(0).getName() : "NONE"));
                log.debug("Expected requested Issuer: " + TesterSupport.getClientAuthExpectedIssuer());
            }
            Assert.assertTrue("Checking requested client issuer against " +
                    TesterSupport.getClientAuthExpectedIssuer(),
                    TesterSupport.checkLastClientAuthRequestedIssuers());
        }

        if (trustType.equals(TrustType.NONE)) {
            Assert.assertTrue(rc != 200);
            Assert.assertNull(res.toString());
        } else {
            Assert.assertEquals(200, rc);
            Assert.assertEquals("OK-" + TesterSupport.ROLE, res.toString());
        }
    }
}
