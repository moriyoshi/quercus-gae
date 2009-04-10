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

import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.expr.ClassConstExpr;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.StringLiteralExpr;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.InstanceInitializer;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Quercus runtime class.
 */
public class QuercusClass {
  private static final L10N L = new L10N(QuercusClass.class);
  private static final Logger log
    = Logger.getLogger(QuercusClass.class.getName());

  private final JavaClassDef _javaClassDef;
  private final ClassDef _classDef;
  private final String _className;

  private QuercusClass _parent;
  
  private WeakReference<QuercusClass> _cacheRef;

  private boolean _isAbstract;
  private boolean _isInterface;
  private boolean _isJavaWrapper;
  
  private ClassDef []_classDefList;

  private AbstractFunction _constructor;
  private AbstractFunction _destructor;

  private AbstractFunction _fieldGet;
  private AbstractFunction _fieldSet;
  
  private AbstractFunction _call;

  private ArrayDelegate _arrayDelegate;
  private TraversableDelegate _traversableDelegate;
  private CountDelegate _countDelegate;

  private final ArrayList<InstanceInitializer> _initializers;
  
  private final ArrayList<StringValue> _fieldNames;
  
  private final IntMap _fieldMap;
  
  private final HashMap<StringValue,Expr> _fieldInitMap;
  
  private final MethodMap<AbstractFunction> _methodMap;

  private final HashMap<String,Expr> _constMap;

  private final HashMap<String,ArrayList<StaticField>> _staticFieldExprMap;

  private final HashSet<String> _instanceofSet;

  private boolean _isModified;

  public QuercusClass(ClassDef classDef, QuercusClass parent)
  {
    this(ModuleContext.getLocalContext(Thread.currentThread().getContextClassLoader()),
         classDef,
         parent);
  }

  public QuercusClass(ModuleContext moduleContext,
                      ClassDef classDef,
                      QuercusClass parent)
  {
    _classDef = classDef.loadClassDef(); // force load of any lazy classes
    _className = classDef.getName();
    _parent = parent;

    _isAbstract = _classDef.isAbstract();
    _isInterface = _classDef.isInterface();
    
    _initializers = new ArrayList<InstanceInitializer>();
    _fieldNames = new ArrayList<StringValue>();
    _fieldMap = new IntMap(16);
  
    _fieldInitMap = new HashMap<StringValue,Expr>();
    _methodMap = new MethodMap<AbstractFunction>();

    _constMap = new HashMap<String,Expr>();

    _staticFieldExprMap = new LinkedHashMap<String,ArrayList<StaticField>>();

    JavaClassDef javaClassDef = null;

    if (classDef instanceof JavaClassDef) {
      javaClassDef = (JavaClassDef) classDef;
      _isJavaWrapper = ! javaClassDef.isDelegate();
    }
    
    for (QuercusClass cls = parent; cls != null; cls = cls.getParent()) {
      AbstractFunction cons = cls.getConstructor();
      
      if (cons != null) {
        addMethod(cls.getName(), cons);
      }
    }

    ClassDef []classDefList;
    
    if (_parent != null) {
      classDefList = new ClassDef[parent._classDefList.length + 1];

      System.arraycopy(parent._classDefList, 0, classDefList, 1,
		       parent._classDefList.length);

      classDefList[0] = classDef;
    }
    else {
      classDefList = new ClassDef[] { classDef };
    }
	  
    _classDefList = classDefList;

    for (int i = 0; i < classDefList.length; i++) {
      if (classDefList[i] instanceof JavaClassDef)
        javaClassDef = (JavaClassDef) classDefList[i];
    }
    
    _javaClassDef = javaClassDef;

    _instanceofSet = new HashSet<String>();

    HashSet<String> ifaces = new HashSet<String>();

    for (int i = classDefList.length - 1; i >= 0; i--) {
      classDef = classDefList[i];

      if (classDef == null) {
        throw new NullPointerException("classDef:" + _classDef
                                       + " i:" + i + " parent:" + parent);
      }

      classDef.init();

      addInstances(_instanceofSet, ifaces, classDef);

      classDef.initClass(this);
    }
    
    if (_constructor == null && parent != null)
      _constructor = parent.getConstructor();
    
    // php/093n
    if (_constructor != null
        && ! _constructor.getName().equals("__construct")) {
      addMethodIfNotExist(_className, _constructor);
    }

    if (_destructor == null && parent != null)
      _destructor = parent.getDestructor();
  }

