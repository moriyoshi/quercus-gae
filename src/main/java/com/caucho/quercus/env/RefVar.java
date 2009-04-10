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

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Represents a reference to a PHP variable in a function call.
 */
public class RefVar extends Value
  implements Serializable
{
  private Var _var;

  public RefVar(Var var)
  {
    _var = var;
  }

  /**
   * Returns true for an implementation of a class
   */
  @Override
  public boolean isA(String name)
  {
    return _var.isA(name);
  }

  /**
   * True for a long
   */
  @Override
  public boolean isLongConvertible()
  {
    return _var.isLongConvertible();
  }

  /**
   * True to a double.
   */
  @Override
  public boolean isDoubleConvertible()
  {
    return _var.isDoubleConvertible();
  }

  /**
   * True for a number
   */
  @Override
  public boolean isNumberConvertible()
  {
    return _var.isNumberConvertible();
  }
  
  /**
   * Returns true for a long-value.
   */
  public boolean isLong()
  {
    return _var.isLong();
  }
  
  /**
   * Returns true for a long-value.
   */
  public boolean isDouble()
  {
    return _var.isDouble();
  }

  /**
   * Converts to a boolean.
   */
  @Override
  public boolean toBoolean()
  {
    return _var.toBoolean();
  }

  /**
   * Converts to a long.
   */
  @Override
  public long toLong()
  {
    return _var.toLong();
  }

  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return _var.toDouble();
  }

  /**
   * Converts to a string.
   * @param env
   */
  @Override
  public StringValue toString(Env env)
  {
    return _var.toString(env);
  }

  /**
   * Converts to an object.
   */
  @Override
  public Value toObject(Env env)
  {
    return _var.toObject(env);
  }

  /**
   * Converts to an object.
   */
  @Override
  public Object toJavaObject()
  {
    return _var.toJavaObject();
  }

  /**
   * Converts to a raw value.
   */
  @Override
  public Value toValue()
  {
    return _var.toValue();
  }

  /**
   * Returns true for an object.
   */
  @Override
  public boolean isObject()
  {
    return _var.isObject();
  }

  /**
   * Returns true for an array.
   */
  @Override
  public boolean isArray()
  {
    return _var.isArray();
  }

  /**
   * Copy the value.
   */
  @Override
  public Value copy()
  {
    // quercus/0d05
    return this;
  }

  /**
   * Converts to an argument value.
   */
  @Override
  public Value toArgValueReadOnly()
  {
    return _var;
  }

  /**
   * Converts to an argument value.
   */
  @Override
  public Value toArgValue()
  {
    // php/0471
    return _var;
  }

  /**
   * Converts to an argument value.
   */
  @Override
  public Value toRefValue()
  {
    return _var;
  }

  /**
   * Converts to a variable
   */
  @Override
  public Var toVar()
  {
    return _var;
  }

  /**
   * Converts to a reference variable
   */
  @Override
  public Var toRefVar()
  {
    return _var;
  }

  @Override
  public StringValue toStringValue()
  {
    return _var.toStringValue();
  }

  @Override
  public StringValue toBinaryValue(Env env)
  {
    return _var.toBinaryValue(env);
  }

  @Override
  public StringValue toUnicodeValue(Env env)
  {
    return _var.toUnicodeValue(env);
  }

  @Override
  public StringValue toStringBuilder()
  {
    return _var.toStringBuilder();
  }

  @Override
  public StringValue toStringBuilder(Env env)
  {
    return _var.toStringBuilder(env);
  }

  @Override
  public java.io.InputStream toInputStream()
  {
    return _var.toInputStream();
  }
  
  /**
   * Negates the value.
   */
  @Override
  public Value neg()
  {
    return _var.neg();
  }

  /**
   * Adds to the following value.
   */
  @Override
  public Value add(Value rValue)
  {
    return _var.add(rValue);
  }

  /**
   * Adds to the following value.
   */
  @Override
  public Value add(long rValue)
  {
    return _var.add(rValue);
  }

  /**
   * Pre-increment the following value.
   */
  @Override
  public Value preincr(int incr)
  {
    return _var.preincr(incr);
  }

  /**
   * Post-increment the following value.
   */
  @Override
  public Value postincr(int incr)
  {
    return _var.postincr(incr);
  }

  /**
   * Increment the following value.
   */
  @Override
  public Value increment(int incr)
  {
    return _var.increment(incr);
  }

  /**
   * Subtracts to the following value.
   */
  @Override
  public Value sub(Value rValue)
  {
    return _var.sub(rValue);
  }

  /**
   * Subtracts to the following value.
   */
  @Override
  public Value sub(long rValue)
  {
    return _var.sub(rValue);
  }
  
  /**
   * Multiplies to the following value.
   */
  @Override
  public Value mul(Value rValue)
  {
    return _var.mul(rValue);
  }

  /**
   * Multiplies to the following value.
   */
  @Override
  public Value mul(long lValue)
  {
    return _var.mul(lValue);
  }

  /**
   * Divides the following value.
   */
  @Override
  public Value div(Value rValue)
  {
    return _var.div(rValue);
  }

  /**
   * Shifts left by the value.
   */
  @Override
  public Value lshift(Value rValue)
  {
    return _var.lshift(rValue);
  }

  /**
   * Shifts right by the value.
   */
  @Override
  public Value rshift(Value rValue)
  {
    return _var.rshift(rValue);
  }
  
  /**
   * Absolute value.
   */
  public Value abs()
  {
    return _var.abs();
  }

  /**
   * Returns true for equality
   */
  @Override
  public boolean eql(Value rValue)
  {
    return _var.eql(rValue);
  }

  /**
   * Returns the array/object size
   */
  @Override
  public int getSize()
  {
    return _var.getSize();
  }

  @Override
  public Iterator<Map.Entry<Value, Value>> getIterator(Env env)
  {
    return _var.getIterator(env);
  }

  @Override
  public Iterator<Value> getKeyIterator(Env env)
  {
    return _var.getKeyIterator(env);
  }

  @Override
  public Iterator<Value> getValueIterator(Env env)
  {
    return _var.getValueIterator(env);
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Value get(Value index)
  {
    return _var.get(index);
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Var getRef(Value index)
  {
    return _var.getRef(index);
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Value put(Value index, Value value)
  {
    return _var.put(index, value);
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Value put(Value value)
  {
    return _var.put(value);
  }

  /**
   * Returns the character at an index
   */
  /* XXX: need test first
  public Value charAt(long index)
  {
    return _ref.charAt(index);
  }
  */

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env,
                          int hash, char []name, int nameLen,
                          Expr []args)
  {
    return _var.callMethod(env, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env,
                          int hash, char []name, int nameLen,
                          Value []args)
  {
    return _var.callMethod(env, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen)
  {
    return _var.callMethod(env, hash, name, nameLen);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env,
                          int hash, char []name, int nameLen,
                          Value a0)
  {
    return _var.callMethod(env, hash, name, nameLen, a0);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value a0, Value a1)
  {
    return _var.callMethod(env, hash, name, nameLen, a0, a1);
  }

  /**
   * Evaluates a method with 3 args.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
			  Value a0, Value a1, Value a2)
  {
    return _var.callMethod(env, hash, name, nameLen,
                           a0, a1, a2);
  }

  /**
   * Evaluates a method with 4 args.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
			  Value a0, Value a1, Value a2, Value a3)
  {
    return _var.callMethod(env, hash, name, nameLen,
                           a0, a1, a2, a3);
  }

  /**
   * Evaluates a method with 5 args.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
			  Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return _var.callMethod(env, hash, name, nameLen,
                           a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env,
                             int hash, char []name, int nameLen,
                             Expr []args)
  {
    return _var.callMethodRef(env, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env,
                             int hash, char []name, int nameLen,
                             Value []args)
  {
    return _var.callMethodRef(env, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen)
  {
    return _var.callMethodRef(env, hash, name, nameLen);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env,
                             int hash, char []name, int nameLen,
                             Value a0)
  {
    return _var.callMethodRef(env, hash, name, nameLen, a0);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1)
  {
    return _var.callMethodRef(env, hash, name, nameLen,
                              a0, a1);
  }

  /**
   * Evaluates a method with 3 args.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1, Value a2)
  {
    return _var.callMethodRef(env, hash, name, nameLen,
                              a0, a1, a2);
  }

  /**
   * Evaluates a method with 4 args.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a0, Value a1, Value a2, Value a3)
  {
    return _var.callMethodRef(env, hash, name, nameLen,
                              a0, a1, a2, a3);
  }

  /**
   * Evaluates a method with 5 args.
   */
  @Override
  public Value callMethodRef(Env env,
                             int hash, char []name, int nameLen,
                             Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return _var.callMethodRef(env, hash, name, nameLen,
                              a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callClassMethod(Env env, AbstractFunction fun, Value []args)
  {
    return _var.callClassMethod(env, fun, args);
  }

  /**
   * Serializes the value.
   */
  public void serialize(Env env, StringBuilder sb)
  {
    _var.serialize(env, sb);
  }
  
  /*
   * Serializes the value.
   * 
   * @param sb holds result of serialization
   * @param serializeMap holds reference indexes
   */
  public void serialize(Env env, StringBuilder sb, SerializeMap serializeMap)
  {
    _var.serialize(env, sb, serializeMap);
  }
  
  /**
   * Prints the value.
   * @param env
   */
  @Override
  public void print(Env env)
  {
    _var.print(env);
  }

  @Override
  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value,String> valueSet)
    throws IOException
  {
    out.print("&");
    toValue().varDumpImpl(env, out, depth, valueSet);
  }

  @Override
  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    toValue().printRImpl(env, out, depth, valueSet);
  }
  
  //
  // Java Serialization
  //

  public Object writeReplace()
  {
    return _var;
  }
}

