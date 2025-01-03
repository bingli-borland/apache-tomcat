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
import org.apache.tomcat.util.res.StringManager;

public class Parameters {

    protected static final Log log = LogFactory.getLog(Parameters.class);

    protected static final StringManager sm = StringManager.getManager("org.apache.tomcat.util.http");

    protected boolean didQueryParameters = false;

    protected MessageBytes queryMB;

    private UDecoder urlDec;
    protected final MessageBytes decodedQuery = MessageBytes.newInstance();

    protected Charset charset = StandardCharsets.ISO_8859_1;
    protected Charset queryStringCharset = StandardCharsets.UTF_8;

    protected int limit = -1;
    protected int parameterCount = 0;

    protected UnsynchronizedStack paramStack = new UnsynchronizedStack();

    protected Map parameters = Globals.PARSE_DISPATCH_QUERY_PARAM ? null : new LinkedHashMap();
    protected LinkedList _queryStringList = null;

    protected UnsynchronizedStack _paramStack = new UnsynchronizedStack();

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
        didQueryParameters = false;
        charset = DEFAULT_BODY_CHARSET;
        decodedQuery.recycle();
        paramStack.clear();
        if (Globals.PARSE_DISPATCH_QUERY_PARAM) {
            parameters = null;
        } else if (parameters != null) {
            parameters.clear();
        }
        _queryStringList = null;
        if (!_paramStack.isEmpty()) {
            _paramStack.clear();
        }
    }


    // -------------------- Data access --------------------
    // Access to the current name/values, no side effect ( processing ).
    // You must explicitly call handleQueryParameters and the post methods.

    public String[] getParameterValues(String name) {
        if (!Globals.PARSE_DISPATCH_QUERY_PARAM) {
            handleQueryParameters();
        }
        // no "facade"
        String[] values = (String[]) getParameterMap().get(name);
        return values;
    }

    public Enumeration<String> getParameterNames() {
        if (!Globals.PARSE_DISPATCH_QUERY_PARAM) {
            handleQueryParameters();
        }
        return Collections.enumeration(getParameterMap().keySet());
    }

    public String getParameter(String name) {
        if (!Globals.PARSE_DISPATCH_QUERY_PARAM) {
            handleQueryParameters();
        }
        String[] values = (String[]) getParameterMap().get(name);
        if (values != null) {
            if (values.length == 0) {
                return "";
            }
            return values[0];
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
        } catch (IOException e) {
            // Can't happen, as decodedQuery can't overflow
            log.error(sm.getString("parameters.copyFail"), e);
        }
        processParameters(decodedQuery, queryStringCharset);
    }


    public void addParameter(String key, String value) throws IllegalStateException {

        if (key == null) {
            return;
        }

        if (limit > -1 && parameterCount >= limit) {
            // Processing this parameter will push us over the limit.
            throw new InvalidParameterException(sm.getString("parameters.maxCountFail", Integer.valueOf(limit)));
        }
        parameterCount++;

        String[] values = null;
        String[] oldValues = (String[]) this.getParameterMap().get(key);
        if (oldValues == null) {
            values = new String[1];
            values[0] = value;
        } else {
            values = new String[oldValues.length + 1];
            for (int i = 0; i < oldValues.length; i++) {
                values[i] = oldValues[i];
            }
            values[oldValues.length] = value;
        }
        this.getParameterMap().put(key, values);
    }

    public void setURLDecoder(UDecoder u) {
        urlDec = u;
    }

    public Map getParameterMap() {
        return parameters;
    }

    public void setParameters(Map parameters) {
        this.parameters = parameters;
    }

    public Map getParamHashValues() {
        return parameters;
    }

    /**
     * Save the state of the parameters before a call to include or forward.
     */
    public void pushParameterStack() {
        if (getParameterMap() == null) {
            _paramStack.push(null);
        } else {
            paramStack.push(((LinkedHashMap) getParameterMap()).clone());
        }
    }

    /**
     * Revert the state of the parameters which was saved before an include call
     *
     */
    public void popParameterStack() {
        try {
            setParameters((LinkedHashMap) paramStack.pop());
        } catch (java.util.EmptyStackException empty) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to remove item from stack", empty);
            }
        }
    }

    public void aggregateQueryStringParams(String additionalQueryString, boolean setQS) {
        QSListItem tmpQS = null;
        if (getParameterMap() == null) {
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
        if (getParameterMap() != null && additionalQueryString != null) {
            MessageBytes qs = MessageBytes.newInstance();
            qs.setString(additionalQueryString);
            if (qs.getType() != MessageBytes.T_BYTES) {
                qs.toBytes();
            }
            ByteChunk bc = qs.getByteChunk();
            LinkedHashMap<String, String[]> parameters = parseQueryStringParameters(bc.getBytes(), bc.getOffset(), bc.getLength(), queryStringCharset);
            // end 249841, 256836
            String[] valArray;
            Iterator<Map.Entry<String, String[]>> iterator = parameters.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String[]> entry = iterator.next();
                String key = entry.getKey();
                String[] newVals = entry.getValue();

                // Check to see if a parameter with the key already exists
                // and prepend the values since QueryString takes precedence
                if (getParameterMap().containsKey(key)) {
                    String[] oldVals = (String[]) getParameterMap().get(key);
                    Vector v = new Vector();

                    for (int i = 0; i < newVals.length; i++) {
                        v.add(newVals[i]);
                    }

                    for (int i = 0; i < oldVals.length; i++) {
                        v.add(oldVals[i]);
                    }

                    valArray = new String[v.size()];
                    v.toArray(valArray);

                    getParameterMap().put(key, valArray);
                } else {
                    getParameterMap().put(key, newVals);
                }
            }
        }
    }

    public void parseQueryStringList() {

        LinkedHashMap<String, String[]> tmpQueryParams = null;
        LinkedList queryStringList = _queryStringList;
        if (queryStringList == null || queryStringList.isEmpty()) { //258025
            if (queryMB != null && !queryMB.isNull())//PM35450
            {
                try {
                    decodedQuery.duplicate(queryMB);
                } catch (IOException e) {
                    // Can't happen, as decodedQuery can't overflow
                    log.error(sm.getString("parameters.copyFail"), e);
                }
                if (decodedQuery.getType() != MessageBytes.T_BYTES) {
                    decodedQuery.toBytes();
                }
                ByteChunk bc = decodedQuery.getByteChunk();
                if (getParameterMap() == null || getParameterMap().isEmpty()) {
                    setParameters(parseQueryStringParameters(bc.getBytes(), bc.getOffset(), bc.getLength(), queryStringCharset));
                } else {
                    tmpQueryParams = parseQueryStringParameters(bc.getBytes(), bc.getOffset(), bc.getLength(), queryStringCharset);
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
                if (qsListItem._qsHashMap != null)
                    mergeQueryParams(qsListItem._qsHashMap);
                else if (queryString != null && !queryString.isNull()) {
                    if (queryString.getType() != MessageBytes.T_BYTES) {
                        queryString.toBytes();
                    }
                    ByteChunk bc = queryString.getByteChunk();
                    if (getParameterMap() == null || getParameterMap().isEmpty())// 258025
                    {
                        qsListItem._qsHashMap = parseQueryStringParameters(bc.getBytes(), bc.getOffset(), bc.getLength(), queryStringCharset);
                        setParameters(qsListItem._qsHashMap);
                        qsListItem._qs = null;
                    } else {
                        tmpQueryParams = parseQueryStringParameters(bc.getBytes(), bc.getOffset(), bc.getLength(), queryStringCharset);
                        qsListItem._qsHashMap = tmpQueryParams;
                        qsListItem._qs = null;
                        mergeQueryParams(tmpQueryParams);
                    }
                }
            }
        }
    }

    private void mergeQueryParams(Map<String, String[]> tmpQueryParams) {
        if (tmpQueryParams != null) {
            Iterator<Map.Entry<String, String[]>> iterator = tmpQueryParams.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String[]> entry = iterator.next();
                String key = entry.getKey();
                // Check for QueryString parms with the same name
                // pre-append to postdata values if necessary
                if (getParameterMap() != null && getParameterMap().containsKey(key)) {
                    String postVals[] = (String[]) getParameterMap().get(key);
                    String queryVals[] = (String[]) tmpQueryParams.get(key);
                    String newVals[] = new String[postVals.length + queryVals.length];
                    int newValsIndex = 0;
                    for (int i = 0; i < queryVals.length; i++) {
                        newVals[newValsIndex++] = queryVals[i];
                    }
                    for (int i = 0; i < postVals.length; i++) {
                        newVals[newValsIndex++] = postVals[i];
                    }
                    getParameterMap().put(key, newVals);
                } else {
                    if (getParameterMap() == null) {
                        setParameters(new LinkedHashMap<>());
                    }
                    getParameterMap().put(key, tmpQueryParams.get(key));
                }
            }
        }
    }


    public void removeQSFromList() {

        LinkedList queryStringList = _queryStringList;
        if (queryStringList != null && !queryStringList.isEmpty()) {
            Map _tmpParameters = getParameterMap();    // Save off reference to current parameters
            popParameterStack();
            if (getParameterMap() == null && _tmpParameters != null) // Parameters above current inluce/forward were never parsed
            {
                setParameters(_tmpParameters);
                LinkedHashMap tmpQueryParams = ((QSListItem) queryStringList.getLast())._qsHashMap;
                if (tmpQueryParams == null) {
                    MessageBytes qs = ((QSListItem) queryStringList.getLast())._qs;
                    if (qs.getType() != MessageBytes.T_BYTES) {
                        qs.toBytes();
                    }
                    ByteChunk bc = qs.getByteChunk();
                    tmpQueryParams = parseQueryStringParameters(bc.getBytes(), bc.getOffset(), bc.getLength(), queryStringCharset);
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

    public void removeQueryParams(Map tmpQueryParams) {
        if (tmpQueryParams != null) {
            Iterator<Map.Entry<String, String[]>> iterator = tmpQueryParams.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String[]> entry = iterator.next();
                String key = entry.getKey();
                // Check for QueryString parms with the same name
                // pre-append to postdata values if necessary
                if (getParameterMap().containsKey(key)) {
                    String postVals[] = (String[]) getParameterMap().get(key);
                    String queryVals[] = (String[]) tmpQueryParams.get(key);
                    if (postVals.length - queryVals.length > 0) {
                        String newVals[] = new String[postVals.length - queryVals.length];
                        int newValsIndex = 0;
                        for (int i = queryVals.length; i < postVals.length; i++) {
                            newVals[newValsIndex++] = postVals[i];
                        }
                        getParameterMap().put(key, newVals);
                    } else if (tmpQueryParams == getParameterMap()) {
                        iterator.remove();
                    } else {
                        getParameterMap().remove(key);
                    }
                }
            }
        }
    }


    // -------------------- Parameter parsing --------------------
    // we are called from a single thread - we can do it the hard way
    // if needed
    private final ByteChunk tmpName = new ByteChunk();
    private final ByteChunk tmpValue = new ByteChunk();
    protected final ByteChunk origName = new ByteChunk();
    protected final ByteChunk origValue = new ByteChunk();
    protected static final Charset DEFAULT_BODY_CHARSET = StandardCharsets.ISO_8859_1;
    private static final Charset DEFAULT_URI_CHARSET = StandardCharsets.UTF_8;


    public void processParameters(byte bytes[], int start, int len) {
        processParameters(bytes, start, len, charset);
    }

    private void processParameters(byte bytes[], int start, int len, Charset charset) {

        if (log.isTraceEnabled()) {
            log.trace(sm.getString("parameters.bytes", new String(bytes, start, len, DEFAULT_BODY_CHARSET)));
        }

        int pos = start;
        int end = start + len;

        while (pos < end) {
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
                String value;

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
                    value = "";
                }

                addParameter(name, value);
            } catch (IOException e) {
                String message;
                if (log.isDebugEnabled()) {
                    message = sm.getString("parameters.decodeFail.debug", origName.toString(), origValue.toString());
                } else {
                    message = sm.getString("parameters.decodeFail.info", tmpName.toString(), tmpValue.toString());
                }
                throw new InvalidParameterException(message, e);
            } finally {
                tmpName.recycle();
                tmpValue.recycle();
                // Only recycle copies if we used them
                if (log.isDebugEnabled()) {
                    origName.recycle();
                    origValue.recycle();
                }
            }
        }
    }

    public LinkedHashMap parsePostParameters(byte bytes[], int start, int len) {
        return parseQueryStringParameters(bytes, start, len, charset);
    }

    private LinkedHashMap<String, String[]> parseQueryStringParameters(byte bytes[], int start, int len, Charset charset) {

        LinkedHashMap<String, String[]> ht = new LinkedHashMap<>();
        if (log.isTraceEnabled()) {
            log.trace(sm.getString("parameters.bytes", new String(bytes, start, len, DEFAULT_BODY_CHARSET)));
        }

        int pos = start;
        int end = start + len;

        while (pos < end) {
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
                String value;

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
                    value = "";
                }

                if (limit > -1 && parameterCount >= limit) {
                    // Processing this parameter will push us over the limit.
                    throw new InvalidParameterException(sm.getString("parameters.maxCountFail", Integer.valueOf(limit)));
                }
                parameterCount++;

                String valArray[] = new String[]{value};
                String[] oldVals = (String[]) ht.put(name, valArray);
                if (oldVals != null) {
                    valArray = new String[oldVals.length + 1];
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
                tmpName.recycle();
                tmpValue.recycle();
                // Only recycle copies if we used them
                if (log.isDebugEnabled()) {
                    origName.recycle();
                    origValue.recycle();
                }
            }
        }
        return ht;
    }

    public void urlDecode(ByteChunk bc) throws IOException {
        if (urlDec == null) {
            urlDec = new UDecoder();
        }
        urlDec.convert(bc, true);
    }

    public void processParameters(MessageBytes data, Charset charset) {
        if (data == null || data.isNull() || data.getLength() <= 0) {
            return;
        }

        if (data.getType() != MessageBytes.T_BYTES) {
            data.toBytes();
        }
        ByteChunk bc = data.getByteChunk();
        processParameters(bc.getBytes(), bc.getStart(), bc.getLength(), charset);
    }

    /**
     * Debug purpose
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Object key : getParameterMap().keySet()) {
            Map.Entry<String, String[]> e = (Map.Entry<String, String[]>) getParameterMap().get(key);
            sb.append(e.getKey()).append('=');
            StringUtils.join(e.getValue(), ',', sb);
            sb.append('\n');
        }
        return sb.toString();
    }

    class QSListItem {
        MessageBytes _qs = null;
        LinkedHashMap _qsHashMap = null;

        QSListItem(MessageBytes qs, LinkedHashMap qsHashMap) {
            _qs = qs;
            _qsHashMap = qsHashMap;
        }
    }

}
