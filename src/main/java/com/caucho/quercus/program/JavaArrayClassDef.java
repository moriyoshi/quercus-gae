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

import com.caucho.quercus.env.*;
import com.caucho.quercus.marshal.Marshal;
import com.caucho.quercus.marshal.MarshalFactory;
import com.caucho.quercus.module.ModuleContext;

import java.lang.reflect.Array;

/**
 * Represents an introspected Java class.
 */
public class JavaArrayClassDef<T> extends JavaClassDef<T[]> {
  public JavaArrayClassDef(ModuleContext moduleContext,
                           String name,
                           Class<T[]> type)
  {
    super(moduleContext, name, type);
  }
  
  public JavaArrayClassDef(ModuleContext moduleContext,
                           String name,
                           Class<T[]> type,
                           String extension)
  {
    super(moduleContext, name, type, extension);
  }

  @Override
  public boolean isArray()
  {
    return true;
  }

  @Override
  public Value wrap(Env env, T[] obj)
  {
    if (! _isInit)
      init();
    
    ArrayValueImpl arrayValueImpl = new ArrayValueImpl();

    // XXX: needs to go into constructor
    Class<?> componentClass = getType().getComponentType();

    MarshalFactory factory = getModuleContext().getMarshalFactory();
    Marshal componentClassMarshal = factory.create(componentClass);

    int length = Array.getLength(obj);
      
    for (int i = 0; i < length; i++) {
      Object component = Array.get(obj, i);
      
      arrayValueImpl.put(componentClassMarshal.unmarshal(env, component));
    }

    return arrayValueImpl;
  }
}

