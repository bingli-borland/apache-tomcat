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

import java.util.Collection;

/**
 * A Container
 */
public interface Container
{
    /**
     * Add a bean.  If the bean is-a {@link Listener}, then also do an implicit {@link #addEventListener(Listener)}.
     *
     * @param o the bean object to add
     * @return true if the bean was added, false if it was already present
     */
    boolean addBean(Object o);

    /**
     * @return the collection of beans known to this aggregate, in the order they were added.
     * @see #getBean(Class)
     */
    Collection<Object> getBeans();

    /**
     * @param clazz the class of the beans
     * @param <T> the Bean type
     * @return a list of beans of the given class (or subclass), in the order they were added.
     * @see #getBeans()
     * @see #getContainedBeans(Class)
     */
    <T> Collection<T> getBeans(Class<T> clazz);

    /**
     * @param clazz the class of the bean
     * @param <T> the Bean type
     * @return the first bean (in order added) of a specific class (or subclass), or null if no such bean exist
     */
    <T> T getBean(Class<T> clazz);

    /**
     * Removes the given bean.
     * If the bean is-a {@link Listener}, then also do an implicit {@link #removeEventListener(Listener)}.
     *
     * @param o the bean to remove
     * @return whether the bean was removed
     */
    boolean removeBean(Object o);

    /**
     * Add an event listener.
     *
     * @param listener the listener to add
     * @see Container#addBean(Object)
     */
    void addEventListener(Listener listener);

    /**
     * Remove an event listener.
     *
     * @param listener the listener to remove
     * @see Container#removeBean(Object)
     */
    void removeEventListener(Listener listener);

    /**
     * Unmanages a bean already contained by this aggregate, so that it is not started/stopped/destroyed with this
     * aggregate.
     *
     * @param bean The bean to unmanage (must already have been added).
     */
    void unmanage(Object bean);

    /**
     * Manages a bean already contained by this aggregate, so that it is started/stopped/destroyed with this
     * aggregate.
     *
     * @param bean The bean to manage (must already have been added).
     */
    void manage(Object bean);

    /**
     * Test if this container manages a bean
     *
     * @param bean the bean to test
     * @return whether this aggregate contains and manages the bean
     */
    boolean isManaged(Object bean);

    /**
     * Adds the given bean, explicitly managing it or not.
     *
     * @param o The bean object to add
     * @param managed whether to managed the lifecycle of the bean
     * @return true if the bean was added, false if it was already present
     */
    boolean addBean(Object o, boolean managed);

    /**
     * A listener for Container events.
     * If an added bean implements this interface it will receive the events
     * for this container.
     */
    interface Listener
    {
        void beanAdded(Container parent, Object child);

        void beanRemoved(Container parent, Object child);
    }

    /**
     * Inherited Listener.
     * If an added bean implements this interface, then it will
     * be added to all contained beans that are themselves Containers
     */
    interface InheritedListener extends Listener
    {
    }

    /**
     * @param clazz the class of the beans
     * @param <T> the Bean type
     * @return the list of beans of the given class from the entire Container hierarchy. The order is primarily depth first
     *         and secondarily added order.
     */
    <T> Collection<T> getContainedBeans(Class<T> clazz);
}
