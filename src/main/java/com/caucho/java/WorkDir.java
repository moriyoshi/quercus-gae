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

package com.caucho.java;

import com.caucho.vfs.MemoryPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.annotation.PostConstruct;

public class WorkDir {
  private static Path _localWorkDir = null;
  
  private Path _path;

  public WorkDir()
  {
  }
  
  /**
   * Returns the local work directory.
   */
  public static Path getLocalWorkDir()
  {
    return getLocalWorkDir(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the local work directory.
   */
  public static Path getLocalWorkDir(ClassLoader loader)
  {
    Path path = _localWorkDir;

    if (path != null)
      return path;

    path = Vfs.lookup("file:/tmp/caucho");

    _localWorkDir = path;
    
    try {
      path.mkdirs();
    } catch (java.io.IOException e) {
    }

    return path;
  }

  /**
   * Sets the work dir.
   */
  public static void setLocalWorkDir(Path path)
  {
    setLocalWorkDir(path, Thread.currentThread().getContextClassLoader());
  }

  /**
   * Sets the work dir.
   */
  public static void setLocalWorkDir(Path path, ClassLoader loader)
  {
    try {
      if (path instanceof MemoryPath) {
        String pathName = path.getPath();

        path = Vfs.lookup("file:/tmp/caucho/qa/" + pathName);
      }
    
      // path.mkdirs();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    _localWorkDir = path;
  }
  
  /**
   * Sets the value.
   */
  public void setValue(Path path)
  {
    _path = path;
  }
  
  /**
   * @deprecated
   */
  public void setId(Path path)
    throws java.io.IOException
  {
    setValue(path);
  }

  /**
   * Stores self.
   */
  @PostConstruct
  public void init()
  {
    setLocalWorkDir(_path);
  }
}

