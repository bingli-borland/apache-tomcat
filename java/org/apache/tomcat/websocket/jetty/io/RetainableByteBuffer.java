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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tomcat.websocket.jetty.util.BufferUtil;
import org.apache.tomcat.websocket.jetty.util.Retainable;

/**
 * A Retainable ByteBuffer.
 * <p>Acquires a ByteBuffer from a {@link ByteBufferPool} and maintains a reference count that is
 * initially 1, incremented with {@link #retain()} and decremented with {@link #release()}. The buffer
 * is released to the pool when the reference count is decremented to 0.</p>
 */
public class RetainableByteBuffer implements Retainable
{
    private final ByteBufferPool pool;
    private final ByteBuffer buffer;
    private final AtomicInteger references;

    public RetainableByteBuffer(ByteBufferPool pool, int size)
    {
        this(pool, size, false);
    }

    public RetainableByteBuffer(ByteBufferPool pool, int size, boolean direct)
    {
        this.pool = pool;
        this.buffer = pool.acquire(size, direct);
        this.references = new AtomicInteger(1);
    }

    public ByteBuffer getBuffer()
    {
        return buffer;
    }

    public int getReferences()
    {
        return references.get();
    }

    @Override
    public void retain()
    {
        while (true)
        {
            int r = references.get();
            if (r == 0)
                throw new IllegalStateException("released " + this);
            if (references.compareAndSet(r, r + 1))
                break;
        }
    }

    public int release()
    {
        int ref = references.decrementAndGet();
        if (ref == 0)
            pool.release(buffer);
        else if (ref < 0)
            throw new IllegalStateException("already released " + this);
        return ref;
    }

    public int remaining()
    {
        return buffer.remaining();
    }

    public boolean hasRemaining()
    {
        return remaining() > 0;
    }

    public boolean isEmpty()
    {
        return !hasRemaining();
    }

    public void clear()
    {
        BufferUtil.clear(buffer);
    }

    @Override
    public String toString()
    {
        return String.format("%s@%x{%s,r=%d}", getClass().getSimpleName(), hashCode(), BufferUtil.toDetailString(buffer), getReferences());
    }
}
