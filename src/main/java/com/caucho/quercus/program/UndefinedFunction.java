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
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.L10N;

/**
 * Represents an undefined
 */
public class UndefinedFunction extends AbstractFunction {
  private static final L10N L = new L10N(UndefinedFunction.class);
  
  private final String _name;

  public UndefinedFunction(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }

  /**
   * Evaluates the function.
   */
  public Value call(Env env, Value []args)
  {
    return env.error(L.l("'{0}' is an unknown function.", _name));
  }
  
  public String toString()
  {
    return "UndefinedFunction[" + _name + "]";
  }
}

