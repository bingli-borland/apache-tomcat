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
 * Expose metrics for an SizeClasses.
 */
public interface SizeClassesMetric {

    /**
     * Computes size from lookup table according to sizeIdx.
     *
     * @return size
     */
    int sizeIdx2size(int sizeIdx);

    /**
     * Computes size according to sizeIdx.
     *
     * @return size
     */
    int sizeIdx2sizeCompute(int sizeIdx);

    /**
     * Computes size from lookup table according to pageIdx.
     *
     * @return size which is multiples of pageSize.
     */
    long pageIdx2size(int pageIdx);

    /**
     * Computes size according to pageIdx.
     *
     * @return size which is multiples of pageSize
     */
    long pageIdx2sizeCompute(int pageIdx);

    /**
     * Normalizes request size up to the nearest size class.
     *
     * @param size request size
     *
     * @return sizeIdx of the size class
     */
    int size2SizeIdx(int size);

    /**
     * Normalizes request size up to the nearest pageSize class.
     *
     * @param pages multiples of pageSizes
     *
     * @return pageIdx of the pageSize class
     */
    int pages2pageIdx(int pages);

    /**
     * Normalizes request size down to the nearest pageSize class.
     *
     * @param pages multiples of pageSizes
     *
     * @return pageIdx of the pageSize class
     */
    int pages2pageIdxFloor(int pages);

    /**
     * Normalizes usable size that would result from allocating an object with the
     * specified size and alignment.
     *
     * @param size request size
     *
     * @return normalized size
     */
    int normalizeSize(int size);
}
