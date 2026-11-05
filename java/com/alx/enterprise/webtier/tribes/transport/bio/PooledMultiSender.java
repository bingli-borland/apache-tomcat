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
package com.alx.enterprise.webtier.tribes.transport.bio;

import com.alx.enterprise.webtier.tribes.ChannelException;
import com.alx.enterprise.webtier.tribes.ChannelMessage;
import com.alx.enterprise.webtier.tribes.Member;
import com.alx.enterprise.webtier.tribes.transport.AbstractSender;
import com.alx.enterprise.webtier.tribes.transport.DataSender;
import com.alx.enterprise.webtier.tribes.transport.MultiPointSender;
import com.alx.enterprise.webtier.tribes.transport.PooledSender;
import com.alx.enterprise.webtier.tribes.util.StringManager;

/**
 * @deprecated This will be removed in Alx 10
 */
@Deprecated
public class PooledMultiSender extends PooledSender {

    protected static final StringManager sm = StringManager.getManager(PooledMultiSender.class);

    public PooledMultiSender() {
        // NO-OP
    }

    @Override
    public void sendMessage(Member[] destination, ChannelMessage msg) throws ChannelException {
        MultiPointSender sender = null;
        try {
            sender = (MultiPointSender) getSender();
            if (sender == null) {
                ChannelException cx = new ChannelException(
                        sm.getString("pooledMultiSender.unable.retrieve.sender", Long.toString(getMaxWait())));
                for (int i = 0; i < destination.length; i++) {
                    cx.addFaultyMember(destination[i],
                            new NullPointerException(sm.getString("pooledMultiSender.retrieve.fail")));
                }
                throw cx;
            } else {
                sender.sendMessage(destination, msg);
            }
            sender.keepalive();
        } finally {
            if (sender != null) {
                returnSender(sender);
            }
        }
    }

    @Override
    public DataSender getNewDataSender() {
        MultipointBioSender sender = new MultipointBioSender();
        AbstractSender.transferProperties(this, sender);
        return sender;
    }
}
