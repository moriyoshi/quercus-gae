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
 * @author Nam Nguyen
 */

package com.caucho.quercus.env;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.program.JavaClassDef;

import java.io.Serializable;

/**
 * Represents a Quercus java value representing a PHP resource value.
 */
public class JavaResourceValue<T> extends JavaValue<T>
  implements Serializable
{
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public JavaResourceValue(Env env, T object, JavaClassDef<T> def)
  {
    super(env, object, def);
  }
  
  /**
   * Returns true for an object.
   */
  @Override
  public boolean isObject()
  {
    return false;
  }
  
  /*
   * Returns true for a resource.
   */
  @Override
  public boolean isResource()
  {
    return true;
  }

  /**
   * Returns the type.
   */
  @Override
  public String getType()
  {
    return "resource";
  }
  
  /*
   * Returns the resource type.
   */
  @Override
  public String getResourceType()
  {
    return getJavaClassDef().getResourceType();
  }
}

