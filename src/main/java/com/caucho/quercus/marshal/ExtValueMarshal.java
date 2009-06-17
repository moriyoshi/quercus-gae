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

/**
 * Code for marshaling (PHP to Java) and unmarshaling (Java to PHP) arguments.
 */
public class ExtValueMarshal extends Marshal
{
  private Class<?> _expectedClass;
  
  public ExtValueMarshal(Class expectedClass)
  {
    _expectedClass = expectedClass;
  }
  
  public boolean isReadOnly()
  {
    return false;
  }

  /**
   * Return true if is a Value.
   */
  @Override
  public boolean isValue()
  {
    return true;
  }
  
  public <T> T marshal(Env env, Expr expr, Class<T> expectedClass)
  {
    return marshal(env, expr.eval(env), expectedClass);
  }

  @SuppressWarnings("unchecked")
  public <T> T marshal(Env env, Value value, Class<T> expectedClass)
  {
    if (value == null || ! value.isset())
      return null;

    // XXX: need QA, added for mantis view bug page
    value = value.toValue();

    if (expectedClass.isAssignableFrom(value.getClass()))
      return (T)value;
    else {
      String className = expectedClass.getName();
      int p = className.lastIndexOf('.');
      className = className.substring(p + 1);

      String valueClassName = value.getClass().getName();
      p = valueClassName.lastIndexOf('.');
      valueClassName = valueClassName.substring(p + 1);

      env.warning(L.l("'{0}' of type `{1}' is an unexpected argument, expected {2}",
                      value, valueClassName, className));

      return null;
    }
  }

  public Value unmarshal(Env env, Object value)
  {
    return (Value) value;
  }
  
  @Override
  protected int getMarshalingCostImpl(Value argValue)
  {
    if (_expectedClass.isAssignableFrom(argValue.getClass()))
      return Marshal.ONE;
    else
      return Marshal.FOUR;
  }
  
  @Override
  public Class<?> getExpectedClass()
  {
    return _expectedClass;
  }
}
