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

package com.caucho.quercus.env;

import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.IdentityHashMap;

/**
 * Represents a call to an object's method
 */
public class CallbackObjectMethod extends Callback {
  private static final L10N L = new L10N(CallbackObjectMethod.class);
  
  private final ObjectValue _obj;
  private final AbstractFunction _fun;
  
  private final String _methodName;
  private final int _hash;
  private final char []_name;
  
  public CallbackObjectMethod(Env env,
                              ObjectValue obj,
                              AbstractFunction fun,
                              String methodName)
  {
    _obj = obj;
    _fun = fun;
    
    _methodName = methodName;
    _hash = MethodMap.hash(methodName);
    _name = _methodName.toCharArray();
  }

  /**
   * Evaluates the callback with no arguments.
   *
   * @param env the calling environment
   */
  @Override
  public Value call(Env env)
  {
    if (_fun != null)
      return _fun.callMethod(env, _obj);
    else
      return _obj.callMethod(env, _hash, _name, _name.length);
  }

  /**
   * Evaluates the callback with 1 argument.
   *
   * @param env the calling environment
   */
  @Override
  public Value call(Env env, Value a1)
  {
    if (_fun != null)
      return _fun.callMethod(env, _obj,
                             a1);
    else
      return _obj.callMethod(env, _hash, _name, _name.length,
                             a1);
  }

  /**
   * Evaluates the callback with 2 arguments.
   *
   * @param env the calling environment
   */
  @Override
  public Value call(Env env, Value a1, Value a2)
  {
    if (_fun != null)
      return _fun.callMethod(env, _obj,
                             a1, a2);
    else
      return _obj.callMethod(env, _hash, _name, _name.length,
                             a1, a2);
  }

  /**
   * Evaluates the callback with 3 arguments.
   *
   * @param env the calling environment
   */
  @Override
  public Value call(Env env, Value a1, Value a2, Value a3)
  {
    if (_fun != null)
      return _fun.callMethod(env, _obj,
                             a1, a2, a3);
    else
      return _obj.callMethod(env, _hash, _name, _name.length,
                             a1, a2, a3);
  }

  /**
   * Evaluates the callback with 3 arguments.
   *
   * @param env the calling environment
   */
  @Override
  public Value call(Env env, Value a1, Value a2, Value a3,
			     Value a4)
  {
    if (_fun != null)
      return _fun.callMethod(env, _obj,
                             a1, a2, a3, a4);
    else
      return _obj.callMethod(env, _hash, _name, _name.length,
                             a1, a2, a3, a4);
  }

  /**
   * Evaluates the callback with 3 arguments.
   *
   * @param env the calling environment
   */
  @Override
  public Value call(Env env, Value a1, Value a2, Value a3,
		    Value a4, Value a5)
  {
    if (_fun != null)
      return _fun.callMethod(env, _obj,
                             a1, a2, a3, a4, a5);
    else
      return _obj.callMethod(env, _hash, _name, _name.length,
                             a1, a2, a3, a4, a5);
  }

  @Override
  public Value call(Env env, Value []args)
  {
    if (_fun != null)
      return _fun.callMethod(env, _obj, args);
    else
      return _obj.callMethod(env, _hash, _name, _name.length, args);
  }

  @Override
  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.print(getClass().getName());
    out.print('[');
    out.print(_methodName);
    out.print(']');
  }
  
  @Override
  public boolean isValid()
  {
    return true;
  }

  @Override
  public String getCallbackName()
  {
    return _methodName;
  }

  @Override
  public boolean isInternal()
  {
    return _fun instanceof JavaInvoker;
  }
  
  private Value error(Env env)
  {
    env.warning(L.l("{0}::{1}() is an invalid callback method",
                    _obj.getClassName(), _methodName));
    
    return NullValue.NULL;
  }
}
