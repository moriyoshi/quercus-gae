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

import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.program.JavaClassDef;

import java.util.*;

/**
 * Represents a marshalled Map argument.
 */
public class JavaMapAdapter<K,V>
  extends JavaAdapter<Map<K,V>>
{
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private Map<K,V> _map;
  
  private long _nextAvailableIndex;

  public JavaMapAdapter(Env env, Map<K,V> map)
  {
    this(env, map, env.getJavaClassDefinition(map.getClass()));
  }
  
  public JavaMapAdapter(Env env, Map<K,V> map, JavaClassDef def)
  {
    super(env, map, def);
    _map = map;
    
    updateNextAvailableIndex();
  }

  /**
   * Clears the array
   */
  @Override
  public void clear()
  {
    _map.clear();
    
    _nextAvailableIndex = 0;
  }

  public int size()
  {
    return _map.size();
  }

  /**
   * Converts to a java object.
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> T toJavaObject(Env env, Class<T> type)
  {
    if (type.isAssignableFrom(_map.getClass())) {
      return (T)_map;
    }
    else {
      env.warning(L.l("Can't assign {0} to {1}",
              _map.getClass().getName(), type.getName()));
    
      return null;
    }
  }

  /**
   * Copy for assignment.
   */
  @Override
  public Value copy()
  {
    try {
      return new JavaMapAdapter<K,V>(getEnv(), _map, getClassDef());
    }
    catch (Exception e) {
      throw new QuercusRuntimeException(e);
    }
  }

  /**
   * Copy for serialization
   */
  @Override
  public Value copy(Env env, IdentityHashMap<Value,Value> map)
  {
    return new JavaMapAdapter<K,V>(env, _map, getClassDef());
  }
  
  /**
   * Returns the size.
   */
  @Override
  public int getSize()
  {
    return size();
  }

  /**
   * Gets a new value.
   */
  @Override
  public Value get(Value key)
  {
    Object obj = _map.get(key.toJavaObject());
    
    if (obj != null)
      return wrapJava(_map.get(key.toJavaObject()));
    else
      return UnsetValue.UNSET;
  }
  
  /**
   * Removes a value.
   */
  @Override
  public Value remove(Value key)
  {
    updateNextAvailableIndex();

    if (key.isLongConvertible() || key instanceof BooleanValue) {
    //if (key instanceof LongValue) {
      long pos = key.toLong();
      
      Object value = _map.remove(Long.valueOf(pos));
      
      if (value != null) {
        if (pos + 1 == _nextAvailableIndex)
          updateNextAvailableIndex();
        
        return wrapJava(value);
      }
    }
    else {
      Object value = _map.remove(key.toJavaObject());
      
      if (value != null)
        return wrapJava(value);
    }
    
    return UnsetValue.UNSET;
  }
  
  /**
   * Creatse a tail index.
   */
  @Override
  public Value createTailKey()
  {
    updateNextAvailableIndex();
    return LongValue.create(_nextAvailableIndex);
  }
  
  /**
   * Adds a new value.
   */
  @Override
  @SuppressWarnings("unchecked")
  public Value putImpl(Value key, Value value)
  {
    K keyObject;
    
    if (key.isLongConvertible() || key instanceof BooleanValue) {
      keyObject = (K)Long.valueOf(key.toLong());
    }
    else {
      keyObject = (K)key.toJavaObject();
    }

    Value val = wrapJava(_map.put(keyObject, (V)value.toJavaObject()));

    updateNextAvailableIndex(keyObject);
    
    return val;
  }

  /**
   * Returns the corresponding valeu if this array contains the given key
   *
   * @param key  the key to search for in the array
   *
   * @return the value if it is found in the array, NULL otherwise
   */
  public Value containsKey(Value key)
  {
    return BooleanValue.create(_map.containsKey(key.toJavaObject()));
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
  
  @Override
  public Iterator<Map.Entry<Value, Value>> getIterator(Env env)
  {
    return new MapIterator();
  }

  /**
   * Returns a set of all the of the entries.
   */
  @Override
  public Set<Map.Entry<Value,Value>> entrySet()
  {
    return new MapSet();
  }
  
  /**
   * Returns a collection of the values.
   */
  @Override
  @SuppressWarnings("unchecked")
  public Set<Map.Entry> objectEntrySet()
  {
    return (Set<Map.Entry>)_map.entrySet();
  }

  /**
   * Returns a collection of the values.
   */
  @Override
  public Collection<Value> values()
  {
    return new ValueCollection();
  }

  /**
   * Updates _nextAvailableIndex on a remove of the highest value
   */
  private void updateNextAvailableIndex()
  {
    _nextAvailableIndex = 0;

    for (Object key : _map.keySet()) {
      updateNextAvailableIndex(key);
    }
  }
  
  /**
   * Updates _nextAvailableIndex.
   */
  private void updateNextAvailableIndex(Object objectKey)
  { 
    if (objectKey instanceof Long) {
      long key = ((Long)objectKey).longValue();
    
      if (_nextAvailableIndex <= key)
        _nextAvailableIndex = key + 1;
    }
  }

  public class MapSet
    extends AbstractSet<Map.Entry<Value,Value>>
  {
    MapSet()
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
      return new MapIterator();
    }
  }

  public class MapIterator
    implements Iterator<Map.Entry<Value,Value>>
  {
    private Iterator<Map.Entry<K,V>> _iterator;

    public MapIterator()
    {
      _iterator = _map.entrySet().iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public Map.Entry<Value,Value> next()
    {
      Map.Entry entry = _iterator.next();
      
      Value key = wrapJava(entry.getKey());
      Value value = wrapJava(entry.getValue());

      return new ArrayValue.Entry(key, value);
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

  public class ValueIterator
    implements Iterator<Value>
  {
    private Iterator _iterator;

    public ValueIterator()
    {
      _iterator = _map.values().iterator();
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
  
  public class KeyIterator
    implements Iterator<Value>
  {
    private Iterator _iterator;

    public KeyIterator()
    {
      _iterator = _map.keySet().iterator();
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
