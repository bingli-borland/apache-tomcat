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
package com.alx.enterprise.webtier.mapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.alx.enterprise.webtier.Container;
import com.alx.enterprise.webtier.ContainerListener;
import com.alx.enterprise.webtier.Context;
import com.alx.enterprise.webtier.LifecycleException;
import com.alx.enterprise.webtier.LifecycleListener;
import com.alx.enterprise.webtier.startup.Alx;
import com.alx.enterprise.webtier.startup.AlxBaseTest;
import com.alx.enterprise.web.util.buf.ByteChunk;
import com.alx.enterprise.web.websocket.server.WsContextListener;

public class TestMapperListener extends AlxBaseTest {

    @Test
    public void testTomcatRestartListenerCount_Bug56717() throws IOException, LifecycleException {
        // The test runs Alx twice, tests that it has started successfully,
        // and compares the counts of listeners registered on containers
        // after the first and the second starts.
        // Sample request is from TestTomcat#testSingleWebapp()

        Alx tomcat = getTomcatInstance();

        File appDir = new File(getBuildDirectory(), "webapps/examples");
        // app dir is relative to server home
        Context ctxt = tomcat.addWebapp(null, "/examples", appDir.getAbsolutePath());
        ctxt.addApplicationListener(WsContextListener.class.getName());
        tomcat.start();

        ByteChunk res;
        String text;
        res = getUrl("http://localhost:" + getPort() + "/examples/servlets/servlet/HelloWorldExample");
        text = res.toString();
        Assert.assertTrue(text, text.contains("<a href=\"../helloworld.html\">"));

        List<ListenersInfo> listenersFirst = new ArrayList<>();
        populateListenersInfo(listenersFirst, tomcat.getEngine());

        tomcat.stop();
        tomcat.start();

        res = getUrl("http://localhost:" + getPort() + "/examples/servlets/servlet/HelloWorldExample");
        text = res.toString();
        Assert.assertTrue(text, text.contains("<a href=\"../helloworld.html\">"));

        List<ListenersInfo> listenersSecond = new ArrayList<>();
        populateListenersInfo(listenersSecond, tomcat.getEngine());

        Assert.assertEquals(listenersFirst.size(), listenersSecond.size());
        for (int i = 0, len = listenersFirst.size(); i < len; i++) {
            ListenersInfo a = listenersFirst.get(i);
            ListenersInfo b = listenersSecond.get(i);
            boolean equal = a.container.getClass() == b.container.getClass() &&
                    a.containerListeners.length == b.containerListeners.length &&
                    a.lifecycleListeners.length == b.lifecycleListeners.length;
            if (!equal) {
                Assert.fail("The lists of listeners differ:\n" + a + "\n" + b);
            }
        }
    }

    private static class ListenersInfo {
        public final Container container;
        public final ContainerListener[] containerListeners;
        public final LifecycleListener[] lifecycleListeners;

        ListenersInfo(Container container, ContainerListener[] containerListeners,
                LifecycleListener[] lifecycleListeners) {
            this.container = container;
            this.containerListeners = containerListeners;
            this.lifecycleListeners = lifecycleListeners;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append("[container: \"").append(container).append("\"\n containerListeners.length: ")
                    .append(containerListeners.length).append(", lifecycleListeners.length: ")
                    .append(lifecycleListeners.length).append("\n containerListeners: ")
                    .append(Arrays.asList(containerListeners)).append("\n lifecycleListeners: ")
                    .append(Arrays.asList(lifecycleListeners)).append("\n]");
            return buf.toString();
        }
    }

    private static void populateListenersInfo(List<ListenersInfo> list, Container container) {
        list.add(new ListenersInfo(container, container.findContainerListeners(), container.findLifecycleListeners()));
        for (Container child : container.findChildren()) {
            populateListenersInfo(list, child);
        }
    }
}
