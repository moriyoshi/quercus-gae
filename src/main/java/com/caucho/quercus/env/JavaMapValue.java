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

import com.caucho.quercus.program.JavaClassDef;

import java.util.Map;

/**
 * Represents a Quercus java value.
 */
public class JavaMapValue<K,V> extends JavaValue<Map<K,V>> {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public JavaMapValue(Env env, Map<K,V> map, JavaClassDef<Map<K,V>> def)
  {
    super(env, map, def);
  }
  
  @Override
  public Value get(Value name)
  {
    return _env.wrapJava(_object.get(name.toJavaObject()));
  }

  @SuppressWarnings("unchecked")
  @Override
  public Value put(Value index, Value value)
  {
    return _env.wrapJava(_object.put((K)index.toJavaObject(), (V)value.toJavaObject()));
  }
}

