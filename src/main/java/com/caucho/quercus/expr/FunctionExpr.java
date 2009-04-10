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

import com.caucho.quercus.*;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.L10N;

import java.util.ArrayList;

/**
 * Represents a PHP function expression.
 */
public class FunctionExpr extends Expr {
  private static final L10N L = new L10N(FunctionExpr.class);
  
  protected final String _name;
  protected final Expr []_args;

  public FunctionExpr(Location location, String name, ArrayList<Expr> args)
  {
    // quercus/120o
    super(location);
    _name = name.intern();

    _args = new Expr[args.size()];
    args.toArray(_args);
  }

  public FunctionExpr(Location location, String name, Expr []args)
  {
    // quercus/120o
    super(location);
    _name = name.intern();

    _args = args;
  }

  public FunctionExpr(String name, ArrayList<Expr> args)
  {
    this(Location.UNKNOWN, name, args);
  }

  public FunctionExpr(String name, Expr []args)
  {
    this(Location.UNKNOWN, name, args);
  }

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _name;
  }
  
  /**
   * Returns the location if known.
   */
  public String getFunctionLocation()
  {
    return " [" + _name + "]";
  }

  /**
   * Returns the reference of the value.
   * @param location
   */
  @Override
  public Expr createRef(QuercusParser parser)
  {
    return parser.getFactory().createRef(this);
  }

  /**
   * Returns the copy of the value.
   * @param location
   */
  @Override
  public Expr createCopy(ExprFactory factory)
  {
    return this;
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
    return evalImpl(env, false, false);
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
    return evalImpl(env, true, false);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalCopy(Env env)
  {
    return evalImpl(env, false, true);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  private Value evalImpl(Env env, boolean isRef, boolean isCopy)
  {
    AbstractFunction fun = env.findFunction(_name);

    if (fun == null) {
      env.error(getLocationLine(), L.l("'{0}' is an unknown function.", _name));

      return NullValue.NULL;
    }

    Value []args = fun.evalArguments(env, this, _args);

    env.pushCall(this, NullValue.NULL, args);
    // XXX: qa/1d14 Value oldThis = env.setThis(UnsetValue.NULL);
    try {
      env.checkTimeout();
	
      if (isRef)
        return fun.callRef(env, args);
      else if (isCopy)
        return fun.callCopy(env, args);
      else
        return fun.call(env, args);
    //} catch (Exception e) {
    //  throw QuercusException.create(e, env.getStackTrace());
    } finally {
      env.popCall();
      // XXX: qa/1d14 env.setThis(oldThis);
    }
  }

  // Return an array containing the Values to be
  // passed in to this function.

  public Value []evalArguments(Env env)
  {
    AbstractFunction fun = env.findFunction(_name);

    if (fun == null) {
      return null;
    }

    return fun.evalArguments(env, this, _args);
  }

  public String toString()
  {
    return _name + "()";
  }
}

