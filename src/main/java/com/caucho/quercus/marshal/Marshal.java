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

package com.caucho.quercus.marshal;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;
import com.caucho.util.L10N;

/**
 * Code for marshaling (PHP to Java) and unmarshaling (Java to PHP) arguments.
 */
abstract public class Marshal {
  protected static final L10N L = new L10N(Marshal.class);

  // scale to describe cost of marshaling an argument
  public static final int ZERO = 0;
  public static final int ONE = 100;
  public static final int TWO = 200;
  public static final int THREE = 300;
  public static final int FOUR = 400;
  public static final int MAX = Integer.MAX_VALUE / 32;

  public static final int PHP5_STRING_VALUE_COST = ZERO;
  public static final int PHP5_BYTE_ARRAY_COST = ONE             + 1;
  public static final int PHP5_CHARACTER_ARRAY_COST = ONE        + 2;
  public static final int PHP5_STRING_COST = ONE                 + 3;
  public static final int PHP5_BYTE_OBJECT_ARRAY_COST = ONE      + 4;
  public static final int PHP5_CHARACTER_OBJECT_ARRAY_COST = ONE + 5;
  public static final int PHP5_BINARY_VALUE_COST = ONE           + 6;

  public static final int UNICODE_STRING_VALUE_COST = ONE           + 0;
  public static final int UNICODE_BINARY_VALUE_COST = ONE           + 1;
  public static final int UNICODE_CHARACTER_ARRAY_COST = ONE        + 2;
  public static final int UNICODE_STRING_COST = ONE +               + 3;
  public static final int UNICODE_BYTE_ARRAY_COST = ONE             + 4;
  public static final int UNICODE_CHARACTER_OBJECT_ARRAY_COST = ONE + 5;
  public static final int UNICODE_BYTE_OBJECT_ARRAY_COST = ONE      + 6;

  public static final int BINARY_BINARY_VALUE_COST = ZERO;
  public static final int BINARY_STRING_VALUE_COST = ONE           + 1;
  public static final int BINARY_BYTE_ARRAY_COST = ONE             + 2;
  public static final int BINARY_STRING_COST = ONE                 + 3;
  public static final int BINARY_CHARACTER_ARRAY_COST = ONE        + 4;
  public static final int BINARY_BYTE_OBJECT_ARRAY_COST = ONE      + 5;
  public static final int BINARY_CHARACTER_OBJECT_ARRAY_COST = ONE + 6;

  public static final int DOUBLE_CONVERTIBLE_DOUBLE_VALUE_COST = THREE;
  public static final int DOUBLE_CONVERTIBLE_DOUBLE_COST = THREE        + 1;
  public static final int DOUBLE_CONVERTIBLE_DOUBLE_OBJECT_COST = THREE + 2;
  public static final int DOUBLE_CONVERTIBLE_FLOAT_COST = THREE         + 3;
  public static final int DOUBLE_CONVERTIBLE_FLOAT_OBJECT_COST = THREE  + 4;
  public static final int DOUBLE_CONVERTIBLE_LONG_VALUE_COST = THREE    + 5;
  public static final int DOUBLE_CONVERTIBLE_LONG_COST = THREE          + 6;
  public static final int DOUBLE_CONVERTIBLE_LONG_OBJECT_COST = THREE   + 7;
  public static final int DOUBLE_CONVERTIBLE_INTEGER_COST = THREE       + 8;
  public static final int DOUBLE_CONVERTIBLE_INTEGER_OBJECT_COST = THREE+ 9;
  public static final int DOUBLE_CONVERTIBLE_BYTE_COST = THREE          + 10;
  public static final int DOUBLE_CONVERTIBLE_BYTE_OBJECT_COST = THREE   + 11;

  public static final int LONG_CONVERTIBLE_DOUBLE_VALUE_COST = THREE;
  public static final int LONG_CONVERTIBLE_LONG_VALUE_COST = THREE    + 1;
  public static final int LONG_CONVERTIBLE_DOUBLE_COST = THREE        + 2;
  public static final int LONG_CONVERTIBLE_DOUBLE_OBJECT_COST = THREE + 3;
  public static final int LONG_CONVERTIBLE_FLOAT_COST = THREE         + 4;
  public static final int LONG_CONVERTIBLE_FLOAT_OBJECT_COST = THREE  + 5;
  public static final int LONG_CONVERTIBLE_LONG_COST = THREE          + 6;
  public static final int LONG_CONVERTIBLE_LONG_OBJECT_COST = THREE   + 7;
  public static final int LONG_CONVERTIBLE_INTEGER_COST = THREE       + 8;
  public static final int LONG_CONVERTIBLE_INTEGER_OBJECT_COST = THREE+ 9;
  public static final int LONG_CONVERTIBLE_BYTE_COST = THREE          + 10;
  public static final int LONG_CONVERTIBLE_BYTE_OBJECT_COST = THREE   + 11;

  /**
   * Returns true if the result is a primitive boolean.
   */
  public boolean isBoolean()
  {
    return false;
  }

  /**
   * Returns true if the result is a string.
   */
  public boolean isString()
  {
    return false;
  }

  /**
   * Returns true if the result is a long.
   */
  public boolean isLong()
  {
    return false;
  }

  /**
   * Returns true if the result is a double.
   */
  public boolean isDouble()
  {
    return false;
  }

  /**
   * Return true for read-only.
   */
  public boolean isReadOnly()
  {
    return true;
  }

  /**
   * Return true for a reference
   */
  public boolean isReference()
  {
    return false;
  }

  /**
   * Return true if is a Value.
   */
  public boolean isValue()
  {
    return false;
  }
  
  abstract public <T> T marshal(Env env, Expr expr, Class<T> argClass);

  @SuppressWarnings("unchecked")
  public <T> T marshal(Env env, Value value, Class<T> argClass)
  {
    return (T)value;
  }

  public Value unmarshal(Env env, Object value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public final int getMarshalingCost(Value value)
  {
    Class expectedClass = getExpectedClass();

    if (expectedClass.equals(value.getClass()))
      return ZERO;
    
    return getMarshalingCostImpl(value);
  }
  
  protected int getMarshalingCostImpl(Value value)
  {
    throw new UnsupportedOperationException(getClass().toString());
  }
  
  public int getMarshalingCost(Expr expr)
  {
    return MAX;
  }
  
  public Class getExpectedClass()
  {
    throw new UnsupportedOperationException(getClass().toString());
  }
}

