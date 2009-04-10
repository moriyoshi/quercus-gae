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

package com.caucho.util;

import java.io.*;

/**
 * Base64 decoding.
 */
public class Base64 {
  private static final int _decode[];
  private static final char _encode[];

  static {
    _decode = new int[256];
    for (int i = 'A'; i <= 'Z'; i++)
      _decode[i] = i - 'A';
    for (int i = 'a'; i <= 'z'; i++)
      _decode[i] = i - 'a' + 26;
    for (int i = '0'; i <= '9'; i++)
      _decode[i] = i - '0' + 52;
    _decode['+'] = 62;
    _decode['/'] = 63;
    
    _decode['='] = 0;
    
    _encode = new char[64];
    
    for (int ch = 'A'; ch <= 'Z'; ch++)
      _encode[ch - 'A'] = (char) ch;
    
    for (int ch = 'a'; ch <= 'z'; ch++)
      _encode[ch - 'a' + 26] = (char) ch;
    
    for (int ch = '0'; ch <= '9'; ch++)
      _encode[ch - '0' + 52] = (char) ch;

    _encode[62] = '+';
    _encode[63] = '/';
  }

  public static void encode(CharBuffer cb, long data)
  {
    cb.append(Base64.encode(data >> 60));
    cb.append(Base64.encode(data >> 54));
    cb.append(Base64.encode(data >> 48));
    cb.append(Base64.encode(data >> 42));
    cb.append(Base64.encode(data >> 36));
    cb.append(Base64.encode(data >> 30));
    cb.append(Base64.encode(data >> 24));
    cb.append(Base64.encode(data >> 18));
    cb.append(Base64.encode(data >> 12));
    cb.append(Base64.encode(data >> 6));
    cb.append(Base64.encode(data));
  }

  public static void encode(StringBuilder sb, long data)
  {
    sb.append(encode(data >> 60));
    sb.append(encode(data >> 54));
    sb.append(encode(data >> 48));
    sb.append(encode(data >> 42));
    sb.append(encode(data >> 36));
    sb.append(encode(data >> 30));
    sb.append(encode(data >> 24));
    sb.append(encode(data >> 18));
    sb.append(encode(data >> 12));
    sb.append(encode(data >> 6));
    sb.append(encode(data));
  }

  public static void encode24(CharBuffer cb, int data)
  {
    cb.append(Base64.encode(data >> 18));
    cb.append(Base64.encode(data >> 12));
    cb.append(Base64.encode(data >> 6));
    cb.append(Base64.encode(data));
  }

  public static void encode(CharBuffer cb, byte []buffer,
                            int offset, int length)
  {
    while (length >= 3) {
      int data = (buffer[offset] & 0xff) << 16;
      data += (buffer[offset + 1] & 0xff) << 8;
      data += (buffer[offset + 2] & 0xff);
      
      cb.append(Base64.encode(data >> 18));
      cb.append(Base64.encode(data >> 12));
      cb.append(Base64.encode(data >> 6));
      cb.append(Base64.encode(data));

      offset += 3;
      length -= 3;
    }

    if (length == 2) {
      int b1 = buffer[offset] & 0xff;
      int b2 = buffer[offset + 1] & 0xff;
      
      int data = (b1 << 16) + (b2 << 8);
      
      cb.append(Base64.encode(data >> 18));
      cb.append(Base64.encode(data >> 12));
      cb.append(Base64.encode(data >> 6));
      cb.append('=');
    }
    else if (length == 1) {
      int data = (buffer[offset] & 0xff) << 16;
      
      cb.append(Base64.encode(data >> 18));
      cb.append(Base64.encode(data >> 12));
      cb.append('=');
      cb.append('=');
    }
  }

  public static void encode(StringBuilder cb, byte []buffer,
                            int offset, int length)
  {
    while (length >= 3) {
      int data = (buffer[offset] & 0xff) << 16;
      data += (buffer[offset + 1] & 0xff) << 8;
      data += (buffer[offset + 2] & 0xff);
      
      cb.append(Base64.encode(data >> 18));
      cb.append(Base64.encode(data >> 12));
      cb.append(Base64.encode(data >> 6));
      cb.append(Base64.encode(data));

      offset += 3;
      length -= 3;
    }

    if (length == 2) {
      int b1 = buffer[offset] & 0xff;
      int b2 = buffer[offset + 1] & 0xff;
      
      int data = (b1 << 16) + (b2 << 8);
      
      cb.append(Base64.encode(data >> 18));
      cb.append(Base64.encode(data >> 12));
      cb.append(Base64.encode(data >> 6));
      cb.append('=');
    }
    else if (length == 1) {
      int data = (buffer[offset] & 0xff) << 16;
      
      cb.append(Base64.encode(data >> 18));
      cb.append(Base64.encode(data >> 12));
      cb.append('=');
      cb.append('=');
    }
  }

  public static void oldEncode(CharBuffer cb, byte []buffer,
			       int offset, int length)
  {
    while (length >= 3) {
      int data = (buffer[offset] & 0xff) << 16;
      data += (buffer[offset + 1] & 0xff) << 8;
      data += (buffer[offset + 2] & 0xff);
      
      cb.append(Base64.encode(data >> 18));
      cb.append(Base64.encode(data >> 12));
      cb.append(Base64.encode(data >> 6));
      cb.append(Base64.encode(data));

      offset += 3;
      length -= 3;
    }

    if (length == 2) {
      int b1 = buffer[offset] & 0xff;
      int b2 = buffer[offset + 1] & 0xff;
      
      int data = (b1 << 8) + (b2);
      
      cb.append(Base64.encode(data >> 12));
      cb.append(Base64.encode(data >> 6));
      cb.append(Base64.encode(data));
      cb.append('=');
    }
    else if (length == 1) {
      int data = (buffer[offset] & 0xff);
      
      cb.append(Base64.encode(data >> 6));
      cb.append(Base64.encode(data));
      cb.append('=');
      cb.append('=');
    }
  }

