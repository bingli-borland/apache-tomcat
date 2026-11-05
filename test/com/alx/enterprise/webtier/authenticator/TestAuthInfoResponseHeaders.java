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
package com.alx.enterprise.webtier.authenticator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;

import com.alx.enterprise.webtier.Context;
import com.alx.enterprise.webtier.startup.TesterMapRealm;
import com.alx.enterprise.webtier.startup.TesterServlet;
import com.alx.enterprise.webtier.startup.Alx;
import com.alx.enterprise.webtier.startup.AlxBaseTest;
import com.alx.enterprise.webtier.valves.RemoteIpValve;
import com.alx.enterprise.web.util.buf.ByteChunk;
import com.alx.enterprise.web.util.descriptor.web.LoginConfig;
import com.alx.enterprise.web.util.descriptor.web.SecurityCollection;
import com.alx.enterprise.web.util.descriptor.web.SecurityConstraint;

public class TestAuthInfoResponseHeaders extends AlxBaseTest {

    private static String USER = "user";
    private static String PWD = "pwd";
    private static String ROLE = "role";
    private static String URI = "/protected";
    private static String CONTEXT_PATH = "/foo";
    private static String CLIENT_AUTH_HEADER = "authorization";

    /*
     * Encapsulate the logic to generate an HTTP header for BASIC Authentication. Note: only used internally, so no need
     * to validate arguments.
     */
    private static final class BasicCredentials {

        private final String method;
        private final String username;
        private final String password;
        private final String credentials;

        private BasicCredentials(String aMethod, String aUsername, String aPassword) {
            method = aMethod;
            username = aUsername;
            password = aPassword;
            String userCredentials = username + ":" + password;
            byte[] credentialsBytes = userCredentials.getBytes(StandardCharsets.ISO_8859_1);
            String base64auth = Base64.getEncoder().encodeToString(credentialsBytes);
            credentials = method + " " + base64auth;
        }

        private String getCredentials() {
            return credentials;
        }
    }

    @Test
    public void testNoHeaders() throws Exception {
        doTest(USER, PWD, CONTEXT_PATH + URI, false);
    }

    @Test
    public void testWithHeaders() throws Exception {
        doTest(USER, PWD, CONTEXT_PATH + URI, true);
    }

    public void doTest(String user, String pwd, String uri, boolean expectResponseAuthHeaders) throws Exception {

        if (expectResponseAuthHeaders) {
            BasicAuthenticator auth =
                    (BasicAuthenticator) getTomcatInstance().getHost().findChild(CONTEXT_PATH).getPipeline().getFirst();
            auth.setSendAuthInfoResponseHeaders(true);
        }
        getTomcatInstance().start();

        Map<String,List<String>> reqHeaders = new HashMap<>();

        List<String> auth = new ArrayList<>();
        auth.add(new BasicCredentials("Basic", user, pwd).getCredentials());
        reqHeaders.put(CLIENT_AUTH_HEADER, auth);

        List<String> forwardedFor = new ArrayList<>();
        forwardedFor.add("192.168.0.10");
        List<String> forwardedHost = new ArrayList<>();
        forwardedHost.add("localhost");
        reqHeaders.put("X-Forwarded-For", forwardedFor);
        reqHeaders.put("X-Forwarded-Host", forwardedHost);

        Map<String,List<String>> respHeaders = new HashMap<>();

        ByteChunk bc = new ByteChunk();
        int rc = getUrl("http://localhost:" + getPort() + uri, bc, reqHeaders, respHeaders);
        Assert.assertEquals(200, rc);
        Assert.assertEquals("OK", bc.toString());

        if (expectResponseAuthHeaders) {
            List<String> remoteUsers = respHeaders.get("remote-user");
            Assert.assertNotNull(remoteUsers);
            Assert.assertEquals(USER, remoteUsers.get(0));
            List<String> authTypes = respHeaders.get("auth-type");
            Assert.assertNotNull(authTypes);
            Assert.assertEquals(HttpServletRequest.BASIC_AUTH, authTypes.get(0));
        } else {
            Assert.assertFalse(respHeaders.containsKey("remote-user"));
            Assert.assertFalse(respHeaders.containsKey("auth-type"));
        }

        bc.recycle();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Configure a context with digest auth and a single protected resource
        Alx tomcat = getTomcatInstance();
        tomcat.getHost().getPipeline().addValve(new RemoteIpValve());

        // No file system docBase required
        Context ctxt = tomcat.addContext(CONTEXT_PATH, null);

        // Add protected servlet
        Alx.addServlet(ctxt, "TesterServlet", new TesterServlet());
        ctxt.addServletMapping(URI, "TesterServlet");
        SecurityCollection collection = new SecurityCollection();
        collection.addPattern(URI);
        SecurityConstraint sc = new SecurityConstraint();
        sc.addAuthRole(ROLE);
        sc.addCollection(collection);
        ctxt.addConstraint(sc);

        // Configure the Realm
        TesterMapRealm realm = new TesterMapRealm();
        realm.addUser(USER, PWD);
        realm.addUserRole(USER, ROLE);
        ctxt.setRealm(realm);

        // Configure the authenticator
        LoginConfig lc = new LoginConfig();
        lc.setAuthMethod(HttpServletRequest.BASIC_AUTH);
        ctxt.setLoginConfig(lc);
        ctxt.getPipeline().addValve(new BasicAuthenticator());
    }
}
