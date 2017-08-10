/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.demo.grid;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * {@code KeyMapper} is the simple two-way map for generating textual keys for
 * objects and retrieving the objects later with the key.
 *
 * @author Vaadin Ltd.
 */
public class KeyMapper<V> implements Serializable {

    private int lastKey = 0;

    private final HashMap<Object, String> objectIdKeyMap = new HashMap<>();

    private final HashMap<String, V> keyObjectMap = new HashMap<>();

    private Function<V, Object> identifierGetter;

    /**
     * Constructs a new mapper
     *
     * @param identifierGetter
     *            has to return a unique key for every bean, and the returned
     *            key has to follow general {@code hashCode()} and
     *            {@code equals()} contract, see {@link Object#hashCode()} for
     *            details.
     */
    public KeyMapper(Function<V, Object> identifierGetter) {
        this.identifierGetter = identifierGetter;
    }

    /**
     * Constructs a new mapper with trivial {@code identifierGetter}
     */
    public KeyMapper() {
        this(v -> v);
    }

    /**
     * Gets key for an object.
     *
     * @param o
     *            the object.
     */
    public String key(V o) {

        if (o == null) {
            return "null";
        }

        // If the object is already mapped, use existing key
        Object id = identifierGetter.apply(o);
        String key = objectIdKeyMap.get(id);
        if (key != null) {
            return key;
        }

        // If the object is not yet mapped, map it
        key = String.valueOf(++lastKey);
        objectIdKeyMap.put(id, key);
        keyObjectMap.put(key, o);

        return key;
    }

    public boolean has(V o) {
        return objectIdKeyMap.containsKey(identifierGetter.apply(o));
    }

    /**
     * Retrieves object with the key.
     *
     * @param key
     *            the name with the desired value.
     * @return the object with the key.
     */
    public V get(String key) {
        return keyObjectMap.get(key);
    }

    /**
     * Removes object from the mapper.
     *
     * @param removeobj
     *            the object to be removed.
     */
    public void remove(V removeobj) {
        final String key = objectIdKeyMap
                .remove(identifierGetter.apply(removeobj));
        if (key != null) {
            keyObjectMap.remove(key);
        }
    }

    /**
     * Removes all objects from the mapper.
     */
    public void removeAll() {
        objectIdKeyMap.clear();
        keyObjectMap.clear();
    }

    /**
     * Checks if the given key is mapped to an object.
     *
     * @param key
     *            the key to check
     * @return <code>true</code> if the key is currently mapped,
     *         <code>false</code> otherwise
     */
    public boolean containsKey(String key) {
        return keyObjectMap.containsKey(key);
    }

    public void refresh(V dataObject) {
        Object id = identifierGetter.apply(dataObject);
        String key = objectIdKeyMap.get(id);
        if (key != null) {
            keyObjectMap.put(key, dataObject);
        }
    }

    public void setIdentifierGetter(Function<V, Object> identifierGetter) {
        if (this.identifierGetter != identifierGetter) {
            this.identifierGetter = identifierGetter;
            objectIdKeyMap.clear();
            for (Map.Entry<String, V> entry : keyObjectMap.entrySet()) {
                objectIdKeyMap.put(identifierGetter.apply(entry.getValue()),
                        entry.getKey());
            }
        }
    }
}
