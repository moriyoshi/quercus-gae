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

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;

import java.util.ArrayList;

/**
 * Represents a list assignment expression.
 */
public class ListHeadExpr extends Expr {
  protected final Expr []_varList;
  protected final Value []_keyList;

  private String _varName;

  public ListHeadExpr(ArrayList<Expr> varList)
  {
    _varList = new Expr[varList.size()];
    varList.toArray(_varList);

    _keyList = new Value[varList.size()];

    for (int i = 0; i < varList.size(); i++)
      _keyList[i] = LongValue.create(i);
  }

  public Expr []getVarList()
  {
    return _varList;
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
    throw new UnsupportedOperationException();
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalAssign(Env env, Value value)
  {
    int len = _varList.length;

    for (int i = 0; i < len; i++) {
      if (_varList[i] != null)
        _varList[i].evalAssign(env, value.get(_keyList[i]).copy());
    }
  }
}

