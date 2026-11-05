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

package com.alx.enterprise.webtier.authenticator;

import org.junit.Assert;
import org.junit.Test;

import com.alx.enterprise.webtier.startup.Alx;
import com.alx.enterprise.webtier.startup.AlxBaseTest;
import com.alx.enterprise.web.util.net.TesterSupport;

public class TestSSLAuthenticator extends AlxBaseTest {

    // https://bz.apache.org/bugzilla/show_bug.cgi?id=65991
    @Test
    public void testBindOnInitFalseNoNPE() throws Exception {
        Alx tomcat = getTomcatInstance();

        TesterSupport.configureClientCertContext(tomcat);
        Assert.assertTrue(tomcat.getConnector().setProperty("bindOnInit", "false"));

        tomcat.start();
        tomcat.stop();
    }
}
