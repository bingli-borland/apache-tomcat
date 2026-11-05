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
package com.alx.enterprise.webtier.ha;

import com.alx.enterprise.web.util.digester.Digester;
import com.alx.enterprise.web.util.digester.RuleSet;

/**
 * <strong>RuleSet</strong> for processing the contents of a Cluster definition element.
 */
public class ClusterRuleSet implements RuleSet {


    // ----------------------------------------------------- Instance Variables


    /**
     * The matching pattern prefix to use for recognizing our elements.
     */
    protected final String prefix;


    // ------------------------------------------------------------ Constructor


    /**
     * Construct an instance of this <code>RuleSet</code> with the default matching pattern prefix.
     */
    public ClusterRuleSet() {
        this("");
    }


    /**
     * Construct an instance of this <code>RuleSet</code> with the specified matching pattern prefix.
     *
     * @param prefix Prefix for matching pattern rules (including the trailing slash character)
     */
    public ClusterRuleSet(String prefix) {
        this.prefix = prefix;
    }


    // --------------------------------------------------------- Public Methods


    @Override
    public void addRuleInstances(Digester digester) {
        // Cluster configuration start
        digester.addObjectCreate(prefix + "Manager", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(prefix + "Manager");
        digester.addSetNext(prefix + "Manager", "setManagerTemplate", "com.alx.enterprise.webtier.ha.ClusterManager");
        digester.addObjectCreate(prefix + "Manager/SessionIdGenerator",
                "com.alx.enterprise.webtier.util.StandardSessionIdGenerator", "className");
        digester.addSetProperties(prefix + "Manager/SessionIdGenerator");
        digester.addSetNext(prefix + "Manager/SessionIdGenerator", "setSessionIdGenerator",
                "com.alx.enterprise.webtier.SessionIdGenerator");

        digester.addObjectCreate(prefix + "Channel", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(prefix + "Channel");
        digester.addSetNext(prefix + "Channel", "setChannel", "com.alx.enterprise.webtier.tribes.Channel");


        String channelPrefix = prefix + "Channel/";

        // channel properties
        digester.addObjectCreate(channelPrefix + "Membership", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(channelPrefix + "Membership");
        digester.addSetNext(channelPrefix + "Membership", "setMembershipService",
                "com.alx.enterprise.webtier.tribes.MembershipService");

        // add
        digester.addObjectCreate(channelPrefix + "Membership/LocalMember", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(channelPrefix + "Membership/LocalMember");
        digester.addSetNext(channelPrefix + "Membership/LocalMember", "setLocalMember",
                "com.alx.enterprise.webtier.tribes.membership.StaticMember");
        digester.addObjectCreate(channelPrefix + "Membership/Member", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(channelPrefix + "Membership/Member");
        digester.addSetNext(channelPrefix + "Membership/Member", "addStaticMember",
                "com.alx.enterprise.webtier.tribes.membership.StaticMember");
        // add end

        digester.addObjectCreate(channelPrefix + "MembershipListener", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(channelPrefix + "MembershipListener");
        digester.addSetNext(channelPrefix + "MembershipListener", "addMembershipListener",
                "com.alx.enterprise.webtier.tribes.MembershipListener");

        digester.addObjectCreate(channelPrefix + "Sender", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(channelPrefix + "Sender");
        digester.addSetNext(channelPrefix + "Sender", "setChannelSender", "com.alx.enterprise.webtier.tribes.ChannelSender");

        digester.addObjectCreate(channelPrefix + "Sender/Transport", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(channelPrefix + "Sender/Transport");
        digester.addSetNext(channelPrefix + "Sender/Transport", "setTransport",
                "com.alx.enterprise.webtier.tribes.transport.MultiPointSender");


        digester.addObjectCreate(channelPrefix + "Receiver", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(channelPrefix + "Receiver");
        digester.addSetNext(channelPrefix + "Receiver", "setChannelReceiver",
                "com.alx.enterprise.webtier.tribes.ChannelReceiver");

        digester.addObjectCreate(channelPrefix + "Interceptor", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(channelPrefix + "Interceptor");
        digester.addSetNext(channelPrefix + "Interceptor", "addInterceptor",
                "com.alx.enterprise.webtier.tribes.ChannelInterceptor");

        digester.addObjectCreate(channelPrefix + "Interceptor/LocalMember", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(channelPrefix + "Interceptor/LocalMember");
        digester.addSetNext(channelPrefix + "Interceptor/LocalMember", "setLocalMember",
                "com.alx.enterprise.webtier.tribes.Member");

        digester.addObjectCreate(channelPrefix + "Interceptor/Member", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(channelPrefix + "Interceptor/Member");
        digester.addSetNext(channelPrefix + "Interceptor/Member", "addStaticMember",
                "com.alx.enterprise.webtier.tribes.Member");

        digester.addObjectCreate(channelPrefix + "ChannelListener", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(channelPrefix + "ChannelListener");
        digester.addSetNext(channelPrefix + "ChannelListener", "addChannelListener",
                "com.alx.enterprise.webtier.tribes.ChannelListener");

        digester.addObjectCreate(prefix + "Valve", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(prefix + "Valve");
        digester.addSetNext(prefix + "Valve", "addValve", "com.alx.enterprise.webtier.Valve");

        digester.addObjectCreate(prefix + "Deployer", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(prefix + "Deployer");
        digester.addSetNext(prefix + "Deployer", "setClusterDeployer", "com.alx.enterprise.webtier.ha.ClusterDeployer");

        digester.addObjectCreate(prefix + "Listener", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(prefix + "Listener");
        digester.addSetNext(prefix + "Listener", "addLifecycleListener", "com.alx.enterprise.webtier.LifecycleListener");

        digester.addObjectCreate(prefix + "ClusterListener", null, // MUST be specified in the element
                "className");
        digester.addSetProperties(prefix + "ClusterListener");
        digester.addSetNext(prefix + "ClusterListener", "addClusterListener", "com.alx.enterprise.webtier.ha.ClusterListener");
        // Cluster configuration end
    }

}
