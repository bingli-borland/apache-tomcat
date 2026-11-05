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
package com.alx.enterprise.webtier.tribes.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.alx.enterprise.webtier.tribes.group.TestGroupChannelMemberArrival;
import com.alx.enterprise.webtier.tribes.group.TestGroupChannelOptionFlag;
import com.alx.enterprise.webtier.tribes.group.TestGroupChannelSenderConnections;
import com.alx.enterprise.webtier.tribes.group.TestGroupChannelStartStop;
import com.alx.enterprise.webtier.tribes.group.interceptors.TestDomainFilterInterceptor;
import com.alx.enterprise.webtier.tribes.group.interceptors.TestNonBlockingCoordinator;
import com.alx.enterprise.webtier.tribes.group.interceptors.TestOrderInterceptor;
import com.alx.enterprise.webtier.tribes.group.interceptors.TestTcpFailureDetector;
import com.alx.enterprise.webtier.tribes.io.TestXByteBuffer;
import com.alx.enterprise.webtier.tribes.membership.TestMemberImplSerialization;
import com.alx.enterprise.webtier.tribes.test.channel.TestDataIntegrity;
import com.alx.enterprise.webtier.tribes.test.channel.TestMulticastPackages;
import com.alx.enterprise.webtier.tribes.test.channel.TestRemoteProcessException;
import com.alx.enterprise.webtier.tribes.test.channel.TestUdpPackages;

@RunWith(Suite.class)
@SuiteClasses({
        // o.a.catalina.tribes.test.channel
        TestGroupChannelStartStop.class, TestGroupChannelOptionFlag.class,
        TestDataIntegrity.class, TestMulticastPackages.class,
        TestRemoteProcessException.class, TestUdpPackages.class,
        // o.a.catalina.tribes.test.interceptors
        TestNonBlockingCoordinator.class, TestOrderInterceptor.class,
        // o.a.catalina.tribes.test.io
        TestGroupChannelSenderConnections.class, TestXByteBuffer.class,
        // o.a.catalina.tribes.test.membership
        TestMemberImplSerialization.class, TestDomainFilterInterceptor.class,
        TestGroupChannelMemberArrival.class, TestTcpFailureDetector.class })
public class TribesTestSuite {

}
