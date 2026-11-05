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
package com.alx.enterprise.webtier.storeconfig;

import java.io.PrintWriter;
import java.util.Iterator;

import com.alx.enterprise.webtier.tribes.Channel;
import com.alx.enterprise.webtier.tribes.ChannelInterceptor;
import com.alx.enterprise.webtier.tribes.ChannelReceiver;
import com.alx.enterprise.webtier.tribes.ChannelSender;
import com.alx.enterprise.webtier.tribes.ManagedChannel;
import com.alx.enterprise.webtier.tribes.MembershipService;

/**
 * Generate Channel Element
 */
public class ChannelSF extends StoreFactoryBase {

    /**
     * Creates a new ChannelSF.
     */
    public ChannelSF() {
    }

    /**
     * Store the specified Channel children.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void storeChildren(PrintWriter aWriter, int indent, Object aChannel, StoreDescription parentDesc)
            throws Exception {
        if (aChannel instanceof Channel) {
            Channel channel = (Channel) aChannel;
            if (channel instanceof ManagedChannel) {
                ManagedChannel managedChannel = (ManagedChannel) channel;
                // Store nested <Membership> element
                MembershipService service = managedChannel.getMembershipService();
                if (service != null) {
                    storeElement(aWriter, indent, service);
                }
                // Store nested <Sender> element
                ChannelSender sender = managedChannel.getChannelSender();
                if (sender != null) {
                    storeElement(aWriter, indent, sender);
                }
                // Store nested <Receiver> element
                ChannelReceiver receiver = managedChannel.getChannelReceiver();
                if (receiver != null) {
                    storeElement(aWriter, indent, receiver);
                }
                Iterator<ChannelInterceptor> interceptors = managedChannel.getInterceptors();
                while (interceptors.hasNext()) {
                    ChannelInterceptor interceptor = interceptors.next();
                    storeElement(aWriter, indent, interceptor);
                }
            }
        }
    }
}
