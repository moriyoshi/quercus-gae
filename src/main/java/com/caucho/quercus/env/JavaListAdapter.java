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
import java.util.logging.*;

/**
 * Represents a marshalled Collection argument.
 */
public class JavaListAdapter<T>
  extends JavaCollectionAdapter<T>
{
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private static final Logger log
    = Logger.getLogger(JavaListAdapter.class.getName());
 
  private int _next = 0;

  public JavaListAdapter(Env env, List<T> list)
  {
    this(env, list, (JavaClassDef<? extends List<T>>)env.getJavaClassDefinition(list.getClass()));
  }
  
  public JavaListAdapter(Env env, List<T> list, JavaClassDef<? extends List<T>> def)
  {
    super(env, list, def);
  }

  /**
   * Adds a new value.
   */
  @SuppressWarnings("unchecked")
  public Value putImpl(Value key, Value value)
  {
    int pos = key.toInt();
    int size = getSize();
    
    if (0 <= pos && pos <= size) {
      if (pos < size)
        ((List<T>)_object).remove(pos);
      
      ((List<T>)_object).add(pos, (T)value.toJavaObject());

      return value;
    }
    else {
      getEnv().warning(L.l("index {0} is out of range", pos));
      log.log(Level.FINE, L.l("index {0} is out of range", pos));
 
      return UnsetValue.UNSET; 
    }
  }
  
  /**
   * Gets a new value.
   */
  public Value get(Value key)
  { 
    int pos = key.toInt();
    
    if (0 <= pos && pos < getSize())
      return wrapJava(((List<T>)_object).get(pos));
    else
      return UnsetValue.UNSET;
  }

  /**
   * Removes a value.
   */
  public Value remove(Value key)
  {
    int pos = key.toInt();
    
    if (0 <= pos && pos < getSize())
      return wrapJava(((List<T>)_object).remove(pos));
    else
      return UnsetValue.UNSET;
  }

  /**
   * Pops the top value.
   */
  public Value pop()
  {    
    if (getSize() == 0)
      return BooleanValue.FALSE;
    
    return wrapJava(((List<T>)_object).remove(0));
  }
  
  /**
   * Returns the corresponding key if this array contains the given value
   *
   * @param value  the value to search for in the array
   *
   * @return the key if it is found in the array, NULL otherwise
   *
   * @throws NullPointerException
   */
  public Value contains(Value value)
  {
    for (Map.Entry<Value,Value> entry : (Set<Map.Entry<Value,Value>>)entrySet()) {
      if (entry.getValue().equals(value))
        return entry.getKey();
    }
    
    return NullValue.NULL;
  }
  
  /**
   * Returns the current value.
   */
  public Value current()
  {
    if (_next < ((List<T>)_object).size())
      return wrapJava(((List<T>)_object).get(_next));
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the current key
   */
  public Value key()
  {    
    if (_next < ((List<T>)_object).size())
      return LongValue.create(_next);
    else
      return NullValue.NULL;
  }

  /**
   * Returns true if there are more elements.
   */
  public boolean hasCurrent()
  {
    return _next < ((List<T>)_object).size();
  }

  /**
   * Returns the next value.
   */
  public Value next()
  {
    if (_next < ((List<T>)_object).size())
      return wrapJava(((List<T>)_object).get(_next++));
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the previous value.
   */
  public Value prev()
  {
    if (_next > 0)
      return wrapJava(((List<T>)_object).get(_next--));
    else
      return BooleanValue.FALSE;
  }

  /**
   * The each iterator
   */
  public Value each()
  {
    if (_next < ((List<T>)_object).size())
    {
      ArrayValue result = new ArrayValueImpl();

      result.put(LongValue.ZERO, key());
      result.put(KEY, key());

      result.put(LongValue.ONE, current());
      result.put(VALUE, current());

      _next++;

      return result;
    }
    else
      return NullValue.NULL;
  }

  /**
   * Returns the first value.
   */
  public Value reset()
  {
    _next = 0;

    return current();
  }

  /**
   * Returns the last value.
   */
  public Value end()
  {
    _next = ((List<T>)_object).size();
    
    return current();
  }
}
