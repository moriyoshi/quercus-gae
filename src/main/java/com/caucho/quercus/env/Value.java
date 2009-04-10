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

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Represents a PHP expression value.
 */
abstract public class Value implements java.io.Serializable
{
  protected static final L10N L = new L10N(Value.class);

  public static final StringValue SCALAR_V = new ConstStringValue("scalar");

  public static final Value []NULL_VALUE_ARRAY = new Value[0];
  public static final Value []NULL_ARGS = new Value[0];

  //
  // Properties
  //
  
  /**
   * Returns the value's class name.
   */
  public String getClassName()
  {
    return getType();
  }

  //
  // Predicates and Relations
  //
  
  /**
   * Returns true for an implementation of a class
   */
  public boolean isA(String name)
  {
    return false;
  }
  
  /**
   * Returns true for an implementation of a class
   */
  final public boolean isA(Value value)
  {
    if (value.isObject())
      return isA(value.getClassName());
    else
      return isA(value.toString());
  }
  
  /**
   * Checks if 'this' is a valid protected call for 'className'
   */
  public void checkProtected(Env env, String className)
  {
  }
  
  /**
   * Checks if 'this' is a valid private call for 'className'
   */
  public void checkPrivate(Env env, String className)
  {
  }

  /**
   * Returns the ValueType.
   */
  public ValueType getValueType()
  {
    return ValueType.VALUE;
  }

  /**
   * Returns true for an array.
   */
  public boolean isArray()
  {
    return false;
  }

  /**
   * Returns true for a double-value.
   */
  public boolean isDoubleConvertible()
  {
    return false;
  }

  /**
   * Returns true for a long-value.
   */
  public boolean isLongConvertible()
  {
    return false;
  }
  
  /**
   * Returns true for a long-value.
   */
  public boolean isLong()
  {
    return false;
  }
  
  /**
   * Returns true for a long-value.
   */
  public boolean isDouble()
  {
    return false;
  }

  /**
   * Returns true for a null.
   */
  public boolean isNull()
  {
    return false;
  }

  /**
   * Returns true for a number.
   */
  public boolean isNumberConvertible()
  {
    return isLongConvertible() || isDoubleConvertible();
  }

  /**
   * Matches is_numeric
   */
  public boolean isNumeric()
  {
    return false;
  }

  /**
   * Returns true for an object.
   */
  public boolean isObject()
  {
    return false;
  }
  
  /*
   * Returns true for a resource.
   */
  public boolean isResource()
  {
    return false;
  }
  
  /**
   * Returns true for a StringValue.
   */
  public boolean isString()
  {
    return false;
  }

  /**
   * Returns true for a BinaryValue.
   */
  public boolean isBinary()
  {
    return false;
  }

  /**
   * Returns true for a UnicodeValue.
   */
  public boolean isUnicode()
  {
    return false;
  }

  /**
   * Returns true for a BooleanValue
   */
  public boolean isBoolean()
  {
    return false;
  }
  
  /**
   * Returns true for a DefaultValue
   */
  public boolean isDefault()
  {
    return false;
  }

  /**
   * Returns true if the value is set.
   */
  public boolean isset()
  {
    return true;
  }

  /**
   * Returns true if the value is empty
   */
  public boolean isEmpty()
  {
    return false;
  }
  
  /**
   * Returns true if there are more elements.
   */
  public boolean hasCurrent()
  {
    return false;
  }

  /**
   * Returns true for equality
   */
  public Value eqValue(Value rValue)
  {
    return eq(rValue) ? BooleanValue.TRUE : BooleanValue.FALSE;
  }

  /**
   * Returns true for equality
   */
  public boolean eq(Value rValue)
  {
    if (rValue instanceof BooleanValue)
      return toBoolean() == rValue.toBoolean();
    else if (isLongConvertible() && rValue.isLongConvertible())
      return toLong() == rValue.toLong();
    else if (isNumberConvertible() || rValue.isNumberConvertible())
      return toDouble() == rValue.toDouble();
    else
      return toString().equals(rValue.toString());
  }

  /**
   * Returns true for equality
   */
  public boolean eql(Value rValue)
  {
    return this == rValue.toValue();
  }

  /**
   * Returns a negative/positive integer if this Value is
   * lessthan/greaterthan rValue.
   */
  public int cmp(Value rValue)
  {
    // This is tricky: implemented according to Table 15-5 of
    // http://us2.php.net/manual/en/language.operators.comparison.php
    
    Value lVal = toValue();
    Value rVal = rValue.toValue();

    if (lVal instanceof StringValue && rVal instanceof NullValue)
      return ((StringValue) lVal).cmpString(StringValue.EMPTY);

    if (lVal instanceof NullValue && rVal instanceof StringValue)
      return StringValue.EMPTY.cmpString((StringValue) rVal);

    if (lVal instanceof StringValue && rVal instanceof StringValue)
      return ((StringValue) lVal).cmpString((StringValue) rVal);

    if (lVal instanceof NullValue
        || lVal instanceof BooleanValue
        || rVal instanceof NullValue
        || rVal instanceof BooleanValue)
    {
      boolean lBool = toBoolean();
      boolean rBool    = rValue.toBoolean();

      if (!lBool && rBool) return -1;
      if (lBool && !rBool) return 1;
      return 0;
    }

    if (lVal.isObject() && rVal.isObject())
      return ((ObjectValue) lVal).cmpObject((ObjectValue) rVal);

    if ((lVal instanceof StringValue
         || lVal instanceof NumberValue
         || lVal instanceof ResourceValue)
        && (rVal instanceof StringValue
            || rVal instanceof NumberValue
            || rVal instanceof ResourceValue))
      return NumberValue.compareNum(lVal, rVal);

    if (lVal instanceof ArrayValue) return 1;
    if (rVal instanceof ArrayValue) return -1;
    if (lVal instanceof ObjectValue) return 1;
    if (rVal instanceof ObjectValue) return -1;

    // XXX: proper default case?
    throw new RuntimeException("values are incomparable: " +
			       lVal + " <=> " + rVal);
  }

