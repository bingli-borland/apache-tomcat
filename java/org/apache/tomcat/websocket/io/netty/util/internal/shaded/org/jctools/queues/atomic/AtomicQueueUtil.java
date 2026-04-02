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

package org.apache.tomcat.websocket.io.netty.util.internal.shaded.org.jctools.queues.atomic;

import org.apache.tomcat.websocket.io.netty.util.internal.shaded.org.jctools.util.InternalAPI;

import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

@InternalAPI
public final class AtomicQueueUtil
{
    public static <E> E lvRefElement(AtomicReferenceArray<E> buffer, int offset)
    {
        return buffer.get(offset);
    }

    public static <E> E lpRefElement(AtomicReferenceArray<E> buffer, int offset)
    {
        return buffer.get(offset); // no weaker form available
    }

    public static <E> void spRefElement(AtomicReferenceArray<E> buffer, int offset, E value)
    {
        buffer.lazySet(offset, value);  // no weaker form available
    }

    public static void soRefElement(AtomicReferenceArray buffer, int offset, Object value)
    {
        buffer.lazySet(offset, value);
    }

    public static <E> void svRefElement(AtomicReferenceArray<E> buffer, int offset, E value)
    {
        buffer.set(offset, value);
    }

    public static int calcRefElementOffset(long index)
    {
        return (int) index;
    }

    public static int calcCircularRefElementOffset(long index, long mask)
    {
        return (int) (index & mask);
    }

    public static <E> AtomicReferenceArray<E> allocateRefArray(int capacity)
    {
        return new AtomicReferenceArray<E>(capacity);
    }

    public static void spLongElement(AtomicLongArray buffer, int offset, long e)
    {
        buffer.lazySet(offset, e);
    }

    public static void soLongElement(AtomicLongArray buffer, int offset, long e)
    {
        buffer.lazySet(offset, e);
    }

    public static long lpLongElement(AtomicLongArray buffer, int offset)
    {
        return buffer.get(offset);
    }

    public static long lvLongElement(AtomicLongArray buffer, int offset)
    {
        return buffer.get(offset);
    }

    public static int calcLongElementOffset(long index)
    {
        return (int) index;
    }

    public static int calcCircularLongElementOffset(long index, int mask)
    {
        return (int) (index & mask);
    }

    public static AtomicLongArray allocateLongArray(int capacity)
    {
        return new AtomicLongArray(capacity);
    }

    public static int length(AtomicReferenceArray<?> buf)
    {
        return buf.length();
    }

    /**
     * This method assumes index is actually (index << 1) because lower bit is used for resize hence the >> 1
     */
    public static int modifiedCalcCircularRefElementOffset(long index, long mask)
    {
        return (int) (index & mask) >> 1;
    }

    public static int nextArrayOffset(AtomicReferenceArray<?> curr)
    {
        return length(curr) - 1;
    }

}
