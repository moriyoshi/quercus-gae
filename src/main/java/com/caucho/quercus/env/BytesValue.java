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

import com.caucho.quercus.*;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.IdentityHashMap;

/**
 * Represents a 8-bit binary string value.
 */
abstract public class BytesValue
  extends BinaryValue
{
  //public static final StringValue EMPTY = new BytesBuilderValue();

  /**
   * Convert to a binary value.
   */
  @Override
  public StringValue toBinaryValue(Env env)
  {
    return this;
  }

  @Override
  public String toDebugString()
  {
    StringBuilder sb = new StringBuilder();

    int length = length();

    sb.append("string(");
    sb.append(length);
    sb.append(") \"");

    int appendLength = length > 256 ? 256 : length;

    for (int i = 0; i < appendLength; i++)
      sb.append(charAt(i));

    if (length > 256)
      sb.append(" ...");

    sb.append('"');

    return sb.toString();
  }

  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    /**
     * XXX: s/b:
    int length = length();

    out.print("string(");
    out.print(length);
    out.print(") \"");

    for (int i = 0; i < length; i++)
      out.print(charAt(i));

    out.print("\"");
     */

    /**
     * XXX: old, has been be moved to BinaryBuidlerValue
     * */
    int length = length();
    
    out.print("string(" + length() + ") \"");

    for (int i = 0; i < length; i++) {
      char ch = charAt(i);

      if (0x20 <= ch && ch < 0x7f)
	out.print(ch);
      else if (ch == '\r' || ch == '\n' || ch == '\t')
	out.print(ch);
      else
	out.print("\\x" + Integer.toHexString(ch >> 4) + Integer.toHexString(ch % 16));
    }

    out.print("\"");
  }

  /**
   * Returns a byte stream.
   * @param charset ignored since BinaryValue has no set encoding
   */
  @Override
  public InputStream toInputStream(String charset)
    throws UnsupportedEncodingException
  {
    return toInputStream();
  }

  /**
   * Returns a Unicode char stream.
   * @param charset encoding of the StringValue 
   */
  @Override
  public Reader toReader(String charset)
    throws UnsupportedEncodingException
  {
    return new InputStreamReader(toInputStream(), charset);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder()
  {
    throw new UnsupportedOperationException();
    /*
    BytesBuilderValue bb = new BytesBuilderValue();
    
    bb.append(this);
    
    return bb;
    */
  }

  /**
   * Converts to a BinaryValue in desired charset.
   *
   * @param env
   * @param charset ignored since BinaryValue has no set encoding
   */
  @Override
  public BytesValue toBinaryValue(String charset)
  {
    return this;
  }

  /**
   * Converts from default charset to a UnicodeValue.
   *
   * @param env
   */
  @Override
  public StringValue toUnicodeValue(Env env)
  {
    UnicodeBuilderValue sb = new UnicodeBuilderValue();

    appendTo(sb);

    return sb;
  }

  /**
   * Append to a string builder.
   */
  public StringValue appendTo(UnicodeBuilderValue sb)
  {
    if (length() == 0)
      return sb;
    
    Env env = Env.getInstance();

    try {
      Reader reader = env.getRuntimeEncodingFactory().create(toInputStream());
      
      if (reader != null) {
        sb.append(reader);

        reader.close();
      }

      return sb;
    } catch (IOException e) {
      throw new QuercusRuntimeException(e);
    }
  }

  /**
   * Returns true for equality
   */
  /*
  @Override
  public boolean eq(Value rValue)
  {
    rValue = rValue.toValue();
    
    if (rValue.isBoolean()) {
      return toBoolean() == rValue.toBoolean();
    }

    int type = getNumericType();

    if (type == IS_STRING) {
      if (rValue.isBinary())
        return equals(rValue);
      else if (rValue.isUnicode()) {
        return toUnicodeValue(Env.getInstance()).equals(rValue);
      }
      else if (rValue.isLongConvertible())
        return toLong() ==  rValue.toLong();
      else if (rValue instanceof BooleanValue)
        return toLong() == rValue.toLong();
      else
        return equals(rValue.toStringValue());
    }
    else if (rValue.isString() && rValue.length() == 0)
      return length() == 0;
    else if (rValue.isNumberConvertible())
      return toDouble() == rValue.toDouble();
    else
      return equals(rValue.toStringValue());

  }
  */
  
  abstract public byte[] toBytes();
}

