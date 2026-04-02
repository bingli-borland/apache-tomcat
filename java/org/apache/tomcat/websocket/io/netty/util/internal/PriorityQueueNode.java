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
package org.apache.tomcat.websocket.io.netty.util.internal;

/**
 * Provides methods for {@link DefaultPriorityQueue} to maintain internal state. These methods should generally not be
 * used outside the scope of {@link DefaultPriorityQueue}.
 */
public interface PriorityQueueNode {
    /**
     * This should be used to initialize the storage returned by {@link #priorityQueueIndex(DefaultPriorityQueue)}.
     */
    int INDEX_NOT_IN_QUEUE = -1;

    /**
     * Get the last value set by {@link #priorityQueueIndex(DefaultPriorityQueue, int)} for the value corresponding to
     * {@code queue}.
     * <p>
     * Throwing exceptions from this method will result in undefined behavior.
     */
    int priorityQueueIndex(DefaultPriorityQueue<?> queue);

    /**
     * Used by {@link DefaultPriorityQueue} to maintain state for an element in the queue.
     * <p>
     * Throwing exceptions from this method will result in undefined behavior.
     * @param queue The queue for which the index is being set.
     * @param i The index as used by {@link DefaultPriorityQueue}.
     */
    void priorityQueueIndex(DefaultPriorityQueue<?> queue, int i);
}
