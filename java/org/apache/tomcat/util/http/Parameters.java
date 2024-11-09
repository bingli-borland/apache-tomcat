/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tomcat.util.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.catalina.Globals;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.buf.StringUtils;
import org.apache.tomcat.util.buf.UDecoder;
import org.apache.tomcat.util.collections.UnsynchronizedStack;
import org.apache.tomcat.util.log.UserDataHelper;
import org.apache.tomcat.util.res.StringManager;

public final class Parameters {

    private static final Log log = LogFactory.getLog(Parameters.class);

    private static final StringManager sm = StringManager.getManager("org.apache.tomcat.util.http");

    private final Map<String, ArrayList<ByteChunk>> paramHashValues = new LinkedHashMap<>();
    private boolean didQueryParameters = false;

    private MessageBytes queryMB;

    private UDecoder urlDec;
    private final MessageBytes decodedQuery = MessageBytes.newInstance();

    private Charset charset = StandardCharsets.ISO_8859_1;
    private Charset queryStringCharset = StandardCharsets.UTF_8;

    private int limit = -1;
    private int parameterCount = 0;

    private UnsynchronizedStack paramStack = new UnsynchronizedStack();

    private Map _parameters = null;
    private LinkedList _queryStringList = null;

    private UnsynchronizedStack _paramStack = new UnsynchronizedStack();

    public Parameters() {
        // NO-OP
    }

    public void setQuery(MessageBytes queryMB) {
        this.queryMB = queryMB;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        if (charset == null) {
            charset = DEFAULT_BODY_CHARSET;
        }
        this.charset = charset;
        if (log.isTraceEnabled()) {
            log.trace("Set encoding to " + charset.name());
        }
    }

    public void setQueryStringCharset(Charset queryStringCharset) {
        if (queryStringCharset == null) {
            queryStringCharset = DEFAULT_URI_CHARSET;
        }
        this.queryStringCharset = queryStringCharset;

        if (log.isTraceEnabled()) {
            log.trace("Set query string encoding to " + queryStringCharset.name());
        }
    }

    public Charset getQueryStringCharset() {
        if (queryStringCharset == null) {
            return DEFAULT_URI_CHARSET;
        }
        return queryStringCharset;
    }

    public int size() {
        return parameterCount;
    }


    public void recycle() {
        parameterCount = 0;
        paramHashValues.clear();
        didQueryParameters = false;
        charset = DEFAULT_BODY_CHARSET;
        decodedQuery.recycle();
        paramStack.clear();
        _parameters = null;
        _queryStringList = null;
        if (!_paramStack.isEmpty()) {
            _paramStack.clear();
        }
    }


    // -------------------- Data access --------------------
    // Access to the current name/values, no side effect ( processing ).
    // You must explicitly call handleQueryParameters and the post methods.

    public ByteChunk[] getParameterValues(String name) {
        if (Globals.COMPATIBLEWEBSPHERE) {
            return (ByteChunk[]) getParameters().get(name);
        }
        handleQueryParameters();
        // no "facade"
        ArrayList<ByteChunk> values = paramHashValues.get(name);
        if (values == null) {
            return null;
        }
        return values.toArray(new ByteChunk[0]);
    }

    public Enumeration<String> getParameterNames() {
        if (Globals.COMPATIBLEWEBSPHERE) {
            return ((Hashtable)getParameters()).keys();
        }
        handleQueryParameters();
        return Collections.enumeration(paramHashValues.keySet());
    }

    public ByteChunk getParameter(String name) {
        if (Globals.COMPATIBLEWEBSPHERE) {
            ByteChunk[] values = (ByteChunk[]) getParameters().get(name);
            ByteChunk value = null;
            if (values != null && values.length > 0) {
                value = values[0];
            }
            return value;
        }
        handleQueryParameters();
        ArrayList<ByteChunk> values = paramHashValues.get(name);
        if (values != null) {
            if (values.isEmpty()) {
                return new ByteChunk();
            }
            return values.get(0);
        } else {
            return null;
        }
    }

    // -------------------- Processing --------------------
    /**
     * Process the query string into parameters
     */
    public void handleQueryParameters() {
        if (didQueryParameters) {
            return;
        }

        didQueryParameters = true;

        if (queryMB == null || queryMB.isNull()) {
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace("Decoding query " + decodedQuery + " " + queryStringCharset.name());
        }

        try {
            decodedQuery.duplicate(queryMB);
        } catch (IOException ioe) {
            // Can't happen, as decodedQuery can't overflow
            log.error(sm.getString("parameters.copyFail"), ioe);
        }
        processParameters(decodedQuery, queryStringCharset, true);
    }


