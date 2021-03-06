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

package com.caucho.quercus.program;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.EnvVar;
import com.caucho.quercus.env.EnvVarImpl;
import com.caucho.quercus.env.NullThisValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Var;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.ExprFactory;
import com.caucho.quercus.expr.RequiredExpr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.statement.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents sequence of statements.
 */
public class Function extends AbstractFunction {
  protected final FunctionInfo _info;
  protected final boolean _isReturnsReference;

  protected final String _name;
  protected final Arg []_args;
  protected final Statement _statement;

  protected boolean _hasReturn;
  
  protected String _comment;

  Function(Location location,
           String name,
           FunctionInfo info,
           Arg []args,
           Statement []statements)
  {
    super(location);
    
    _name = name.intern();
    _info = info;
    _info.setFunction(this);
    _isReturnsReference = info.isReturnsReference();
    _args = args;
    _statement = new BlockStatement(location, statements);

    setGlobal(info.isPageStatic());
    
    _isStatic = true;
  }

  public Function(ExprFactory exprFactory,
                  Location location,
                  String name,
                  FunctionInfo info,
                  Arg []args,
                  Statement []statements)
  {
    super(location);
    
    _name = name.intern();
    _info = info;
    _info.setFunction(this);
    _isReturnsReference = info.isReturnsReference();

    _args = new Arg[args.length];
    
    System.arraycopy(args, 0, _args, 0, args.length);

    _statement = exprFactory.createBlock(location, statements);

    setGlobal(info.isPageStatic());
    
    _isStatic = true;
  }

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _name;
  }
  
  /*
   * Returns the declaring class
   */
  @Override
  public ClassDef getDeclaringClass()
  {
    return _info.getDeclaringClass();
  }
  
  /*
   * Returns the declaring class
   */
  @Override
  public String getDeclaringClassName()
  {
    ClassDef declaringClass = _info.getDeclaringClass();
    
    if (declaringClass != null)
      return declaringClass.getName();
    else
      return null;
  }

  /**
   * Returns the args.
   */
  public Arg []getArgs()
  {
    return _args;
  }

  public boolean isObjectMethod()
  {
    return false;
  }

  /**
   * True for a returns reference.
   */
  public boolean isReturnsReference()
  {
    return _isReturnsReference;
  }
  
  /**
   * Sets the documentation for this function.
   */
  public void setComment(String comment)
  {
    _comment = comment;
  }
  
  /**
   * Returns the documentation for this function.
   */
  @Override
  public String getComment()
  {
    return _comment;
  }

  public Value execute(Env env)
  {
    return null;
  }

  /**
   * Evaluates a function's argument, handling ref vs non-ref
   */
  @Override
  public Value []evalArguments(Env env, Expr fun, Expr []args)
  {
    Value []values = new Value[args.length];

    for (int i = 0; i < args.length; i++) {
      Arg arg = null;

      if (i < _args.length)
        arg = _args[i];

      if (arg == null)
        values[i] = args[i].eval(env).copy();
      else if (arg.isReference())
        values[i] = args[i].evalRef(env);
      else {
        // php/0d04
        values[i] = args[i].eval(env);
      }
    }

    return values;
  }

  public Value call(Env env, Expr []args)
  {
    return callImpl(env, args, false);
  }

  public Value callCopy(Env env, Expr []args)
  {
    return callImpl(env, args, false);
  }

  public Value callRef(Env env, Expr []args)
  {
    return callImpl(env, args, true);
  }

  private Value callImpl(Env env, Expr []args, boolean isRef)
  {
    HashMap<String,EnvVar> map = new HashMap<String,EnvVar>();

    Value []values = new Value[args.length];

    for (int i = 0; i < args.length; i++) {
      Arg arg = null;

      if (i < _args.length) {
        arg = _args[i];
      }

      if (arg == null) {
        values[i] = args[i].eval(env).copy();
      }
      else if (arg.isReference()) {
        values[i] = args[i].evalRef(env);

        map.put(arg.getName(), new EnvVarImpl(values[i].toRefVar()));
      }
      else {
        // php/0d04
        values[i] = args[i].eval(env);

        Var var = values[i].toVar();

        map.put(arg.getName(), new EnvVarImpl(var));

        values[i] = var.toValue();
      }
    }

    for (int i = args.length; i < _args.length; i++) {
      Arg arg = _args[i];

      Expr defaultExpr = arg.getDefault();

      if (defaultExpr == null)
        return env.error("expected default expression");
      else if (arg.isReference())
        map.put(arg.getName(),
                new EnvVarImpl(defaultExpr.evalRef(env).toVar()));
      else {
        map.put(arg.getName(),
                new EnvVarImpl(defaultExpr.eval(env).copy().toVar()));
      }
    }

    Map<String,EnvVar> oldMap = env.pushEnv(map);
    Value []oldArgs = env.setFunctionArgs(values); // php/0476
    Value oldThis;

    if (isStatic()) {
      // php/0967
      oldThis = env.setThis(NullThisValue.NULL);
    }
    else
      oldThis = env.getThis();

    try {
      Value value = _statement.execute(env);

      if (value == null)
        return NullValue.NULL;
      else if (_isReturnsReference && isRef)
        return value;
      else
        return value.copyReturn();
    } finally {
      env.restoreFunctionArgs(oldArgs);
      env.popEnv(oldMap);
      env.setThis(oldThis);
    }
  }

  public Value call(Env env, Value []args)
  {
    return callImpl(env, args, false);
  }

  public Value callCopy(Env env, Value []args)
  {
    return callImpl(env, args, false);
  }

  public Value callRef(Env env, Value []args)
  {
    return callImpl(env, args, true);
  }

  private Value callImpl(Env env, Value []args, boolean isRef)
  {
    HashMap<String,EnvVar> map = new HashMap<String,EnvVar>(8);

    for (int i = 0; i < args.length; i++) {
      Arg arg = null;

      if (i < _args.length) {
        arg = _args[i];
      }

      if (arg == null) {
      }
      else if (arg.isReference()) {
        map.put(arg.getName(), new EnvVarImpl(args[i].toRefVar()));
      }
      else {
        Var var = args[i].copy().toVar();

        if (arg.getExpectedClass() != null
            && arg.getDefault() instanceof RequiredExpr) {
          env.checkTypeHint(var,
                            arg.getExpectedClass(),
                            arg.getName(),
                            getName());
        }
          
        // quercus/0d04
        map.put(arg.getName(), new EnvVarImpl(var));
      }
    }

    for (int i = args.length; i < _args.length; i++) {
      Arg arg = _args[i];

      Expr defaultExpr = arg.getDefault();

      if (defaultExpr == null)
        return env.error("expected default expression");
      else if (arg.isReference())
        map.put(arg.getName(), new EnvVarImpl(defaultExpr.evalRef(env).toVar()));
      else {
        map.put(arg.getName(), new EnvVarImpl(defaultExpr.eval(env).copy().toVar()));
      }
    }

    Map<String,EnvVar> oldMap = env.pushEnv(map);
    Value []oldArgs = env.setFunctionArgs(args);
    Value oldThis;

    if (isStatic()) {
      // php/0967, php/091i
      oldThis = env.setThis(NullThisValue.NULL);
    }
    else
      oldThis = env.getThis();

    try {
      Value value = _statement.execute(env);

      if (value == null)
        return NullValue.NULL;
      else if (_isReturnsReference && isRef)
        return value;
      else
        return value.copyReturn();
    } finally {
      env.restoreFunctionArgs(oldArgs);
      env.popEnv(oldMap);
      env.setThis(oldThis);
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}

