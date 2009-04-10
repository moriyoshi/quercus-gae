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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

/**
 * A wrapper for Caucho system variables, allowing tests to override
 * the default variables.
 */
class CauchoNative {
  static CauchoNative jni;
  static boolean isInit;

  private CauchoNative()
  {
  }

  static CauchoNative create()
  {
    if (jni != null)
      return jni;

    if (! isInit) {
      isInit = true;
      //System.loadLibrary("caucho");
      //jni = new CauchoNative();
    }

    return jni;
  }

  native void calculateUsage();

  native int getPid();

  native double getUserTime();
  native double getSystemTime();
  native int getMaxResidentSetSize();
  native int getResidentSetSize();
  native int getSwaps();
  native int getContextSwitches();

  native boolean setUser(String user, String group);
}
