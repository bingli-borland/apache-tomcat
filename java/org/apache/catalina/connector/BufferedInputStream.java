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

package org.apache.catalina.connector;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import org.apache.catalina.tribes.util.StringManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class BufferedInputStream extends ServletInputStream {

    protected static final StringManager sm = StringManager.getManager(BufferedInputStream.class);

    private ByteArrayInputStream postCached;

    protected BufferedInputStream(ByteArrayInputStream data) {
        this.postCached = data;
    }

    /**
     * Prevent cloning the facade
     *
     * @return
     * @throws CloneNotSupportedException
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    @Override
    public int read() throws IOException {
        return this.postCached.read();
    }

    @Override
    public int available() throws IOException {
        return this.postCached.available();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return this.postCached.read(b, 0, b.length);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return this.postCached.read(b, off, len);
    }

    @Override
    public int readLine(byte[] b, int off, int len) throws IOException {
        return super.readLine(b, off, len);
    }

    /**
     * Close the stream Since we re-cycle, we can't allow the call to super.close() which would permanently disable us
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean isFinished() {
        if (this.postCached.available() > 0)
            return false;
        return true;
    }

    @Override
    public boolean isReady() {
        return isFinished();
    }

    @Override
    public void setReadListener(ReadListener listener) {
        throw new UnsupportedOperationException();
    }
}
