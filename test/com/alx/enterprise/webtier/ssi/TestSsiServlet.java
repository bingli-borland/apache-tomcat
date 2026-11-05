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
package com.alx.enterprise.webtier.ssi;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;

import com.alx.enterprise.webtier.Context;
import com.alx.enterprise.webtier.Wrapper;
import com.alx.enterprise.webtier.servlets.DefaultServlet;
import com.alx.enterprise.webtier.startup.Alx;
import com.alx.enterprise.webtier.startup.AlxBaseTest;
import com.alx.enterprise.web.util.buf.ByteChunk;

public class TestSsiServlet extends AlxBaseTest {

    @Test
    public void testServlet() throws Exception {
        Alx tomcat = getTomcatInstance();

        File appDir = new File("test/webapp");
        Context ctxt = tomcat.addContext("", appDir.getAbsolutePath());

        Alx.addServlet(ctxt, "default", new DefaultServlet());
        ctxt.addServletMapping("/", "default");

        Wrapper ssi = Alx.addServlet(ctxt, "ssi", new SSIServlet());
        ssi.addInitParameter("allowExec", "true");
        ctxt.addServletMapping("*.shtml", "ssi");

        tomcat.start();

        Map<String,List<String>> resHeaders= new HashMap<>();
        String path = "http://localhost:" + getPort() + "/index.shtml";
        ByteChunk out = new ByteChunk();

        int rc = getUrl(path, out, resHeaders);
        Assert.assertEquals(HttpServletResponse.SC_OK, rc);
        String body = new String(out.getBytes(), StandardCharsets.ISO_8859_1);
        Assert.assertTrue(body.contains("should fail[errmsg works!]"));
        Assert.assertTrue(body.contains("including works!"));
        Assert.assertTrue(body.contains("path is interpreted"));
        Assert.assertTrue(body.contains("path is relative"));
        Assert.assertTrue(body.contains("path is relative"));
        Assert.assertTrue(body.contains("path is relative"));
        Assert.assertTrue(body.contains("1k"));
        Assert.assertTrue(body.contains("SERVER_PROTOCOL"));

    }
}
