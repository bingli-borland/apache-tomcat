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
package org.apache.catalina.core;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.collections.UnsynchronizedStack;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 */
public class ServletRequestThreadData {

    protected static final Log logger = LogFactory.getLog(ServletRequestThreadData.class);


    private Map _parameters = null;
    private LinkedList _queryStringList = null; // 256836


    private UnsynchronizedStack _paramStack = new UnsynchronizedStack();

    private static ThreadLocal<ServletRequestThreadData> instance = new ThreadLocal<ServletRequestThreadData>();

    public static ServletRequestThreadData getInstance() {

        ServletRequestThreadData tempState = null;
        tempState = (ServletRequestThreadData) instance.get();

        if (tempState == null) {
            tempState = new ServletRequestThreadData();
            instance.set(tempState);
        }

        return tempState;
    }


    public ServletRequestThreadData() {
    }


    public void init(ServletRequestThreadData data) {


        if (data == null) {
            _parameters = null;
            _queryStringList = null;
        } else {
            _parameters = data.getParameters();
            _queryStringList = data.getQueryStringList();
        }
        if (!_paramStack.isEmpty()) {
            _paramStack.clear();
        }
    }


    /**
     * @return the _parameters
     */
    public Map getParameters() {
        return _parameters;
    }


    /**
     * @param parameters the _parameters to set
     */
    public void setParameters(Map parameters) {
        this._parameters = parameters;
    }

    /**
     * Save the state of the parameters before a call to include or forward.
     */
    public void pushParameterStack(Map parameters) {
        if (parameters == null) {
            _paramStack.push(null);
        } else {
            _paramStack.push(((Hashtable) parameters).clone());
        }
    }


    /**
     * @return the _queryStringList
     */
    public LinkedList getQueryStringList() {
        return _queryStringList;
    }


    /**
     * @param _queryStringList the _queryStringList to set
     */
    public void setQueryStringList(LinkedList _queryStringList) {
        this._queryStringList = _queryStringList;
    }


}
