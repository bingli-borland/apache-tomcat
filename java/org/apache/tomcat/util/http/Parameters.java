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

    protected final Map<String, String[]> paramHashValues = new LinkedHashMap<>();
    protected boolean didQueryParameters = false;

    protected MessageBytes queryMB;

    private UDecoder urlDec;
    protected final MessageBytes decodedQuery = MessageBytes.newInstance();

    protected Charset charset = StandardCharsets.ISO_8859_1;
    protected Charset queryStringCharset = StandardCharsets.UTF_8;

    protected int limit = -1;
    protected int parameterCount = 0;

    protected UnsynchronizedStack paramStack = new UnsynchronizedStack();

    protected Map _parameters = null;
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

    public String[] getParameterValues(String name) {
        if (Globals.COMPATIBLEWEBSPHERE) {
            return (String[]) getParameters().get(name);
        }
        handleQueryParameters();
        // no "facade"
        String[] values = paramHashValues.get(name);
        return values;
        }

    public Enumeration<String> getParameterNames() {
        if (Globals.COMPATIBLEWEBSPHERE) {
            return ((Hashtable) getParameters()).keys();
        }
        handleQueryParameters();
        return Collections.enumeration(paramHashValues.keySet());
    }

    public String getParameter(String name) {
        if (Globals.COMPATIBLEWEBSPHERE) {
            String[] values = (String[]) getParameters().get(name);
            String value = null;
            if (values != null && values.length > 0) {
                value = values[0];
            }
            return value;
        }
        handleQueryParameters();
        String[] values = paramHashValues.get(name);
        if (values != null) {
            if (values.isEmpty()) {
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
        } catch (IOException ioe) {
            // Can't happen, as decodedQuery can't overflow
            log.error(sm.getString("parameters.copyFail"), ioe);
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
        String[] oldValues = (String[]) this.paramHashValues.get(key);
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
        this.paramHashValues.put(key, values);
    }

    public void setURLDecoder(UDecoder u) {
        urlDec = u;
    }

    public Map getParameters(){
        return _parameters;
    }

    public void setParameters(Hashtable parameters) {
        _parameters = parameters;
    }

    public Map getParamHashValues() {
        return paramHashValues;
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
            Hashtable<String, String[]> parameters = parseQueryStringParameters(bc.getBytes(), bc.getOffset(), bc.getLength(), queryStringCharset);
            // end 249841, 256836
            String[] valArray;
            for (Enumeration e = parameters.keys(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                String[] newVals = (String[]) parameters.get(key);

                // Check to see if a parameter with the key already exists
                // and prepend the values since QueryString takes precedence
                if (getParameters().containsKey(key)) {
                    String[] oldVals = (String[]) getParameters().get(key);
                    Vector v = new Vector();

                    for (int i = 0; i < newVals.length; i++) {
                        v.add(newVals[i]);
                    }

                    for (int i = 0; i < oldVals.length; i++) {
                        v.add(oldVals[i]);
                    }

                    valArray = new String[v.size()];
                    v.toArray(valArray);

                    getParameters().put(key, valArray);
                } else {
                    getParameters().put(key, newVals);
                }
            }
        }
    }

    public void parseQueryStringList() {

        Hashtable<String, String[]> tmpQueryParams = null;
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
                if (getParameters() == null || getParameters().isEmpty()) {
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
                if (qsListItem._qsHashtable != null)
                    mergeQueryParams(qsListItem._qsHashtable);
                else if (queryString != null && !queryString.isNull()) {
                    if (queryString.getType() != MessageBytes.T_BYTES) {
                        queryString.toBytes();
                    }
                    ByteChunk bc = queryString.getByteChunk();
                    if (getParameters() == null || getParameters().isEmpty())// 258025
                    {
                        qsListItem._qsHashtable = parseQueryStringParameters(bc.getBytes(), bc.getOffset(), bc.getLength(), queryStringCharset);
                        setParameters(qsListItem._qsHashtable);
                        qsListItem._qs = null;
                    } else {
                        tmpQueryParams = parseQueryStringParameters(bc.getBytes(), bc.getOffset(), bc.getLength(), queryStringCharset);
                        qsListItem._qsHashtable = tmpQueryParams;
                        qsListItem._qs = null;
                        mergeQueryParams(tmpQueryParams);
                    }
                }
            }
        }
    }

    private void mergeQueryParams(Hashtable<String, String[]> tmpQueryParams) {
        if (tmpQueryParams != null) {
            Enumeration enumeration = tmpQueryParams.keys();
            while (enumeration.hasMoreElements()) {
                Object key = enumeration.nextElement();
                // Check for QueryString parms with the same name
                // pre-append to postdata values if necessary
                if (getParameters() != null && getParameters().containsKey(key)) {
                    String postVals[] = (String[]) getParameters().get(key);
                    String queryVals[] = (String[]) tmpQueryParams.get(key);
                    String newVals[] = new String[postVals.length + queryVals.length];
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
                setParameters((Hashtable) _tmpParameters);
                Hashtable tmpQueryParams = ((QSListItem) queryStringList.getLast())._qsHashtable;
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

    public void removeQueryParams(Hashtable tmpQueryParams) {
        if (tmpQueryParams != null) {
            Enumeration enumeration = tmpQueryParams.keys();
            while (enumeration.hasMoreElements()) {
                Object key = enumeration.nextElement();
                // Check for QueryString parms with the same name
                // pre-append to postdata values if necessary
                if (getParameters().containsKey(key)) {
                    String postVals[] = (String[]) getParameters().get(key);
                    String queryVals[] = (String[]) tmpQueryParams.get(key);
                    if (postVals.length - queryVals.length > 0) {
                        String newVals[] = new String[postVals.length - queryVals.length];
                        int newValsIndex = 0;
                        for (int i = queryVals.length; i < postVals.length; i++) {
                            newVals[newValsIndex++] = postVals[i];
                        }
                        getParameters().put(key, newVals);
                    } else {
                        getParameters().remove(key);
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
            } catch (IOException ioe) {
                String message;
                if (log.isDebugEnabled()) {
                    message = sm.getString("parameters.decodeFail.debug", origName.toString(), origValue.toString());
                } else {
                    message = sm.getString("parameters.decodeFail.info", tmpName.toString(), tmpValue.toString());
                }
                throw new InvalidParameterException(message, ioe);
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

    public Hashtable parsePostParameters(byte bytes[], int start, int len) {
        return parseQueryStringParameters(bytes, start, len, charset);
    }

    private Hashtable parseQueryStringParameters(byte bytes[], int start, int len, Charset charset) {

        Hashtable<String, String[]> ht = new Hashtable<>();
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
        for (Map.Entry<String, String[]> e : paramHashValues.entrySet()) {
            sb.append(e.getKey()).append('=');
            StringUtils.join(e.getValue(), ',', sb);
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
