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
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.Arg;
import com.caucho.util.L10N;

import java.util.ArrayList;

/**
 * Represents a PHP parent:: method call expression.
 */
public class ClassMethodExpr extends Expr {
  private static final L10N L = new L10N(ClassMethodExpr.class);

  protected final String _className;
  protected final String _name;
  protected final Expr []_args;

  protected boolean _isMethod;

  public ClassMethodExpr(Location location, String className, String name, ArrayList<Expr> args)
  {
    super(location);
    _className = className.intern();
    
    _name = name.intern();

    _args = new Expr[args.size()];
    args.toArray(_args);
  }

  public ClassMethodExpr(Location location, String className, String name, Expr []args)
  {
    super(location);
    _className = className.intern();
    
    _name = name.intern();

    _args = args;
  }

  public ClassMethodExpr(String className, String name, ArrayList<Expr> args)
  {
    this(Location.UNKNOWN, className, name, args);
  }

  public ClassMethodExpr(String className, String name, Expr []args)
  {
    this(Location.UNKNOWN, className, name, args);
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
    QuercusClass cl = env.findClass(_className);

    if (cl == null)
      throw env.createErrorException(L.l("{0} is an unknown class", _className));

    AbstractFunction fun = cl.getFunction(_name);
    
    Value []values = new Value[_args.length];
    
    for (int i = 0; i < values.length; i++) {
      // php/09e1
      values[i] = _args[i].evalArg(env, true);
    }

    Value obj = env.getThis();
    env.pushCall(this, obj, values);

    try {
      env.checkTimeout();

      return fun.callMethod(env, obj, values);
    } finally {
      env.popCall();
    }
  }
  
  public String toString()
  {
    return _className + "::" + _name + "()";
  }
}

