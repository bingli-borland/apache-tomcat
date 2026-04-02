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
package org.apache.tomcat.websocket.io.netty.util.concurrent;

import org.apache.tomcat.websocket.io.netty.util.internal.ObjectUtil;
import org.apache.tomcat.websocket.io.netty.util.internal.PlatformDependent;

/**
 * The {@link CompleteFuture} which is failed already.  It is
 * recommended to use {@link EventExecutor#newFailedFuture(Throwable)}
 * instead of calling the constructor of this future.
 */
public final class FailedFuture<V> extends CompleteFuture<V> {

    private final Throwable cause;

    /**
     * Creates a new instance.
     *
     * @param executor the {@link EventExecutor} associated with this future
     * @param cause   the cause of failure
     */
    public FailedFuture(EventExecutor executor, Throwable cause) {
        super(executor);
        this.cause = ObjectUtil.checkNotNull(cause, "cause");
    }

    @Override
    public Throwable cause() {
        return cause;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public Future<V> sync() {
        PlatformDependent.throwException(cause);
        return this;
    }

    @Override
    public Future<V> syncUninterruptibly() {
        PlatformDependent.throwException(cause);
        return this;
    }

    @Override
    public V getNow() {
        return null;
    }
}
