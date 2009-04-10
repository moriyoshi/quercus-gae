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
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.*;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.L10N;

import java.util.ArrayList;

/**
 * Represents a PHP static method expression.
 */
public class LateStaticBindingStaticMethodExpr extends Expr {
  private static final L10N L
    = new L10N(LateStaticBindingStaticMethodExpr.class);
  
  protected final String _methodName;
  protected final int _hash;
  protected final char []_name;
  protected final Expr []_args;

  protected Expr []_fullArgs;

  protected AbstractFunction _fun;
  protected boolean _isMethod;

  public LateStaticBindingStaticMethodExpr(Location location,
                                           String name,
                                           ArrayList<Expr> args)
  {
    super(location);
    
    _methodName = name;
    _name = name.toCharArray();
    _hash = MethodMap.hash(_name, _name.length);

    _args = new Expr[args.size()];
    args.toArray(_args);
  }

  public LateStaticBindingStaticMethodExpr(Location location,
                                           String name,
                                           Expr []args)
  {
    super(location);
    
    _methodName = name;
    _name = name.toCharArray();
    _hash = MethodMap.hash(_name, _name.length);

    _args = args;
  }


  public LateStaticBindingStaticMethodExpr(String name, ArrayList<Expr> args)
  {
    this(Location.UNKNOWN, name, args);
  }

  public LateStaticBindingStaticMethodExpr(String name, Expr []args)
  {
    this(Location.UNKNOWN, name, args);
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
    return factory.createCopy(this);
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

    // php/0954 - what appears to be a static call may be a call to a super constructor
    Value thisValue = env.getThis();    

    //Value thisValue = NullThisValue.NULL;

    env.pushCall(this, thisValue, new Value[0]);
    try {
      env.checkTimeout();

      return cl.callMethod(env, thisValue, _hash, _name, _name.length, _args);
    } finally {
      env.popCall();
    }
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

    // qa/0954 - what appears to be a static call may be a call to a super constructor
    Value thisValue = env.getThis();

    return cl.callMethodRef(env, thisValue, _hash, _name, _name.length, _args);
  }
  
  public String toString()
  {
    return _methodName + "()";
  }
}

