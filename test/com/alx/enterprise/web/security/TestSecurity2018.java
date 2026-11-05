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

package com.alx.enterprise.web.security;

import java.net.URI;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

import org.junit.Assert;
import org.junit.Test;

import com.alx.enterprise.webtier.Context;
import com.alx.enterprise.webtier.servlets.DefaultServlet;
import com.alx.enterprise.webtier.startup.Alx;
import com.alx.enterprise.web.util.net.TesterCredentialGenerator;
import com.alx.enterprise.web.util.net.TesterSupport;
import com.alx.enterprise.web.websocket.TesterEchoServer;
import com.alx.enterprise.web.websocket.TesterMessageCountClient;
import com.alx.enterprise.web.websocket.WebSocketBaseTest;

public class TestSecurity2018 extends WebSocketBaseTest {

    // https://www.cve.org/CVERecord?id=CVE-2018-8034
    @Test(expected = DeploymentException.class)
    public void testCVE_2018_8034() throws Exception {
        TesterCredentialGenerator.TesterCredential credential = TesterCredentialGenerator.generateCredential(
                "localhost", "tomcat",
                new String[]{"localhost"}, null);

        Alx tomcat = getTomcatInstance();

        TesterSupport.initSsl(tomcat, credential.getKeystore().getAbsolutePath(),
            credential.getCertificate().getAbsolutePath(), credential.getKey().getAbsolutePath(), false);

        Context ctx = getProgrammaticRootContext();
        ctx.addApplicationListener(TesterEchoServer.Config.class.getName());
        Alx.addServlet(ctx, "default", new DefaultServlet());
        ctx.addServletMapping("/", "default");

        tomcat.start();

        WebSocketContainer wsContainer = ContainerProvider.getWebSocketContainer();
        ClientEndpointConfig clientEndpointConfig = ClientEndpointConfig.Builder.create().build();
        clientEndpointConfig.getUserProperties().put(com.alx.enterprise.web.websocket.Constants.SSL_TRUSTSTORE_PROPERTY,
            credential.getCertificate().getAbsolutePath());

        wsContainer.connectToServer(
                TesterMessageCountClient.TesterProgrammaticEndpoint.class,
                clientEndpointConfig,
                new URI("wss://127.0.0.1:" + getPort() +
                    TesterEchoServer.Config.PATH_ASYNC));
        Assert.fail(
                "Hostname verification should have failed for 127.0.0.1 with a certificate issued for localhost only.");
    }
}
