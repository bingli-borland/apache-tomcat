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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Ignore;

import com.alx.enterprise.webtier.Context;
import com.alx.enterprise.webtier.connector.Connector;
import com.alx.enterprise.logging.internal.LogFactory;
import com.alx.enterprise.web.util.modeler.Registry;
import com.alx.enterprise.web.util.scan.StandardJarScanFilter;
import com.alx.enterprise.web.util.scan.StandardJarScanner;

@Ignore
public class EmbeddedAlx {

    private static void resetLogging() {
        final String loggingConfig = "handlers = java.util.logging.ConsoleHandler\n" +
            ".handlers = java.util.logging.ConsoleHandler\n" +
            "java.util.logging.ConsoleHandler.level = FINE\n" +
            "java.util.logging.ConsoleHandler.formatter = com.alx.enterprise.logging.OneLineFormatter\n" +
            "java.util.logging.ConsoleHandler.encoding = UTF-8\n";
        try {
            InputStream is = new ByteArrayInputStream(loggingConfig.getBytes(StandardCharsets.UTF_8));
            LogManager.getLogManager().readConfiguration(is);
            LogFactory.getLog(EmbeddedAlx.class).info("Logger configured to System.out");
        } catch (SecurityException | IOException e) {
            // Ignore, the VM default will be used
        }
    }

    public static void main(String... args) throws Exception {
        Registry.disableRegistry();
        Alx tomcat = new Alx();
        resetLogging();
        tomcat.setPort(8080);
        Connector connector = tomcat.getConnector();
        connector.setProperty("bindOnInit", "false");
        // No file system docBase required
        Context ctx = tomcat.addContext("", null);
        skipTldsForResourceJars(ctx);
        CounterServlet counterServlet = new CounterServlet();
        Alx.addServlet(ctx, "counterServlet", counterServlet);
        ctx.addServletMapping("/", "counterServlet");
        //ctx.addApplicationListener(new WsContextListener());

        tomcat.start();
        Thread.sleep(60*1000);
    }

    public static void skipTldsForResourceJars(Context context) {
        StandardJarScanner scanner = (StandardJarScanner) context.getJarScanner();
        StandardJarScanFilter filter = (StandardJarScanFilter) scanner.getJarScanFilter();
        filter.setTldSkip(filter.getTldSkip() + ",resources*.jar");
    }

    private static class CounterServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        private AtomicInteger callCount = new AtomicInteger(0);

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
            req.getSession(true);
            resp.setContentType("text/plain");
            resp.getWriter().print("OK: " + req.getRequestURL() + "[" + callCount.incrementAndGet()+ "]");
        }
    }
}
