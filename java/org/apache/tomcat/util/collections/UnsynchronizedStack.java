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

package org.apache.tomcat.util.collections;

import java.util.LinkedList;
import java.util.NoSuchElementException;

public class UnsynchronizedStack <E> extends LinkedList<E>  {
    private static final long serialVersionUID = 3257562923390809657L;
    public UnsynchronizedStack() {
        super();
    }
    public E peek(){
        try{
            return this.getLast();
        }catch (NoSuchElementException e){
            return null;
        }
    }
    public E pop(){
        try{
            return this.removeLast();
        }catch (NoSuchElementException e){
            return null;
        }
    }

    public void push(E obj){
        this.add(obj);
    }
}
