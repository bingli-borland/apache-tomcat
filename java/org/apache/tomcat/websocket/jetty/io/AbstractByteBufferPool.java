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

package org.apache.tomcat.websocket.jetty.io;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.apache.tomcat.websocket.jetty.util.annotation.ManagedAttribute;
import org.apache.tomcat.websocket.jetty.util.annotation.ManagedObject;
import org.apache.tomcat.websocket.jetty.util.annotation.ManagedOperation;

@ManagedObject
abstract class AbstractByteBufferPool implements ByteBufferPool
{
    private final int _factor;
    private final int _maxQueueLength;
    private final long _maxHeapMemory;
    private final long _maxDirectMemory;
    private final AtomicLong _heapMemory = new AtomicLong();
    private final AtomicLong _directMemory = new AtomicLong();

    /**
     * Creates a new ByteBufferPool with the given configuration.
     *
     * @param factor the capacity factor
     * @param maxQueueLength the maximum ByteBuffer queue length
     * @param maxHeapMemory the max heap memory in bytes, -1 for unlimited memory or 0 to use default heuristic.
     * @param maxDirectMemory the max direct memory in bytes, -1 for unlimited memory or 0 to use default heuristic.
     */
    protected AbstractByteBufferPool(int factor, int maxQueueLength, long maxHeapMemory, long maxDirectMemory)
    {
        _factor = factor <= 0 ? 1024 : factor;
        _maxQueueLength = maxQueueLength;
        _maxHeapMemory = (maxHeapMemory != 0) ? maxHeapMemory : Runtime.getRuntime().maxMemory() / 4;
        _maxDirectMemory = (maxDirectMemory != 0) ? maxDirectMemory : Runtime.getRuntime().maxMemory() / 4;
    }

    protected int getCapacityFactor()
    {
        return _factor;
    }

    protected int getMaxQueueLength()
    {
        return _maxQueueLength;
    }

    @Deprecated
    protected void decrementMemory(ByteBuffer buffer)
    {
        updateMemory(buffer, false);
    }

    @Deprecated
    protected void incrementMemory(ByteBuffer buffer)
    {
        updateMemory(buffer, true);
    }

    private void updateMemory(ByteBuffer buffer, boolean addOrSub)
    {
        AtomicLong memory = buffer.isDirect() ? _directMemory : _heapMemory;
        int capacity = buffer.capacity();
        memory.addAndGet(addOrSub ? capacity : -capacity);
    }

    protected void releaseExcessMemory(boolean direct, Consumer<Boolean> clearFn)
    {
        long maxMemory = direct ? _maxDirectMemory : _maxHeapMemory;
        if (maxMemory > 0)
        {
            while (getMemory(direct) > maxMemory)
            {
                clearFn.accept(direct);
            }
        }
    }

    @ManagedAttribute("The bytes retained by direct ByteBuffers")
    public long getDirectMemory()
    {
        return getMemory(true);
    }

    @ManagedAttribute("The bytes retained by heap ByteBuffers")
    public long getHeapMemory()
    {
        return getMemory(false);
    }

    @ManagedAttribute("The max num of bytes that can be retained from direct ByteBuffers")
    public long getMaxDirectMemory()
    {
        return _maxDirectMemory;
    }

    @ManagedAttribute("The max num of bytes that can be retained from heap ByteBuffers")
    public long getMaxHeapMemory()
    {
        return _maxHeapMemory;
    }

    public long getMemory(boolean direct)
    {
        AtomicLong memory = direct ? _directMemory : _heapMemory;
        return memory.get();
    }

    IntConsumer updateMemory(boolean direct)
    {
        return (direct) ? _directMemory::addAndGet : _heapMemory::addAndGet;
    }

    @ManagedOperation(value = "Clears this ByteBufferPool", impact = "ACTION")
    public void clear()
    {
        _heapMemory.set(0);
        _directMemory.set(0);
    }
}
