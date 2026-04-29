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

/**
 * Extension of the {@link ArrayByteBufferPool} whose bucket sizes increase exponentially instead of linearly.
 * Each bucket will be double the size of the previous bucket, this decreases the amounts of buckets required
 * which can lower total memory usage if buffers are often being acquired of different sizes. However as there are
 * fewer buckets this will also increase the contention on each bucket.
 */
public class LogarithmicArrayByteBufferPool extends ArrayByteBufferPool
{
    /**
     * Creates a new ByteBufferPool with a default configuration.
     */
    public LogarithmicArrayByteBufferPool()
    {
        this(-1, -1, -1);
    }

    /**
     * Creates a new ByteBufferPool with the given configuration.
     *
     * @param minCapacity the minimum ByteBuffer capacity
     * @param maxCapacity the maximum ByteBuffer capacity
     */
    public LogarithmicArrayByteBufferPool(int minCapacity, int maxCapacity)
    {
        this(minCapacity, maxCapacity, -1, -1, -1);
    }

    /**
     * Creates a new ByteBufferPool with the given configuration.
     *
     * @param minCapacity the minimum ByteBuffer capacity
     * @param maxCapacity the maximum ByteBuffer capacity
     * @param maxQueueLength the maximum ByteBuffer queue length
     */
    public LogarithmicArrayByteBufferPool(int minCapacity, int maxCapacity, int maxQueueLength)
    {
        this(minCapacity, maxCapacity, maxQueueLength, -1, -1);
    }

    /**
     * Creates a new ByteBufferPool with the given configuration.
     *
     * @param minCapacity the minimum ByteBuffer capacity
     * @param maxCapacity the maximum ByteBuffer capacity
     * @param maxQueueLength the maximum ByteBuffer queue length
     * @param maxHeapMemory the max heap memory in bytes
     * @param maxDirectMemory the max direct memory in bytes
     */
    public LogarithmicArrayByteBufferPool(int minCapacity, int maxCapacity, int maxQueueLength, long maxHeapMemory, long maxDirectMemory)
    {
        super(minCapacity, 1, maxCapacity, maxQueueLength, maxHeapMemory, maxDirectMemory);
    }

    @Override
    protected int bucketFor(int capacity)
    {
        return 32 - Integer.numberOfLeadingZeros(capacity - 1);
    }

    @Override
    protected int capacityFor(int bucket)
    {
        return 1 << bucket;
    }

    @Override
    protected void releaseMemory(boolean direct)
    {
        long oldest = Long.MAX_VALUE;
        int index = -1;
        Bucket[] buckets = bucketsFor(direct);
        for (int i = 0; i < buckets.length; ++i)
        {
            Bucket bucket = buckets[i];
            if (bucket.isEmpty())
                continue;
            long lastUpdate = bucket.getLastUpdate();
            if (lastUpdate < oldest)
            {
                oldest = lastUpdate;
                index = i;
            }
        }
        if (index >= 0)
        {
            Bucket bucket = buckets[index];
            // Acquire a buffer but never return it to the pool.
            bucket.acquire();
            bucket.resetUpdateTime();
        }
    }
}
