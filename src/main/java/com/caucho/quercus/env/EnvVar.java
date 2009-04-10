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

/**
 * Encapsulates an environment entry for a variable.  The EnvVar is a
 * container for Vars.
 */
abstract public class EnvVar
{
  /**
   * Returns the current value.
   */
  abstract public Value get();

  /**
   * Sets the current value.
   */
  abstract public Value set(Value value);

  /**
   * Returns the current Var.
   */
  abstract public Var getRef();

  /**
   * Sets the var.
   */
  abstract public Var setRef(Var var);

  /**
   * Sets the var.
   */
  public Var setRef(Value value)
  {
    if (value instanceof Var)
      setRef((Var) value);
    else
      set(value);
    
    return getRef();
  }
}

