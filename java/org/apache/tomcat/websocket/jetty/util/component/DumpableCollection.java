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

package org.apache.tomcat.websocket.jetty.util.component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class DumpableCollection implements Dumpable
{
    private final String _name;
    private final Collection<?> _collection;

    public DumpableCollection(String name, Collection<?> collection)
    {
        _name = name;
        _collection = collection;
    }

    public static DumpableCollection fromArray(String name, Object[] array)
    {
        return new DumpableCollection(name, array == null ? Collections.emptyList() : Arrays.asList(array));
    }

    public static DumpableCollection from(String name, Object... items)
    {
        return new DumpableCollection(name, items == null ? Collections.emptyList() : Arrays.asList(items));
    }

    @Override
    public String dump()
    {
        return Dumpable.dump(this);
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException
    {
        Object[] array = (_collection == null ? null : _collection.toArray());
        Dumpable.dumpObjects(out, indent, _name + " size=" + (array == null ? 0 : array.length), array);
    }
}
