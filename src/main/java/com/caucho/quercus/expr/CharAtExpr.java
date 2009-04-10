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

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

/**
 * Represents the character at expression
 */
public class CharAtExpr extends AbstractVarExpr {
  private static final L10N L = new L10N(CharAtExpr.class);

  protected final Expr _objExpr;
  protected final Expr _indexExpr;

  public CharAtExpr(Location location, Expr objExpr, Expr indexExpr)
  {
    super(location);
    _objExpr = objExpr;
    _indexExpr = indexExpr;
  }
  
  public CharAtExpr(Expr objExpr, Expr indexExpr)
  {
    _objExpr = objExpr;
    _indexExpr = indexExpr;
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
    Value obj = _objExpr.eval(env);

    return obj.charValueAt(_indexExpr.evalLong(env));
  }
  
  /**
   * Evaluates the expression as an assignment.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalAssign(Env env, Value value)
  {
    Value obj = _objExpr.eval(env);

    Value result = obj.setCharValueAt(_indexExpr.evalLong(env),
                                      value);

    _objExpr.evalAssign(env, result);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalUnset(Env env)
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
  @Override
  public Value evalArg(Env env, boolean isTop)
  {
    return eval(env);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalRef(Env env)
  {
    return eval(env);
  }

  public String toString()
  {
    return _objExpr + "{" + _indexExpr + "}";
  }
}

