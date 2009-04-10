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

import com.caucho.quercus.Location;
import com.caucho.quercus.marshal.Marshal;
import com.caucho.quercus.marshal.MarshalFactory;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a PHP array value.
 */
abstract public class ArrayValue extends Value {
  private static final Logger log
    = Logger.getLogger(ArrayValue.class.getName());

  protected static final StringValue KEY = new ConstStringValue("key");
  protected static final StringValue VALUE = new ConstStringValue("value");

  public static final GetKey GET_KEY = new GetKey();
  public static final GetValue GET_VALUE = new GetValue();

  protected Entry _current;

  protected ArrayValue()
  {
  }

  /**
   * Returns the type.
   */
  @Override
  public String getType()
  {
    return "array";
  }

  /**
   * Returns the ValueType.
   */
  @Override
  public ValueType getValueType()
  {
    return ValueType.ARRAY;
  }

  /**
   * Converts to a boolean.
   */
  @Override
  public boolean toBoolean()
  {
    return getSize() != 0;
  }
  
  /**
   * Converts to a long.
   */
  @Override
  public long toLong()
  {
    if (getSize() > 0)
      return 1;
    else
      return 0;
  }
  
  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return toLong();
  }

  /**
   * Converts to a string.
   */
  @Override
  public String toString()
  {
    return "Array";
  }

  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return null;
  }

  /**
   * Converts to a java object.
   */
  @Override
  public Object toJavaObject()
  {
    return this;
  }

  //
  // Conversions
  //
  
  /**
   * Converts to an object.
   */
  @Override
  public Value toArray()
  {
    return this;
  }
  
  /**
   * Converts to an array value
   */
  @Override
  public ArrayValue toArrayValue(Env env)
  {
    return this;
  }

  /**
   * Converts to an object.
   */
  @Override
  public Value toObject(Env env)
  {
    Value obj = env.createObject();

    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      Value key = entry.getKey();

      // php/03oe
      obj.putField(env, key.toString(), entry.getValue());
    }

    return obj;
  }

  /**
   * Converts to a java List object.
   */
  @Override
  public Collection toJavaCollection(Env env, Class type)
  {
    Collection coll = null;
    
    if (type.isAssignableFrom(HashSet.class)) {
      coll = new HashSet();
    }
    else if (type.isAssignableFrom(TreeSet.class)) {
      coll = new TreeSet();
    }
    else {
      try {
        coll = (Collection) type.newInstance();
      }
      catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
        env.warning(L.l("Can't assign array to {0}", type.getName()));

        return null;
      }
    }
    
    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      coll.add(entry.getValue().toJavaObject());
    }

    return coll;
  }
  
  /**
   * Converts to a java List object.
   */
  @Override
  public List toJavaList(Env env, Class type)
  {
    List list = null;
    
    if (type.isAssignableFrom(ArrayList.class)) {
      list = new ArrayList();
    }
    else if (type.isAssignableFrom(LinkedList.class)) {
      list = new LinkedList();
    }
    else if (type.isAssignableFrom(Vector.class)) {
      list = new Vector();
    }
    else {
      try {
        list = (List) type.newInstance();
      }
      catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
        env.warning(L.l("Can't assign array to {0}", type.getName()));

        return null;
      }
    }

    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      list.add(entry.getValue().toJavaObject());
    }

    return list;
  }
  
  /**
   * Converts to a java object.
   */
  @Override
  public Map toJavaMap(Env env, Class type)
  {
    Map map = null;
    
    if (type.isAssignableFrom(TreeMap.class)) {
      map = new TreeMap();
    }
    else if (type.isAssignableFrom(LinkedHashMap.class)) {
      map = new LinkedHashMap();
    }
    else {
      try {
        map = (Map) type.newInstance();
      }
      catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
	
        env.warning(L.l("Can't assign array to {0}",
			            type.getName()));

	return null;
      }
    }

    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      map.put(entry.getKey().toJavaObject(),
	          entry.getValue().toJavaObject());
    }

    return map;
  }

  /**
   * Returns true for an array.
   */
  @Override
  public boolean isArray()
  {
    return true;
  }

  /**
   * Copy as a return value
   */
  @Override
  public Value copyReturn()
  {
    return copy(); // php/3a5e
  }

  /**
   * Copy for assignment.
   */
  @Override
  abstract public Value copy();

  /**
   * Copy for serialization
   */
  @Override
  abstract public Value copy(Env env, IdentityHashMap<Value,Value> map);

  /**
   * Returns the size.
   */
  @Override
  abstract public int getSize();

  /**
   * Returns the count().
   */
  @Override
  public int getCount(Env env)
  {
    return getSize();
  }

  /**
   * Returns the count().
   */
  @Override
  public int getCountRecursive(Env env)
  {
    env.stub("recursive count of array unimplemented");

    return getSize();
  }

  /**
   * Returns true if the value is empty
   */
  @Override
  public boolean isEmpty()
  {
    return getSize() == 0;
  }

  /**
   * Clears the array
   */
  abstract public void clear();

  @Override
  public int cmp(Value rValue)
  {
    return cmpImpl(rValue, 1);
  }

  private int cmpImpl(Value rValue, int resultIfKeyMissing)
  {
    // "if key from operand 1 is not found in operand 2 then
    // arrays are uncomparable, otherwise - compare value by value"

    // php/335h

    if (!rValue.isArray())
      return 1;

    int lSize =  getSize();
    int rSize = rValue.toArray().getSize();
    
    if (lSize != rSize)
      return lSize < rSize ? -1 : 1;

    for (Map.Entry<Value,Value> entry : entrySet()) {
      Value lElementValue = entry.getValue();
      Value rElementValue = rValue.get(entry.getKey());

      if (!rElementValue.isset())
        return resultIfKeyMissing;

      int cmp = lElementValue.cmp(rElementValue);

      if (cmp != 0)
        return cmp;
    }

    return 0;
  }

  /**
   * Returns true for less than
   */
  @Override
  public boolean lt(Value rValue)
  {
    // php/335h
    return cmpImpl(rValue, 1) < 0;
  }

  /**
   * Returns true for less than or equal to
   */
  @Override
  public boolean leq(Value rValue)
  {
    // php/335h
    return cmpImpl(rValue, 1) <= 0;
  }

  /**
   * Returns true for greater than
   */
  @Override
  public boolean gt(Value rValue)
  {
    // php/335h
    return cmpImpl(rValue, -1) > 0;
  }

  /**
   * Returns true for greater than or equal to
   */
  @Override
  public boolean geq(Value rValue)
  {
    // php/335h
    return cmpImpl(rValue, -1) >= 0;
  }

  /**
   * Adds a new value.
   */
  @Override
  public Value put(Value key, Value value)
  {
    append(key, value);
    
    return value;
  }

  /**
   * Add
   */
  @Override
  abstract public Value put(Value value);

  /**
   * Add to front.
   */
  abstract public ArrayValue unshift(Value value);

  /**
   * Splices.
   */
  abstract public ArrayValue splice(int begin, int end, ArrayValue replace);

  /**
   * Slices.
   */
  public ArrayValue slice(Env env, int start, int end, boolean isPreserveKeys)
  {
    ArrayValueImpl array = new ArrayValueImpl();
    
    Iterator<Map.Entry<Value,Value>> iter = array.getIterator(env);
    
    for (int i = 0; i < end && iter.hasNext(); i++) {
      Map.Entry<Value,Value> entry = iter.next();
      
      if (start <= i) {
        Value key = entry.getKey();

        Value value = entry.getValue();

        if ((key.isString()) || isPreserveKeys)
          array.put(key, value);
        else
          array.put(value);
      }
    }

    return array;
  }
  
  /**
   * Returns the value as an array.
   */
  @Override
  public Value getArray(Value index)
  {
    Value value = get(index);

    Value array = value.toAutoArray();
    
    if (value != array) {
      value = array;

      put(index, value);
    }

    return value;
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  @Override
  abstract public Value getArg(Value index, boolean isTop);

  /**
   * Returns the field value, creating an object if it's unset.
   */
  @Override
  public Value getObject(Env env, Value fieldName)
  {
    Value value = get(fieldName);

    Value object = value.toAutoObject(env);
    if (value != object) {
      value = object;

      put(fieldName, value);
    }

    return value;
  }

  /**
   * Sets the array ref.
   */
  @Override
  abstract public Var putRef();

  /**
   * Creatse a tail index.
   */
  abstract public Value createTailKey();

  /**
   * Returns a union of this array and the rValue as array.
   * If the rValue is not an array, the returned union contains the elements
   * of this array only.
   *
   * To append a value to this ArrayValue use the {@link #put(Value)} method.
   */
  @Override
  public Value add(Value rValue)
  {
    rValue = rValue.toValue();

    if (! rValue.isArray())
      return copy();
    
    ArrayValue result = new ArrayValueImpl(this);
    
    Iterator<Map.Entry<Value,Value>> iter 
      = ((ArrayValue) rValue).getIterator(Env.getInstance());

    while (iter.hasNext()) {
      Map.Entry<Value,Value> entry = iter.next();
      
      Value key = entry.getKey();
      
      if (result.get(key) == UnsetValue.UNSET)
        result.put(key, entry.getValue());
    }

    return result;
  }

  @Override
  public Iterator<Map.Entry<Value, Value>> getIterator(Env env)
  {
    return new EntryIterator(getHead());
  }
  
  public Iterator<Map.Entry<Value, Value>> getIterator()
  {
    return new EntryIterator(getHead());
  }

  @Override
  public Iterator<Value> getKeyIterator(Env env)
  {
    return new KeyIterator(getHead());
  }

  @Override
  public Iterator<Value> getValueIterator(Env env)
  {
    return new ValueIterator(getHead());
  }

  /**
   * Gets a new value.
   */
  @Override
  abstract public Value get(Value key);

  /**
   * Returns the value in the array as-is.
   * (i.e. without calling toValue() on it).
   */
  public Value getRaw(Value key)
  {
    return get(key);
  }
  
  /**
   * Removes a value.
   */
  @Override
  abstract public Value remove(Value key);

  /**
   * Returns the array ref.
   */
  @Override
  abstract public Var getRef(Value index);

  /**
   * Returns an iterator of the entries.
   */
  public Set<Value> keySet()
  {
    return new KeySet();
  }

  /**
   * Returns a set of all the of the entries.
   */
  public Set<Map.Entry<Value,Value>> entrySet()
  {
    return new EntrySet();
  }

  /**
   * Returns a collection of the values.
   */
  public Collection<Value> values()
  {
    return new ValueCollection();
  }
  
  /**
   * Convenience for lib.
   */
  public void put(String key, String value)
  {
    // XXX: this needs an Env arg because of i18n
    // XXX: but some  modules have arrays that are static constants
    put(StringValue.create(key), StringValue.create(value));
  }

  /**
   * Convenience for lib.
   */
  public void put(Env env, String key, String value)
  {
    put(env.createString(key), env.createString(value));
  }

  /**
   * Convenience for lib.
   */
  public void put(String key, char value)
  {
    // XXX: this needs an Env arg because of i18n
    put(StringValue.create(key), StringValue.create(value));
  }

  /**
   * Convenience for lib.
   */
  public void put(String key, long value)
  {
    // XXX: this needs an Env arg because of i18n
    put(StringValue.create(key), LongValue.create(value));
  }

  /**
   * Convenience for lib.
   */
  public void put(Env env, String key, long value)
  {
    put(env.createString(key), LongValue.create(value));
  }
  
  /**
   * Convenience for lib.
   */
  public void put(String key, double value)
  {
    // XXX: this needs an Env arg because of i18n
    put(StringValue.create(key), new DoubleValue(value));
  }

  /**
   * Convenience for lib.
   */
  public void put(String key, boolean value)
  {
    // XXX: this needs an Env arg because of i18n
    put(StringValue.create(key), value ? BooleanValue.TRUE : BooleanValue.FALSE);
  }

  /**
   * Convenience for lib.
   */
  public void put(Env env, String key, boolean value)
  {
    put(env.createString(key),
	value ? BooleanValue.TRUE : BooleanValue.FALSE);
  }

  /**
   * Convenience for lib.
   */
  public void put(String value)
  {
    // XXX: this needs an Env arg because of i18n
    put(StringValue.create(value));
  }

  /**
   * Convenience for lib.
   */
  public void put(long value)
  {
    put(LongValue.create(value));
  }

  /**
   * Appends as an argument - only called from compiled code
   *
   * XXX: change name to appendArg
   */
  abstract public ArrayValue append(Value key, Value value);

  /**
   * Appends as an argument - only called from compiled code
   *
   * XXX: change name to appendArg
   */
  public ArrayValue append(Value value)
  {
    put(value);

    return this;
  }

  /**
   * Puts all of the arg elements into this array.
   */
  public void putAll(ArrayValue array)
  {
    for (Map.Entry<Value, Value> entry : array.entrySet())
      put(entry.getKey(), entry.getValue());
  }

  /**
   * Convert to an array.
   */
  public static Value toArray(Value value)
  {
    value = value.toValue();

    if (value instanceof ArrayValue)
      return value;
    else
      return new ArrayValueImpl().put(value);
  }


  /**
   * Prints the value.
   * @param env
   */
  @Override
  public void print(Env env)
  {
    env.print("Array");
  }

  /**
   * Pops the top value.
   */
  abstract public Value pop();

  /**
   * Shuffles the array
   */
  abstract public void shuffle();

  /**
   * Returns the head.
   */
  abstract protected Entry getHead();

  /**
   * Returns the tail.
   */
  abstract protected Entry getTail();

  /**
   * Returns the current value.
   */
  @Override
  public Value current()
  {
    if (_current != null)
      return _current.getValue();
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the current key
   */
  @Override
  public Value key()
  {
    if (_current != null)
      return _current.getKey();
    else
      return NullValue.NULL;
  }

  /**
   * Returns true if there are more elements.
   */
  @Override
  public boolean hasCurrent()
  {
    return _current != null;
  }

  /**
   * Returns the next value.
   */
  @Override
  public Value next()
  {
    if (_current != null)
      _current = _current._next;

    return current();
  }

  /**
   * Returns the previous value.
   */
  public Value prev()
  {
    if (_current != null)
      _current = _current._prev;

    return current();
  }

  /**
   * The each iterator
   */
  public Value each()
  {
    if (_current == null)
      return NullValue.NULL;

    ArrayValue result = new ArrayValueImpl();

    result.put(LongValue.ZERO, _current.getKey());
    result.put(KEY, _current.getKey());

    result.put(LongValue.ONE, _current.getValue());
    result.put(VALUE, _current.getValue());

    _current = _current._next;

    return result;
  }

  /**
   * Returns the first value.
   */
  public Value reset()
  {
    _current = getHead();

    return current();
  }

  /**
   * Returns the last value.
   */
  public Value end()
  {
    _current = getTail();

    return current();
  }

  /**
   * Returns the corresponding key if this array contains the given value
   *
   * @param value to search for in the array
   *
   * @return the key if it is found in the array, NULL otherwise
   */
  abstract public Value contains(Value value);

  /**
   * Returns the corresponding key if this array contains the given value
   *
   * @param value to search for in the array
   *
   * @return the key if it is found in the array, NULL otherwise
   */
  abstract public Value containsStrict(Value value);

  /**
   * Returns the corresponding value if this array contains the given key
   *
   * @param key to search for in the array
   *
   * @return the value if it is found in the array, NULL otherwise
   */
  abstract public Value containsKey(Value key);

  /**
   * Returns an object array of this array.  This is a copy of this object's
   * backing structure.  Null elements are not included.
   *
   * @return an object array of this array
   */
  public Map.Entry<Value, Value>[] toEntryArray()
  {
    ArrayList<Map.Entry<Value, Value>> array
      = new ArrayList<Map.Entry<Value, Value>>(getSize());

    for (Entry entry = getHead(); entry != null; entry = entry._next)
      array.add(entry);

    Map.Entry<Value, Value>[]result = new Entry[array.size()];

    return array.toArray(result);
  }

  /**
   * Sorts this array based using the passed Comparator
   *
   * @param comparator the comparator for sorting the array
   * @param resetKeys  true if the keys should not be preserved
   * @param strict  true if alphabetic keys should not be preserved
   */
  public void sort(Comparator<Map.Entry<Value, Value>> comparator,
                   boolean resetKeys, boolean strict)
  {
    Entry []entries;

    entries = new Entry[getSize()];

    int i = 0;
    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      entries[i++] = entry;
    }

    Arrays.sort(entries, comparator);

    clear();

    long base = 0;

    if (! resetKeys)
      strict = false;

    for (int j = 0; j < entries.length; j++) {
      Value key = entries[j].getKey();

      if (resetKeys && (! (key instanceof StringValue) || strict))
        put(LongValue.create(base++), entries[j].getValue());
      else
        put(entries[j].getKey(), entries[j].getValue());
    }
  }

  /*
   * Serializes the value.
   * 
   * @param sb holds result of serialization
   * @param serializeMap holds reference indexes
   */
  @Override
  public void serialize(Env env, StringBuilder sb, SerializeMap serializeMap)
  {
    sb.append("a:");
    sb.append(getSize());
    sb.append(":{");
    
    serializeMap.incrementIndex();

    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      entry.getKey().serialize(env, sb);
      entry.getRawValue().serialize(env, sb, serializeMap);
    }

    sb.append("}");
  }

  /**
   * Exports the value.
   */
  @Override
  public void varExport(StringBuilder sb)
  {
    sb.append("array (");
    sb.append("\n");

    //boolean isFirst = true;
    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      sb.append("  ");
      entry.getKey().varExport(sb);
      sb.append(" => ");
      entry.getValue().varExport(sb);
      sb.append(",\n");
    }

    sb.append(")");
  }

  /**
   * Resets all numerical keys with the first index as base
   *
   * @param base  the initial index
   * @param strict  if true, string keys are also reset
   */
  public boolean keyReset(long base, boolean strict)
  {
    Entry []entries;

    entries = new Entry[getSize()];

    int i = 0;
    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      entries[i++] = entry;
    }

    clear();

    for (int j = 0; j < entries.length; j++) {
      Value key = entries[j].getKey();

      if (! (key instanceof StringValue) || strict)
        put(LongValue.create(base++), entries[j].getValue());
      else
        put(entries[j].getKey(), entries[j].getValue());
    }

    return true;
  }

  /**
   * Test for equality
   *
   * @param rValue rhs ArrayValue to compare to
   *
   * @return true if this is equal to rValue, false otherwise
   */
  @Override
  public boolean eq(Value rValue)
  {
    if (rValue == null)
      return false;

    for (Map.Entry<Value, Value> entry: entrySet()) {
      Value entryValue = entry.getValue();

      Value entryKey = entry.getKey();

      Value rEntryValue = rValue.get(entryKey);

      if ((rEntryValue instanceof ArrayValue) &&
          ! entryValue.eq((ArrayValue) rEntryValue))
        return false;

      if (! entryValue.eq(rEntryValue))
        return false;
    }

    return true;
  }

  /**
   * Test for ===
   *
   * @param rValue rhs ArrayValue to compare to
   *
   * @return true if this is equal to rValue, false otherwise
   */
  @Override
  public boolean eql(Value rValue)
  {
    if (rValue == null)
      return false;
    else if (getSize() != rValue.getSize())
      return false;

    rValue = rValue.toValue();

    if (! (rValue instanceof ArrayValue))
      return false;

    ArrayValue rArray = (ArrayValue) rValue;

    Iterator<Map.Entry<Value,Value>> iterA = entrySet().iterator();
    Iterator<Map.Entry<Value,Value>> iterB = rArray.entrySet().iterator();

    while (iterA.hasNext() && iterB.hasNext()) {
      Map.Entry<Value,Value> entryA = iterA.next();
      Map.Entry<Value,Value> entryB = iterB.next();

      if (! entryA.getKey().eql(entryB.getKey()))
        return false;

      if (! entryA.getValue().eql(entryB.getValue()))
        return false;
    }

    if (iterA.hasNext() || iterB.hasNext())
      return false;
    else
      return true;
  }

  @Override
  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.println("array(" + getSize() + ") {");

    for (Map.Entry<Value,Value> mapEntry : entrySet()) {
      varDumpEntry(env, out, depth + 1, valueSet, mapEntry);

      out.println();
    }

    printDepth(out, 2 * depth);

    out.print("}");
  }

  protected void varDumpEntry(Env env,
                              WriteStream out,
                              int depth,
                              IdentityHashMap<Value, String> valueSet,
                              Map.Entry<Value, Value> mapEntry)
    throws IOException
  {
    ArrayValue.Entry entry = (ArrayValue.Entry) mapEntry;

    entry.varDumpImpl(env, out, depth, valueSet);
  }
  
  @Override
  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.println("Array");
    printDepth(out, 8 * depth);
    out.println("(");
    
    for (Map.Entry<Value,Value> mapEntry : entrySet()) {
      ArrayValue.Entry entry = (ArrayValue.Entry) mapEntry;
      
      entry.printRImpl(env, out, depth, valueSet);
    }

    printDepth(out, 8 * depth);
    out.println(")");
  }
  
  protected void printREntry(Env env,
                             WriteStream out,
                             int depth,
                             IdentityHashMap<Value, String> valueSet,
                             Map.Entry<Value, Value> mapEntry)
    throws IOException
  {
    ArrayValue.Entry entry = (ArrayValue.Entry) mapEntry;

    entry.printRImpl(env, out, depth, valueSet);
  }

  public static final class Entry
    implements Map.Entry<Value,Value>, Serializable  
  {
    final Value _key;
    
    Value _value;
    Var _var;

    Entry _prev;
    Entry _next;

    Entry _nextHash;

    public Entry(Value key)
    {
      _key = key;
      _value = NullValue.NULL;
    }

    public Entry(Value key, Value value)
    {
      _key = key;
      _value = value;
    }

    public Entry(Entry entry)
    {
      _key = entry._key;

      if (entry._var != null)
        _var = entry._var;
      else
        _value = entry._value.copyArrayItem();
    }

    public Entry getNext()
    {
      return _next;
    }

    public Value getRawValue()
    {
      return _var != null ? _var : _value;
    }

    public Value getValue()
    {
      return _var != null ? _var.toValue() : _value;
    }

    public Value getKey()
    {
      return _key;
    }

    public Value toValue()
    {
      // The value may be a var
      // XXX: need test
      return _var != null ? _var.toValue() : _value;
    }

    /**
     * Argument used/declared as a ref.
     */
    public Var toRefVar()
    {
      // php/376a

      if (_var != null)
        return _var;
      else {
        _var = new Var(_value);

        return _var;
      }
    }

    /**
     * Converts to an argument value.
     */
    public Value toArgValue()
    {
      return _var != null ? _var.toValue() : _value;
    }

    public Value setValue(Value value)
    {
      Value oldValue = _value;

      _value = value;
      _var = null;

      return oldValue;
    }

    public Value set(Value value)
    {
      Value oldValue = _value;

      if (value instanceof Var)
	_var = (Var) value;
      else if (_var != null)
	_var.set(value);
      else
	_value = value;

      return oldValue;
    }

    /**
     * Converts to a variable reference (for function  arguments)
     */
    public Value toRef()
    {
      if (_var == null)
        _var = new Var(_value);
      
      return new RefVar(_var);
    }

    /**
     * Converts to a variable reference (for function  arguments)
     */
    public Value toArgRef()
    {
      if (_var == null)
        _var = new Var(_value);
      
      return new RefVar(_var);
    }

    public Value toArg()
    {
      if (_var == null)
        _var = new Var(_value);
      
      return _var;
    }

    public void varDumpImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
      throws IOException
    {
      printDepth(out, 2 * depth);
      out.print("[");

      if (_key instanceof StringValue)
        out.print("\"" + _key + "\"");
      else
        out.print(_key);

      out.println("]=>");

      printDepth(out, 2 * depth);

      getRawValue().varDump(env, out, depth, valueSet);
    }

    protected void printRImpl(Env env,
                              WriteStream out,
                              int depth,
                              IdentityHashMap<Value, String> valueSet)
      throws IOException
    {
      printDepth(out, 8 * depth);
      out.print("    [");
      out.print(_key);
      out.print("] => ");
      if (getRawValue() != null)
        getRawValue().printR(env, out, depth + 1, valueSet);
      out.println();
    }

    private void printDepth(WriteStream out, int depth)
      throws java.io.IOException
    {
      for (int i = depth; i > 0; i--)
        out.print(' ');
    }

    @Override
    public String toString()
    {
      return "ArrayValue.Entry[" + getKey() + "]";
    }
  }

  /**
   * Returns the field keys.
   */
  public Value []getKeyArray(Env env)
  {
    int len = getSize();
    Value []keys = new Value[len];

    Iterator<Value> iter = getKeyIterator(env);
    
    for (int i = 0; i < len; i++) {
      keys[i] = iter.next();
    }
    
    return keys;
  }
  
  /**
   * Returns the field values.
   */
  public Value []getValueArray(Env env)
  {
    int len = getSize();
    Value []values = new Value[len];

    Iterator<Value> iter = getValueIterator(env);
    
    for (int i = 0; i < len; i++) {
      values[i] = iter.next();
    }
    
    return values;
  }
  
  /**
   * Takes the values of this array and puts them in a java array
   */
  public Value[] keysToArray()
  {
    Value[] values = new Value[getSize()];

    int i = 0;
    for (Entry ptr = getHead(); ptr != null; ptr = ptr.getNext()) {
      values[i++] = ptr.getKey();
    }

    return values;
  }
  
  /**
   * Takes the values of this array and puts them in a java array
   */
  public Value[] valuesToArray()
  {
    Value[] values = new Value[getSize()];

    int i = 0;
    for (Entry ptr = getHead(); ptr != null; ptr = ptr.getNext()) {
      values[i++] = ptr.getValue();
    }

    return values;
  }
  
  /**
   * Returns the keys.
   */
  public Value getKeys()
  {
    return new ArrayValueImpl(keysToArray());
  }

  /**
   * Returns the keys.
   */
  public Value getValues()
  {
    return new ArrayValueImpl(valuesToArray());
  }
  
  /**
   * Takes the values of this array, unmarshals them to objects of type
   * <i>elementType</i>, and puts them in a java array.
   */
  @Override
  public Object valuesToArray(Env env, Class elementType)
  {
    int size = getSize();

    Object array = Array.newInstance(elementType, size);

    MarshalFactory factory = env.getModuleContext().getMarshalFactory();
    Marshal elementMarshal = factory.create(elementType);

    int i = 0;

    for (Entry ptr = getHead(); ptr != null; ptr = ptr.getNext()) {
      Array.set(array, i++, elementMarshal.marshal(env,
                                                   ptr.getValue(),
                                                   elementType));
    }

    return array;
  }
  
  @Override
  public int hashCode()
  {
    return getSize();
  }
  
  @Override
  public boolean equals(Object o)
  {
    if (! (o instanceof ArrayValue))
      return false;
    
    // toValue() for CopyArrayValue
    ArrayValue array = (ArrayValue) ((Value) o).toValue();
    
    return cmp(array) == 0;
  }

  public class EntrySet extends AbstractSet<Map.Entry<Value,Value>> {
    EntrySet()
    {
    }

    @Override
    public int size()
    {
      return ArrayValue.this.getSize();
    }

    @Override
    public Iterator<Map.Entry<Value,Value>> iterator()
    {
      return new EntryIterator(getHead());
    }
  }

  public class KeySet extends AbstractSet<Value> {
    KeySet()
    {
    }

    @Override
    public int size()
    {
      return ArrayValue.this.getSize();
    }

    @Override
    public Iterator<Value> iterator()
    {
      return new KeyIterator(getHead());
    }
  }

  public class ValueCollection extends AbstractCollection<Value> {
    ValueCollection()
    {
    }

    @Override
    public int size()
    {
      return ArrayValue.this.getSize();
    }

    @Override
    public Iterator<Value> iterator()
    {
      return new ValueIterator(getHead());
    }
  }

  public static class EntryIterator
    implements Iterator<Map.Entry<Value,Value>> {
    private Entry _current;

    EntryIterator(Entry head)
    {
      _current = head;
    }

    public boolean hasNext()
    {
      return _current != null;
    }

    public Map.Entry<Value,Value> next()
    {
      if (_current != null) {
        Map.Entry<Value,Value> next = _current;
        _current = _current._next;

        return next;
      }
      else
        return null;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public static class KeyIterator
    implements Iterator<Value> {
    private Entry _current;

    KeyIterator(Entry head)
    {
      _current = head;
    }

    public boolean hasNext()
    {
      return _current != null;
    }

    public Value next()
    {
      if (_current != null) {
        Value next = _current.getKey();
        _current = _current._next;

        return next;
      }
      else
        return null;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public static class ValueIterator
    implements Iterator<Value> {
    private Entry _current;

    ValueIterator(Entry head)
    {
      _current = head;
    }

    public boolean hasNext()
    {
      return _current != null;
    }

    public Value next()
    {
      if (_current != null) {
        Value next = _current.getValue();
        _current = _current._next;

        return next;
      }
      else
        return null;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public static class ValueComparator
    implements Comparator<Map.Entry<Value,Value>>
  {
    public static final ValueComparator CMP = new ValueComparator();
    
    private ValueComparator()
    {
    }
    
    public int compare(Map.Entry<Value,Value> aEntry,
                       Map.Entry<Value,Value> bEntry)
    {
      try {
        Value aValue = aEntry.getValue();
        Value bValue = bEntry.getValue();

        if (aValue.eq(bValue))
          return 0;
        else if (aValue.lt(bValue))
          return -1;
        else
          return 1;
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class KeyComparator
    implements Comparator<Map.Entry<Value,Value>>
  {
    public static final KeyComparator CMP = new KeyComparator();
    
    private KeyComparator()
    {
    }
    
    public int compare(Map.Entry<Value,Value> aEntry,
                       Map.Entry<Value,Value> bEntry)
    {
      try {
        Value aKey = aEntry.getKey();
        Value bKey = bEntry.getKey();

        if (aKey.eq(bKey))
          return 0;
        else if (aKey.lt(bKey))
          return -1;
        else
          return 1;
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static abstract class AbstractGet {
    public abstract Value get(Map.Entry<Value, Value> entry);
  }

  public static class GetKey extends AbstractGet
  {
    public static final GetKey GET = new GetKey();
    
    private GetKey()
    {
    }

    @Override
    public Value get(Map.Entry<Value, Value> entry)
    {
      return entry.getKey();
    }
  }

  public static class GetValue extends AbstractGet {
    public static final GetValue GET = new GetValue();
    
    private GetValue()
    {
    }

    @Override
    public Value get(Map.Entry<Value, Value> entry)
    {
      return entry.getValue();
    }
  }
}


