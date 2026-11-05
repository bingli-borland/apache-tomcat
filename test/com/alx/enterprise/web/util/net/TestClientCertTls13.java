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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import com.alx.enterprise.webtier.Lifecycle;
import com.alx.enterprise.webtier.LifecycleEvent;
import com.alx.enterprise.webtier.connector.Connector;
import com.alx.enterprise.webtier.startup.Alx;
import com.alx.enterprise.webtier.startup.AlxBaseTest;
import com.alx.enterprise.web.jni.AprStatus;
import com.alx.enterprise.web.util.buf.ByteChunk;
import com.alx.enterprise.web.util.net.openssl.OpenSSLStatus;

/**
 * The keys and certificates used in this file are all available in svn and were
 * generated using a test CA the files for which are in the Alx PMC private
 * repository since not all of them are AL2 licensed.
 *
 * The JSSE implementation of TLSv1.3 only supports authentication during the
 * initial handshake. This test requires TLSv1.3 on client and server so it is
 * skipped unless running on a Java version that supports TLSv1.3.
 */
@RunWith(Parameterized.class)
public class TestClientCertTls13 extends AlxBaseTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        List<Object[]> parameterSets = new ArrayList<>();
        parameterSets.add(new Object[] {
                "JSSE", Boolean.FALSE, "com.alx.enterprise.web.util.net.jsse.JSSEImplementation", Boolean.FALSE});
        parameterSets.add(new Object[] {
                "OpenSSL", Boolean.TRUE, "com.alx.enterprise.web.util.net.openssl.OpenSSLImplementation", Boolean.TRUE});
        parameterSets.add(new Object[] {
                "OpenSSL-FFM", Boolean.TRUE, "com.alx.enterprise.web.util.net.openssl.panama.OpenSSLImplementation", Boolean.FALSE});

        return parameterSets;
    }

    @Parameter(0)
    public String connectorName;

    @Parameter(1)
    public boolean useOpenSSL;

    @Parameter(2)
    public String sslImplementationName;

    @Parameter(3)
    public boolean initSslImplementation;


    @Test
    public void testClientCertGet() throws Exception {
        Alx tomcat = getTomcatInstance();
        tomcat.start();

        Assume.assumeFalse("LibreSSL does not allow PHA",
                TesterSupport.isOpenSSLVariant(sslImplementationName, OpenSSLStatus.Name.LIBRESSL));

        ByteChunk res = getUrl("https://localhost:" + getPort() + "/protected");
        Assert.assertEquals("OK-" + TesterSupport.ROLE, res.toString());
    }

    @Test
    public void testClientCertPost() throws Exception {
        Alx tomcat = getTomcatInstance();
        tomcat.start();

        Assume.assumeFalse("LibreSSL does not allow PHA",
                TesterSupport.isOpenSSLVariant(sslImplementationName, OpenSSLStatus.Name.LIBRESSL));

        int size = 32 * 1024;

        byte[] body = new byte[size];
        Arrays.fill(body, TesterSupport.DATA);

        // Protected resource
        ByteChunk res = new ByteChunk();
        int rc = postUrl(body, "https://localhost:" + getPort() + "/protected", res, null);

        Assert.assertEquals(200, rc);
        Assert.assertEquals("OK-" + size, res.toString());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        Alx tomcat = getTomcatInstance();

        TesterSupport.configureClientCertContext(tomcat);

        TesterSupport.configureClientSsl();

        Connector connector = tomcat.getConnector();
        TesterSupport.configureSSLImplementation(tomcat, sslImplementationName, useOpenSSL);

        if (useOpenSSL) {
            // getOpenSSLVersion() requires that the listener has been initialised
            if (initSslImplementation) {
                tomcat.getServer().findLifecycleListeners()[0].lifecycleEvent(
                        new LifecycleEvent(tomcat.getServer(), Lifecycle.BEFORE_INIT_EVENT, null));
            }
            Assume.assumeTrue(AprStatus.getOpenSSLVersion() >= 0x1010100f || OpenSSLStatus.getVersion() >= 0x1010100f);
        }

        // Tests default to TLSv1.2 when client cert auth is used
        // Need to override some of the previous settings
        SSLHostConfig[] sslHostConfigs = connector.findSslHostConfigs();
        Assert.assertNotNull(sslHostConfigs);
        Assert.assertEquals(1, sslHostConfigs.length);

        SSLHostConfig sslHostConfig = sslHostConfigs[0];

        // TLS 1.3 support
        sslHostConfig.setProtocols(Constants.SSL_PROTO_TLSv1_3);
        // And add force authentication to occur on the initial handshake
        sslHostConfig.setCertificateVerification("required");
    }
}
