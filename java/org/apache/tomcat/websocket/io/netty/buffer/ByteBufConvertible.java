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
package org.apache.tomcat.websocket.io.netty.buffer;

/**
 * An interface that can be implemented by any object that know how to turn itself into a {@link ByteBuf}.
 * All {@link ByteBuf} classes implement this interface, and return themselves.
 */
public interface ByteBufConvertible {
    /**
     * Turn this object into a {@link ByteBuf}.
     * This does <strong>not</strong> increment the reference count of the {@link ByteBuf} instance.
     * The conversion or exposure of the {@link ByteBuf} must be idempotent, so that this method can be called
     * either once, or multiple times, without causing any change in program behaviour.
     *
     * @return A {@link ByteBuf} instance from this object.
     */
    ByteBuf asByteBuf();
}
