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

package com.caucho.quercus.expr;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;

/**
 * Represents a PHP list() = each() assignment expression.
 */
public class ListEachExpr extends Expr {
  protected final AbstractVarExpr _keyVar;
  protected final AbstractVarExpr _valueVar;
  protected final Expr _value;

  public ListEachExpr(Expr []varList, EachExpr each)
  {
    if (varList.length > 0) {
      // XXX: need test
      _keyVar = (AbstractVarExpr) varList[0];
    }
    else
      _keyVar = null;

    if (varList.length > 1) {
      // XXX: need test
      _valueVar = (AbstractVarExpr) varList[1];
    }
    else
      _valueVar = null;

    _value = each.getExpr();
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value eval(Env env)
  {
    Value value = _value.eval(env);

    if (! (value instanceof ArrayValue))
      return NullValue.NULL;

    ArrayValue array = (ArrayValue) value;
    /*
    if (! array.hasCurrent())
      return BooleanValue.FALSE;
    */

    if (_keyVar != null)
      _keyVar.evalAssign(env, array.key());

    if (_valueVar != null)
      _valueVar.evalAssign(env, array.current());

    return array.each();
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public boolean evalBoolean(Env env)
  {
    Value value = _value.eval(env);

    if (! (value instanceof ArrayValue))
      return false;

    ArrayValue array = (ArrayValue) value;

    if (! array.hasCurrent())
      return false;

    if (_keyVar != null)
      _keyVar.evalAssign(env, array.key());

    if (_valueVar != null)
      _valueVar.evalAssign(env, array.current());

    array.next();

    return true;
  }
}

