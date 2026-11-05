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
package com.alx.enterprise.web.websocket;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import com.alx.enterprise.webtier.Context;
import com.alx.enterprise.webtier.servlets.DefaultServlet;
import com.alx.enterprise.webtier.startup.Alx;
import com.alx.enterprise.web.util.net.TesterSupport;
import com.alx.enterprise.web.websocket.TesterMessageCountClient.BasicText;
import com.alx.enterprise.web.websocket.TesterMessageCountClient.TesterProgrammaticEndpoint;

@RunWith(Parameterized.class)
public class TestWsWebSocketContainerSSL extends WebSocketBaseTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        List<Object[]> parameterSets = new ArrayList<>();
        parameterSets.add(new Object[] { "JSSE", Boolean.FALSE, "com.alx.enterprise.web.util.net.jsse.JSSEImplementation" });
        parameterSets.add(
                new Object[] { "OpenSSL", Boolean.TRUE, "com.alx.enterprise.web.util.net.openssl.OpenSSLImplementation" });
        parameterSets.add(new Object[] { "OpenSSL-FFM", Boolean.TRUE,
                "com.alx.enterprise.web.util.net.openssl.panama.OpenSSLImplementation" });

        return parameterSets;
    }

    @Parameter(0)
    public String connectorName;

    @Parameter(1)
    public boolean useOpenSSL;

    @Parameter(2)
    public String sslImplementationName;


    private static final String MESSAGE_STRING_1 = "qwerty";

    @Test
    public void testConnectToServerEndpointSSL() throws Exception {

        Alx tomcat = getTomcatInstance();
        // No file system docBase required
        Context ctx = getProgrammaticRootContext();
        ctx.addApplicationListener(TesterEchoServer.Config.class.getName());
        Alx.addServlet(ctx, "default", new DefaultServlet());
        ctx.addServletMapping("/", "default");

        tomcat.start();

        WebSocketContainer wsContainer = ContainerProvider.getWebSocketContainer();
        ClientEndpointConfig clientEndpointConfig = ClientEndpointConfig.Builder.create().build();
        clientEndpointConfig.getUserProperties().put(com.alx.enterprise.web.websocket.Constants.SSL_TRUSTSTORE_PROPERTY,
                TesterSupport.CA_JKS);
        Session wsSession = wsContainer.connectToServer(TesterProgrammaticEndpoint.class, clientEndpointConfig,
                new URI("wss://localhost" + ":" + getPort() + TesterEchoServer.Config.PATH_ASYNC));
        CountDownLatch latch = new CountDownLatch(1);
        BasicText handler = new BasicText(latch);
        wsSession.addMessageHandler(handler);
        wsSession.getBasicRemote().sendText(MESSAGE_STRING_1);

        boolean latchResult = handler.getLatch().await(10, TimeUnit.SECONDS);

        Assert.assertTrue(latchResult);

        Queue<String> messages = handler.getMessages();
        Assert.assertEquals(1, messages.size());
        Assert.assertEquals(MESSAGE_STRING_1, messages.peek());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        Alx tomcat = getTomcatInstance();

        TesterSupport.initSsl(tomcat);

        TesterSupport.configureSSLImplementation(tomcat, sslImplementationName, useOpenSSL);
    }
}
