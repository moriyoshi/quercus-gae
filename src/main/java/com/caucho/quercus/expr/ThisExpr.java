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
import com.caucho.quercus.env.*;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.util.L10N;

/**
 * Represents the 'this' expression.
 */
public class ThisExpr extends AbstractVarExpr {
  private static final L10N L = new L10N(ThisExpr.class);

  protected final InterpretedClassDef _quercusClass;
  
  public ThisExpr(Location location, InterpretedClassDef quercusClass)
  {
    super(location);
    _quercusClass = quercusClass;
  }
  
  public ThisExpr(InterpretedClassDef quercusClass)
  {
    _quercusClass = quercusClass;
  }

  /**
   * Creates a field ref
   */
  @Override
  public Expr createFieldGet(ExprFactory factory,
                             Location location,
                             StringValue name)
  {
    return new ThisFieldExpr(_quercusClass, name);
  }

  /**
   * Creates a field ref
   */
  public Expr createFieldGet(ExprFactory factory,
                             Location location,
                             Expr name)
  {
    return new ThisFieldVarGetExpr(location, name);
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
    return env.getThis();
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
    return env.getThis();
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
    return env.getThis();
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
    env.error(getLocation(), "can't assign $this");
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
    env.error(getLocation(), "can't unset $this");
  }
  
  public String toString()
  {
    return "$this";
  }
}

