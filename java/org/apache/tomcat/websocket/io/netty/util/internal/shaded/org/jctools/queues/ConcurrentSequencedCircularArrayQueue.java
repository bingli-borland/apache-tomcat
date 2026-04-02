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

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomcat.websocket.io.netty.util.internal.shaded.org.jctools.queues;

import static org.apache.tomcat.websocket.io.netty.util.internal.shaded.org.jctools.util.UnsafeLongArrayAccess.*;

public abstract class ConcurrentSequencedCircularArrayQueue<E> extends ConcurrentCircularArrayQueue<E>
{
    protected final long[] sequenceBuffer;

    public ConcurrentSequencedCircularArrayQueue(int capacity)
    {
        super(capacity);
        int actualCapacity = (int) (this.mask + 1);
        // pad data on either end with some empty slots. Note that actualCapacity is <= MAX_POW2_INT
        sequenceBuffer = allocateLongArray(actualCapacity);
        for (long i = 0; i < actualCapacity; i++)
        {
            soLongElement(sequenceBuffer, calcCircularLongElementOffset(i, mask), i);
        }
    }
}
