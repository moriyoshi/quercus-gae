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

import java.io.*;
import java.util.*;

import com.caucho.vfs.*;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.util.*;

/**
 * Represents a 8-bit PHP 6 style binary builder (unicode.semantics = on)
 */
public class BinaryBuilderValue
  extends StringBuilderValue
{
  public static final BinaryBuilderValue EMPTY = new BinaryBuilderValue("");

  private final static BinaryBuilderValue []CHAR_STRINGS;

  public BinaryBuilderValue()
  {
    _buffer = new byte[128];
  }
  
  public BinaryBuilderValue(BinaryBuilderValue v)
  {
    if (v._isCopy) {
      _buffer = new byte[v._buffer.length];
      System.arraycopy(v._buffer, 0, _buffer, 0, v._length);
      _length = v._length;
    }
    else {
      _buffer = v._buffer;
      _length = v._length;
      v._isCopy = true;
    }
  }

  public BinaryBuilderValue(int capacity)
  {
    if (capacity < 64)
      capacity = 128;
    else
      capacity = 2 * capacity;

    _buffer = new byte[capacity];
  }

  public BinaryBuilderValue(byte []buffer, int offset, int length)
  {
    _buffer = new byte[length];
    _length = length;

    System.arraycopy(buffer, offset, _buffer, 0, length);
  }

  public BinaryBuilderValue(byte []buffer)
  {
    this(buffer, 0, buffer.length);
  }

  public BinaryBuilderValue(String s)
  {
    int len = s.length();
    
    _buffer = new byte[len];
    _length = len;

    for (int i = 0; i < len; i++)
      _buffer[i] = (byte) s.charAt(i);
  }
  
  public BinaryBuilderValue(char []buffer)
  {
    _buffer = new byte[buffer.length];
    _length = buffer.length;

    for (int i = 0; i < buffer.length; i++)
      _buffer[i] = (byte) buffer[i];
  }
  
  public BinaryBuilderValue(char []s, Value v1)
  {
    int len = s.length;

    if (len < 128)
      _buffer = new byte[128];
    else
      _buffer = new byte[len + 32];
    
    _length = len;

    for (int i = 0; i < len; i++) {
      _buffer[i] = (byte) s[i];
    }

    v1.appendTo(this);
  }
  
  public BinaryBuilderValue(Byte []buffer)
  {
    int length = buffer.length;
    
    _buffer =  new byte[length];
    _length = length;
    
    for (int i = 0; i < length; i++) {
      _buffer[i] = buffer[i].byteValue();
    }
  }

  public BinaryBuilderValue(byte ch)
  {
    _buffer = new byte[1];
    _length = 1;

    _buffer[0] = ch;
  }

  /**
   * Creates the string.
   */
  public static StringValue create(int value)
  {
    if (value < CHAR_STRINGS.length)
      return CHAR_STRINGS[value];
    else
      return new BinaryBuilderValue(value);
  }

  /**
   * Returns the type.
   */
  @Override
  public String getType()
  {
    return "string";
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder()
  {
    // XXX: can this just return this, or does it need to return a copy?
    return new BinaryBuilderValue(this);
  }
  
  /**
   * Returns the character at an index
   */
  @Override
  public Value charValueAt(long index)
  {
    int len = _length;

    if (index < 0 || len <= index)
      return UnsetBinaryValue.UNSET;
    else
      return BinaryBuilderValue.create(_buffer[(int) index] & 0xff);
  }

  /**
   * Returns a subsequence
   */
  @Override
  public CharSequence subSequence(int start, int end)
  {
    if (end <= start)
      return EMPTY;

    return new BinaryBuilderValue(_buffer, start, end - start);
  }

  /**
   * Convert to lower case.
   */
  @Override
  public StringValue toLowerCase()
  {
    int length = _length;
    
    BinaryBuilderValue string = new BinaryBuilderValue(length);
    
    byte []srcBuffer = _buffer;
    byte []dstBuffer = string._buffer;

    for (int i = 0; i < length; i++) {
      byte ch = srcBuffer[i];
      
      if ('A' <= ch && ch <= 'Z')
        dstBuffer[i] = (byte) (ch + 'a' - 'A');
      else
        dstBuffer[i] = ch;
    }

    string._length = length;

    return string;
  }
  
  /**
   * Convert to lower case.
   */
  @Override
  public StringValue toUpperCase()
  {
    int length = _length;
    
    BinaryBuilderValue string = new BinaryBuilderValue(_length);

    byte []srcBuffer = _buffer;
    byte []dstBuffer = string._buffer;

    for (int i = 0; i < length; i++) {
      byte ch = srcBuffer[i];
      
      if ('a' <= ch && ch <= 'z')
        dstBuffer[i] = (byte) (ch + 'A' - 'a');
      else
        dstBuffer[i] = ch;
    }

    string._length = length;

    return string;
  }

  //
  // append code
  //

  /**
   * Creates a string builder of the same type.
   */
  @Override
  public StringValue createStringBuilder()
  {
    return new BinaryBuilderValue();
  }

  /**
   * Creates a string builder of the same type.
   */
  @Override
  public StringValue createStringBuilder(int length)
  {
    return new BinaryBuilderValue(length);
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    return new BinaryBuilderValue(_buffer, 0, _length);
  }
  
  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env, Value value)
  {
    if (value.isUnicode()) {
      UnicodeBuilderValue sb = new UnicodeBuilderValue(this);
      
      value.appendTo(sb);
      
      return sb;
    }
    else {
      BinaryBuilderValue v = new BinaryBuilderValue(this);

      value.appendTo(v);
      
      return v;
    }
  }
  
  /**
   * Converts to a string builder
   */
  public StringValue toStringBuilder(Env env, StringValue value)
  {
    if (value.isUnicode()) {
      UnicodeBuilderValue sb = new UnicodeBuilderValue(this);
      
      value.appendTo(sb);
      
      return sb;
    }
    else {
      BinaryBuilderValue v = new BinaryBuilderValue(this);

      value.appendTo(v);
      
      return v;
    }
  }

  /**
   * Append a Java buffer to the value.
   */
  // @Override
  public final StringValue append(BinaryBuilderValue sb, int head, int tail)
  {
    int length = tail - head;
    
    if (_buffer.length < _length + length)
      ensureCapacity(_length + length);

    System.arraycopy(sb._buffer, head, _buffer, _length, tail - head);

    _length += tail - head;

    return this;
  }

  /**
   * Append a Java buffer to the value.
   */
  @Override
  public final StringValue appendUnicode(char []buf, int offset, int length)
  {
    UnicodeBuilderValue sb = new UnicodeBuilderValue();

    appendTo(sb);
    sb.append(buf, offset, length);

    return sb;
  }
  
  /**
   * Append a Java string to the value.
   */
  @Override
  public final StringValue appendUnicode(String s)
  {
    UnicodeBuilderValue sb = new UnicodeBuilderValue();

    appendTo(sb);
    sb.append(s);

    return sb;
  }

  /**
   * Append a Java string to the value.
   */
  @Override
  public final StringValue appendUnicode(String s, int start, int end)
  {
    UnicodeBuilderValue sb = new UnicodeBuilderValue();

    appendTo(sb);
    sb.append(s, start, end);

    return sb;
  }

  /**
   * Append a value to the value.
   */
  @Override
  public final StringValue appendUnicode(Value value)
  {
    value = value.toValue();

    if (value instanceof BinaryBuilderValue) {
      append((BinaryBuilderValue) value);

      return this;
    }
    else if (value.isString()) {
      UnicodeBuilderValue sb = new UnicodeBuilderValue();

      appendTo(sb);
      sb.append(value);

      return sb;
    }
    else
      return value.appendTo(this);
  }

  /**
   * Append a Java char to the value.
   */
  @Override
  public final StringValue appendUnicode(char ch)
  {
    UnicodeBuilderValue sb = new UnicodeBuilderValue();

    appendTo(sb);
    sb.append(ch);

    return sb;
  }

  /**
   * Append a Java boolean to the value.
   */
  @Override
  public final StringValue appendUnicode(boolean v)
  {
    return append(v ? "true" : "false");
  }

  /**
   * Append a Java long to the value.
   */
  @Override
  public StringValue appendUnicode(long v)
  {
    // XXX: this probably is frequent enough to special-case
    
    return append(String.valueOf(v));
  }

  /**
   * Append a Java double to the value.
   */
  @Override
  public StringValue appendUnicode(double v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java object to the value.
   */
  @Override
  public StringValue appendUnicode(Object v)
  {
    if (v instanceof String)
      return appendUnicode(v.toString());
    else
      return append(v.toString());
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
  @Override
  public boolean eq(Value rValue)
  {
    ValueType typeA = getValueType();
    ValueType typeB = rValue.getValueType();

    if (typeB.isNumber()) {
      double l = toDouble();
      double r = rValue.toDouble();

      return l == r;
    }
    else if (typeB.isBoolean()) {
      return toBoolean() == rValue.toBoolean();
    }
    else if (typeA.isNumberCmp() && typeB.isNumberCmp()) {
      double l = toDouble();
      double r = rValue.toDouble();

      return l == r;
    }

    rValue = rValue.toValue();
    
    if (rValue instanceof BinaryBuilderValue) {
      BinaryBuilderValue value = (BinaryBuilderValue) rValue;

      int length = _length;
      
      if (length != value._length)
        return false;

      byte []bufferA = _buffer;
      byte []bufferB = value._buffer;

      for (int i = length - 1; i >= 0; i--) {
        if (bufferA[i] != bufferB[i])
          return false;
      }

      return true;
    }
    else {
      return toString().equals(rValue.toString());
    }
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this)
      return true;
    
    if (o instanceof BinaryBuilderValue) {
      BinaryBuilderValue value = (BinaryBuilderValue) o;

      int length = _length;
      
      if (length != value._length)
        return false;

      byte []bufferA = _buffer;
      byte []bufferB = value._buffer;

      for (int i = length - 1; i >= 0; i--) {
        if (bufferA[i] != bufferB[i])
          return false;
      }

      return true;
    }
    /*
    else if (o instanceof UnicodeValue) {
      UnicodeValue value = (UnicodeValue)o;
      
      return value.equals(this);
    }
    */
    else
      return false;
  }

  @Override
  public boolean eql(Value o)
  {
    o = o.toValue();
    
    if (o == this)
      return true;
    
    if (o instanceof BinaryBuilderValue) {
      BinaryBuilderValue value = (BinaryBuilderValue) o;

      int length = _length;
      
      if (length != value._length)
        return false;

      byte []bufferA = _buffer;
      byte []bufferB = value._buffer;

      for (int i = length - 1; i >= 0; i--) {
        if (bufferA[i] != bufferB[i])
          return false;
      }

      return true;
    }
    else
      return false;
  }

  @Override
  public String toDebugString()
  {
    StringBuilder sb = new StringBuilder();

    int length = length();

    sb.append("binary(");
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

  @Override
  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    int length = length();

    if (length < 0)
        length = 0;

    // QA needs to distinguish php5 string from php6 binary
    out.print("string");
    
    out.print("(");
    out.print(length);
    out.print(") \"");

    for (int i = 0; i < length; i++) {
      char ch = charAt(i);

      if (0x20 <= ch && ch <= 0x7f || ch == '\t' || ch == '\r' || ch == '\n')
	out.print(ch);
      else if (ch <= 0xff)
	out.print("\\x" + Integer.toHexString(ch / 16) + Integer.toHexString(ch % 16));
      else {
	out.print("\\u"
		  + Integer.toHexString((ch >> 12) & 0xf)
		  + Integer.toHexString((ch >> 8) & 0xf)
		  + Integer.toHexString((ch >> 4) & 0xf)
		  + Integer.toHexString((ch) & 0xf));
      }
    }

    out.print("\"");
  }

  
  static {
    CHAR_STRINGS = new BinaryBuilderValue[256];

    for (int i = 0; i < CHAR_STRINGS.length; i++)
      CHAR_STRINGS[i] = new BinaryBuilderValue((byte) i);
  }
}

