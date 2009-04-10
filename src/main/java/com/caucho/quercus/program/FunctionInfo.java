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

import com.caucho.quercus.Quercus;
import com.caucho.quercus.expr.VarInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Information about a function.
 */
public class FunctionInfo
{
  private final Quercus _quercus;

  private final String _name;
  
  private final HashMap<String,VarInfo> _varMap
    = new HashMap<String,VarInfo>();

  private final ArrayList<String> _tempVarList
    = new ArrayList<String>();

  private ClassDef _classDef;
  private Function _fun;

  private boolean _hasThis; // if true, override default
  private boolean _isGlobal;
  
  private boolean _isPageMain;
  private boolean _isPageStatic;
  
  private boolean _isReturnsReference;
  private boolean _isVariableVar;
  private boolean _isOutUsed;

  private boolean _isVariableArgs;
  private boolean _isUsesSymbolTable;
  private boolean _isUsesGlobal;

  private boolean _isReadOnly = true;

  public FunctionInfo(Quercus quercus, String name)
  {
    _quercus = quercus;
    _name = name;
  }

  public FunctionInfo copy()
  {
    FunctionInfo copy = createCopy();

    copy._varMap.putAll(_varMap);
    copy._tempVarList.addAll(_tempVarList);
    copy._classDef = _classDef;
    copy._fun = _fun;
    copy._hasThis = _hasThis;
    copy._isGlobal = _isGlobal;
    copy._isPageMain = _isPageMain;
    copy._isPageStatic = _isPageStatic;
    copy._isReturnsReference = _isReturnsReference;
    copy._isVariableVar = _isVariableVar;
    copy._isOutUsed = _isOutUsed;
    copy._isVariableArgs = _isVariableArgs;
    copy._isUsesSymbolTable = _isUsesSymbolTable;
    copy._isReadOnly = _isReadOnly;

    return copy;
  }
  
  protected FunctionInfo createCopy()
  {
    return new FunctionInfo(_quercus, _name);
  }

  /**
   * Returns the owning quercus.
   */
  public Quercus getQuercus()
  {
    return _quercus;
  }

  public String getName()
  {
    return _name;
  }

  /**
   * Sets the actual function.
   */
  public void setFunction(Function fun)
  {
    _fun = fun;
  }

  /**
   * True for a global function (top-level script).
   */
  public boolean isGlobal()
  {
    return _isGlobal;
  }

  /**
   * True for a global function.
   */
  public void setGlobal(boolean isGlobal)
  {
    _isGlobal = isGlobal;
  }
  
  /*
   * True for a final function.
   */
  public boolean isFinal()
  {
    return _fun.isFinal();
  }

  /**
   * True for a main function (top-level script).
   */
  public boolean isPageMain()
  {
    return _isPageMain;
  }

  /**
   * True for a main function (top-level script).
   */
  public void setPageMain(boolean isPageMain)
  {
    _isPageMain = isPageMain;
  }

  /**
   * True for a static function (top-level script).
   */
  public boolean isPageStatic()
  {
    return _isPageStatic;
  }

  /**
   * True for a static function (top-level script).
   */
  public void setPageStatic(boolean isPageStatic)
  {
    _isPageStatic = isPageStatic;
  }

  public void setHasThis(boolean hasThis)
  {
    _hasThis = hasThis;
  }

  /**
   * Return true if the function allows $this
   */
  public boolean hasThis()
  {
    return _hasThis || (_classDef != null && ! _fun.isStatic());
  }
  
  /**
   * Sets the owning class.
   */
  public void setDeclaringClass(ClassDef classDef)
  {
    _classDef = classDef;
  }

  /**
   * Gets the owning class.
   */
  public ClassDef getDeclaringClass()
  {
    return _classDef;
  }

  /**
   * True for a method.
   */
  public boolean isMethod()
  {
    return _classDef != null && ! _fun.isStatic();
  }

  /**
   * True if the function returns a reference.
   */
  public boolean isReturnsReference()
  {
    return _isReturnsReference;
  }

  /**
   * True if the function returns a reference.
   */
  public void setReturnsReference(boolean isReturnsReference)
  {
    _isReturnsReference = isReturnsReference;
  }

  /**
   * True if the function has variable vars.
   */
  public boolean isVariableVar()
  {
    return _isVariableVar;
  }

  /**
   * True if the function has variable vars
   */
  public void setVariableVar(boolean isVariableVar)
  {
    _isVariableVar = isVariableVar;
  }

  /**
   * True if the function has variable numbers of arguments
   */
  public boolean isVariableArgs()
  {
    return _isVariableArgs;
  }

  /**
   * True if the function has variable numbers of arguments
   */
  public void setVariableArgs(boolean isVariableArgs)
  {
    _isVariableArgs = isVariableArgs;
  }

  /**
   * True if the function uses the symbol table
   */
  public boolean isUsesSymbolTable()
  {
    return _isUsesSymbolTable;
  }

  /**
   * True if the function uses the symbol table
   */
  public void setUsesSymbolTable(boolean isUsesSymbolTable)
  {
    _isUsesSymbolTable = isUsesSymbolTable;
  }
  
  /*
   * True if the global statement is used.
   */
  public boolean isUsesGlobal()
  {
    return _isUsesGlobal;
  }
  
  /*
   * True if the global statement is used.
   */
  public void setUsesGlobal(boolean isUsesGlobal)
  {
    _isUsesGlobal = isUsesGlobal;
  }

  /**
   * Returns true if the out is used.
   */
  public boolean isOutUsed()
  {
    return _isOutUsed;
  }

  /**
   * Set true if the out is used.
   */
  public void setOutUsed()
  {
    _isOutUsed = true;
  }

  /**
   * Returns true for a read-only function, i.e. no values are changed.
   */
  public boolean isReadOnly()
  {
    return _isReadOnly;
  }

  /**
   * True for a non-read-only function
   */
  public void setModified()
  {
    _isReadOnly = false;
  }

  /**
   * Returns the variable.
   */
  public VarInfo createVar(String name)
  {
    VarInfo var = _varMap.get(name);

    if (var == null) {
      var = createVarInfo(name);

      _varMap.put(name, var);
    }

    return var;
  }
  
  protected VarInfo createVarInfo(String name)
  {
    return new VarInfo(name, this);
  }

  /**
   * Returns the variables.
   */
  public Collection<VarInfo> getVariables()
  {
    return _varMap.values();
  }

  /**
   * Adds a temp variable.
   */
  public void addTempVar(String name)
  {
    if (! _tempVarList.contains(name))
      _tempVarList.add(name);
  }

  /**
   * Returns the temp variables.
   */
  public Collection<String> getTempVariables()
  {
    return _tempVarList;
  }

  public int getTempIndex()
  {
    return _tempVarList.size();
  }

  public String toString()
  {
    return "FunctionInfo[" + _name + "]";
  }

}

