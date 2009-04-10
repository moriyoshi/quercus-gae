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

import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.IdentityHashMap;

/**
 * Represents a PHP boolean value.
 */
public class BooleanValue extends Value
  implements Serializable
{
  public static final BooleanValue TRUE = new BooleanValue(true);
  public static final BooleanValue FALSE = new BooleanValue(false);

  private final boolean _value;

  private BooleanValue(boolean value)
  {
    _value = value;
  }

  public static BooleanValue create(boolean value)
  {
    return value ? TRUE : FALSE;
  }

  public static Value create(Boolean value)
  {
    if (value == null) {
      // php/3c23
      return NullValue.NULL;
    }
    else if (Boolean.TRUE.equals(value))
      return TRUE;
    else
      return FALSE;
  }

  /**
   * Returns the type.
   */
  public String getType()
  {
    return "boolean";
  }

  /**
   * Returns the ValueType.
   */
  @Override
  public ValueType getValueType()
  {
    return ValueType.BOOLEAN;
  }

  /**
   * Returns true for a BooleanValue
   */
  @Override
  public boolean isBoolean()
  {
    return true;
  }
  
  /**
   * Returns true for a scalar
   */
  public boolean isScalar()
  {
    return true;
  }

  /**
   * Converts to a boolean.
   */
  public final boolean toBoolean()
  {
    return this == TRUE;
  }

  /**
   * Returns true if the value is empty
   */
  @Override
  public boolean isEmpty()
  {
    return ! _value;
  }

  /**
   * Converts to a long.
   */
  public long toLong()
  {
    return _value ? 1 : 0;
  }

  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    return _value ? 1 : 0;
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    return _value ? "1" : "";
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    StringValue sb = env.createUnicodeBuilder();
    
    if (_value)
      sb.append("1");

    return sb;
  }

  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return _value ? Boolean.TRUE : Boolean.FALSE;
  }

  /**
   * Converts to a java object.
   */
  public Object toJavaObject()
  {
    return _value ? Boolean.TRUE : Boolean.FALSE;
  }

  /**
   * Converts to an array if null.
   */
  public Value toAutoArray()
  {
    if (! _value)
      return new ArrayValueImpl();
    else
      return this;
  }

  /**
   * Converts to an object if null.
   */
  public Value toAutoObject(Env env)
  {
    if (! _value)
      return env.createObject();
    else
      return this;
  }

  /**
   * Sets the array value, returning the new array, e.g. to handle
   * string update ($a[0] = 'A').  Creates an array automatically if
   * necessary.
   */
  public Value append(Value index, Value value)
  {
    if (_value)
      return this;
    else
      return new ArrayValueImpl().append(index, value);
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    return _value ? LongValue.ONE : LongValue.ZERO;
  }

  /**
   * Returns true for equality
   */
  public boolean eq(Value rValue)
  {
    return _value == rValue.toBoolean();
  }
  
  /**
   * Returns true for equality
   */
  public int cmp(Value rValue)
  {
    boolean rBool = rValue.toBoolean();
    
    if (! _value && rBool)
      return -1;
    if (_value && ! rBool)
      return 1;
    
    return 0;
  }

  /**
   * Return the length as a string.
   */
  @Override
  public int length()
  {
    return _value ? 1 : 0;
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
  {
    env.print(_value ? "1" : "");
  }

  //
  // Java generator code
  //

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PrintWriter out)
    throws IOException
  {
    if (_value)
      out.print("com.caucho.quercus.env.BooleanValue.TRUE");
    else
      out.print("com.caucho.quercus.env.BooleanValue.FALSE");
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateBoolean(PrintWriter out)
    throws IOException
  {
    if (_value)
      out.print("true");
    else
      out.print("false");
  }

  /**
   * Serializes the value.
   */
  public void serialize(Env env, StringBuilder sb)
  {
    sb.append("b:");
    sb.append(_value ? 1 : 0);
    sb.append(';');
  }

  /**
   * Exports the value.
   */
  public void varExport(StringBuilder sb)
  {
    sb.append(_value ? "true" : "false");
  }

  /**
   * Returns the hash code
   */
  public int hashCode()
  {
    return _value ? 17 : 37;
  }

  /**
   * Compare for equality.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (o.getClass() != getClass())
      return false;

    BooleanValue value = (BooleanValue) o;

    return _value == value._value;
  }

  @Override
  public String toDebugString()
  {
    if (toBoolean())
      return "true";
    else
      return "false";
  }

  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value,String> valueSet)
    throws IOException
  {
    if (toBoolean())
      out.print("bool(true)");
    else
      out.print("bool(false)");
  }
  
  //
  // Java Serialization
  //
  
  private Object readResolve()
  {
    if (_value == true)
      return TRUE;
    else
      return FALSE;
  }
}

