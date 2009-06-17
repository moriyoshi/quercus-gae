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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.IdentityHashMap;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Represents a PHP null value.
 */
public class NullValue extends Value
  implements Serializable
{
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  public static final NullValue NULL = new NullValue();

  protected NullValue()
  {
  }

  /**
   * Returns the null value singleton.
   */
  public static NullValue create()
  {
    return NULL;
  }

  /**
   * Returns the type.
   */
  @Override
  public String getType()
  {
    return "NULL";
  }

  /**
   * Returns the ValueType.
   */
  @Override
  public ValueType getValueType()
  {
    return ValueType.NULL;
  }

  /**
   * Returns true for a set type.
   */
  @Override
  public boolean isset()
  {
    return false;
  }

  /**
   * Returns true if the value is empty
   */
  @Override
  public boolean isEmpty()
  {
    return true;
  }

  /**
   * Converts to a boolean.
   */
  @Override
  public boolean toBoolean()
  {
    return false;
  }

  /**
   * Returns true for a null.
   */
  @Override
  public boolean isNull()
  {
    return true;
  }

  /**
   * Converts to a long.
   */
  @Override
  public long toLong()
  {
    return 0;
  }

  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return 0;
  }

  /**
   * Converts to a string.
   * @param env
   */
  @Override
  public String toString()
  {
    return "";
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    return env.createUnicodeBuilder();
  }

  /**
   * Converts to an object.
   */
  @Override
  public Object toJavaObject()
  {
    return null;
  }

  /**
   * Converts to a java object.
   */
  @Override
  public <T> T toJavaObject(Env env, Class<T> type)
  {
    return null;
  }

  /**
   * Converts to a java object.
   */
  @Override
  public <T> T toJavaObjectNotNull(Env env, Class<T> type)
  {
    env.warning(L.l("null is an unexpected argument; expected '{0}'",
                    type.getName()));
    
    return null;
  }

  /**
   * Converts to a java boolean object.
   */
  @Override
  public Boolean toJavaBoolean()
  {
    return null;
  }

  /**
   * Converts to a java Byte object.
   */
  @Override
  public Byte toJavaByte()
  {
    return null;
  }

  /**
   * Converts to a java Short object.
   */
  @Override
  public Short toJavaShort()
  {
    return null;
  }

  /**
   * Converts to a java Integer object.
   */
  @Override
  public Integer toJavaInteger()
  {
    return null;
  }

  /**
   * Converts to a java Long object.
   */
  @Override
  public Long toJavaLong()
  {
    return null;
  }

  /**
   * Converts to a java Float object.
   */
  @Override
  public Float toJavaFloat()
  {
    return null;
  }

  /**
   * Converts to a java Double object.
   */
  @Override
  public Double toJavaDouble()
  {
    return null;
  }

  /**
   * Converts to a java Character object.
   */
  @Override
  public Character toJavaCharacter()
  {
    return null;
  }

  /**
   * Converts to a java String object.
   */
  @Override
  public String toJavaString()
  {
    return null;
  }

  /**
   * Converts to a java object.
   */
  @Override
  public <T> Collection<T> toJavaCollection(Env env, Class<? extends Collection<T>> type)
  {
    return null;
  }

  /**
   * Converts to a java object.
   */
  @Override
  public <T> List<T> toJavaList(Env env, Class<? extends List<T>> type)
  {
    return null;
  }

  /**
   * Converts to a java object.
   */
  @Override
  public <K,V> Map<K,V> toJavaMap(Env env, Class<? extends Map<K,V>> type)
  {
    return null;
  }

  /**
   * Converts to a Java Calendar.
   */
  @Override
  public Calendar toJavaCalendar()
  {
    return null;
  }
  
  /**
   * Converts to a Java Date.
   */
  @Override
  public Date toJavaDate()
  {
    return null;
  }
  
  /**
   * Converts to a Java URL.
   */
  @Override
  public URL toJavaURL(Env env)
  {
    return null;
  }
  
  /**
   * Converts to a Java BigDecimal.
   */
  @Override
  public BigDecimal toBigDecimal()
  {
    return BigDecimal.ZERO;
  }
  
  /**
   * Converts to a Java BigInteger.
   */
  @Override
  public BigInteger toBigInteger()
  {
    return BigInteger.ZERO;
  }
  
  /**
   * Takes the values of this array, unmarshalls them to objects of type
   * <i>elementType</i>, and puts them in a java array.
   */
  @Override
  public <T> T[] valuesToArray(Env env, Class<T> elementType)
  {
    return null;
  }
  
  /**
   * Converts to an object.
   */
  @Override
  public Value toObject(Env env)
  {
    return NullValue.NULL;
  }

  /**
   * Converts to an array
   */
  @Override
  public Value toArray()
  {
    return new ArrayValueImpl();
  }

  /**
   * Converts to an array if null.
   */
  @Override
  public Value toAutoArray()
  {
    return new ArrayValueImpl();
  }

  /**
   * Sets the array value, returning the new array, e.g. to handle
   * string update ($a[0] = 'A').  Creates an array automatically if
   * necessary.
   */
  public Value append(Value index, Value value)
  {
    return new ArrayValueImpl().append(index, value);
  }

  /**
   * Casts to an array.
   */
  @Override
  public ArrayValue toArrayValue(Env env)
  {
    return null;
  }

  /**
   * Converts to a StringValue.
   */
  public StringValue toStringValue()
  {
    Env env = Env.getInstance();

    if (env != null && env.isUnicodeSemantics())
      return UnicodeBuilderValue.EMPTY;
    else
      return StringBuilderValue.EMPTY;
  }

  @Override
  public int getCount(Env env)
  {
    return 0;
  }

  /**
   * Returns the array size.
   */
  @Override
  public int getSize()
  {
    return 0;
  }

  /**
   * Converts to an object if null.
   */
  @Override
  public Value toAutoObject(Env env)
  {
    return env.createObject();
  }

  /**
   * Converts to a key.
   */
  @Override
  public Value toKey()
  {
    return StringValue.EMPTY;
  }

  /**
   * Returns true for equality
   */
  @Override
  public boolean eql(Value rValue)
  {
    return rValue.isNull();
  }

  /**
   * Adds to the following value.
   */
  @Override
  public Value add(long lLong)
  {
    return LongValue.create(lLong);
  }

  /**
   * Subtracts the following value.
   */
  @Override
  public Value sub(long rLong)
  {
    return LongValue.create( - rLong);
  }
  
  /**
   * Returns true for equality
   */
  @Override
  public boolean eq(Value rValue)
  {
    if (rValue.isString())
      return toString().equals(rValue.toString());
    else
      return toBoolean() == rValue.toBoolean();
  }

  /**
   * Returns true for equality
   */
  @Override
  public int cmp(Value rValue)
  {
    rValue = rValue.toValue();

    if (! (rValue instanceof StringValue)) {
      int l = 0;
      int r = rValue.toBoolean() ? 1 : 0;

      return l - r;
    }
    else if (rValue.isNumberConvertible()) {
      double l = 0;
      double r = rValue.toDouble();

      if (l == r)
        return 0;
      else if (l < r)
        return -1;
      else
        return 1;
    }
    else
      return "".compareTo(rValue.toString());
  }

  /**
   * Prints the value.
   * @param env
   */
  @Override
  public void print(Env env)
  {
  }

  /**
   * Serializes the value.
   */
  @Override
  public void serialize(Env env, StringBuilder sb)
  {
    sb.append("N;");
  }

  /**
   * Exports the value.
   */
  @Override
  public void varExport(StringBuilder sb)
  {
    sb.append("NULL");
  }

  /**
   * Returns a new array.
   */
  @Override
  public Value getArray()
  {
    return new ArrayValueImpl();
  }

  /**
   * Append to a binary builder.
   */
  @Override
  public StringValue appendTo(BinaryBuilderValue sb)
  {
    return sb;
  }

  /**
   * Append to a unicode builder.
   */
  @Override
  public StringValue appendTo(UnicodeBuilderValue sb)
  {
    return sb;
  }

  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(StringBuilderValue sb)
  {
    return sb;
  }
  
  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(LargeStringBuilderValue sb)
  {
    return sb;
  }

  //
  // Java generator code
  //

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  @Override
  public void generate(PrintWriter out)
    throws IOException
  {
    out.print("NullValue.NULL");
  }

  /**
   * Returns a new object.
   */
  @Override
  public Value getObject(Env env)
  {
    return env.createObject();
  }

  @Override
  public String toDebugString()
  {
    return "null";
  }

  @Override
  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.print("NULL");
  }
  
  //
  // Java Serialization
  //
  
  private Object readResolve()
  {
    return NULL;
  }
}

