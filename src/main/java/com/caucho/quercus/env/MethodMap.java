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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */
 
package com.caucho.quercus.env;

import java.util.*;

/**
 * Case-insensitive method mapping
 */
public class MethodMap<V>
{
  private Entry<V> []_entries = new Entry[16];
  private int _size;
    
  public void put(char []buffer, int length, V value)
  {
    if (_entries.length <= _size * 4)
      resize();
    
    int hash = hash(buffer, length);

    char []key = new char[length];
    System.arraycopy(buffer, 0, key, 0, length);

    int bucket = hash & (_entries.length - 1);

    Entry<V> entry;
    for (entry = _entries[bucket]; entry != null; entry = entry._next) {
      if (match(entry._key, key, length)) {
        entry._value = value;

        return;
      }
    }
    
    entry = new Entry<V>(buffer, value);

    entry._next = _entries[bucket];
    _entries[bucket] = entry;
    _size++;

  }

  public boolean containsKey(String key)
  {
    return get(key) != null;
  }

  public V get(int hash, char []buffer, int length)
  {
    int bucket = hash & (_entries.length - 1);

    for (Entry<V> entry = _entries[bucket];
         entry != null;
         entry = entry._next) {
      char []key = entry._key;

      if (match(key, buffer, length))
        return entry._value;
    }

    return null;
  }

  public void put(String keyString, V value)
  {
    char []key = keyString.toCharArray();

    put(key, key.length, value);
  }

  public V get(String keyString)
  {
    char []key = keyString.toCharArray();
    int hash = hash(key, key.length);

    return get(hash, key, key.length);
  }

  public Iterable<V> values()
  {
    return new ValueIterator(_entries);
  }

  private boolean match(char []a, char []b, int length)
  {
    if (a.length != length)
      return false;

    for (int i = length - 1; i >= 0; i--) {
      int chA = a[i];
      int chB = b[i];

      if (chA == chB) {
      }
      /*
      else if ((chA & ~0x20) != (chB & ~0x20))
        return false;
      */
      else {
        if ('A' <= chA && chA <= 'Z')
          chA += 'a' - 'A';
          
        if ('A' <= chB && chB <= 'Z')
          chB += 'a' - 'A';

        if (chA != chB)
          return false;
      }
    }

    return true;
  }

  private void resize()
  {
    Entry<V> []newEntries = new Entry[2 * _entries.length];

    for (int i = 0; i < _entries.length; i++) {
      Entry<V> entry = _entries[i];
      
      while (entry != null) {
        Entry<V> next = entry._next;

        int hash = hash(entry._key, entry._key.length);
        int bucket = hash & (newEntries.length - 1);

        entry._next = newEntries[bucket];
        newEntries[bucket] = entry;
        
        entry = next;
      }
    }

    _entries = newEntries;
  }

  public static int hash(char []buffer, int length)
  {
    int hash = 17;

    for (length--; length >= 0; length--) {
      int ch = buffer[length];

      if ('A' <= ch && ch <= 'Z')
        ch += 'a' - 'A';
      
      hash = 65537 * hash + ch;
    }

    return hash;
  }

  public static int hash(String string)
  {
    int hash = 17;

    int length = string.length();
    for (length--; length >= 0; length--) {
      int ch = string.charAt(length);

      if ('A' <= ch && ch <= 'Z')
        ch += 'a' - 'A';
      
      hash = 65537 * hash + ch;
    }

    return hash;
  }
    

  final static class Entry<V> {
    final char []_key;
    V _value;
    
    Entry<V> _next;

    Entry(char []key, V value)
    {
      _key = key;
      _value = value;
    }
  }

  final static class ValueIterator<V> implements Iterable<V>, Iterator<V>
  {
    int _index;
    Entry<V> []_entries;
    Entry<V> _next;
    
    public ValueIterator(Entry<V> []entries)
    {
      _entries = entries;

      getNext();
    }
    
    private void getNext()
    {
      Entry<V> entry = _next == null ? null : _next._next;

      while (entry == null
             && _index < _entries.length
             && (entry = _entries[_index++]) == null) {
      }

      _next = entry;
    }

    public boolean hasNext()
    {
      return _next != null;
    }
    
    public V next()
    {
      V value = _next._value;
      
      getNext();

      return value;
    }
    
    public Iterator<V> iterator()
    {
      return this;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