  private void addInstances(HashSet<String> instanceofSet,
			    HashSet<String> ifaces,
			    ClassDef classDef)
  {
    // _instanceofSet.add(classDef.getName());
    classDef.addInterfaces(instanceofSet);

    for (String iface : classDef.getInterfaces()) {
      // XXX: php/0cn2, but this is wrong:
      QuercusClass cl = Env.getInstance().findClass(iface, true, true);
        
      if (cl == null)
	throw new QuercusRuntimeException(L.l("cannot find interface {0}",
					      iface));

      // _instanceofSet.addAll(cl.getInstanceofSet());
        
      ClassDef ifaceDef = cl.getClassDef();
      // ClassDef ifaceDef = moduleContext.findClass(iface);

      if (ifaceDef != null) {
	if (ifaces.add(iface)) {
	  addInstances(instanceofSet, ifaces, ifaceDef);

	  ifaceDef.initClass(this);
	}
      }
    }
  }

  /**
   * Copy based on a cached value
   */
  public QuercusClass(QuercusClass cacheClass, QuercusClass parent)
  {
    _cacheRef = new WeakReference<QuercusClass>(cacheClass);
    
    _javaClassDef = cacheClass._javaClassDef;
    _classDef = cacheClass._classDef;
    _className = cacheClass._className;

    _isJavaWrapper = cacheClass._isJavaWrapper;
    _classDefList = cacheClass._classDefList;

    _parent = parent;

    _constructor = cacheClass._constructor;
    _destructor = cacheClass._destructor;
    
    _fieldGet = cacheClass._fieldGet;
    _fieldSet = cacheClass._fieldSet;
  
    _call = cacheClass._call;

    _arrayDelegate = cacheClass._arrayDelegate;
    _traversableDelegate = cacheClass._traversableDelegate;
    _countDelegate = cacheClass._countDelegate;

    _initializers = cacheClass._initializers;
  
    _fieldNames = cacheClass._fieldNames;
    _fieldMap = cacheClass._fieldMap;
    _fieldInitMap = cacheClass._fieldInitMap;
    _methodMap = cacheClass._methodMap;
    _constMap = cacheClass._constMap;
    _staticFieldExprMap = cacheClass._staticFieldExprMap;
    _instanceofSet = cacheClass._instanceofSet;
  }

  public ClassDef getClassDef()
  {
    return _classDef;
  }
  
  public JavaClassDef getJavaClassDef()
  {
    return _javaClassDef;
  }

  public MethodMap<AbstractFunction> getMethodMap()
  {
    return _methodMap;
  }

