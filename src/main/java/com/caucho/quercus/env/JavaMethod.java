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

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.annotation.Name;
import com.caucho.quercus.module.ModuleContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Represents a function created from a java method.
 */
public class JavaMethod extends JavaInvoker {
  private final Method _method;

  /**
   * Creates a function from an introspected java method.
   *
   * @param method the introspected method.
   */
  public JavaMethod(ModuleContext moduleContext, Method method)
  {
    super(moduleContext,
          getName(method),
          method.getParameterTypes(),
          method.getParameterAnnotations(),
          method.getAnnotations(),
          method.getReturnType());
    
    _method = method;
    
    // php/069a
    // Java 6 fixes the need to do this for methods of inner classes
    _method.setAccessible(true);
  }

  private static String getName(Method method)
  {
    String name;

    Name nameAnn = method.getAnnotation(Name.class);

    if (nameAnn != null)
      name = nameAnn.value();
    else
      name = method.getName();

    return name;
  }
  
  @Override
  public String getDeclaringClassName()
  {
    return _method.getDeclaringClass().getSimpleName();
  }

  /**
   * Returns the function's method.
   *
   * @return the reflection method.
   */
  public Method getMethod()
  {
    return _method;
  }

  @Override
  public Class<?>[] getJavaParameterTypes()
  {
    return _method.getParameterTypes();
  }

  @Override
  public Class<?> getJavaDeclaringClass()
  {
    return _method.getDeclaringClass();
  }

  @Override
  public Object invoke(Object obj, Object []args)
  {
    try {
      return _method.invoke(obj, args);
    } catch (InvocationTargetException e) {
      Throwable e1 = e.getCause();
      
      // php/0g0h
      if (e1 instanceof QuercusException)
        throw (QuercusException) e1;

      if (e1 instanceof QuercusException)
        throw (QuercusException) e1;
      throw new QuercusException(getStringizedMethodName()+ ": " + e1.getMessage(), e1);
    } catch (Exception e) {
      throw new QuercusException(getStringizedMethodName() + ": " + e.getMessage(), e);
    }
  }

  private String getStringizedMethodName() {
    StringBuffer retval = new StringBuffer();
    retval.append(_method.getDeclaringClass().getName());
    retval.append(".");
    retval.append(_method.getName());
    retval.append("(");
    Class<?>[] paramTypes = _method.getParameterTypes();
    for (int i = 0; i < paramTypes.length; ++i) {
      if (i != 0)
        retval.append(",");
      retval.append(paramTypes[i].getName());
    }
    retval.append(")");
    return retval.toString();
  }

  @Override
  public String toString()
  {
    return "JavaMethod[" + getStringizedMethodName() + "]";
  }
}
