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

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.StringLiteralExpr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a PHP object which extends a Java value.
 */
public class ObjectExtJavaValue extends ObjectExtValue
  implements Serializable
{
  private final Object _object;
  private final JavaClassDef _javaClassDef;
  
  public ObjectExtJavaValue(QuercusClass cl, Object object,
                            JavaClassDef javaClassDef)
  {
    super(cl);

    _object = object;
    _javaClassDef = javaClassDef;
  }
  
  //
  // field
  //

  /**
   * Returns fields not explicitly specified by this value.
   */
  @Override
  protected Value getFieldExt(Env env, StringValue name)
  {
    Value value = _javaClassDef.getField(env, this, name);

    if (value != null)
      return value;
    else
      return super.getFieldExt(env, name);
  }

  /**
   * Sets fields not specified by the value.
   */
  protected Value putFieldExt(Env env, StringValue name, Value value)
  {
    return _javaClassDef.putField(env, this, name, value);
  }

  /**
   * Returns the java object.
   */
  @Override
  public Object toJavaObject()
  {
    return _object;
  }
  
  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    if (! _javaClassDef.varDumpImpl(env, _object, out, depth, valueSet))
      super.varDumpImpl(env, out, depth, valueSet);
  }

  @Override
  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    _javaClassDef.printRImpl(env, _object, out, depth, valueSet);
  }
}

