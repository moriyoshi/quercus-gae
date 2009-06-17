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

import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.Expr;

/**
 * Code for marshalling arguments.
 */
public class EnumMarshal extends Marshal {
  private Class<Enum<?>> _enumClass;

  public EnumMarshal(Class<Enum<?>> enumClass)
  {
    _enumClass = enumClass;
  }

  @SuppressWarnings("unchecked")
  public <TT> TT marshal(Env env, Expr expr, Class<TT> argClass)
  {
    String name = expr.evalString(env);

    return (TT)Enum.valueOf((Class<Enum>)_enumClass, name);
  }
  
  @SuppressWarnings("unchecked")
  public <TT> TT marshal(Env env, Value value, Class<TT> argClass)
  {
    String name = value.toString();

    return (TT)Enum.valueOf((Class<Enum>)_enumClass, name);
  }
  
  public Value unmarshal(Env env, Object value)
  {
    if (value == null)
      return NullValue.NULL;
    else
      return env.createStringOld(value.toString());
  }
  
  @Override
  public Class getExpectedClass()
  {
    return _enumClass;
  }
}

