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

import com.caucho.vfs.StreamImplInputStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempReadStream;

import java.io.InputStream;
import java.io.Serializable;

/**
 * Represents a PHP string value implemented as a TempBuffer, with
 * encoding iso-8859-1..
 */
public class TempBufferBytesValue
  extends BytesValue
  implements Serializable
{
  private TempBuffer _head;

  private String _string;

  public TempBufferBytesValue(TempBuffer buffer)
  {
    _head = buffer;
  }
  
  /*
   * Creates an empty string builder of the same type.
   */
  @Override
  public StringValue createStringBuilder()
  {
    return new BinaryBuilderValue();
  }
  
  
  /*
   * Creates an empty string builder of the same type.
   */
  @Override
  public StringValue createStringBuilder(int length)
  {
    return new BinaryBuilderValue(length);
  }

  /**
   * 
   * @return _head as inputstream
   */
  public InputStream toInputStream()
  {
    TempReadStream ts = new TempReadStream(_head);
    ts.setFreeWhenDone(false);
    
    return new StreamImplInputStream(ts);
  }

  //
  // CharSegment
  //
  
  /**
   * Returns the length as a string.
   */
  public int length()
  {
    int len = 0;

    for (TempBuffer ptr = _head; ptr != null; ptr = ptr.getNext()) {
      len += ptr.getLength();
    }

    return len;
  }
  
  /**
   * Returns the character at a given position
   */
  public char charAt(int index)
  {
    int len = 0;

    for (TempBuffer ptr = _head; ptr != null; ptr = ptr.getNext()) {
      int sublen = ptr.getLength();

      if (index < len + sublen) {
	return (char) (ptr.getBuffer()[index - len] & 0xff);
      }
      
      len += sublen;
    }

    return 0;
  }

  /**
   * Prints the value.
   *
   * @param env
   */
  public void print(Env env)
  {
    for (TempBuffer ptr = _head; ptr != null; ptr = ptr.getNext()) {
      env.write(ptr.getBuffer(), 0, ptr.getLength());
    }
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    if (_string == null) {
      char []cbuf = new char[length()];

      int i = 0;
      for (TempBuffer ptr = _head; ptr != null; ptr = ptr.getNext()) {
	byte []buf = ptr.getBuffer();

	int len = ptr.getLength();

	for (int j = 0; j < len; j++)
	  cbuf[i++] = (char) (buf[j] & 0xff);
      }

      _string = new String(cbuf);
    }

    return _string;
  }

  /**
   * Calculate the hash code
   */
  public int hashCode()
  {
    // Matches hashCode calculated in StringValue
    
    int hash = 37;
    
    for (TempBuffer ptr = _head; ptr != null; ptr = ptr.getNext()) {
      byte []buffer = ptr.getBuffer();
      int length = ptr.getLength();

      for (int i = 0; i < length; i++)
	hash = 65521 * hash + (buffer[i] & 0xff);
    }

    return hash;
  }

  /**
   * Test for equality.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if ((o instanceof TempBufferBytesValue)) {
      TempBufferBytesValue tb = (TempBufferBytesValue) o;

      TempBuffer ptrA = _head;
      TempBuffer ptrB = tb._head;
      
      while (ptrA != null && ptrB != null) {
	byte []bufferA = ptrA.getBuffer();
	int lengthA = ptrA.getLength();
	
	byte []bufferB = ptrB.getBuffer();
	int lengthB = ptrB.getLength();

	if (lengthA != lengthB)
	  return false;

	while (--lengthA >= 0) {
	  if (bufferA[lengthA] != bufferB[lengthA])
	    return false;
	}

	ptrA = ptrA.getNext();
	ptrB = ptrB.getNext();
      }

      return ptrA == null && ptrB == null;
    }
    else
      return super.equals(o);
  }

  public byte[] toBytes()
  {
    int len = 0;

    byte []buffer = new byte[length()];

    for (TempBuffer ptr = _head; ptr != null; ptr = ptr.getNext()) {
      System.arraycopy(ptr.getBuffer(), 0, buffer, len, ptr.getLength());

      len += ptr.getLength();
    }

    return buffer;
  }
  
  //
  // Java Serialization
  //
  
  public Object writeReplace()
  {
    return new BinaryBuilderValue(toBytes());
  }
}

