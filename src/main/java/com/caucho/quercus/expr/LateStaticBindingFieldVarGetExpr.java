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
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

/**
 * Represents a PHP static field reference.
 */
public class LateStaticBindingFieldVarGetExpr extends AbstractVarExpr {
  private static final L10N L
    = new L10N(LateStaticBindingFieldVarGetExpr.class);

  protected final Expr _varName;

  public LateStaticBindingFieldVarGetExpr(Location location, Expr varName)
  {
    super(location);
    
    _varName = varName;
  }

  public LateStaticBindingFieldVarGetExpr(Expr varName)
  {
    _varName = varName;
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
    QuercusClass cl = env.getCallingClass();

    if (cl == null) {
      env.error(getLocation(), L.l("no calling class found"));
      
      return NullValue.NULL;
    }
    
    return cl.getStaticField(env, _varName.evalString(env)).toValue();
  }

  /**
   * Evaluates the expression as a copy
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalCopy(Env env)
  {
    QuercusClass cl = env.getCallingClass();

    if (cl == null) {
      env.error(getLocation(), L.l("no calling class found"));
      
      return NullValue.NULL;
    }
    
    return cl.getStaticField(env, _varName.evalString(env)).copy();
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
    QuercusClass cl = env.getCallingClass();

    if (cl == null) {
      env.error(getLocation(), L.l("no calling class found"));
      
      return NullValue.NULL;
    }
    
    return cl.getStaticField(env, _varName.evalString(env));
  }

  /**
   * Evaluates the expression, creating an array for unassigned values.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArray(Env env)
  {
    QuercusClass cl = env.getCallingClass();

    if (cl == null) {
      env.error(getLocation(), L.l("no calling class found"));
      
      return NullValue.NULL;
    }
    
    return cl.getStaticField(env, _varName.evalString(env)).getArray();
  }
  
  /**
   * Evaluates the expression, creating an array for unassigned values.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalObject(Env env)
  {
    QuercusClass cl = env.getCallingClass();

    if (cl == null) {
      env.error(getLocation(), L.l("no calling class found"));
      
      return NullValue.NULL;
    }
    
    return cl.getStaticField(env, _varName.evalString(env)).getObject(env);
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
    QuercusClass cl = env.getCallingClass();

    if (cl == null) {
      env.error(getLocation(), L.l("no calling class found"));
      
      return NullValue.NULL;
    }
    
    return cl.getStaticField(env, _varName.evalString(env));
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
    QuercusClass cl = env.getCallingClass();

    if (cl == null) {
      env.error(getLocation(), L.l("no calling class found"));
      
      return;
    }
    
    cl.getStaticField(env, _varName.evalString(env)).set(value);
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
    env.error(getLocation(),
              L.l("{0}::${1}: Cannot unset static variables.",
                  env.getCallingClass().getName(), _varName));
  }
  
  public String toString()
  {
    return "static::$" + _varName;
  }
}