    public void addParameter(String key, ByteChunk value) throws IllegalStateException {

        if (key == null) {
            return;
        }

        if (limit > -1 && parameterCount >= limit) {
            // Processing this parameter will push us over the limit.
            throw new InvalidParameterException(sm.getString("parameters.maxCountFail", Integer.valueOf(limit)));
        }
        parameterCount++;

        paramHashValues.computeIfAbsent(key, k -> new ArrayList<>(1)).add(value);
    }

    public void setURLDecoder(UDecoder u) {
        urlDec = u;
    }

    public Map getParameters(){
        return _parameters;
    }

    public void setParameters(Hashtable<String, ByteChunk[]> parameters) {
        _parameters = parameters;
    }

    /**
     * Save the state of the parameters before a call to include or forward.
     */
    public void pushParameterStack() {
        if (getParameters() == null) {
            _paramStack.push(null);
        } else {
            paramStack.push(((Hashtable) getParameters()).clone());
        }
    }

    /**
     * Revert the state of the parameters which was saved before an include call
     *
     */
    public void popParameterStack() {
        try {
            setParameters((Hashtable) paramStack.pop());
        } catch (java.util.EmptyStackException empty) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to remove item from stack", empty);
            }
        }
    }

    public void aggregateQueryStringParams(String additionalQueryString, boolean setQS) {
        QSListItem tmpQS = null;
        if (getParameters() == null) {
            LinkedList queryStringList = _queryStringList;
            if (queryStringList == null || queryStringList.isEmpty()) {
                if (queryStringList == null) {
                    queryStringList = new LinkedList();
                }

                if (queryMB != null && !queryMB.isNull()) {
                    MessageBytes tmp = MessageBytes.newInstance();
                    try {
                        tmp.duplicate(queryMB);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    tmpQS = new QSListItem(tmp, null);
                    queryStringList.add(tmpQS);
                }
                _queryStringList = queryStringList;

            }
            // End 258025, Part 2
            if (additionalQueryString != null) {
                MessageBytes qs = MessageBytes.newInstance();
                qs.setString(additionalQueryString);
                tmpQS = new QSListItem(qs, null);
                queryStringList.add(tmpQS);
            }

        }
        if (setQS) {
            queryMB.setString(additionalQueryString);
        }

        // if _parameters is not null, then this is part of a forward or include...add the additional query parms
        // if _parameters is null, then the string will be parsed if needed
        if (getParameters() != null && additionalQueryString != null) {
            MessageBytes qs = MessageBytes.newInstance();
            qs.setString(additionalQueryString);
            if (qs.getType() != MessageBytes.T_BYTES) {
                qs.toBytes();
            }
            ByteChunk bc = qs.getByteChunk();
            Hashtable<String, ByteChunk[]> parameters = parseQueryStringParameters(bc.getBytes(), bc.getOffset(), bc.getLength(), queryStringCharset, true);
            // end 249841, 256836
            ByteChunk[] valArray;
            for (Enumeration e = parameters.keys(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                ByteChunk[] newVals = (ByteChunk[]) parameters.get(key);

                // Check to see if a parameter with the key already exists
                // and prepend the values since QueryString takes precedence
                if (getParameters().containsKey(key)) {
                    ByteChunk[] oldVals = (ByteChunk[]) getParameters().get(key);
                    Vector v = new Vector();

                    for (int i = 0; i < newVals.length; i++) {
                        v.add(newVals[i]);
                    }

                    for (int i = 0; i < oldVals.length; i++) {
                        v.add(oldVals[i]);
                    }

                    valArray = new ByteChunk[v.size()];
                    v.toArray(valArray);

                    getParameters().put(key, valArray);
                } else {
                    getParameters().put(key, newVals);
                }
            }
        }
    }

    public void parseQueryStringList() {

        Hashtable<String, ByteChunk[]> tmpQueryParams = null;
        LinkedList queryStringList = _queryStringList;
        if (queryStringList == null || queryStringList.isEmpty()) { //258025
            if (queryMB != null && !queryMB.isNull())//PM35450
            {
                if (queryMB.getType() != MessageBytes.T_BYTES) {
                    queryMB.toBytes();
                }
                ByteChunk bc = queryMB.getByteChunk();
                if (getParameters() == null || getParameters().isEmpty()) {
                    setParameters(parseQueryStringParameters(bc.getBytes(), bc.getOffset(), bc.getLength(), queryStringCharset, true));
                } else {
                    tmpQueryParams = parseQueryStringParameters(bc.getBytes(), bc.getOffset(), bc.getLength(), queryStringCharset, true);
                    mergeQueryParams(tmpQueryParams);
                }
            }
        } else {
            Iterator i = queryStringList.iterator();
            QSListItem qsListItem = null;
            MessageBytes queryString;
            while (i.hasNext()) {
                qsListItem = ((QSListItem) i.next());
                queryString = qsListItem._qs;
                if (qsListItem._qsHashtable != null)
                    mergeQueryParams(qsListItem._qsHashtable);
                else if (queryString != null && !queryString.isNull()) {
                    if (queryString.getType() != MessageBytes.T_BYTES) {
                        queryString.toBytes();
                    }
                    ByteChunk bc = queryString.getByteChunk();
                    if (getParameters() == null || getParameters().isEmpty())// 258025
                    {
                        qsListItem._qsHashtable = parseQueryStringParameters(bc.getBytes(), bc.getOffset(), bc.getLength(), queryStringCharset, true);
                        setParameters(qsListItem._qsHashtable);
                        qsListItem._qs = null;
                    } else {
                        tmpQueryParams = parseQueryStringParameters(bc.getBytes(), bc.getOffset(), bc.getLength(), queryStringCharset, true);
                        qsListItem._qsHashtable = tmpQueryParams;
                        qsListItem._qs = null;
                        mergeQueryParams(tmpQueryParams);
                    }
                }
            }
        }
    }

    private void mergeQueryParams(Hashtable<String, ByteChunk[]> tmpQueryParams) {
        if (tmpQueryParams != null) {
            Enumeration enumeration = tmpQueryParams.keys();
            while (enumeration.hasMoreElements()) {
                Object key = enumeration.nextElement();
                // Check for QueryString parms with the same name
                // pre-append to postdata values if necessary
                if (getParameters() != null && getParameters().containsKey(key)) {
                    ByteChunk postVals[] = (ByteChunk[]) getParameters().get(key);
                    ByteChunk queryVals[] = (ByteChunk[]) tmpQueryParams.get(key);
                    ByteChunk newVals[] = new ByteChunk[postVals.length + queryVals.length];
                    int newValsIndex = 0;
                    for (int i = 0; i < queryVals.length; i++) {
                        newVals[newValsIndex++] = queryVals[i];
                    }
                    for (int i = 0; i < postVals.length; i++) {
                        newVals[newValsIndex++] = postVals[i];
                    }
                    getParameters().put(key, newVals);
                } else {
                    if (getParameters() == null) {
                        setParameters(new Hashtable());
                    }
                    getParameters().put(key, tmpQueryParams.get(key));
                }
            }
        }
    }


    public void removeQSFromList() {

        LinkedList queryStringList = _queryStringList;
        if (queryStringList != null && !queryStringList.isEmpty()) {
            Map _tmpParameters = getParameters();    // Save off reference to current parameters
            popParameterStack();
            if (getParameters() == null && _tmpParameters != null) // Parameters above current inluce/forward were never parsed
            {
                setParameters((Hashtable<String, ByteChunk[]>) _tmpParameters);
                Hashtable<String, ByteChunk[]> tmpQueryParams = ((QSListItem) queryStringList.getLast())._qsHashtable;
                if (tmpQueryParams == null) {
                    MessageBytes qs = ((QSListItem) queryStringList.getLast())._qs;
                    if (qs.getType() != MessageBytes.T_BYTES) {
                        qs.toBytes();
                    }
                    ByteChunk bc = qs.getByteChunk();
                    tmpQueryParams = parseQueryStringParameters(bc.getBytes(), bc.getOffset(), bc.getLength(), queryStringCharset, true);
                }
                removeQueryParams(tmpQueryParams);
            }
            queryStringList.removeLast();
        } else {
            //We need to pop parameter stack regardless of whether queryStringList is null
            //because the queryString parameters could have been added directly to parameter list without
            // adding ot the queryStringList
            popParameterStack();
        }
    }

    private void removeQueryParams(Hashtable<String, ByteChunk[]> tmpQueryParams) {
        if (tmpQueryParams != null) {
            Enumeration enumeration = tmpQueryParams.keys();
            while (enumeration.hasMoreElements()) {
                Object key = enumeration.nextElement();
                // Check for QueryString parms with the same name
                // pre-append to postdata values if necessary
                if (getParameters().containsKey(key)) {
                    ByteChunk postVals[] = (ByteChunk[]) getParameters().get(key);
                    ByteChunk queryVals[] = (ByteChunk[]) tmpQueryParams.get(key);
                    if (postVals.length - queryVals.length > 0) {
                        ByteChunk newVals[] = new ByteChunk[postVals.length - queryVals.length];
                        int newValsIndex = 0;
                        for (int i = queryVals.length; i < postVals.length; i++) {
                            newVals[newValsIndex++] = postVals[i];
                        }
                        getParameters().put(key, newVals);
                    } else
                        getParameters().remove(key);
                }
            }
        }
    }


    // -------------------- Parameter parsing --------------------
    // we are called from a single thread - we can do it the hard way
    // if needed
    private final ByteChunk origName = new ByteChunk();
    private final ByteChunk origValue = new ByteChunk();
    private static final Charset DEFAULT_BODY_CHARSET = StandardCharsets.ISO_8859_1;
    private static final Charset DEFAULT_URI_CHARSET = StandardCharsets.UTF_8;


    public void processParameters(byte[] bytes, int start, int len, boolean queryParams) {
        processParameters(bytes, start, len, charset, queryParams);
    }

    private void processParameters(byte[] bytes, int start, int len, Charset charset, boolean queryParams) {

        if (log.isTraceEnabled()) {
            log.trace(sm.getString("parameters.bytes", new String(bytes, start, len, DEFAULT_BODY_CHARSET)));
        }

        int pos = start;
        int end = start + len;

        while (pos < end) {
            ByteChunk tmpName = new ByteChunk();
            tmpName.setQuery(queryParams);
            ByteChunk tmpValue = new ByteChunk();
            tmpValue.setQuery(queryParams);
            int nameStart = pos;
            int nameEnd = -1;
            int valueStart = -1;
            int valueEnd = -1;

            boolean parsingName = true;
            boolean decodeName = false;
            boolean decodeValue = false;
            boolean parameterComplete = false;

            do {
                switch (bytes[pos]) {
                    case '=':
                        if (parsingName) {
                            // Name finished. Value starts from next character
                            nameEnd = pos;
                            parsingName = false;
                            valueStart = ++pos;
                        } else {
                            // Equals character in value
                            pos++;
                        }
                        break;
                    case '&':
                        if (parsingName) {
                            // Name finished. No value.
                            nameEnd = pos;
                        } else {
                            // Value finished
                            valueEnd = pos;
                        }
                        parameterComplete = true;
                        pos++;
                        break;
                    case '%':
                    case '+':
                        // Decoding required
                        if (parsingName) {
                            decodeName = true;
                        } else {
                            decodeValue = true;
                        }
                        pos++;
                        break;
                    default:
                        pos++;
                        break;
                }
            } while (!parameterComplete && pos < end);

            if (pos == end) {
                if (nameEnd == -1) {
                    nameEnd = pos;
                } else if (valueStart > -1 && valueEnd == -1) {
                    valueEnd = pos;
                }
            }

            if (log.isDebugEnabled() && valueStart == -1) {
                log.debug(sm.getString("parameters.noequal", Integer.valueOf(nameStart), Integer.valueOf(nameEnd),
                        new String(bytes, nameStart, nameEnd - nameStart, DEFAULT_BODY_CHARSET)));
            }

            if (nameEnd <= nameStart) {
                if (valueStart == -1) {
                    // &&
                    if (log.isDebugEnabled()) {
                        log.debug(sm.getString("parameters.emptyChunk"));
                    }
                    // Do not flag as error
                    continue;
                }
                // &=foo&
                String extract;
                if (valueEnd > nameStart) {
                    extract = new String(bytes, nameStart, valueEnd - nameStart, DEFAULT_BODY_CHARSET);
                } else {
                    extract = "";
                }
                String message = sm.getString("parameters.invalidChunk", Integer.valueOf(nameStart),
                        Integer.valueOf(valueEnd), extract);
                throw new InvalidParameterException(message);
            }

            tmpName.setBytes(bytes, nameStart, nameEnd - nameStart);
            if (valueStart >= 0) {
                tmpValue.setBytes(bytes, valueStart, valueEnd - valueStart);
            } else {
                tmpValue.setBytes(bytes, 0, 0);
            }

            // Take copies as if anything goes wrong originals will be
            // corrupted. This means original values can be logged.
            // For performance - only done for debug
            if (log.isDebugEnabled()) {
                try {
                    origName.append(bytes, nameStart, nameEnd - nameStart);
                    if (valueStart >= 0) {
                        origValue.append(bytes, valueStart, valueEnd - valueStart);
                    } else {
                        origValue.append(bytes, 0, 0);
                    }
                } catch (IOException ioe) {
                    // Should never happen...
                    log.error(sm.getString("parameters.copyFail"), ioe);
                }
            }

            try {
                String name;
                ByteChunk value;

                if (decodeName) {
                    urlDecode(tmpName);
                }
                tmpName.setCharset(charset);
                name = tmpName.toString(CodingErrorAction.REPORT, CodingErrorAction.REPORT);

                if (valueStart >= 0) {
                    if (decodeValue) {
                        urlDecode(tmpValue);
                    }
                    tmpValue.setCharset(charset);
                    value = tmpValue.toString(CodingErrorAction.REPORT, CodingErrorAction.REPORT);
                } else {
                    value = new ByteChunk();
                }

                addParameter(name, value);
            } catch (IOException ioe) {
                String message;
                if (log.isDebugEnabled()) {
                    message = sm.getString("parameters.decodeFail.debug", origName.toString(), origValue.toString());
                } else {
                    message = sm.getString("parameters.decodeFail.info", tmpName.toString(), tmpValue.toString());
                }
                throw new InvalidParameterException(message, ioe);
            } finally {
                // Only recycle copies if we used them
                if (log.isDebugEnabled()) {
                    origName.recycle();
                    origValue.recycle();
                }
            }
        }
    }

    public Hashtable parsePostParameters(byte bytes[], int start, int len, boolean queryParams) {
        return parseQueryStringParameters(bytes, start, len, charset, queryParams);
    }

    private Hashtable parseQueryStringParameters(byte bytes[], int start, int len, Charset charset, boolean queryParams) {

        Hashtable<String, ByteChunk[]> ht = new Hashtable<>();
        if (log.isTraceEnabled()) {
            log.trace(sm.getString("parameters.bytes", new String(bytes, start, len, DEFAULT_BODY_CHARSET)));
        }

        int pos = start;
        int end = start + len;

        while (pos < end) {
            ByteChunk tmpName = new ByteChunk();
            tmpName.setQuery(queryParams);
            ByteChunk tmpValue = new ByteChunk();
            tmpValue.setQuery(queryParams);
            int nameStart = pos;
            int nameEnd = -1;
            int valueStart = -1;
            int valueEnd = -1;

            boolean parsingName = true;
            boolean decodeName = false;
            boolean decodeValue = false;
            boolean parameterComplete = false;

            do {
                switch (bytes[pos]) {
                    case '=':
                        if (parsingName) {
                            // Name finished. Value starts from next character
                            nameEnd = pos;
                            parsingName = false;
                            valueStart = ++pos;
                        } else {
                            // Equals character in value
                            pos++;
                        }
                        break;
                    case '&':
                        if (parsingName) {
                            // Name finished. No value.
                            nameEnd = pos;
                        } else {
                            // Value finished
                            valueEnd = pos;
                        }
                        parameterComplete = true;
                        pos++;
                        break;
                    case '%':
                    case '+':
                        // Decoding required
                        if (parsingName) {
                            decodeName = true;
                        } else {
                            decodeValue = true;
                        }
                        pos++;
                        break;
                    default:
                        pos++;
                        break;
                }
            } while (!parameterComplete && pos < end);

            if (pos == end) {
                if (nameEnd == -1) {
                    nameEnd = pos;
                } else if (valueStart > -1 && valueEnd == -1) {
                    valueEnd = pos;
                }
            }

            if (log.isDebugEnabled() && valueStart == -1) {
                log.debug(sm.getString("parameters.noequal", Integer.valueOf(nameStart), Integer.valueOf(nameEnd),
                    new String(bytes, nameStart, nameEnd - nameStart, DEFAULT_BODY_CHARSET)));
            }

            if (nameEnd <= nameStart) {
                if (valueStart == -1) {
                    // &&
                    if (log.isDebugEnabled()) {
                        log.debug(sm.getString("parameters.emptyChunk"));
                    }
                    // Do not flag as error
                    continue;
                }
                // &=foo&
                String extract;
                if (valueEnd > nameStart) {
                    extract = new String(bytes, nameStart, valueEnd - nameStart, DEFAULT_BODY_CHARSET);
                } else {
                    extract = "";
                }
                String message = sm.getString("parameters.invalidChunk", Integer.valueOf(nameStart),
                    Integer.valueOf(valueEnd), extract);
                throw new InvalidParameterException(message);
            }

            tmpName.setBytes(bytes, nameStart, nameEnd - nameStart);
            if (valueStart >= 0) {
                tmpValue.setBytes(bytes, valueStart, valueEnd - valueStart);
            } else {
                tmpValue.setBytes(bytes, 0, 0);
            }

            // Take copies as if anything goes wrong originals will be
            // corrupted. This means original values can be logged.
            // For performance - only done for debug
            if (log.isDebugEnabled()) {
                try {
                    origName.append(bytes, nameStart, nameEnd - nameStart);
                    if (valueStart >= 0) {
                        origValue.append(bytes, valueStart, valueEnd - valueStart);
                    } else {
                        origValue.append(bytes, 0, 0);
                    }
                } catch (IOException ioe) {
                    // Should never happen...
                    log.error(sm.getString("parameters.copyFail"), ioe);
                }
            }

            try {
                String name;
                ByteChunk value;

                if (decodeName) {
                    urlDecode(tmpName);
                }
                tmpName.setCharset(charset);
                name = tmpName.toString(CodingErrorAction.REPORT, CodingErrorAction.REPORT);

                if (valueStart >= 0) {
                    if (decodeValue) {
                        urlDecode(tmpValue);
                    }
                    tmpValue.setCharset(charset);
                    value = tmpValue.toString(CodingErrorAction.REPORT, CodingErrorAction.REPORT);
                } else {
                    value = new ByteChunk();
                }

                if (limit > -1 && parameterCount >= limit) {
                    // Processing this parameter will push us over the limit.
                    throw new InvalidParameterException(sm.getString("parameters.maxCountFail", Integer.valueOf(limit)));
                }
                parameterCount++;

                ByteChunk valArray[] = new ByteChunk[]{value};
                ByteChunk[] oldVals = (ByteChunk[]) ht.put(name, valArray);
                if (oldVals != null) {
                    valArray = new ByteChunk[oldVals.length + 1];
                    System.arraycopy(oldVals, 0, valArray, 0, oldVals.length);
                    valArray[oldVals.length] = value;
                    ht.put(name, valArray);
                }
            } catch (IOException e) {
                String message;
                if (log.isDebugEnabled()) {
                    message = sm.getString("parameters.decodeFail.debug", origName.toString(), origValue.toString());
                } else {
                    message = sm.getString("parameters.decodeFail.info", tmpName.toString(), tmpValue.toString());
                }
                throw new InvalidParameterException(message, e);
            } finally {
                // Only recycle copies if we used them
                if (log.isDebugEnabled()) {
                    origName.recycle();
                    origValue.recycle();
                }
            }
        }
        return ht;
    }

    private void urlDecode(ByteChunk bc) throws IOException {
        if (urlDec == null) {
            urlDec = new UDecoder();
        }
        urlDec.convert(bc, true);
    }

    public void processParameters(MessageBytes data, Charset charset, boolean queryParams) {
        if (data == null || data.isNull() || data.getLength() <= 0) {
            return;
        }

        if (data.getType() != MessageBytes.T_BYTES) {
            data.toBytes();
        }
        ByteChunk bc = data.getByteChunk();
        processParameters(bc.getBytes(), bc.getStart(), bc.getLength(), charset, queryParams);
    }

    /**
     * Debug purpose
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ArrayList<ByteChunk>> e : paramHashValues.entrySet()) {
            sb.append(e.getKey()).append('=');
            List<ByteChunk> valuesList = e.getValue();
            if (valuesList.size() > 0) {
                String[] arrays = new String[valuesList.size()];
                for (int i = 0; i < valuesList.size(); i++) {
                    arrays[i] = new String(valuesList.get(i).getBytes(), getCharset());
                }
                StringUtils.join(arrays, ',', sb);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    class QSListItem {
        MessageBytes _qs = null;
        Hashtable _qsHashtable = null;
        QSListItem(MessageBytes qs, Hashtable qsHashtable){
            _qs = qs;
            _qsHashtable = qsHashtable;
        }
    }

}
