/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

/**
 * Fixed length cache with a LRU replacement policy.  If cache items
 * implement CacheListener, they will be informed when they're removed
 * from the cache.
 *
 * <p>Null keys are not allowed.  LruCache is synchronized.
 */
public class FlatCache<K,V> {
  private Object []_keys;
  private V []_values;

  // maximum allowed entries
  private int _capacity;
  private int _mask;

  private static Object NULL = new Object();

  public FlatCache(int initialCapacity)
  {
    for (_capacity = 32; _capacity < 2 * initialCapacity; _capacity *= 2) {
    }

    _keys = new Object[_capacity];
    _values = (V []) new Object[_capacity];

    _mask = _capacity - 1;
  }

  /**
   * Clears the cache
   */
  public synchronized void clear()
  {
    for (int i = 0; i < _capacity; i++) {
      if (_values[i] instanceof CacheListener)
        ((CacheListener) _values[i]).removeEvent();

      _keys[i] = null;
      _values[i] = null;
    }
  }

  /**
   * Get an item from the cache and make it most recently used.
   *
   * @param key key to lookup the item
   * @return the matching object in the cache
   */
  public Object get(K key)
  {
    Object okey = key;
    if (okey == null)
      okey = NULL;

    int hash = okey.hashCode() & _mask;

    Object testKey = _keys[hash];

    if (testKey != null && testKey.equals(okey) && testKey == _keys[hash])
      return _values[hash];
    else
      return null;
  }

  /**
   * Puts a new item in the cache.  If the cache is full, remove the
   * LRU item.
   *
   * @param key key to store data
   * @param value value to be stored
   *
   * @return old value stored under the key
   */
  public synchronized V put(K key, V value)
  {
    Object okey = key;
    if (okey == null)
      okey = NULL;

    int hash = okey.hashCode() & _mask;

    V old = _values[hash];

    _keys[hash] = null;
    _values[hash] = value;
    _keys[hash] = okey;

    if (old instanceof CacheListener)
      ((CacheListener) old).removeEvent();

    return old;
  }

  /**
   * Removes an item from the cache
   *
   * @param key the key to remove
   *
   * @return the value removed
   */
  public V remove(K key)
  {
    return put(key, null);
  }
}
