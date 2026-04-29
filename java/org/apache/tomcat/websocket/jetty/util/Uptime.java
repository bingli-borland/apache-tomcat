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

package org.apache.tomcat.websocket.jetty.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Provide for a Uptime class that is compatible with Android, GAE, and the new Java 8 compact profiles
 */
public class Uptime
{
    public static final int NOIMPL = -1;

    public interface Impl
    {
        long getUptime();
    }

    public static class DefaultImpl implements Impl
    {
        public Object mxBean;
        public Method uptimeMethod;

        public DefaultImpl()
        {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try
            {
                Class<?> mgmtFactory = Class.forName("java.lang.management.ManagementFactory", true, cl);
                Class<?> runtimeClass = Class.forName("java.lang.management.RuntimeMXBean", true, cl);
                Class<?>[] noparams = new Class<?>[0];
                Method mxBeanMethod = mgmtFactory.getMethod("getRuntimeMXBean", noparams);
                if (mxBeanMethod == null)
                {
                    throw new UnsupportedOperationException("method getRuntimeMXBean() not found");
                }
                mxBean = mxBeanMethod.invoke(mgmtFactory);
                if (mxBean == null)
                {
                    throw new UnsupportedOperationException("getRuntimeMXBean() method returned null");
                }
                uptimeMethod = runtimeClass.getMethod("getUptime", noparams);
                if (mxBean == null)
                {
                    throw new UnsupportedOperationException("method getUptime() not found");
                }
            }
            catch (ClassNotFoundException |
                NoClassDefFoundError |
                NoSuchMethodException |
                SecurityException |
                IllegalAccessException |
                IllegalArgumentException |
                InvocationTargetException e)
            {
                throw new UnsupportedOperationException("Implementation not available in this environment", e);
            }
        }

        @Override
        public long getUptime()
        {
            try
            {
                return (long)uptimeMethod.invoke(mxBean);
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
            {
                return NOIMPL;
            }
        }
    }

    private static final Uptime INSTANCE = new Uptime();

    public static Uptime getInstance()
    {
        return INSTANCE;
    }

    private Impl impl;

    private Uptime()
    {
        try
        {
            impl = new DefaultImpl();
        }
        catch (UnsupportedOperationException e)
        {
            System.err.printf("Defaulting Uptime to NOIMPL due to (%s) %s%n", e.getClass().getName(), e.getMessage());
            impl = null;
        }
    }

    public Impl getImpl()
    {
        return impl;
    }

    public void setImpl(Impl impl)
    {
        this.impl = impl;
    }

    public static long getUptime()
    {
        Uptime u = getInstance();
        if (u == null || u.impl == null)
        {
            return NOIMPL;
        }
        return u.impl.getUptime();
    }
}
