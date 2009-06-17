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
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;

public class ShortObjectMarshal extends Marshal
{
  public static final Marshal MARSHAL = new ShortObjectMarshal();
  
  public boolean isReadOnly()
  {
    return true;
  }

  @SuppressWarnings("unchecked")
  public <T> T marshal(Env env, Expr expr, Class<T> expectedClass)
  {
    return (T)new Short((short) expr.evalLong(env));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T marshal(Env env, Value value, Class<T> expectedClass)
  {
    return (T)value.toJavaShort();
  }

  public Value unmarshal(Env env, Object value)
  {
    if (value == null)
      return LongValue.ZERO;
    else
      return LongValue.create(((Number) value).longValue());
  }
  
  @Override
  protected int getMarshalingCostImpl(Value argValue)
  {
    if (argValue instanceof LongValue)
      return Marshal.ONE;
    else if (argValue.isLongConvertible())
      return Marshal.THREE;
    else if (argValue.isNumeric())
      return Marshal.THREE;
    else
      return Marshal.FOUR;
  }
  
  @Override
  public Class getExpectedClass()
  {
    return Short.class;
  }
}