  public HashSet<String> getInstanceofSet()
  {
    return _instanceofSet;
  }

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _className;
  }

  /**
   * Returns the parent class.
   */
  public QuercusClass getParent()
  {
    return _parent;
  }

  /*
   * Returns the class definitions for this class.
   */
  public ClassDef []getClassDefList()
  {
    return _classDefList;
  }
  
  /*
   * Returns the name of the extension that this class is part of.
   */
  public String getExtension()
  {
    return _classDef.getExtension();
  }

  public boolean isInterface()
  {
    return _isInterface;
  }
  
  public boolean isAbstract()
  {
    return _isAbstract;
  }
  
  public boolean isFinal()
  {
    return _classDef.isFinal();
  }

  /**
   * Sets the constructor.
   */
  public void setConstructor(AbstractFunction fun)
  {
    _constructor = fun;
  }

  /**
   * Gets the constructor.
   */
  public AbstractFunction getConstructor()
  {
    return _constructor;
  }

  /**
   * Sets the destructor.
   */
  public void setDestructor(AbstractFunction fun)
  {
    _destructor = fun;
  }

  /**
   * Gets the destructor.
   */
  public AbstractFunction getDestructor()
  {
    return _destructor;
  }

  /**
   * Returns true if the class is modified for caching.
   */
  public boolean isModified()
  {
    if (_isModified)
      return true;
    else if (_parent != null)
      return _parent.isModified();
    else
      return false;
  }

  /**
   * Mark the class as modified for caching.
   */
  public void setModified()
  {
    if (! _isModified) {
      _isModified = true;
      
      if (_cacheRef != null) {
	QuercusClass cacheClass = _cacheRef.get();

	if (cacheClass != null)
	  cacheClass.setModified();
      }
    }
  }
  
  /**
   * Sets the array delegate (see ArrayAccess)
   */
  public void setArrayDelegate(ArrayDelegate delegate)
  {
    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, L.l("{0} adding array delegate {1}",
				this,  delegate));

    _arrayDelegate = delegate;
  }
  
  /**
   * Gets the array delegate (see ArrayAccess)
   */
  public final ArrayDelegate getArrayDelegate()
  {
    return _arrayDelegate;
  }
  
  /**
   * Sets the traversable delegate
   */
  public void setTraversableDelegate(TraversableDelegate delegate)
  {
    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, L.l("{0} setting traversable delegate {1}",
				this,  delegate));

    _traversableDelegate = delegate;
  }
  
  /**
   * Gets the traversable delegate
   */
  public final TraversableDelegate getTraversableDelegate()
  {
    return _traversableDelegate;
  }
  
  /**
   * Sets the count delegate
   */
  public void setCountDelegate(CountDelegate delegate)
  {
    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, L.l("{0} setting count delegate {1}",
				this,  delegate));

    _countDelegate = delegate;
  }
  
  /**
   * Gets the count delegate
   */
  public final CountDelegate getCountDelegate()
  {
    return _countDelegate;
  }

  /**
   * Sets the __fieldGet
   */
  public void setFieldGet(AbstractFunction fun)
  {
    _fieldGet = fun;
  }

  /**
   * Returns the __fieldGet
   */
  public AbstractFunction getFieldGet()
  {
    return _fieldGet;
  }

  /**
   * Sets the __fieldSet
   */
  public void setFieldSet(AbstractFunction fun)
  {
    _fieldSet = fun;
  }

  /**
   * Returns the __fieldSet
   */
  public AbstractFunction getFieldSet()
  {
    return _fieldSet;
  }

  /**
   * Sets the __call
   */
  public void setCall(AbstractFunction fun)
  {
    _call = fun;
  }

  /**
   * Gets the __call
   */
  public AbstractFunction getCall()
  {
    return _call;
  }

  /**
   * Adds an initializer
   */
  public void addInitializer(InstanceInitializer init)
  {
    _initializers.add(init);
  }

  /**
   * Adds a field.
   */
  public void addField(StringValue name,
		       int index,
		       Expr initExpr,
		       FieldVisibility visibility)
  {
    _fieldNames.add(name);
    _fieldMap.put(name, index);
    _fieldInitMap.put(name, initExpr);
  }

  /**
   * Adds a field.
   */
  public int addFieldIndex(StringValue name)
  {
    int index = _fieldMap.get(name);

    if (index >= 0)
      return index;
    else {
      index = _fieldNames.size();
    
      _fieldMap.put(name, index);
      _fieldNames.add(name);

      return index;
    }
  }
  
  /**
   * Returns a set of the fields and their initial values
   */
  public HashMap<StringValue,Expr> getClassVars()
  {
    return _fieldInitMap;
  }
  
  /**
   * Returns the declared functions.
   */
  public Iterable<AbstractFunction> getClassMethods()
  {
    return _methodMap.values();
  }

  /**
   * Adds a method.
   */
  public void addMethod(String name, AbstractFunction fun)
  {
    if (fun == null)
      throw new NullPointerException(L.l("'{0}' is a null function", name));
    
    //php/09j9
    // XXX: this is a hack to get Zend Framework running, the better fix is
    // to initialize all interface classes before any concrete classes
    AbstractFunction existingFun = _methodMap.get(name);
    
    if (existingFun == null || ! fun.isAbstract())
      _methodMap.put(name, fun);
  }
  
  /*
   * Adds a method if it does not exist.
   */
  public void addMethodIfNotExist(String name, AbstractFunction fun)
  {
    if (fun == null)
      throw new NullPointerException(L.l("'{0}' is a null function", name));
    
    //php/09j9
    // XXX: this is a hack to get Zend Framework running, the better fix is
    // to initialize all interface classes before any concrete classes
    AbstractFunction existingFun = _methodMap.get(name);
    
    if (existingFun == null && ! fun.isAbstract())
      _methodMap.put(name, fun);
  }

  /**
   * Adds a static class field.
   */
  public void addStaticFieldExpr(String className, String name, Expr value)
  {
    ArrayList<StaticField> fieldList = _staticFieldExprMap.get(className);
    
    if (fieldList == null) {
      fieldList = new ArrayList<StaticField>();

      _staticFieldExprMap.put(className, fieldList);
    }
    
    fieldList.add(new StaticField(name, value));
  }

  /**
   * Returns the static field names.
   */
  public ArrayList<String> getStaticFieldNames()
  {
    ArrayList<String> names = new ArrayList<String>();

    if (_staticFieldExprMap != null) {
      for (Map.Entry<String,ArrayList<StaticField>> entry
           : _staticFieldExprMap.entrySet()) {
        ArrayList<StaticField> fieldList = entry.getValue();
        
        for (StaticField field : fieldList) {
          names.add(field.getName());
        }
      }
    }

    return names;
  }

  /**
   * Adds a constant definition
   */
  public void addConstant(String name, Expr expr)
  {
    _constMap.put(name, expr);
  }

  /**
   * Returns the number of fields.
   */
  public int getFieldSize()
  {
    return _fieldNames.size();
  }

  /**
   * Returns the field index.
   */
  public int findFieldIndex(StringValue name)
  {
    return _fieldMap.get(name);
  }

  /**
   * Returns the key set.
   */
  public ArrayList<StringValue> getFieldNames()
  {
    return _fieldNames;
  }

  public void validate(Env env)
  {
    if (! _isAbstract && ! _isInterface) {
      for (AbstractFunction fun : _methodMap.values()) {
        /* XXX: abstract methods need to be validated
              php/393g, php/393i, php/39j2
        if (! (absFun instanceof Function))
          continue;

        Function fun = (Function) absFun;
         */

        boolean isAbstract;

        // php/093g constructor
        if (_constructor != null && fun.getName().equals(_constructor.getName()))
          isAbstract = _constructor.isAbstract();
        else
          isAbstract = fun.isAbstract();

        if (isAbstract) {
          throw env.createErrorException(
            _classDef.getLocation(),
            L.l("Abstract function '{0}' must be implemented in concrete class {1}.",
                fun.getName(), getName()));
        }
      }
    }
  }

  public void init(Env env)
  {
    if (_staticFieldExprMap.size() == 0)
      return;

    for (Map.Entry<String,ArrayList<StaticField>> map
	   : _staticFieldExprMap.entrySet()) {
      if (env.isInitializedClass(map.getKey()))
        continue;
      
      for (StaticField field : map.getValue()) {
        Value val;
        Expr expr = field._expr;

        //php/096f
        if (expr instanceof ClassConstExpr)
          val = ((ClassConstExpr) expr).eval(env);
        else
          val = expr.eval(env);

	String fullName = _className + "::" + field._name;
	
        env.setGlobalValue(fullName, val);
      }
      
      env.addInitializedClass(map.getKey());
    }
  }

  public Var getStaticField(Env env, String name)
  {
    Var var = getStaticFieldRec(env, name);

    if (var != null)
      return var;

    /*
    String fullName = _className + "::" + name;
      
    EnvVar envVar = env.getGlobalEnvVar(fullName);
      
    return envVar.getRef();
    */

    return null;
  }

  protected Var getStaticFieldRec(Env env, String name)
  {
    String fullName = _className + "::" + name;
      
    EnvVar envVar = env.getGlobalRaw(fullName);

    if (envVar != null)
      return envVar.getRef();
    
    QuercusClass parent = getParent();
    
    if (parent != null)
      return parent.getStaticFieldRec(env, name);
    else
      return null;
  }
  
  //
  // Constructors
  //
  
  /**
   * Creates a new instance.
   */
  /*
  public Value callNew(Env env, Expr []args)
  {
    Value object = _classDef.callNew(env, args);

    if (object != null)
      return object;
    
    object = newInstance(env);

    AbstractFunction fun = findConstructor();

    if (fun != null) {
      fun.callMethod(env, object, args);
    }

    return object;
  }
  */
  
  /*
   * Creates a new object without calling the constructor.  This is used
   * for unserializing classes.
   */
  public Value createObject(Env env)
  {
    if (_isAbstract) {
      throw env.createErrorException(L.l("abstract class '{0}' cannot be instantiated.",
                                     _className));
    }
    else if (_isInterface) {
      throw env.createErrorException(L.l("interface '{0}' cannot be instantiated.",
                                     _className));
    }

    ObjectValue objectValue = null;

    if (_isJavaWrapper) {
      // Java objects always need to call the constructor?
      return _javaClassDef.callNew(env, Value.NULL_ARGS);
    }
    else if (_javaClassDef != null && _javaClassDef.isDelegate()) {
      objectValue = new ObjectExtValue(this);
    }
    else if (_javaClassDef != null && ! _javaClassDef.isDelegate()) {
      // Java objects always need to call the constructor?
      Value javaWrapper = _javaClassDef.callNew(env, Value.NULL_ARGS);
      Object object = javaWrapper.toJavaObject();
      
      objectValue = new ObjectExtJavaValue(this, object, _javaClassDef);
    }
    else {
      objectValue = _classDef.createObject(env, this);
    }

    initObject(env, objectValue);

    return objectValue;
  }
  
  /*
   * Initializes the object's methods and fields.
   */
  public void initObject(Env env, ObjectValue obj)
  {
    for (int i = 0; i < _initializers.size(); i++) {
      _initializers.get(i).initInstance(env, obj);
    }
  }

  /**
   * Creates a new instance.
   */
  public Value callNew(Env env, Value ...args)
  {
    QuercusClass oldCallingClass = env.setCallingClass(this);
    
    try {
      if (_classDef.isAbstract()) {
        throw env.createErrorException(L.l("abstract class '{0}' cannot be instantiated.",
                                       _className));
      }
      else if (_classDef.isInterface()) {
        throw env.createErrorException(L.l("interface '{0}' cannot be instantiated.",
                                       _className));
      }

      ObjectValue objectValue = null;

      if (_isJavaWrapper) {
        return _javaClassDef.callNew(env, args);
      }
      else if (_javaClassDef != null && _javaClassDef.isDelegate()) {
        objectValue = new ObjectExtValue(this);
      }
      else if (_javaClassDef != null && ! _javaClassDef.isDelegate()) {
        // php/0k3-
        Value javaWrapper = _javaClassDef.callNew(env, args);
        Object object = javaWrapper.toJavaObject();
        
        objectValue = new ObjectExtJavaValue(this, object, _javaClassDef);
      }
      else {
        objectValue = _classDef.newInstance(env, this);
      }

      initObject(env, objectValue);
      
      AbstractFunction fun = findConstructor();

      if (fun != null)
        fun.callMethod(env, objectValue, args);
      else {
        //  if expr
      }

      return objectValue;

    } finally {
      env.setCallingClass(oldCallingClass);
    }
  }

  /**
   * Returns the parent class.
   */
  public String getParentName()
  {
    return _classDefList[0].getParentName();
  }

  /**
   * Returns true for an implementation of a class
   */
  public boolean isA(String name)
  {
    return _instanceofSet.contains(name.toLowerCase());
  }
  
  /*
   * Returns an array of the interfaces that this class and its parents
   * implements.
   */
  public ArrayValue getInterfaces(Env env, boolean autoload)
  {
    ArrayValue array = new ArrayValueImpl();
    
    getInterfaces(env, array, autoload, true);
    
    return array;
  }
  
  /*
   * Puts the interfaces that this class and its parents implements
   * into the array.
   */
  private void getInterfaces(Env env, ArrayValue array,
                             boolean autoload, boolean isTop)
  {
    ClassDef [] defList = _classDefList;
    
    for (int i = 0; i < defList.length; i++) {
      ClassDef def = defList[i];
      
      if (! isTop && def.isInterface()) {
        String name = def.getName();
        
        array.put(name, name);
      }

      String []defNames = def.getInterfaces();
      
      for (int j = 0; j < defNames.length; j++) {
        QuercusClass cls = env.findClass(defNames[j]);
        
        cls.getInterfaces(env, array, autoload, false);
      }
    }

    if (_parent != null)
      _parent.getInterfaces(env, array, autoload, false);
  }
  
  /*
   * Returns true if this class or its parents implements specified interface.
   */
  public boolean implementsInterface(Env env, String name)
  {
    ClassDef [] defList = _classDefList;
    
    for (int i = 0; i < defList.length; i++) {
      ClassDef def = defList[i];
      
      if (def.isInterface() && def.getName().equals(name))
        return true;

      String []defNames = def.getInterfaces();
      
      for (int j = 0; j < defNames.length; j++) {
        QuercusClass cls = env.findClass(defNames[j]);

        if (cls.implementsInterface(env, name))
          return true;
      }
    }
    
    if (_parent != null)
      return _parent.implementsInterface(env, name);
    else
      return false;
  }

  /**
   * Finds the matching constructor.
   */
  public AbstractFunction findConstructor()
  {
    return _constructor;
  }

  //
  // Fields
  //

  /**
   * Implements the __get method call.
   */
  public Value getField(Env env, Value qThis, StringValue name)
  {
    // php/09km, php/09kn
    // push/pop to prevent infinite recursion
    
    if (_fieldGet != null) {
      if (! env.pushFieldGet(qThis.getClassName(), name))
        return UnsetValue.UNSET;
      
      try {
        return _fieldGet.callMethod(env, qThis, name);
      } finally {
        env.popFieldGet();
      }
    }
    else
      return UnsetValue.UNSET;
  }

  /**
   * Implements the __set method call.
   */
  public void setField(Env env, Value qThis, StringValue name, Value value)
  {
    if (_fieldSet != null)
      _fieldSet.callMethod(env, qThis, name, value);
  }

  /**
   * Finds the matching function.
   */
  public AbstractFunction findFunction(String name)
  {
    char []key = name.toCharArray();
    int hash = MethodMap.hash(key, key.length);

    AbstractFunction fun = _methodMap.get(hash, key, key.length);

    return fun;
  }

  /**
   * Finds the matching function.
   */
  public AbstractFunction findStaticFunction(String name)
  {
    return findFunction(name);
  }

  /**
   * Finds the matching function.
   */
  public final AbstractFunction getFunction(String name)
  {
    return _methodMap.get(name);
  }

  /**
   * Finds the matching function.
   */
  public final AbstractFunction getFunction(int hash, char []name, int nameLen)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);
    
    if (fun != null)
      return fun;
    else if (_className.equalsIgnoreCase(toMethod(name, nameLen))
	     && _parent != null) {
      // php/093j
      return _parent.getFunction(_parent.getName());
    }
    else {
      throw new QuercusRuntimeException(L.l("{0}::{1} is an unknown method",
					getName(), toMethod(name, nameLen)));
    }
  }

  /**
   * calls the function.
   */
  public Value callMethod(Env env,
                          Value thisValue,
                          int hash, char []name, int nameLength,
                          Expr []args)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    try {
      AbstractFunction fun = _methodMap.get(hash, name, nameLength);
      
      if (fun != null)
        return fun.callMethod(env, thisValue, args);
      else if (getCall() != null) {
        Expr []newArgs = new Expr[args.length + 1];
        newArgs[0] = new StringLiteralExpr(toMethod(name, nameLength));
        System.arraycopy(args, 0, newArgs, 1, args.length);
        
        return getCall().callMethod(env, thisValue, newArgs);
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}",
                             getName(), toMethod(name, nameLength)));
    } finally {
      env.setCallingClass(oldClass);
    }
  }

  /**
   * calls the function.
   */
  public Value callMethod(Env env,
                          Value thisValue,
                          StringValue methodName,
                          Expr []args)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(methodName.toString());
      
      if (fun != null)
        return fun.callMethod(env, thisValue, args);
      else if (getCall() != null) {
        Expr []newArgs = new Expr[args.length + 1];
        newArgs[0] = new StringLiteralExpr(methodName.toString());
        System.arraycopy(args, 0, newArgs, 1, args.length);
        
        return getCall().callMethod(env, thisValue, newArgs);
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}",
                             getName(), methodName));
    } finally {
      env.setCallingClass(oldClass);
    }
  }

  /**
   * calls the function.
   */
  public Value callMethod(Env env,
                          Value thisValue,
                          int hash, char []name, int nameLen,
                          Value []args)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(hash, name, nameLen);

      if (fun != null)
        return fun.callMethod(env, thisValue, args);
      else if (getCall() != null) {
        return getCall().callMethod(env,
                    thisValue,
                    env.createString(name, nameLen),
                    new ArrayValueImpl(args));
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), toMethod(name, nameLen)));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env,
                          Value thisValue,
                          StringValue name,

                          Value []args)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(name.toString());

      if (fun != null)
        return fun.callMethod(env, thisValue, args);
      else if (getCall() != null) {
        return getCall().callMethod(env,
                    thisValue,
                    name,
                    new ArrayValueImpl(args));
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), name));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue,
                          int hash, char []name, int nameLen)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(hash, name, nameLen);

      if (fun != null)
        return fun.callMethod(env, thisValue);
      else if (getCall() != null) {
        return getCall().callMethod(env,
                    thisValue,
                    env.createString(name, nameLen),
                    new ArrayValueImpl());
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), toMethod(name, nameLen)));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue,
			  int hash, char []name, int nameLen,
			  Value a1)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(hash, name, nameLen);

      if (fun != null)
        return fun.callMethod(env, thisValue, a1);
      else if (getCall() != null) {
        return getCall().callMethod(env,
                    thisValue,
                    env.createString(name, nameLen),
                    new ArrayValueImpl()
                    .append(a1));
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), toMethod(name, nameLen)));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue,
                          int hash, char []name, int nameLen,
                          Value a1, Value a2)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(hash, name, nameLen);

      if (fun != null)
        return fun.callMethod(env, thisValue, a1, a2);
      else if (getCall() != null) {
        return getCall().callMethod(env,
                    thisValue,
                    env.createString(name, nameLen),
                    new ArrayValueImpl()
                    .append(a1)
                    .append(a2));
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), toMethod(name, nameLen)));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue, 
                          int hash, char []name, int nameLen,
			  Value a1, Value a2, Value a3)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(hash, name, nameLen);

      if (fun != null)
        return fun.callMethod(env, thisValue, a1, a2, a3);
      else if (getCall() != null) {
        return getCall().callMethod(env,
                    thisValue,
                    env.createString(name, nameLen),
                    new ArrayValueImpl()
                    .append(a1)
                    .append(a2)
                    .append(a3));
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), toMethod(name, nameLen)));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue, 
                          int hash, char []name, int nameLen,
			  Value a1, Value a2, Value a3, Value a4)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(hash, name, nameLen);

      if (fun != null)
        return fun.callMethod(env, thisValue, a1, a2, a3, a4);
      else if (getCall() != null) {
        return getCall().callMethod(env,
                    thisValue,
                    env.createString(name, nameLen),
                    new ArrayValueImpl()
                    .append(a1)
                    .append(a2)
                    .append(a3)
                    .append(a4));
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), toMethod(name, nameLen)));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue,
                          int hash, char []name, int nameLen,
			  Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(hash, name, nameLen);

      if (fun != null)
        return fun.callMethod(env, thisValue, a1, a2, a3, a4, a5);
      else if (getCall() != null) {
        return getCall().callMethod(env,
                    thisValue,
                    env.createString(name, nameLen),
                    new ArrayValueImpl()
                    .append(a1)
                    .append(a2)
                    .append(a3)
                    .append(a4)
                    .append(a5));
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), toMethod(name, nameLen)));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen,
                             Expr []args)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = getFunction(hash, name, nameLen);
      
      return fun.callMethodRef(env, thisValue, args);
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env,
			     Value thisValue,
			     StringValue methodName,
			     Expr []args)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(methodName.toString());
      
      if (fun != null)
        return fun.callMethodRef(env, thisValue, args);
      else if (getCall() != null) {
        Expr []newArgs = new Expr[args.length + 1];
        newArgs[0] = new StringLiteralExpr(methodName.toString());
        System.arraycopy(args, 0, newArgs, 1, args.length);
        
        return getCall().callMethodRef(env, thisValue, newArgs);
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}",
                             getName(), methodName));
    } finally {
      env.setCallingClass(oldClass);
    }
  }

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen,
                             Value []args)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(hash, name, nameLen);

      if (fun != null)
        return fun.callMethodRef(env, thisValue, args);
      else if (getCall() != null) {
        return getCall().callMethodRef(env,
                                       thisValue,
                                       env.createString(name, nameLen),
                                       new ArrayValueImpl(args));
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), toMethod(name, nameLen)));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             StringValue name,
                             Value []args)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(name.toString());

      if (fun != null)
        return fun.callMethodRef(env, thisValue, args);
      else if (getCall() != null) {
        return getCall().callMethodRef(env,
                                       thisValue,
                                       name,
                                       new ArrayValueImpl(args));
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), name));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(hash, name, nameLen);

      if (fun != null)
        return fun.callMethodRef(env, thisValue);
      else if (getCall() != null) {
        return getCall().callMethodRef(env,
                                       thisValue,
                                       env.createString(name, nameLen),
                                       new ArrayValueImpl());
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), toMethod(name, nameLen)));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen,
                             Value a1)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(hash, name, nameLen);

      if (fun != null)
        return fun.callMethodRef(env, thisValue, a1);
      else if (getCall() != null) {
        return getCall().callMethodRef(env,
                                       thisValue,
                                       env.createString(name, nameLen),
                                       new ArrayValueImpl()
                                       .append(a1));
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), toMethod(name, nameLen)));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen,
                             Value a1, Value a2)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(hash, name, nameLen);

      if (fun != null)
        return fun.callMethodRef(env, thisValue, a1, a2);
      else if (getCall() != null) {
        return getCall().callMethodRef(env,
                                       thisValue,
                                       env.createString(name, nameLen),
                                       new ArrayValueImpl()
                                       .append(a1)
                                       .append(a2));
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), toMethod(name, nameLen)));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen,
                             Value a1, Value a2, Value a3)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(hash, name, nameLen);

      if (fun != null)
        return fun.callMethodRef(env, thisValue, a1, a2, a3);
      else if (getCall() != null) {
        return getCall().callMethodRef(env,
                                       thisValue,
                                       env.createString(name, nameLen),
                                       new ArrayValueImpl()
                                       .append(a1)
                                       .append(a2)
                                       .append(a3));
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), toMethod(name, nameLen)));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen,
                             Value a1, Value a2, Value a3, Value a4)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(hash, name, nameLen);

      if (fun != null)
        return fun.callMethodRef(env, thisValue, a1, a2, a3, a4);
      else if (getCall() != null) {
        return getCall().callMethodRef(env,
                                       thisValue,
                                       env.createString(name, nameLen),
                                       new ArrayValueImpl()
                                       .append(a1)
                                       .append(a2)
                                       .append(a3)
                                       .append(a4));
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), toMethod(name, nameLen)));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen,
                             Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    QuercusClass oldClass = env.setCallingClass(this);
    
    try {
      AbstractFunction fun = _methodMap.get(hash, name, nameLen);

      if (fun != null)
        return fun.callMethodRef(env, thisValue, a1, a2, a3, a4, a5);
      else if (getCall() != null) {
        return getCall().callMethodRef(env,
                                       thisValue,
                                       env.createString(name, nameLen),
                                       new ArrayValueImpl()
                                       .append(a1)
                                       .append(a2)
                                       .append(a3)
                                       .append(a4)
                                       .append(a5));
      }
      else
        return env.error(L.l("Call to undefined method {0}::{1}()",
                             getName(), toMethod(name, nameLen)));
    } finally {
      env.setCallingClass(oldClass);
    }
  }  

  private String toMethod(char []key, int keyLength)
  {
    return new String(key, 0, keyLength);
  }

  /**
   * Finds a function.
   */
  public AbstractFunction findStaticFunctionLowerCase(String name)
  {
    return null;
  }

  /**
   * Finds the matching function.
   */
  public final AbstractFunction getStaticFunction(String name)
  {
    AbstractFunction fun = findStaticFunction(name);
    /*
    if (fun != null)
      return fun;

    fun = findStaticFunctionLowerCase(name.toLowerCase());
    */
    
    if (fun != null)
      return fun;
    else {
      throw new QuercusRuntimeException(L.l("{0}::{1} is an unknown method",
					getName(), name));
    }
  }

  /**
   * Finds the matching constant
   */
  public final Value getConstant(Env env, String name)
  {
    Expr expr = _constMap.get(name);

    if (expr != null)
      return expr.eval(env);

    throw new QuercusRuntimeException(L.l("{0}::{1} is an unknown constant",
					getName(), name));
  }
  
  /**
   * Returns true if the constant exists.
   */
  public final boolean hasConstant(String name)
  {
    return _constMap.get(String.valueOf(name)) != null;
  }
  
  /*
   * Returns the constants defined in this class.
   */
  public final HashMap<String, Expr> getConstantMap()
  {
    return _constMap;
  }

  public int hashCode()
  {
    return _className.hashCode();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof QuercusClass))
      return false;

    QuercusClass qClass = (QuercusClass) o;

    if (_classDef != qClass._classDef)
      return false;
    
    if (_javaClassDef != qClass._javaClassDef)
      return false;

    if (_parent == qClass._parent)
      return true;

    else
      return (_parent != null && _parent.equals(qClass._parent));
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }
  
  static class StaticField
  {
    String _name;
    Expr _expr;
    
    StaticField(String name, Expr expr)
    {
      _name = name;
      _expr = expr;
    }
    
    String getName()
    {
      return _name;
    }
  }
}

