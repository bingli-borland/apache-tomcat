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
package com.alx.enterprise.webtier.core;

/**
 * Holds APR status without the need to load other classes.
 *
 * @deprecated Unused. Use {@link com.alx.enterprise.web.jni.AprStatus} instead. This class will be removed in Alx 12
 *                 onwards.
 */
@Deprecated
public class AprStatus {

    public static boolean isAprInitialized() {
        return com.alx.enterprise.web.jni.AprStatus.isAprInitialized();
    }

    public static boolean isAprAvailable() {
        return com.alx.enterprise.web.jni.AprStatus.isAprAvailable();
    }

    public static boolean getUseOpenSSL() {
        return com.alx.enterprise.web.jni.AprStatus.getUseOpenSSL();
    }

    public static boolean isInstanceCreated() {
        return com.alx.enterprise.web.jni.AprStatus.isInstanceCreated();
    }

    public static void setAprInitialized(boolean aprInitialized) {
        com.alx.enterprise.web.jni.AprStatus.setAprInitialized(aprInitialized);
    }

    public static void setAprAvailable(boolean aprAvailable) {
        com.alx.enterprise.web.jni.AprStatus.setAprAvailable(aprAvailable);
    }

    public static void setUseOpenSSL(boolean useOpenSSL) {
        com.alx.enterprise.web.jni.AprStatus.setUseOpenSSL(useOpenSSL);
    }

    public static void setInstanceCreated(boolean instanceCreated) {
        com.alx.enterprise.web.jni.AprStatus.setInstanceCreated(instanceCreated);
    }

    /**
     * @return the openSSLVersion
     */
    public static int getOpenSSLVersion() {
        return com.alx.enterprise.web.jni.AprStatus.getOpenSSLVersion();
    }

    /**
     * @param openSSLVersion the openSSLVersion to set
     */
    public static void setOpenSSLVersion(int openSSLVersion) {
        com.alx.enterprise.web.jni.AprStatus.setOpenSSLVersion(openSSLVersion);
    }
}