  public static char encode(long d)
  {
    return _encode[(int) (d & 0x3f)];
  }

  public static int decode(int d)
  {
    return _decode[d];
  }

  public static String encode(String value)
  {
    try {
      StringWriter sw = new StringWriter();
      encode(sw, new ByteArrayInputStream(value.getBytes()));
      return sw.toString();
    }
    catch (IOException e) {
      throw new RuntimeException("this should not be possible: " + e);
    }
  }

  public static String encodeFromByteArray(byte[] value)
  {
    try {
      StringWriter sw = new StringWriter();
      encode(sw, new ByteArrayInputStream(value));
      return sw.toString();
    }
    catch (IOException e) {
      throw new RuntimeException("this should not be possible " + e);
    }
  }

  public static String encodeFromByteArray(byte[] value,
					   int offset,
					   int length)
  {
    try {
      StringWriter sw = new StringWriter();
      encode(sw, new ByteArrayInputStream(value, offset, length));
      return sw.toString();
    }
    catch (IOException e) {
      throw new RuntimeException("this should not be possible " + e);
    }
  }

  public static void encode(Writer w, InputStream i)
    throws IOException
  {
    while(true) {
      int value1 = i.read();
      int value2 = i.read();
      int value3 = i.read();

      if (value3 >= 0) {
	long chunk = (value1 & 0xff);
	chunk = (chunk << 8) + (value2 & 0xff);
	chunk = (chunk << 8) + (value3 & 0xff);
        
	w.write(encode(chunk >> 18));
	w.write(encode(chunk >> 12));
	w.write(encode(chunk >> 6));
	w.write(encode(chunk));
	continue;
      }
    
      if (value2 >= 0) {
	long chunk = (value1 & 0xff);
	chunk = (chunk << 8) + (value2 & 0xff);
	chunk <<= 8;
	
	w.write(encode(chunk >> 18));
	w.write(encode(chunk >> 12));
	w.write(encode(chunk >> 6));
	w.write('=');
      }
      else if (value1 >= 0) {
	long chunk = (value1 & 0xff);
	chunk <<= 16;
	
	w.write(encode(chunk >> 18));
	w.write(encode(chunk >> 12));
	w.write('=');
	w.write('=');
      }
      break;
    }
    w.flush();
  }

  public static String decode(String value)
  {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      decode(new StringReader(value), baos);
      return new String(baos.toByteArray());
    }
    catch (IOException e) {
      throw new RuntimeException("this should not be possible: " + e);
    }
  }

  public static byte[] decodeToByteArray(String value)
  {
    try {
      if (value == null)
	return new byte[0];
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      decode(new StringReader(value), baos);
      return baos.toByteArray();
    }
    catch (IOException e) {
      throw new RuntimeException("this should not be possible: " + e);
    }
  }

  public static String oldDecode(String value)
  {
    CharBuffer cb = new CharBuffer();

    int length = value.length();
    for (int i = 0; i + 3 < length; i += 4) {
      int ch0 = value.charAt(i + 0) & 0xff;

      // skip whitespace
      if (ch0 == ' ' || ch0 == '\n' || ch0 == '\r') {
        i -= 3;
        continue;
      }
      
      int ch1 = value.charAt(i + 1) & 0xff;
      int ch2 = value.charAt(i + 2) & 0xff;
      int ch3 = value.charAt(i + 3) & 0xff;

      int chunk = ((_decode[ch0] << 18) +
		   (_decode[ch1] << 12) +
		   (_decode[ch2] << 6) +
		   (_decode[ch3]));

      cb.append((char) ((chunk >> 16) & 0xff));

      if (ch2 != '=')
	cb.append((char) ((chunk >> 8) & 0xff));
      if (ch3 != '=')
	cb.append((char) ((chunk & 0xff)));
    }

    return cb.toString();
  }

  private static int readNonWhitespace(Reader r)
    throws IOException
  {
    while(true) {
      int ret = r.read();
      // skip whitespace
      if (ret == ' ' || ret == '\n' || ret == '\r')
	continue;
      return ret;
    }
  }

  public static void decode(Reader r, OutputStream os)
    throws IOException
  {
    while (true) {
      int ch0 = readNonWhitespace(r);
      int ch1 = r.read();
      int ch2 = r.read();
      int ch3 = r.read();

      if (ch1 < 0)
	break;
      if (ch2 < 0)
	ch2 = '=';
      if (ch3 < 0)
	ch3 = '=';
      
      int chunk = ((_decode[ch0] << 18)
		   + (_decode[ch1] << 12)
		   + (_decode[ch2] << 6)
		   + (_decode[ch3]));
      
      os.write((byte) ((chunk >> 16) & 0xff));
      
      if (ch2 != '='  && ch2 != -1)
	os.write((byte) ((chunk >> 8) & 0xff));
      if (ch3 != '=' && ch3 != -1)
	os.write((byte) ((chunk & 0xff)));
      else
	break;
    }
    os.flush();
  }
}
