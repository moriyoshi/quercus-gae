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

package com.caucho.quercus.lib;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReadOnly;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.L10N;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Quercus class information
 */
public class ClassesModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(ClassesModule.class);
  private static final Logger log
    = Logger.getLogger(ClassesModule.class.getName());

  /**
   * Calls an object method.
   */
  public static Value call_user_method(Env env,
                                       String name,
                                       Value obj,
                                       Value []args)
  {
    if (obj.isObject()) {
      AbstractFunction fun = obj.findFunction(name);
      
      return fun.callMethod(env, obj, args).copyReturn();
    }
    else {
      QuercusClass cls = env.findClass(obj.toString());
      
      AbstractFunction fun = cls.findFunction(name);
      
      return fun.call(env, args).copyReturn();
    }
  }

  /*
   * Calls a object method with arguments in an array.
   */
  public static Value call_user_method_array(Env env,
                                             String methodName,
                                             Value obj,
                                             ArrayValue params)
  {
    Value []args = params.valuesToArray();
    
    return call_user_method(env, methodName, obj, args);
  }
  
  /**
   * returns true if the class exists.
   */
  public boolean class_exists(Env env,
                              String className,
                              @Optional("true") boolean useAutoload)
  {
    if (className == null)
      return false;
    
    QuercusClass cl =  env.findClass(className, useAutoload, true);

    // php/[03]9m1
    return cl != null && ! cl.isInterface();
  }

  /**
   * Returns the object's class name
   */
  public Value get_class(Env env, Value value)
  {
    if (value instanceof ObjectValue) {
      ObjectValue obj = (ObjectValue) value;

      return env.createString(obj.getName());
    }
    else if (value instanceof JavaValue) {
      JavaValue obj = (JavaValue) value;

      return env.createString(obj.getClassName());
    }
    else
      return BooleanValue.FALSE;
  }
  
  /*
   * Returns the calling class name.
   */
  @ReturnNullAsFalse
  public String get_called_class(Env env)
  {
    QuercusClass cls = env.getCallingClass();
    
    if (cls == null) {
      env.warning("called from outside class scope");
      
      return null;
    }
    
    return cls.getName();
  }

  /**
   * Returns an array of method names
   *
   * @param clss the name of the class, or an instance of a class
   *
   * @return an array of method names
   */
  public static Value get_class_methods(Env env, Value cls)
  {
    // php/1j11

    QuercusClass cl;

    if (cls.isObject())
      cl = ((ObjectValue) cls).getQuercusClass();
    else
      cl = env.findClass(cls.toString());

    if (cl == null)
      return NullValue.NULL;

    ArrayValue array = new ArrayValueImpl();

    HashSet<String> set = new HashSet<String>();
    
    // to combine __construct and class name constructors
    for (AbstractFunction fun : cl.getClassMethods()) {
      set.add(fun.getName());
    }
    
    for (String name : set) {
      array.put(name);
    }

    return array;
  }

  /**
   * Returns an array of member names and values
   *
   * @param clss the name of the class, or an instance of a class
   *
   * @return an array of member names and values
   */
  public static Value get_class_vars(Env env, Value clss)
  {
    // php/1j10

    QuercusClass cl;

    if (clss instanceof ObjectValue)
      cl = ((ObjectValue) clss).getQuercusClass();
    else
      cl = env.findClass(clss.toString());

    if (cl == null)
      return BooleanValue.FALSE;

    ArrayValue varArray = new ArrayValueImpl();

    for (Map.Entry<StringValue,Expr> entry : cl.getClassVars().entrySet()) {
      Value key = entry.getKey();
      Value value = entry.getValue().eval(env);

      varArray.append(key, value);
    }

    ArrayModule.ksort(env, varArray, ArrayModule.SORT_STRING);

    return varArray;
  }

  /**
   * Returns the declared classes
   */
  public static Value get_declared_classes(Env env)
  {
    return env.getDeclaredClasses();
  }

  // XXX: get_declared_interfaces

  /**
   * Returns the object's variables
   */
  public static Value get_object_vars(Env env, Value obj)
  {
    ArrayValue result = new ArrayValueImpl();

    // #3253, php/4as7 - XXX: needs cleanup

    if (obj instanceof ObjectValue) {
      for (Map.Entry<Value,Value> entry : ((ObjectValue) obj).entrySet()) {
	result.put(entry.getKey(), entry.getValue());
      }
    }
    else {
      Iterator<Map.Entry<Value,Value>> iter = obj.getIterator(env);

      while (iter.hasNext()) {
	Map.Entry<Value,Value> entry = iter.next();

	result.put(entry.getKey(), entry.getValue());
      }
    }

    return result;
  }

  /**
   * Returns the object's class name
   */
  public Value get_parent_class(Env env, @ReadOnly Value value)
  {
    if (value instanceof ObjectValue) {
      ObjectValue obj = (ObjectValue) value;

      String parent = obj.getParentClassName();

      if (parent != null)
        return env.createString(parent);
    }
    else if (value.isString()) {
      String className = value.toString();

      QuercusClass cl = env.findClass(className);

      if (cl != null) {
        String parent = cl.getParentName();

        if (parent != null)
          return env.createString(parent);
      }
    }

    return BooleanValue.FALSE;
  }

  /**
   * Returns true if the class exists.
   */
  public boolean interface_exists(Env env,
                                  String interfaceName,
                                  @Optional("true") boolean useAutoload)
  {
    QuercusClass cl =  env.findClass(interfaceName, useAutoload, true);

    // php/[03]9m0
    return cl != null && cl.isInterface();
  }

  /**
   * Returns true if the object implements the given class.
   */
  public static boolean is_a(@ReadOnly Value value, String name)
  {
    return value.isA(name);
  }

  /**
   * Returns true if the argument is an object.
   */
  public static boolean is_object(@ReadOnly Value value)
  {
    return value.isObject();
  }

  /**
   * Returns true if the object implements the given class.
   */
  public static boolean is_subclass_of(Env env,
                                       @ReadOnly Value value,
                                       String name)
  {
    if (value instanceof StringValue) {
      QuercusClass cl = env.findClass(value.toString());

      return cl.isA(name) && !cl.getName().equalsIgnoreCase(name);
    }
    else
      return value.isA(name) && !value.getClassName().equalsIgnoreCase(name);
  }

  /**
   * Returns true if the named method exists on the object.
   *
   * @param obj the object to test
   * @param methodName the name of the method
   */
  public static boolean method_exists(Value obj, String methodName)
  {
    return obj.findFunction(methodName.intern()) != null;
  }
  
  /**
   * Returns true if the named property exists on the object.
   */
  public static Value property_exists(Env env,
                                      Value obj,
                                      StringValue name)
  {
    QuercusClass cls;
    
    if (obj.isString())
      cls = env.findClass(obj.toString());
    else if (obj.isObject())
      cls = ((ObjectValue) obj.toValue()).getQuercusClass();
    else {
      env.warning("must pass in object or name of class");
      return NullValue.NULL;
    }
    
    if (cls != null && cls.findFieldIndex(name) >= 0)
      return BooleanValue.TRUE;
    else
      return BooleanValue.FALSE;
  }

}
