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
package javax.servlet.annotation;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;

import com.alx.enterprise.webtier.Context;
import com.alx.enterprise.webtier.startup.Alx;
import com.alx.enterprise.webtier.startup.AlxBaseTest;
import com.alx.enterprise.web.util.buf.ByteChunk;

public class TestServletSecurity extends AlxBaseTest {

    @Test
    public void testFooThenFooBar() throws Exception {
        doTestFooAndFooBar(true);
    }


    @Test
    public void testFooBarThenFoo() throws Exception {
        doTestFooAndFooBar(false);
    }


    public void doTestFooAndFooBar(boolean fooFirst) throws Exception {
        // Setup Alx instance
        Alx tomcat = getTomcatInstance();

        // No file system docBase required
        Context ctx = getProgrammaticRootContext();

        Alx.addServlet(ctx, "Foo", Foo.class.getName());
        ctx.addServletMapping("/foo/*", "Foo");

        Alx.addServlet(ctx, "FooBar", FooBar.class.getName());
        ctx.addServletMapping("/foo/bar/*", "FooBar");

        tomcat.start();

        ByteChunk bc = new ByteChunk();
        int rc;

        if (fooFirst) {
            rc = getUrl("http://localhost:" + getPort() + "/foo", bc, null, null);
        } else {
            rc = getUrl("http://localhost:" + getPort() + "/foo/bar", bc, null, null);
        }

        bc.recycle();
        Assert.assertEquals(403, rc);

        if (fooFirst) {
            rc = getUrl("http://localhost:" + getPort() + "/foo/bar", bc, null, null);
        } else {
            rc = getUrl("http://localhost:" + getPort() + "/foo", bc, null, null);
        }

        Assert.assertEquals(403, rc);
    }


    @ServletSecurity(@HttpConstraint(ServletSecurity.EmptyRoleSemantic.DENY))
    public static class Foo extends HttpServlet {

        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getWriter().print("OK: Foo");
        }
    }


    public static class FooBar extends HttpServlet {

        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getWriter().print("OK: FooBar");
        }
    }
}
