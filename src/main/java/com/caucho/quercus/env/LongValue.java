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
import java.util.IdentityHashMap;

/**
 * Represents a PHP long value.
 */
public class LongValue extends NumberValue
{
  public static final LongValue MINUS_ONE = new LongValue(-1);
  public static final LongValue ZERO = new LongValue(0);
  public static final LongValue ONE = new LongValue(1);

  public static final int STATIC_MIN = -1024;
  public static final int STATIC_MAX = 2048;

  public static final LongValue[]STATIC_VALUES;

  private final long _value;

  public LongValue(long value)
  {
    _value = value;
  }

  public static LongValue create(long value)
  {
    if (STATIC_MIN <= value && value <= STATIC_MAX)
      return STATIC_VALUES[(int) (value - STATIC_MIN)];
    else
      return new LongValue(value);
  }

  public static LongValue create(Number value)
  {
    if (value == null)
      return LongValue.ZERO;
    else
      return LongValue.create(value.longValue());
  }

  /**
   * Returns the type.
   */
  public String getType()
  {
    return "integer";
  }

  /**
   * Returns the ValueType.
   */
  @Override
  public ValueType getValueType()
  {
    return ValueType.LONG;
  }

  /**
   * Returns true for a long.
   */
  public boolean isLongConvertible()
  {
    return true;
  }

  /**
   * Returns true for is_numeric
   */
  @Override
  public boolean isNumeric()
  {
    return true;
  }
  
  /**
   * Returns true for a long-value.
   */
  @Override
  public boolean isLong()
  {
    return true;
  }
  
  /**
   * Returns true for a double-value.
   */
  @Override
  public boolean isDouble()
  {
    return false;
  }

  /**
   * Returns true for a scalar
   */
  public boolean isScalar()
  {
    return true;
  }

  /**
   * Returns true if the value is empty
   */
  @Override
  public boolean isEmpty()
  {
    return _value == 0;
  }

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return _value != 0;
  }

  /**
   * Converts to a long.
   */
  public long toLong()
  {
    return _value;
  }

  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    return _value;
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    return String.valueOf(_value);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    return env.createUnicodeBuilder().append(_value);
  }

  /**
   * Converts to a long value
   */
  public LongValue toLongValue()
  {
    return this;
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    return this;
  }

  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return String.valueOf(_value);
  }

  /**
   * Converts to a java object.
   */
  public Object toJavaObject()
  {
    return new Long(_value);
  }

  /**
   * Negates the value.
   */
  public Value neg()
  {
    return LongValue.create(- _value);
  }

  /**
   * Negates the value.
   */
  public Value pos()
  {
    return this;
  }

  /**
   * Post-increment the following value.
   */
  public Value increment(int incr)
  {
    return LongValue.create(_value + incr);
  }

  /**
   * Adds to the following value.
   */
  @Override
  public Value add(Value value)
  {
    return value.add(_value);
  }

  /**
   * Adds to the following value.
   */
  @Override
  public Value add(long lLong)
  {
    return LongValue.create(lLong + _value);
  }

  /**
   * Subtracts to the following value.
   */
  @Override
  public Value sub(Value rValue)
  {
    if (rValue.isLongConvertible())
      return LongValue.create(_value - rValue.toLong());
    else
      return DoubleValue.create(_value - rValue.toDouble());
  }

  /**
   * Subtracts the following value.
   */
  @Override
  public Value sub(long rLong)
  {
    return LongValue.create(_value - rLong);
  }
  
  /**
   * Absolute value.
   */
  public Value abs()
  {
    if (_value >= 0)
      return this;
    else
      return LongValue.create(- _value);
  }

  /**
   * Returns true for equality
   */
  public boolean eql(Value rValue)
  {
    rValue = rValue.toValue();

    if (! (rValue instanceof LongValue))
      return false;

    long rLong = ((LongValue) rValue)._value;

    return _value == rLong;
  }
  /**
   * Returns true for equality
   */
  public int cmp(Value rValue)
  {
    if (rValue.isBoolean()) {
      boolean lBool = toBoolean();
      boolean rBool = rValue.toBoolean();
      
      if (! lBool && rBool)
        return -1;
      if (lBool && ! rBool)
        return 1;
      
      return 0;
    }
    
    long l = _value;
    double r = rValue.toDouble();

    if (l == r)
      return 0;
    else if (l < r)
      return -1;
    else
      return 1;
  }

  /**
   * Returns the next array index based on this value.
   */
  @Override
  public long nextIndex(long oldIndex)
  {
    if (oldIndex <= _value)
      return _value + 1;
    else
      return oldIndex;
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
  {
    env.print(_value);
  }

  /**
   * Append to a unicode builder.
   */
  @Override
  public StringValue appendTo(UnicodeBuilderValue sb)
  {
    return sb.append(_value);
  }

  /**
   * Append to a binary builder.
   */
  @Override
  public StringValue appendTo(BinaryBuilderValue sb)
  {
    return sb.append(_value);
  }

  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(StringBuilderValue sb)
  {
    return sb.append(_value);
  }
  
  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(LargeStringBuilderValue sb)
  {
    return sb.append(_value);
  }

  /**
   * Serializes the value.
   */
  public void serialize(Env env, StringBuilder sb)
  {
    sb.append("i:");
    sb.append(_value);
    sb.append(";");
  }

  /**
   * Exports the value.
   */
  public void varExport(StringBuilder sb)
  {
    sb.append(_value);
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
    if (_value == 0)
      out.print("LongValue.ZERO");
    else if (_value == 1)
      out.print("LongValue.ONE");
    else if (_value == -1)
      out.print("LongValue.MINUS_ONE");
    else if (STATIC_MIN <= _value && _value <= STATIC_MAX)
      out.print("LongValue.STATIC_VALUES[" + (_value - STATIC_MIN) + "]");
    else
      out.print("new LongValue(" + _value + "L)");
  }

  /**
   * Returns the hash code
   */
  public final int hashCode()
  {
    long v = _value;
    
    return (int) (17 * v + 65537 * (v >> 32));
  }

  /**
   * Compare for equality.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof LongValue))
      return false;

    LongValue value = (LongValue) o;

    return _value == value._value;
  }

  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value,String> valueSet)
    throws IOException
  {
    out.print("int(" + toLong() + ")");
  }
  
  //
  // Java Serialization
  //
  
  private Object readResolve()
  {
    if (STATIC_MIN <= _value && _value <= STATIC_MAX)
      return STATIC_VALUES[(int) (_value - STATIC_MIN)];
    else
      return this;
  }

  static {
    STATIC_VALUES = new LongValue[STATIC_MAX - STATIC_MIN + 1];

    for (int i = STATIC_MIN; i <= STATIC_MAX; i++) {
      STATIC_VALUES[i - STATIC_MIN] = new LongValue(i);
    }
  }
}
