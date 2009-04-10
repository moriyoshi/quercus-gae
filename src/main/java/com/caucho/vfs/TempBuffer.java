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

package com.caucho.vfs;

import com.caucho.util.FreeList;

import java.io.IOException;
import java.util.logging.*;

/**
 * Pooled temporary byte buffer.
 */
public class TempBuffer implements java.io.Serializable {
  private static Logger _log;
  
  private static final FreeList<TempBuffer> _freeList
    = new FreeList<TempBuffer>(32);

  private static final boolean _isSmallmem;
  public static final int SIZE;

  TempBuffer _next;
  final byte []_buf;
  int _offset;
  int _length;
  int _bufferCount;

  private boolean _isFree;

  /**
   * Create a new TempBuffer.
   */
  public TempBuffer(int size)
  {
    _buf = new byte[size];
  }

  /**
   * Returns true for a smallmem configuration
   */
  public static boolean isSmallmem()
  {
    return _isSmallmem;
  }

  /**
   * Allocate a TempBuffer, reusing one if available.
   */
  public static TempBuffer allocate()
  {
    TempBuffer next = _freeList.allocate();

    if (next == null)
      return new TempBuffer(SIZE);

    next._isFree = false;
    next._next = null;

    next._offset = 0;
    next._length = 0;
    next._bufferCount = 0;

    return next;
  }

  /**
   * Clears the buffer.
   */
  public void clear()
  {
    _next = null;

    _offset = 0;
    _length = 0;
    _bufferCount = 0;
  }

  /**
   * Returns the buffer's underlying byte array.
   */
  public final byte []getBuffer()
  {
    return _buf;
  }

  /**
   * Returns the number of bytes in the buffer.
   */
  public final int getLength()
  {
    return _length;
  }

  /**
   * Sets the number of bytes used in the buffer.
   */
  public final void setLength(int length)
  {
    _length = length;
  }

  public final int getCapacity()
  {
    return _buf.length;
  }

  public int getAvailable()
  {
    return _buf.length - _length;
  }

  public final TempBuffer getNext()
  {
    return _next;
  }

  public final void setNext(TempBuffer next)
  {
    _next = next;
  }

  public int write(byte []buf, int offset, int length)
  {
    byte []thisBuf = _buf;
    int thisLength = _length;

    if (thisBuf.length - thisLength < length)
      length = thisBuf.length - thisLength;

    System.arraycopy(buf, offset, thisBuf, thisLength, length);

    _length = thisLength + length;

    return length;
  }

  public static TempBuffer copyFromStream(ReadStream is)
    throws IOException
  {
    TempBuffer head = TempBuffer.allocate();
    TempBuffer tail = head;
    int len;

    while ((len = is.readAll(tail._buf, 0, tail._buf.length)) == tail._buf.length) {
      TempBuffer buf = TempBuffer.allocate();
      tail._length = len;
      tail._next = buf;
      tail = buf;
    }

    if (len == 0 && head == tail)
      return null; // XXX: TempBuffer leak of _head?
    else if (len == 0) {
      for (TempBuffer ptr = head; ptr.getNext() != null; ptr = ptr.getNext()) {
        TempBuffer next = ptr.getNext();

	if (next.getNext() == null) {
	  TempBuffer.free(next);
          next = null;
	  ptr._next = null;
	}
      }
    }
    else
      tail._length = len;

    return head;
  }

  /**
   * Frees a single buffer.
   */
  public static void free(TempBuffer buf)
  {
    buf._next = null;

    if (buf._buf.length == SIZE) {
      if (buf._isFree) {
	RuntimeException e
	  = new IllegalStateException("illegal TempBuffer.free.  Please report at http://bugs.caucho.com");
	log().log(Level.SEVERE, e.toString(), e);
	throw e;
      }

      buf._isFree = true;
      
      _freeList.free(buf);
    }
  }

  public static void freeAll(TempBuffer buf)
  {
    while (buf != null) {
      TempBuffer next = buf._next;
      buf._next = null;
      
      if (buf._buf.length == SIZE) {
	if (buf._isFree) {
	  RuntimeException e
	    = new IllegalStateException("illegal TempBuffer.free.  Please report at http://bugs.caucho.com");
	  
	  log().log(Level.SEVERE, e.toString(), e);
	  throw e;
	}

	buf._isFree = true;
      
	_freeList.free(buf);
      }
      
      buf = next;
    }
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(TempBuffer.class.getName());

    return _log;
  }

  static {
    int size = 16 * 1024;
    boolean isSmallmem = false;

    String smallmem = System.getProperty("caucho.smallmem");
    
    if (smallmem != null && ! "false".equals(smallmem)) {
      isSmallmem = true;
      size = 1024;
    }

    _isSmallmem = isSmallmem;
    SIZE = size;
  }
}
