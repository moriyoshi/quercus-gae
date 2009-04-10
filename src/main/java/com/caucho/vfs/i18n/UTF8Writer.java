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

package com.caucho.vfs.i18n;

import com.caucho.vfs.OutputStreamWithBuffer;

import java.io.IOException;

/**
 * Implements an encoding char-to-byte writer for UTF8 and the associated
 * factory.
 */
public class UTF8Writer extends EncodingWriter {
  private final static UTF8Writer _writer = new UTF8Writer();

  /**
   * Null-arg constructor for instantiation by com.caucho.vfs.Encoding only.
   */
  public UTF8Writer()
  {
  }
  
  /**
   * Returns the Java encoding for the writer.
   */
  public String getJavaEncoding()
  {
    return "UTF8";
  }

  /**
   * Returns the UTF8_Writer
   *
   * @return the UTF8_Writer
   */
  public EncodingWriter create(String javaEncoding)
  {
    return _writer;
  }

  /**
   * Returns the UTF8_Writer
   *
   * @return the UTF8_Writer
   */
  public EncodingWriter create()
  {
    return _writer;
  }

  /**
   * Writes a character to the output stream with the correct encoding.
   *
   * @param ch the character to write.
   */
  public void write(OutputStreamWithBuffer os, char ch)
    throws IOException
  {
    byte []buffer = os.getBuffer();
    int length = os.getBufferOffset();
    
    if (buffer.length <= length + 2) {
      buffer = os.nextBuffer(length);
      length = os.getBufferOffset();
    }
    
    if (ch < 0x80) {
      buffer[length++] = (byte) ch;
    }
    else if (ch < 0x800) {
      buffer[length++] = (byte) (0xc0 + (ch >> 6));
      buffer[length++] = (byte) (0x80 + (ch & 0x3f));
    }
    else {
      buffer[length++] = (byte) (0xe0 + (ch >> 12));
      buffer[length++] = (byte) (0x80 + ((ch >> 6) & 0x3f));
      buffer[length++] = (byte) (0x80 + (ch & 0x3f));
    }

    os.setBufferOffset(length);
  }

  /**
   * Writes into a character buffer using the correct encoding.
   *
   * @param cbuf character array with the data to write.
   * @param off starting offset into the character array.
   * @param len the number of characters to write.
   */
  public void write(OutputStreamWithBuffer os, char []cbuf, int off, int len)
    throws IOException
  {
    byte []buffer = os.getBuffer();
    int length = os.getBufferOffset();
    int capacity = buffer.length;
    
    for (int i = 0; i < len; i++) {
      if (capacity <= length + 2) {
	buffer = os.nextBuffer(length);
	length = os.getBufferOffset();
      }
      
      char ch = cbuf[off + i];

      if (ch < 0x80)
	buffer[length++] = (byte) ch;
      else if (ch < 0x800) {
	buffer[length++] = (byte) (0xc0 + (ch >> 6));
	buffer[length++] = (byte) (0x80 + (ch & 0x3f));
      }
      else if (ch < 0xd800 || 0xdfff < ch) {
	buffer[length++] = (byte) (0xe0 + (ch >> 12));
	buffer[length++] = (byte) (0x80 + ((ch >> 6) & 0x3f));
	buffer[length++] = (byte) (0x80 + (ch & 0x3f));
      }
      else {
	char ch2 = cbuf[off + i + 1];
	int v = 0x10000 + (ch & 0x3ff) * 0x400 + (ch2 & 0x3ff);
	
	i += 1;
	
	buffer[length++] = (byte) (0xf0 + (v >> 18));
	buffer[length++] = (byte) (0x80 + ((v >> 12) & 0x3f));
	buffer[length++] = (byte) (0x80 + ((v >> 6) & 0x3f));
	buffer[length++] = (byte) (0x80 + (v & 0x3f));
      }
    }

    os.setBufferOffset(length);
  }
}


