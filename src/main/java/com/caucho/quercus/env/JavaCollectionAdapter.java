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

import com.caucho.quercus.program.JavaClassDef;

import java.util.*;

/**
 * Represents a marshalled Collection argument.
 */
public class JavaCollectionAdapter<V> extends JavaAdapter<Collection<V>>
{
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public JavaCollectionAdapter(Env env, Collection<V> coll, JavaClassDef def)
  {
    super(env, coll, def);
  }

  /**
   * Clears the array
   */
  @Override
  public void clear()
  {
    _object.clear();
  }

  //
  // Conversions
  //

  /**
   * Copy for assignment.
   */
  @Override
  public Value copy()
  {
    return new JavaCollectionAdapter<V>(getEnv(), _object, getClassDef());
  }

  /**
   * Copy for serialization
   */
  @Override
  public Value copy(Env env, IdentityHashMap<Value,Value> map)
  {
    return new JavaCollectionAdapter<V>(env, _object, getClassDef());
  }

  /**
   * Returns the size.
   */
  @Override
  public int getSize()
  {
    return _object.size();
  }

  /**
   * Creatse a tail index.
   */
  @Override
  public Value createTailKey()
  {
    return LongValue.create(getSize());
  }

  @Override
  @SuppressWarnings("unchecked")
  public Value putImpl(Value key, Value value)
  {
    if (key.toInt() != getSize())
      throw new UnsupportedOperationException("random assignment into Collection");
    
    _object.add((V)value.toJavaObject());
    
    return value;
  }

  /**
   * Gets a new value.
   */
  @Override
  public Value get(Value key)
  {
    int pos = key.toInt();
    
    if (pos < 0)
      return UnsetValue.UNSET;
    
    for (Object obj : _object) {
      if (pos-- > 0)
        continue;
      
      return wrapJava(obj);
    }
    
    return UnsetValue.UNSET;
  }
  
  /**
   * Removes a value.
   */
  @Override
  public Value remove(Value key)
  { 
    int pos = key.toInt();
    
    if (pos < 0)
      return UnsetValue.UNSET;
    
    for (Object obj : _object) {
      if (pos-- > 0)
        continue;
      
      Value val = wrapJava(obj);
      
      _object.remove(obj);
      return val;
    }

    return UnsetValue.UNSET;
  }
  
  /**
   * Returns a set of all the of the entries.
   */
  @Override
  public Set<Map.Entry<Value,Value>> entrySet()
  {
    return new CollectionValueSet();
  }

  /**
   * Returns a collection of the values.
   */
  @Override
  @SuppressWarnings("unchecked")
  public Set<Map.Entry> objectEntrySet()
  {
    return (Set<Map.Entry>)new CollectionSet();
  }
  
  /**
   * Returns a collection of the values.
   */
  @Override
  public Collection<Value> values()
  {
    return new ValueCollection();
  }

  @Override
  public Iterator<Map.Entry<Value,Value>> getIterator(Env env)
  {
    return new CollectionValueIterator();
  }

  @Override
  public Iterator<Value> getKeyIterator(Env env)
  {
    return new KeyIterator();
  }

  @Override
  public Iterator<Value> getValueIterator(Env env)
  {
    return new ValueIterator();
  }

  public class CollectionSet
    extends AbstractSet<Map.Entry<Integer,V>>
  {
    CollectionSet()
    {
    }

    @Override
    public int size()
    {
      return getSize();
    }

    @Override
    public Iterator<Map.Entry<Integer,V>> iterator()
    {
      return new CollectionIterator();
    }
  }
  
  public class CollectionIterator
    implements Iterator<Map.Entry<Integer,V>>
  {
    private int _index;
    private Iterator<V> _iterator;

    public CollectionIterator()
    {
      _index = 0;
      _iterator = _object.iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public Map.Entry<Integer,V> next()
    {
      return new CollectionEntry<V>(_index++, _iterator.next());
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public static class CollectionEntry<V>
    implements Map.Entry<Integer,V>
  {
    private final int _key;
    private V _value;

    public CollectionEntry(int key, V value)
    {
      _key = key;
      _value = value;
    }

    public Integer getKey()
    {
      return _key;
    }

    public V getValue()
    {
      return _value;
    }

    public V setValue(V value)
    {
      V oldValue = _value;

      _value = value;

      return oldValue;
    }
  }

  public class CollectionValueSet
    extends AbstractSet<Map.Entry<Value,Value>>
  {
    CollectionValueSet()
    {
    }

    @Override
    public int size()
    {
      return getSize();
    }

    @Override
    public Iterator<Map.Entry<Value,Value>> iterator()
    {
      return new CollectionValueIterator();
    }
  }

  public class CollectionValueIterator
    implements Iterator<Map.Entry<Value,Value>>
  {
    private int _index;
    private Iterator _iterator;

    public CollectionValueIterator()
    {
      _index = 0;
      _iterator = _object.iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public Map.Entry<Value,Value> next()
    {
       Value val = wrapJava(_iterator.next());

       return new ArrayValue.Entry(LongValue.create(_index++), val);
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  public class ValueCollection
    extends AbstractCollection<Value>
  {
    ValueCollection()
    {
    }

    @Override
    public int size()
    {
      return getSize();
    }

    @Override
    public Iterator<Value> iterator()
    {
      return new ValueIterator();
    }
  }

  public class KeyIterator
    implements Iterator<Value>
  {
    private int _index;
    private Iterator _iterator;

    public KeyIterator()
    {
      _index = 0;
      _iterator = _object.iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public Value next()
    {
      _iterator.next();

      return LongValue.create(_index++);
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public class ValueIterator
    implements Iterator<Value>
  {
    private Iterator _iterator;

    public ValueIterator()
    {
      _iterator = _object.iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public Value next()
    {
      return wrapJava(_iterator.next());
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

}
