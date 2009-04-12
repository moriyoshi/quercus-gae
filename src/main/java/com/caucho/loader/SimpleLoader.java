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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.loader;

import com.caucho.config.ConfigException;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class SimpleLoader extends Loader {
  private static final Logger log
    = Logger.getLogger(SimpleLoader.class.getName());
  
  // The class directory
  private Path _path;
  private String _prefix;
  private String _pathPrefix;

  private CodeSource _codeSource;

  /**
   * Null constructor for the simple loader.
   */
  public SimpleLoader()
  {
  }

  /**
   * Creates the simple loader with the specified path.
   *
   * @param path specifying the root of the resources
   */
  public SimpleLoader(Path path)
  {
    setPath(path);
  }

  /**
   * Creates the simple loader with the specified path and prefix.
   *
   * @param path specifying the root of the resources
   * @param prefix the prefix that the resources must match
   */
  public SimpleLoader(Path path, String prefix)
  {
    setPath(path);
    setPrefix(prefix);
  }

  /**
   * Sets the resource directory.
   */
  public void setPath(Path path)
  {
    if (path.getPath().endsWith(".jar")
        || path.getPath().endsWith(".zip")) {
      path = JarPath.create(path);
    }

    _path = path;
  }

  /**
   * Gets the resource path.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Sets the resource prefix
   */
  public void setPrefix(String prefix)
  {
    _prefix = prefix;
    
    if (prefix != null)
      _pathPrefix = prefix.replace('.', '/');
  }

  /**
   * Gets the resource prefix
   */
  public String getPrefix()
  {
    return _prefix;
  }

  public static ClassLoader create(Path path)
  {
    return Thread.currentThread().getContextClassLoader();
  }

  /**
   * Initializes the loader.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    try {
      _codeSource = new CodeSource(new URL(_path.getURL()), (Certificate []) null);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Given a class or resource name, returns a patch to that resource.
   *
   * @param name the class or resource name.
   *
   * @return the path representing the class or resource.
   */
  public Path getPath(String name)
  {
    if (_prefix != null && _pathPrefix == null)
      _pathPrefix = _prefix.replace('.', '/');

    if (_pathPrefix != null && ! name.startsWith(_pathPrefix))
      return null;

    if (name.startsWith("/"))
      return _path.lookup("." + name);
    else
      return _path.lookup(name);
  }

  /**
   * Returns the code source for the directory.
   */
  protected CodeSource getCodeSource(Path path)
  {
    return _codeSource;
  }

  /**
   * Adds the class of this resource.
   */
  @Override
  protected void buildClassPath(ArrayList<String> pathList)
  {
    String path = null;
    
    if (_path instanceof JarPath)
      path = ((JarPath) _path).getContainer().getNativePath();
    else if (_path.isDirectory())
      path = _path.getNativePath();

    if (path != null && ! pathList.contains(path))
      pathList.add(path);
  }

  /**
   * Returns a printable representation of the loader.
   */
  public String toString()
  {
    if (_prefix != null)
      return "SimpleLoader[" + _path + ",prefix=" + _prefix + "]";
    else
      return "SimpleLoader[" + _path + "]";
  }
}
