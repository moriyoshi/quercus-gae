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

/* The text cursor is purposely lightweight.  It does not update with the
 * text, nor does is allow changes.
 */
public class StringCharCursor extends CharCursor {
  CharSequence string;
  int pos;

  public StringCharCursor(CharSequence string)
  {
    this.string = string;
    this.pos = 0;
  }

  public StringCharCursor(CharSequence string, int offset)
  {
    this.string = string;
    this.pos = offset;
  }

  /** 
   * returns the current location of the cursor
   */
  public int getIndex() { return pos; }

  public int getBeginIndex() { return 0; }
  public int getEndIndex() { return string.length(); }
  /**
   * sets the cursor to the position
   */
  public char setIndex(int pos) 
  { 
    if (pos < 0) {
      this.pos = 0;
      return DONE;
    }
    else if (pos >= string.length()) {
      this.pos = string.length();
      return DONE;
    }
    else {
      this.pos = pos; 
      return string.charAt(pos);
    }
  }

  /**
   * reads a character from the cursor
   *
   * @return -1 on EOF
   */
  public char next() 
  { 
    if (++pos >= string.length()) {
      pos = string.length();
      return DONE;
    }
    else
      return string.charAt(pos);
  }

  /**
   * reads a character from the cursor
   *
   * @return -1 on EOF
   */
  public char previous() 
  { 
    if (--pos < 0) {
      pos = 0;
      return DONE;
    }
    else
      return string.charAt(pos);
  }

  public char current() 
  { 
    if (pos >= string.length())
      return DONE;
    else
      return string.charAt(pos);
  }

  /**
   * Skips the next n characters
   */
  public char skip(int n)
  {
    pos += n;
    if (pos >= string.length()) {
      pos = string.length();
      return DONE;
    } else
      return string.charAt(pos);
  }

  public void init(CharSequence string)
  {
    this.string = string;
    this.pos = 0;
  }

  public Object clone()
  {
    return new StringCharCursor(string);
  }
}