  /**
   * Returns true for less than
   */
  public boolean lt(Value rValue)
  {
    return cmp(rValue) < 0;
  }

  /**
   * Returns true for less than or equal to
   */
  public boolean leq(Value rValue)
  {
    return cmp(rValue) <= 0;
  }

  /**
   * Returns true for greater than
   */
  public boolean gt(Value rValue)
  {
    return cmp(rValue) > 0;
  }

  /**
   * Returns true for greater than or equal to
   */
  public boolean geq(Value rValue)
  {
    return cmp(rValue) >= 0;
  }

  //
  // Conversions
  //

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return true;
  }

  /**
   * Converts to a long.
   */
  public long toLong()
  {
    return toBoolean() ? 1 : 0;
  }

  /**
   * Converts to an int
   */
  public int toInt()
  {
    return (int) toLong();
  }

  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    return 0;
  }

  /**
   * Converts to a char
   */
  public char toChar()
  {
    String s = toString();

    if (s == null || s.length() < 1)
      return 0;
    else
      return s.charAt(0);
  }

  /**
   * Converts to a string.
   *
   * @param env
   */
  public StringValue toString(Env env)
  {
    return toStringValue();
  }

  /**
   * Converts to an array.
   */
  public Value toArray()
  {
    return new ArrayValueImpl().append(this);
  }

  /**
   * Converts to an array if null.
   */
  public Value toAutoArray()
  {
    return this;
  }

  /**
   * Casts to an array.
   */
  public ArrayValue toArrayValue(Env env)
  {
    env.warning(L.l("'{0}' ({1}) is not assignable to ArrayValue",
		  this, getType()));

    return null;
  }

  /**
   * Converts to an object if null.
   */
  public Value toAutoObject(Env env)
  {
    return this;
  }

  /**
   * Converts to an object.
   */
  public Value toObject(Env env)
  {
    ObjectValue obj = env.createObject();
    
    obj.putField(env, env.createString("scalar"), this);
    
    return obj;
  }

  /**
   * Converts to a java object.
   */
  public Object toJavaObject()
  {
    return null;
  }

  /**
   * Converts to a java object.
   */
  public Object toJavaObject(Env env, Class type)
  {
    env.warning(L.l("Can't convert {0} to Java {1}",
		    getClass().getName(), type.getName()));
    
    return null;
  }

  /**
   * Converts to a java object.
   */
  public Object toJavaObjectNotNull(Env env, Class type)
  {
    env.warning(L.l("Can't convert {0} to Java {1}",
		    getClass().getName(), type.getName()));
    
    return null;
  }

  /**
   * Converts to a java boolean object.
   */
  public Boolean toJavaBoolean()
  {
    return toBoolean() ? Boolean.TRUE : Boolean.FALSE;
  }

  /**
   * Converts to a java byte object.
   */
  public Byte toJavaByte()
  {
    return new Byte((byte) toLong());
  }

  /**
   * Converts to a java short object.
   */
  public Short toJavaShort()
  {
    return new Short((short) toLong());
  }

  /**
   * Converts to a java Integer object.
   */
  public Integer toJavaInteger()
  {
    return new Integer((int) toLong());
  }

  /**
   * Converts to a java Long object.
   */
  public Long toJavaLong()
  {
    return new Long((int) toLong());
  }

  /**
   * Converts to a java Float object.
   */
  public Float toJavaFloat()
  {
    return new Float((float) toDouble());
  }

  /**
   * Converts to a java Double object.
   */
  public Double toJavaDouble()
  {
    return new Double(toDouble());
  }

  /**
   * Converts to a java Character object.
   */
  public Character toJavaCharacter()
  {
    return new Character(toChar());
  }

  /**
   * Converts to a java String object.
   */
  public String toJavaString()
  {
    return toString();
  }

  /**
   * Converts to a java Collection object.
   */
  public Collection toJavaCollection(Env env, Class type)
  {
    env.warning(L.l("Can't convert {0} to Java {1}",
            getClass().getName(), type.getName()));
    
    return null;
  }
  
  /**
   * Converts to a java List object.
   */
  public List toJavaList(Env env, Class type)
  {
    env.warning(L.l("Can't convert {0} to Java {1}",
            getClass().getName(), type.getName()));
    
    return null;
  }
  
  /**
   * Converts to a java Map object.
   */
  public Map toJavaMap(Env env, Class type)
  {
    env.warning(L.l("Can't convert {0} to Java {1}",
            getClass().getName(), type.getName()));
    
    return null;
  }

  /**
   * Converts to a Java Calendar.
   */
  public Calendar toJavaCalendar()
  {
    Calendar cal = Calendar.getInstance();
    
    cal.setTimeInMillis(toLong());
    
    return cal;
  }
  
  /**
   * Converts to a Java Date.
   */
  public Date toJavaDate()
  {
    return new Date(toLong());
  }
  
  /**
   * Converts to a Java URL.
   */
  public URL toJavaURL(Env env)
  {
    try {
      return new URL(toString());
    }
    catch (MalformedURLException e) {
      env.warning(L.l(e.getMessage()));
      return null;
    }
  }
  
  /**
   * Converts to a Java BigDecimal.
   */
  public BigDecimal toBigDecimal()
  {
    return new BigDecimal(toString());
  }
  
  /**
   * Converts to a Java BigInteger.
   */
  public BigInteger toBigInteger()
  {
    return new BigInteger(toString());
  }

  /**
   * Converts to an exception.
   */
  public QuercusException toException(Env env, String file, int line)
  {
    putField(env, env.createString("file"), env.createString(file));
    putField(env, env.createString("line"), LongValue.create(line));
    
    return new QuercusLanguageException(this);
  }

  /**
   * Converts to a raw value.
   */
  public Value toValue()
  {
    return this;
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    throw new QuercusRuntimeException(L.l("{0} is not a valid key", this));
  }

  /**
   * Convert to a ref.
   */
  public Value toRef()
  {
    return this;
  }

  /**
   * Convert to a function argument value, e.g. for
   *
   * function foo($a)
   *
   * where $a is never assigned or modified
   */
  public Value toArgValueReadOnly()
  {
    return this;
  }

  /**
   * Convert to a function argument value, e.g. for
   *
   * function foo($a)
   *
   * where $a is never assigned, but might be modified, e.g. $a[3] = 9
   */
  public Value toArgValue()
  {
    return this;
  }

  /**
   * Convert to a function argument reference value, e.g. for
   *
   * function foo(&$a)
   *
   * where $a is never assigned in the function
   */
  public Value toRefValue()
  {
    return this;
  }

  /**
   * Convert to a function argument value, e.g. for
   *
   * function foo($a)
   *
   * where $a is used as a variable in the function
   */
  public Var toVar()
  {
    return new Var(toArgValue());
  }

  /**
   * Convert to a function argument reference value, e.g. for
   *
   * function foo(&$a)
   *
   * where $a is used as a variable in the function
   */
  public Var toRefVar()
  {
    return new Var(this);
  }

  /**
   * Converts to a StringValue.
   */
  public StringValue toStringValue()
  {
    return toStringValue(Env.getInstance());
  }
  
  /*
   * Converts to a StringValue.
   */
  public StringValue toStringValue(Env env)
  {
    return toStringBuilder(env);
  }

  /**
   * Converts to a UnicodeValue.
   */
  public StringValue toUnicodeValue()
  {
    return toUnicodeValue(Env.getInstance());
  }

  /**
   * Converts to a UnicodeValue.
   */
  public StringValue toUnicodeValue(Env env)
  {
    return env.createString(toString());
  }

  /**
   * Converts to a BinaryValue.
   */
  public StringValue toBinaryValue()
  {
    return toBinaryValue(Env.getInstance());
  }

  /**
   * Converts to a BinaryValue.
   */
  public StringValue toBinaryValue(String charset)
  {
    return toBinaryValue();
  }
  
  /**
   * Converts to a BinaryValue.
   */
  public StringValue toBinaryValue(Env env)
  {
    StringValue bb = env.createBinaryBuilder();

    bb.append(this);

    return bb;
      
      /*
    try {
      int length = 0;
      while (true) {
        bb.ensureCapacity(bb.getLength() + 256);

        int sublen = is.read(bb.getBuffer(),
                             bb.getOffset(),
                             bb.getLength() - bb.getOffset());

        if (sublen <= 0)
          return bb;
        else {
          length += sublen;
          bb.setOffset(length);
        }
      }
    } catch (IOException e) {
      throw new QuercusException(e);
    }
      */
  }

  /**
   * Returns a byteArrayInputStream for the value.
   * See TempBufferStringValue for how this can be overriden
   *
   * @return InputStream
   */
  public InputStream toInputStream()
  {
    return new StringInputStream(toString());
  }

  /**
   * Converts to a string builder
   */
  public StringValue toStringBuilder()
  {
    return toStringBuilder(Env.getInstance());
  }

  /**
   * Converts to a string builder
   */
  public StringValue toStringBuilder(Env env)
  {
    return env.createUnicodeBuilder().appendUnicode(this);
  }

  /**
   * Converts to a string builder
   */
  public StringValue toStringBuilder(Env env, Value value)
  {
    return toStringBuilder(env).appendUnicode(value);
  }

  /**
   * Converts to a string builder
   */
  public StringValue toStringBuilder(Env env, StringValue value)
  {
    return toStringBuilder(env).appendUnicode(value);
  }

  /**
   * Converts to a string builder
   */
  public StringValue copyStringBuilder()
  {
    return toStringBuilder();
  }

  /**
   * Converts to a long vaule
   */
  public LongValue toLongValue()
  {
    return LongValue.create(toLong());
  }

  /**
   * Converts to a double vaule
   */
  public DoubleValue toDoubleValue()
  {
    return new DoubleValue(toDouble());
  }

  //
  // Operations
  //

  /**
   * Append to a string builder.
   */
  public StringValue appendTo(UnicodeBuilderValue sb)
  {
    return sb.append(toString());
  }

  /**
   * Append to a binary builder.
   */
  public StringValue appendTo(StringBuilderValue sb)
  {
    return sb.append(toString());
  }

  /**
   * Append to a binary builder.
   */
  public StringValue appendTo(BinaryBuilderValue sb)
  {
    return sb.appendBytes(toString());
  }
  
  /**
   * Append to a binary builder.
   */
  public StringValue appendTo(LargeStringBuilderValue sb)
  {
    return sb.append(toString());
  }

  /**
   * Copy for assignment.
   */
  public Value copy()
  {
    return this;
  }

  /**
   * Copy as an array item
   */
  public Value copyArrayItem()
  {
    return copy();
  }

  /**
   * Copy as a return value
   */
  public Value copyReturn()
  {
    // php/3a5d

    return this;
  }

  /**
   * Copy for serialization
   */
  public final Value copy(Env env)
  {
    return copy(env, new IdentityHashMap<Value,Value>());
  }

  /**
   * Copy for serialization
   */
  public Value copy(Env env, IdentityHashMap<Value,Value> map)
  {
    return this;
  }

  /**
   * Copy for serialization
   */
  public Value copyTree(Env env, CopyRoot root)
  {
    return this;
  }

  /**
   * Clone for the clone keyword
   */
  public Value clone()
  {
    return this;
  }

  /**
   * Returns the type.
   */
  public String getType()
  {
    return "value";
  }
  
  /*
   * Returns the resource type.
   */
  public String getResourceType()
  {
    return null;
  }

  /**
   * Returns the current key
   */
  public Value key()
  {
    return BooleanValue.FALSE;
  }

  /**
   * Returns the current value
   */
  public Value current()
  {
    return BooleanValue.FALSE;
  }

  /**
   * Returns the current value
   */
  public Value next()
  {
    return BooleanValue.FALSE;
  }

  /**
   * Finds the method name.
   */
  public AbstractFunction findFunction(String methodName)
  {
    return null;
  }

  //
  // Methods from hash parameters
  //

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env,
                          int hash, char []name, int nameLen,
                          Expr []args)
  {
    Value []value = new Value[args.length];

    for (int i = 0; i < args.length; i++) {
      value[i] = args[i].eval(env);
    }

    return callMethod(env, hash, name, nameLen,
                      value);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env,
                          int hash, char []name, int nameLen,
                          Value []args)
  {
    switch (args.length) {
    case 0:
      return callMethod(env, hash, name, nameLen);

    case 1:
      return callMethod(env, hash, name, nameLen,
                        args[0]);

    case 2:
      return callMethod(env, hash, name, nameLen,
                        args[0], args[1]);

    case 3:
      return callMethod(env, hash, name, nameLen,
                        args[0], args[1], args[2]);

    case 4:
      return callMethod(env, hash, name, nameLen,
                        args[0], args[1], args[2], args[3]);

    case 5:
      return callMethod(env, hash, name, nameLen,
                        args[0], args[1], args[2], args[3], args[4]);

    default:
      return errorNoMethod(env, name, nameLen);
    }
  }

  /**
   * Evaluates a method with 0 args.
   */
  public Value callMethod(Env env,
                          int hash, char []name, int nameLen)
  {
    return errorNoMethod(env, name, nameLen);
  }

  /**
   * Evaluates a method with 1 arg.
   */
  public Value callMethod(Env env,
                          int hash, char []name, int nameLen,
                          Value a0)
  {
    return errorNoMethod(env, name, nameLen);
  }

  /**
   * Evaluates a method with 1 arg.
   */
  public Value callMethod(Env env,
                          int hash, char []name, int nameLen,
                          Value a0, Value a1)
  {
    return errorNoMethod(env, name, nameLen);
  }

  /**
   * Evaluates a method with 3 args.
   */
  public Value callMethod(Env env, 
                          int hash, char []name, int nameLen,
			  Value a0, Value a1, Value a2)
  {
    return errorNoMethod(env, name, nameLen);
  }

  /**
   * Evaluates a method with 4 args.
   */
  public Value callMethod(Env env, 
                          int hash, char []name, int nameLen,
			  Value a0, Value a1, Value a2, Value a3)
  {
    return errorNoMethod(env, name, nameLen);
  }

  /**
   * Evaluates a method with 5 args.
   */
  public Value callMethod(Env env, 
                          int hash, char []name, int nameLen,
			  Value a0, Value a1, Value a2, Value a3, Value a5)
  {
    return errorNoMethod(env, name, nameLen);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, 
                             int hash, char []name, int nameLen,
                             Expr []args)
  {
    Value []value = new Value[args.length];

    for (int i = 0; i < args.length; i++) {
      value[i] = args[i].eval(env);
    }

    return callMethodRef(env, hash, name, nameLen, value);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env,
                             int hash, char []name, int nameLen,
                             Value []args)
  {
    switch (args.length) {
    case 0:
      return callMethodRef(env, hash, name, nameLen);

    case 1:
      return callMethodRef(env, hash, name, nameLen,
                           args[0]);

    case 2:
      return callMethodRef(env, hash, name, nameLen,
                           args[0], args[1]);

    case 3:
      return callMethodRef(env, hash, name, nameLen,
                           args[0], args[1], args[2]);

    case 4:
      return callMethodRef(env, hash, name, nameLen,
                           args[0], args[1], args[2], args[3]);

    case 5:
      return callMethodRef(env, hash, name, nameLen,
                           args[0], args[1], args[2], args[3], args[4]);

    default:
      return errorNoMethod(env, name, nameLen);
    }
  }

  /**
   * Evaluates a method with 0 args.
   */
  public Value callMethodRef(Env env, int hash, char []name, int nameLen)
  {
    return errorNoMethod(env, name, nameLen);
  }

  /**
   * Evaluates a method with 1 arg.
   */
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0)
  {
    return errorNoMethod(env, name, nameLen);
  }

  /**
   * Evaluates a method with 1 arg.
   */
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1)
  {
    return errorNoMethod(env, name, nameLen);
  }

  /**
   * Evaluates a method with 3 args.
   */
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1, Value a2)
  {
    return errorNoMethod(env, name, nameLen);
  }

  /**
   * Evaluates a method with 4 args.
   */
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1, Value a2, Value a3)
  {
    return errorNoMethod(env, name, nameLen);
  }

  /**
   * Evaluates a method with 5 args.
   */
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1, Value a2, Value a3, Value a5)
  {
    return errorNoMethod(env, name, nameLen);
  }

  //
  // Methods from StringValue
  //

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, StringValue nameValue, Expr []args)
  {
    Value []value = new Value[args.length];

    for (int i = 0; i < args.length; i++) {
      value[i] = args[i].eval(env);
    }

    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);

    return callMethod(env, hash, name, nameLen,
                      value);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env,
                          StringValue nameValue,
                          Value []args)
  {
    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);
    
    switch (args.length) {
    case 0:
      return callMethod(env, hash, name, nameLen);

    case 1:
      return callMethod(env, hash, name, nameLen,
                        args[0]);

    case 2:
      return callMethod(env, hash, name, nameLen,
                        args[0], args[1]);

    case 3:
      return callMethod(env, hash, name, nameLen,
                        args[0], args[1], args[2]);

    case 4:
      return callMethod(env, hash, name, nameLen,
                        args[0], args[1], args[2], args[3]);

    case 5:
      return callMethod(env, hash, name, nameLen,
                        args[0], args[1], args[2], args[3], args[4]);

    default:
      return callMethod(env, hash, name, nameLen, args);
    }
  }

  /**
   * Evaluates a method with 0 args.
   */
  public Value callMethod(Env env, StringValue nameValue)
  {
    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);

    return callMethod(env, hash, name, nameLen);
  }

  /**
   * Evaluates a method with 1 arg.
   */
  public Value callMethod(Env env,
                          StringValue nameValue,
                          Value a0)
  {
    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);
    
    return callMethod(env, hash, name, nameLen,
                      a0);
  }

  /**
   * Evaluates a method with 1 arg.
   */
  public Value callMethod(Env env,
                          StringValue nameValue,
                          Value a0, Value a1)
  {
    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);
    
    return callMethod(env, hash, name, nameLen,
                      a0, a1);
  }

  /**
   * Evaluates a method with 3 args.
   */
  public Value callMethod(Env env, 
                          StringValue nameValue,
			  Value a0, Value a1, Value a2)
  {
    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);
    
    return callMethod(env, hash, name, nameLen,
                      a0, a1, a2);
  }

  /**
   * Evaluates a method with 4 args.
   */
  public Value callMethod(Env env, 
                          StringValue nameValue,
			  Value a0, Value a1, Value a2, Value a3)
  {
    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);
    
    return callMethod(env, hash, name, nameLen,
                      a0, a1, a2, a3);
  }

  /**
   * Evaluates a method with 5 args.
   */
  public Value callMethod(Env env, 
                          StringValue nameValue,
			  Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);
    
    return callMethod(env, hash, name, nameLen,
                      a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, 
                             StringValue nameValue,
                             Expr []args)
  {
    Value []value = new Value[args.length];

    for (int i = 0; i < args.length; i++) {
      value[i] = args[i].eval(env);
    }
    
    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);

    return callMethodRef(env, hash, name, nameLen, value);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env,
                             StringValue nameValue,
                             Value []args)
  {
    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);
    
    switch (args.length) {
    case 0:
      return callMethodRef(env, hash, name, nameLen);

    case 1:
      return callMethodRef(env, hash, name, nameLen,
                           args[0]);

    case 2:
      return callMethodRef(env, hash, name, nameLen,
                           args[0], args[1]);

    case 3:
      return callMethodRef(env, hash, name, nameLen,
                           args[0], args[1], args[2]);

    case 4:
      return callMethodRef(env, hash, name, nameLen,
                           args[0], args[1], args[2], args[3]);

    case 5:
      return callMethodRef(env, hash, name, nameLen,
                           args[0], args[1], args[2], args[3], args[4]);

    default:
      return errorNoMethod(env, name, nameLen);
    }
  }

  /**
   * Evaluates a method with 0 args.
   */
  public Value callMethodRef(Env env, StringValue nameValue)
  {
    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);
    
    return callMethodRef(env, hash, name, nameLen);
  }

  /**
   * Evaluates a method with 1 arg.
   */
  public Value callMethodRef(Env env, StringValue nameValue,
                             Value a0)
  {
    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);
    
    return callMethodRef(env, hash, name, nameLen,
                         a0);
  }

  /**
   * Evaluates a method with 2 args.
   */
  public Value callMethodRef(Env env, StringValue nameValue,
                             Value a0, Value a1)
  {
    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);
    
    return callMethodRef(env, hash, name, nameLen,
                         a0, a1);
  }

  /**
   * Evaluates a method with 3 args.
   */
  public Value callMethodRef(Env env, StringValue nameValue,
                             Value a0, Value a1, Value a2)
  {
    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);
    
    return callMethodRef(env, hash, name, nameLen,
                         a0, a1, a2);
  }

  /**
   * Evaluates a method with 4 args.
   */
  public Value callMethodRef(Env env, StringValue nameValue,
                             Value a0, Value a1, Value a2, Value a3)
  {
    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);
    
    return callMethodRef(env, hash, name, nameLen,
                         a0, a1, a2, a3);
  }

  /**
   * Evaluates a method with 5 args.
   */
  public Value callMethodRef(Env env, StringValue nameValue,
                             Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    char []name = nameValue.getRawCharArray();
    int nameLen = nameValue.length();
    int hash = MethodMap.hash(name, nameLen);
    
    return callMethodRef(env, hash, name, nameLen,
                         a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  public Value callClassMethod(Env env, AbstractFunction fun, Value []args)
  {
    return NullValue.NULL;
  }

  private Value errorNoMethod(Env env, char []name, int nameLen)
  {
    String methodName =  new String(name, 0, nameLen);

    if (isNull())
      return env.error(L.l("Method call '{0}' is not allowed for a null value.",
			   methodName));
    else
      return env.error(L.l("'{0}' is an unknown method of {1}.", methodName, toDebugString()));
  }

  //
  // Arithmetic operations
  //

  /**
   * Negates the value.
   */
  public Value neg()
  {
    return LongValue.create(- toLong());
  }

  /**
   * Negates the value.
   */
  public Value pos()
  {
    return LongValue.create(toLong());
  }

  /**
   * Adds to the following value.
   */
  public Value add(Value rValue)
  {
    if (getValueType().isLongAdd() && rValue.getValueType().isLongAdd())
      return LongValue.create(toLong() + rValue.toLong());
    
    return DoubleValue.create(toDouble() + rValue.toDouble());
  }

  /**
   * Multiplies to the following value.
   */
  public Value add(long lLong)
  {
    return new DoubleValue(lLong + toDouble());
  }

  /**
   * Pre-increment the following value.
   */
  public Value preincr(int incr)
  {
    return increment(incr);
  }

  /**
   * Post-increment the following value.
   */
  public Value postincr(int incr)
  {
    return increment(incr);
  }

  /**
   * Increment the following value.
   */
  public Value increment(int incr)
  {
    long lValue = toLong();

    return LongValue.create(lValue + incr);
  }

  /**
   * Subtracts to the following value.
   */
  public Value sub(Value rValue)
  {
    if (getValueType().isLongAdd() && rValue.getValueType().isLongAdd())
      return LongValue.create(toLong() - rValue.toLong());
    
    return DoubleValue.create(toDouble() - rValue.toDouble());
  }

  /**
   * Subtracts
   */
  public Value sub(long rLong)
  {
    return new DoubleValue(toDouble() - rLong);
  }


  /**
   * Substracts from the previous value.
   */
  public Value sub_rev(long lLong)
  {
    if (getValueType().isLongAdd())
      return LongValue.create(lLong - toLong());
    else
      return new DoubleValue(lLong - toDouble());
  }

  /**
   * Multiplies to the following value.
   */
  public Value mul(Value rValue)
  {
    if (getValueType().isLongAdd() && rValue.getValueType().isLongAdd())
      return LongValue.create(toLong() * rValue.toLong());
    else
      return new DoubleValue(toDouble() * rValue.toDouble());
  }

  /**
   * Multiplies to the following value.
   */
  public Value mul(long r)
  {
    if (isLongConvertible())
      return LongValue.create(toLong() * r);
    else
      return new DoubleValue(toDouble() * r);
  }

  /**
   * Divides the following value.
   */
  public Value div(Value rValue)
  {
    if (getValueType().isLongAdd() && rValue.getValueType().isLongAdd()) {
      long l = toLong();
      long r = rValue.toLong();
      
      if (r != 0 && l % r == 0)
        return LongValue.create(l / r);
      else
        return new DoubleValue(toDouble() / rValue.toDouble());
    }
    else
      return new DoubleValue(toDouble() / rValue.toDouble());
  }
  
  /**
   * Multiplies to the following value.
   */
  public Value div(long r)
  {
    long l = toLong();
    
    if (r != 0 && l % r == 0)
      return LongValue.create(l / r);
    else
      return new DoubleValue(toDouble() / r);
  }

  /**
   * modulo the following value.
   */
  public Value mod(Value rValue)
  {
    double lDouble = toDouble();
    double rDouble = rValue.toDouble();

    return LongValue.create((long) lDouble % rDouble);
  }

  /**
   * Shifts left by the value.
   */
  public Value lshift(Value rValue)
  {
    long lLong = toLong();
    long rLong = rValue.toLong();

    return LongValue.create(lLong << rLong);
  }

  /**
   * Shifts right by the value.
   */
  public Value rshift(Value rValue)
  {
    long lLong = toLong();
    long rLong = rValue.toLong();

    return LongValue.create(lLong >> rLong);
  }
  
  /*
   * Binary And.
   */
  public Value bitAnd(Value rValue)
  {
    return LongValue.create(toLong() & rValue.toLong());
  }
  
  /*
   * Binary or.
   */
  public Value bitOr(Value rValue)
  {
    return LongValue.create(toLong() | rValue.toLong());
  }
  
  /*
   * Binary xor.
   */
  public Value bitXor(Value rValue)
  {
    return LongValue.create(toLong() ^ rValue.toLong());
  }
  
  /**
   * Absolute value.
   */
  public Value abs()
  {
    if (getValueType().isDoubleCmp())
      return new DoubleValue(Math.abs(toDouble()));
    else
      return LongValue.create(Math.abs(toLong()));
  }

  /**
   * Returns the next array index based on this value.
   */
  public long nextIndex(long oldIndex)
  {
    return oldIndex;
  }

  //
  // string functions
  //

  /**
   * Returns the length as a string.
   */
  public int length()
  {
    return toStringValue().length();
  }

  //
  // Array functions
  //

  /**
   * Returns the array size.
   */
  public int getSize()
  {
    return 1;
  }

  /**
   * Returns the count, as returned by the global php count() function
   */
  public int getCount(Env env)
  {
    return 1;
  }

  /**
   * Returns the count, as returned by the global php count() function
   */
  public int getCountRecursive(Env env)
  {
    return getCount(env);
  }

  /**
   * Returns an iterator for the key => value pairs.
   */
  public Iterator<Map.Entry<Value, Value>> getIterator(Env env)
  {
    Set<Map.Entry<Value, Value>> emptySet = Collections.emptySet();

    return emptySet.iterator();
  }

  /**
   * Returns an iterator for the field keys.
   * The default implementation uses the Iterator returned
   * by {@link #getIterator(Env)}; derived classes may override and
   * provide a more efficient implementation.
   */
  public Iterator<Value> getKeyIterator(Env env)
  {
    final Iterator<Map.Entry<Value, Value>> iter = getIterator(env);

    return new Iterator<Value>() {
      public boolean hasNext() { return iter.hasNext(); }
      public Value next()      { return iter.next().getKey(); }
      public void remove()     { iter.remove(); }
    };
  }

  /**
   * Returns the field keys.
   */
  public Value []getKeyArray(Env env)
  {
    return NULL_VALUE_ARRAY;
  }

  /**
   * Returns the field values.
   */
  public Value []getValueArray(Env env)
  {
    return NULL_VALUE_ARRAY;
  }
  
  /**
   * Returns an iterator for the field values.
   * The default implementation uses the Iterator returned
   * by {@link #getIterator(Env)}; derived classes may override and
   * provide a more efficient implementation.
   */
  public Iterator<Value> getValueIterator(Env env)
  {
    final Iterator<Map.Entry<Value, Value>> iter = getIterator(env);

    return new Iterator<Value>() {
      public boolean hasNext() { return iter.hasNext(); }
      public Value next()      { return iter.next().getValue(); }
      public void remove()     { iter.remove(); }
    };
  }
  
  //
  // Object field references
  //

  /**
   * Returns the field value
   */
  public Value getField(Env env, StringValue name)
  {
    return NullValue.NULL;
  }

  /**
   * Returns the field ref.
   */
  public Var getFieldRef(Env env, StringValue name)
  {
    Value value = getField(env, name);

    if (value instanceof Var)
      return (Var) value;
    else
      return new Var(value);
  }

  /**
   * Returns the field used as a method argument
   */
  public Value getFieldArg(Env env, StringValue name, boolean isTop)
  {
    return getFieldRef(env, name);
  }

  /**
   * Returns the field ref for an argument.
   */
  public Value getFieldArgRef(Env env, StringValue name)
  {
    return getFieldRef(env, name);
  }

  /**
   * Returns the value for a field, creating an object if the field
   * is unset.
   */
  public Value getFieldObject(Env env, StringValue name)
  {
    Value v = getField(env, name);

    if (! v.isset()) {
      v = env.createObject();

      putField(env, name, v);
    }

    return v;
  }

  /**
   * Returns the value for a field, creating an object if the field
   * is unset.
   */
  public Value getFieldArray(Env env, StringValue name)
  {
    Value v = getField(env, name);

    Value array = v.toAutoArray();

    if (v == array)
      return v;
    else {
      putField(env, name, array);

      return array;
    }
  }

  /**
   * Returns the field ref.
   */
  public Value putField(Env env, StringValue name, Value object)
  {
    return NullValue.NULL;
  }
  
  public final Value putField(Env env, StringValue name, Value value,
                              Value innerIndex, Value innerValue)
  {
    Value result = value.append(innerIndex, innerValue);

    return putField(env, name, result);
  }

  /**
   * Returns true if the field is set
   */
  public boolean issetField(StringValue name)
  {
    return false;
  }

  /**
   * Removes the field ref.
   */
  public void unsetField(StringValue name)
  {
  }

  /**
   * Returns the field value
   */
  public Value getThisField(Env env, StringValue name)
  {
    return getField(env, name);
  }

  /**
   * Returns the field ref.
   */
  public Var getThisFieldRef(Env env, StringValue name)
  {
    Value value = getThisField(env, name);

    if (value instanceof Var)
      return (Var) value;
    else
      return new Var(value);
  }

  /**
   * Returns the field used as a method argument
   */
  public Value getThisFieldArg(Env env, StringValue name)
  {
    return getThisFieldRef(env, name);
  }

  /**
   * Returns the field ref for an argument.
   */
  public Value getThisFieldArgRef(Env env, StringValue name)
  {
    return getThisFieldRef(env, name);
  }

  /**
   * Returns the value for a field, creating an object if the field
   * is unset.
   */
  public Value getThisFieldObject(Env env, StringValue name)
  {
    Value v = getThisField(env, name);

    if (! v.isset()) {
      v = env.createObject();

      putThisField(env, name, v);
    }

    return v;
  }

  /**
   * Returns the value for a field, creating an object if the field
   * is unset.
   */
  public Value getThisFieldArray(Env env, StringValue name)
  {
    Value v = getThisField(env, name);

    Value array = v.toAutoArray();

    if (v == array)
      return v;
    else {
      putField(env, name, array);

      return array;
    }
  }

  /**
   * Initializes a new field, does not call __set if it is defined.
   */
  public void initField(StringValue key,
                        Value value,
                        FieldVisibility visibility)
  {
    putThisField(Env.getInstance(), key, value);
  }
  
  /**
   * Returns the field ref.
   */
  public Value putThisField(Env env, StringValue name, Value object)
  {
    return putField(env, name, object);
  }

  /**
   * Returns true if the field is set
   */
  public boolean issetThisField(StringValue name)
  {
    return issetField(name);
  }

  /**
   * Removes the field ref.
   */
  public void unsetThisField(StringValue name)
  {
    unsetField(name);
  }

  //
  // field convenience
  //
  
  public Value putField(Env env, String name, Value value)
  {
    return putThisField(env, env.createString(name), value);
  }
  
  /**
   * Returns the array ref.
   */
  public Value get(Value index)
  {
    return UnsetValue.UNSET;
  }

  /**
   * Returns a reference to the array value.
   */
  public Var getRef(Value index)
  {
    Value value = get(index);

    if (value instanceof Var)
      return (Var) value;
    else
      return new Var(value);
  }

  /**
   * Returns the array ref as a function argument.
   */
  public Value getArg(Value index, boolean isTop)
  {
    return get(index);
  }

  /**
   * Returns the array value, copying on write if necessary.
   */
  public Value getDirty(Value index)
  {
    return get(index);
  }

  /**
   * Returns the value for a field, creating an array if the field
   * is unset.
   */
  public Value getArray()
  {
    return this;
  }

  /**
   * Returns the value for a field, creating an array if the field
   * is unset.
   */
  public Value getArray(Value index)
  {
    return NullValue.NULL;
  }

  /**
   * Returns the value for the variable, creating an object if the var
   * is unset.
   */
  public Value getObject(Env env)
  {
    return NullValue.NULL;
  }

  /**
   * Returns the value for a field, creating an object if the field
   * is unset.
   */
  public Value getObject(Env env, Value index)
  {
    return NullValue.NULL;
  }

  /**
   * Sets the value ref.
   */
  public Value set(Value value)
  {
    return value;
  }

  /**
   * Sets the array ref.
   */
  public Value put(Value index, Value value)
  {
    return value;
  }
  
  /**
   * Sets the array ref.
   */
  public final Value put(Value index, Value value,
                         Value innerIndex, Value innerValue)
  {
    Value result = value.append(innerIndex, innerValue);
    
    put(index, result);
    
    return innerValue;
  }

  /**
   * Appends an array value
   */
  public Value put(Value value)
  {
    return value;
  }

  /**
   * Sets the array value, returning the new array, e.g. to handle
   * string update ($a[0] = 'A').  Creates an array automatically if
   * necessary.
   */
  public Value append(Value index, Value value)
  {
    return this;
  }

  /**
   * Sets the array ref.
   */
  public Var putRef()
  {
    return new Var();
  }

  /**
   * Appends the array
   */
  public Value putArray()
  {
    ArrayValue value = new ArrayValueImpl();

    put(value);

    return value;
  }

  /**
   * Appends a new object
   */
  public Value putObject(Env env)
  {
    Value value = env.createObject();

    put(value);

    return value;
  }

  /**
   * Return true if the array value is set
   */
  public boolean isset(Value index)
  {
    return false;
  }

  /**
   * Return unset the value.
   */
  public Value remove(Value index)
  {
    return UnsetValue.UNSET;
  }
  
  /**
   * Takes the values of this array, unmarshalls them to objects of type
   * <i>elementType</i>, and puts them in a java array.
   */
  public Object valuesToArray(Env env, Class elementType)
  {
    env.error(L.l("Can't assign {0} with type {1} to {2}[]", this, this.getClass(), elementType));
    return null;
  }

  /**
   * Returns the character at the named index.
   */
  public Value charValueAt(long index)
  {
    return NullValue.NULL;
  }

  /**
   * Sets the character at the named index.
   */
  public Value setCharValueAt(long index, Value value)
  {
    return NullValue.NULL;
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
  {
    env.print(toString(env));
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env, WriteStream out)
  {
    try {
      out.print(toString(env));
    } catch (IOException e) {
      throw new QuercusRuntimeException(e);
    }
  }
  
  /*
   * Serializes the value.
   * 
   * @param env
   * @param sb holds result of serialization
   * @param serializeMap holds reference indexes
   */
  public void serialize(Env env,
                        StringBuilder sb,
                        SerializeMap serializeMap)
  {
    serializeMap.incrementIndex();
    
    serialize(env, sb);
  }
  
  /**
   * Serializes the value.
   */
  public void serialize(Env env, StringBuilder sb)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Exports the value.
   */
  public void varExport(StringBuilder sb)
  {
    throw new UnsupportedOperationException(getClass().getName());
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
  }
  
  protected static void printJavaChar(PrintWriter out, char ch)
  {
    switch (ch) {
      case '\r':
        out.print("\\r");
        break;
      case '\n':
        out.print("\\n");
        break;
      //case '\"':
      //  out.print("\\\"");
      //  break;
      case '\'':
        out.print("\\\'");
        break;
      case '\\':
        out.print("\\\\");
        break;
      default:
        out.print(ch);
        break;
      }
  }

  protected static void printJavaString(PrintWriter out, StringValue s)
  {
    if (s == null) {
      out.print("");
      return;
    }

    int len = s.length();
    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      switch (ch) {
      case '\r':
	out.print("\\r");
	break;
      case '\n':
	out.print("\\n");
	break;
      case '\"':
	out.print("\\\"");
	break;
      case '\'':
	out.print("\\\'");
	break;
      case '\\':
	out.print("\\\\");
	break;
      default:
	out.print(ch);
	break;
      }
    }
  }

  public String toInternString()
  {
    return toString().intern();
  }

  public String toDebugString()
  {
    return toString();
  }

  public final void varDump(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    if (valueSet.get(this) != null) {
      out.print("#recursion#");
      return;
    }

    valueSet.put(this, "printing");

    try {
      varDumpImpl(env, out, depth, valueSet);
    }
    finally {
      valueSet.remove(this);
    }
  }

  protected void varDumpImpl(Env env,
                             WriteStream out,
                             int depth,
                             IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.print("resource(" + toString() + ")");
  }

  public final void printR(Env env,
                           WriteStream out,
                           int depth,
                           IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    if (valueSet.get(this) != null) {
      out.print("#recursion#");
      return;
    }

    valueSet.put(this, "printing");

    try {
      printRImpl(env, out, depth, valueSet);
    }
    finally {
      valueSet.remove(this);
    }
  }

  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.print(toString());
  }

  protected void printDepth(WriteStream out, int depth)
    throws IOException
  {
    for (int i = 0; i < depth; i++)
      out.print(' ');
  }
}

