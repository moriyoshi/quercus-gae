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

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.lib.file.BinaryInput;
import com.caucho.quercus.lib.i18n.Decoder;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempCharBuffer;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.IdentityHashMap;

/**
 * Represents a Quercus string value.
 */
abstract public class StringValue
  extends Value
  implements CharSequence
{
  public static final StringValue EMPTY = new ConstStringValue("");
  
  protected static final int MIN_LENGTH = 32;

  protected static final int IS_STRING = 0;
  protected static final int IS_LONG = 1;
  protected static final int IS_DOUBLE = 2;

  /**
   * Creates a string builder of the same type.
   */
  abstract public StringValue createStringBuilder();

  /**
   * Creates a string builder of the same type.
   */
  abstract public StringValue createStringBuilder(int length);
  
  /**
   * Creates the string.
   */
  public static Value create(String value)
  {
    // XXX: needs updating for i18n, currently php5 only
    
    if (value == null)
      return NullValue.NULL;
    else
      return new ConstStringValue(value);
  }

  /**
   * Creates the string.
   */
  public static StringValue create(char value)
  {
    // XXX: needs updating for i18n, currently php5 only
    
    return ConstStringValue.create(value);
    
    /*
    if (value < CHAR_STRINGS.length)
      return CHAR_STRINGS[value];
    else
      return new StringBuilderValue(String.valueOf(value));
    */
  }

  /**
   * Creates the string.
   */
  public static Value create(Object value)
  {
    // XXX: needs updating for i18n, currently php5 only
    
    if (value == null)
      return NullValue.NULL;
    else
      return new StringBuilderValue(value.toString());
  }
  
  /*
   * Decodes the Unicode str from charset.
   * 
   * @param str should be a Unicode string
   * @param charset to decode string from
   */
  public StringValue create(Env env, StringValue unicodeStr, String charset)
  {
    if (! unicodeStr.isUnicode())
      return unicodeStr;
    
    try {
      StringValue sb = createStringBuilder();
      
      byte []bytes = unicodeStr.toString().getBytes(charset);
      
      sb.append(bytes);
      return sb;
      
    }
    catch (UnsupportedEncodingException e) {
      env.warning(e);
      
      return unicodeStr;
    }
  }
  
  //
  // Predicates and relations
  //

  /**
   * Returns the type.
   */
  public String getType()
  {
    return "string";
  }

  /**
   * Returns the ValueType.
   */
  @Override
  public ValueType getValueType()
  {
    return ValueType.STRING;
  }

  /**
   * Returns true for a long
   */
  public boolean isLongConvertible()
  {
    return getValueType().isLongCmp();
  }

  /**
   * Returns true for a double
   */
  public boolean isDoubleConvertible()
  {
    return getValueType().isNumberCmp();
  }

  /**
   * Returns true for a number
   */
  public boolean isNumber()
  {
    return false;
  }

  /**
   * Returns true for is_numeric
   */
  @Override
  public boolean isNumeric()
  {
    // php/120y

    return getValueType().isNumberCmp();
  }

  /**
   * Returns true for a scalar
   */
  public boolean isScalar()
  {
    return true;
  }

  /**
   * Returns true for StringValue
   */
  @Override
  public boolean isString()
  {
    return true;
  }
  
  /*
   * Returns true if this is a PHP5 string.
   */
  public boolean isPHP5String()
  {
    return false;
  }

  /**
   * Returns true if the value is empty
   */
  @Override
  public boolean isEmpty()
  {
    return length() == 0 || length() == 1 && charAt(0) == '0';
  }

  /**
   * Returns true for equality
   */
  public int cmp(Value rValue)
  {
    if (isNumberConvertible() || rValue.isNumberConvertible()) {
      double l = toDouble();
      double r = rValue.toDouble();

      if (l == r)
        return 0;
      else if (l < r)
        return -1;
      else
        return 1;
    }
    else {
      int result = toString().compareTo(rValue.toString());
      
      if (result == 0)
        return 0;
      else if (result > 0)
        return 1;
      else
        return -1;
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
    else {
      return toString().equals(rValue.toString());
    }
  }

  /**
   * Compare two strings
   */
  public int cmpString(StringValue rValue)
  {
    return toString().compareTo(rValue.toString());
  }

  // Conversions

  /**
   * Converts to a string value.
   */
  public StringValue toStringValue()
  {
    return this;
  }

  /**
   * Converts to a long.
   */
  public static long toLong(String string)
  {
    return parseLong(string);
  }

  /**
   * String to long conversion routines used by this module
   * and other modules in this package. These methods are
   * only invoked by other implementations of a "string" object.
   * The 3 implementations should be identical except for the
   * char data source.
   */

  static long parseLong(char []buffer, int offset, int len)
  {
    if (len == 0)
      return 0;

    long value = 0;
    long sign = 1;
    boolean isResultSet = false;
    long result = 0;

    int end = offset + len;
    
    while (offset < end && Character.isWhitespace(buffer[offset])) {
      offset++;
    }
    
    int ch;
    
    if (offset + 1 < end && buffer[offset] == '0'
      && ((ch = buffer[offset + 1]) == 'x' || ch == 'X')) {
    
    for (offset += 2; offset < end; offset++) {
      ch = buffer[offset] & 0xFF;

      long oldValue = value;
      
      if ('0' <= ch && ch <= '9')
        value = value * 16 + ch - '0';
      else if ('a' <= ch && ch <= 'z')
        value = value * 16 + ch - 'a' + 10;
      else if ('A' <= ch && ch <= 'Z')
        value = value * 16 + ch - 'A' + 10;
      else
        return value;
      
      if (value < oldValue)
        return Integer.MAX_VALUE;
    }
    
    return value;
  }

    if (offset < end && buffer[offset] == '-') {
      sign = -1;
      offset++;
    }
    else if (offset < end && buffer[offset] == '+') {
      sign = +1;
      offset++;
    }

    while (offset < end) {
      ch = buffer[offset++];

      if ('0' <= ch && ch <= '9') {
        long newValue = 10 * value + ch - '0';
        if (newValue < value) {
          // php/0143
          // long value overflowed
          result = Integer.MAX_VALUE;
          isResultSet = true;
          break;
        }
        value = newValue;
      }
      else {
        result = sign * value;
        isResultSet = true;
        break;
      }
    }

    if (! isResultSet)
      result = sign * value;

    return result;
  }

  static long parseLong(byte []buffer, int offset, int len)
  {
    if (len == 0)
      return 0;

    long value = 0;
    long sign = 1;
    boolean isResultSet = false;
    long result = 0;

    int end = offset + len;

    while (offset < end && Character.isWhitespace(buffer[offset])) {
      offset++;
    }
    
    int ch;
    
    if (offset + 1 < end && buffer[offset] == '0'
        && ((ch = buffer[offset + 1]) == 'x' || ch == 'X')) {
      
      for (offset += 2; offset < end; offset++) {
        ch = buffer[offset] & 0xFF;
        
        long oldValue = value;
        
        if ('0' <= ch && ch <= '9')
          value = value * 16 + ch - '0';
        else if ('a' <= ch && ch <= 'z')
          value = value * 16 + ch - 'a' + 10;
        else if ('A' <= ch && ch <= 'Z')
          value = value * 16 + ch - 'A' + 10;
        else
          return value;
        
        if (value < oldValue)
          return Integer.MAX_VALUE;
      }
      
      return value;
    }
    
    if (offset < end && buffer[offset] == '-') {
      sign = -1;
      offset++;
    }
    else if (offset < end && buffer[offset] == '+') {
      sign = +1;
      offset++;
    }

    while (offset < end) {
      ch = buffer[offset++];

      if ('0' <= ch && ch <= '9') {
        long newValue = 10 * value + ch - '0';
        if (newValue < value) {
          // long value overflowed, set result to integer max
          result = Integer.MAX_VALUE;
          isResultSet = true;
          break;
        }
        value = newValue;
      }
      else {
        result = sign * value;
        isResultSet = true;
        break;
      }
    }

    if (! isResultSet)
      result = sign * value;

    return result;
  }

  static long parseLong(CharSequence string)
  {
    final int len = string.length();

    if (len == 0)
      return 0;

    long value = 0;
    long sign = 1;
    boolean isResultSet = false;
    long result = 0;

    int offset = 0;
    int end = offset + len;

    while (offset < end && Character.isWhitespace(string.charAt(offset))) {
      offset++;
    }
    
    if (offset < end && string.charAt(offset) == '-') {
      sign = -1;
      offset++;
    }
    else if (offset < end && string.charAt(offset) == '+') {
      sign = +1;
      offset++;
    }

    while (offset < end) {
      int ch = string.charAt(offset++);

      if ('0' <= ch && ch <= '9') {
        long newValue = 10 * value + ch - '0';
        if (newValue < value) {
          // long value overflowed, set result to integer max
          result = Integer.MAX_VALUE;
          isResultSet = true;
          break;
        }
        value = newValue;
      }
      else {
        result = sign * value;
        isResultSet = true;
        break;
      }
    }

    if (! isResultSet)
      result = sign * value;

    return result;
  }

  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    return toDouble(toString());
  }

  /**
   * Converts to a double.
   */
  public static double toDouble(String s)
  {
    int len = s.length();
    
    int start = 0;
    
    int i = 0;
    int ch = 0;

    while (i < len && Character.isWhitespace(s.charAt(i))) {
      start++;
      i++;
    }
    
    if (i + 1 < len && s.charAt(i) == '0'
        && ((ch = s.charAt(i)) == 'x' || ch == 'X')) {
      
      double value = 0;
      
      for (i += 2; i < len; i++) {
        ch = s.charAt(i);

        if ('0' <= ch && ch <= '9')
          value = value * 16 + ch - '0';
        else if ('a' <= ch && ch <= 'z')
          value = value * 16 + ch - 'a' + 10;
        else if ('A' <= ch && ch <= 'Z')
          value = value * 16 + ch - 'A' + 10;
        else
          return value;
      }
      
      return value;
    }
    
    if (i < len && ((ch = s.charAt(i)) == '+' || ch == '-')) {
      i++;
    }

    for (; i < len && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
    }

    if (ch == '.') {
      for (i++; i < len && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
      }
    }

    if (ch == 'e' || ch == 'E') {
      int e = i++;

      if (i < len && (ch = s.charAt(i)) == '+' || ch == '-') {
        i++;
      }

      for (; i < len && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
      }

      if (i == e + 1)
        i = e;
    }

    if (i == 0)
      return 0;
    else if (i == len && start == 0)
      return Double.parseDouble(s);
    else
      return Double.parseDouble(s.substring(start, i));
  }

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    int length = length();

    if (length == 0)
      return false;
    else if (length > 1)
      return true;
    else
      return charAt(0) != '0';
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    int len = length();

    if (len == 0)
      return this;

    int sign = 1;
    long value = 0;

    int i = 0;
    char ch = charAt(i);
    if (ch == '-') {
      sign = -1;
      i++;
    }

    for (; i < len; i++) {
      ch = charAt(i);

      if ('0' <= ch && ch <= '9')
        value = 10 * value + ch - '0';
      else
        return this;
    }

    return LongValue.create(sign * value);
  }
  
  /**
   * Converts to an object.
   */
  @Override
  final public Value toAutoObject(Env env)
  {
    return env.createObject();
  }

  /**
   * Converts to a Java object.
   */
  public Object toJavaObject()
  {
    return toString();
  }

  /**
   * Takes the values of this array, unmarshalls them to objects of type
   * <i>elementType</i>, and puts them in a java array.
   */
  @Override
  public Object valuesToArray(Env env, Class elementType)
  {
    if (char.class.equals(elementType)) {
      return toUnicodeValue(env).toCharArray();
    }
    else if (Character.class.equals(elementType)) {
      char[] chars = toUnicodeValue(env).toCharArray();
      
      int length = chars.length;
      
      Character[] charObjects = new Character[length];
      
      for (int i = 0; i <length; i++) {
        charObjects[i] = Character.valueOf(chars[i]);
      }
      
      return charObjects;
    }
    else if (byte.class.equals(elementType)) {
      return toBinaryValue(env).toBytes();
    }
    else if (Byte.class.equals(elementType)) {
      byte[] bytes = toBinaryValue(env).toBytes();
      
      int length = bytes.length;
      
      Byte[] byteObjects = new Byte[length];
      
      for (int i = 0; i <length; i++) {
        byteObjects[i] = Byte.valueOf(bytes[i]);
      }
      
      return byteObjects;
    }
    else {
      env.error(L.l("Can't assign {0} with type {1} to {2}", this, this.getClass(), elementType));
      return null;
    }
  }
  
  /**
   * Converts to an array if null.
   */
  public Value toAutoArray()
  {
    if (length() == 0)
      return new ArrayValueImpl();
    else
      return this;
  }

  /**
   * Sets the array value, returning the new array, e.g. to handle
   * string update ($a[0] = 'A').  Creates an array automatically if
   * necessary.
   */
  public Value append(Value index, Value value)
  {
    if (length() == 0)
      return new ArrayValueImpl().append(index, value);
    else
      return this;
  }

  // Operations

  /**
   * Returns the character at an index
   */
  public Value get(Value key)
  {
    return charValueAt(key.toLong());
  }

  /**
   * Returns the character at an index
   */
  public Value getArg(Value key, boolean isTop)
  {
    // php/03ma
    return charValueAt(key.toLong());
  }

  /**
   * Returns the character at an index
   */
  @Override
  public Value charValueAt(long index)
  {
    int len = length();

    if (index < 0 || len <= index)
      return UnsetUnicodeValue.UNSET;
    else {
      return StringValue.create(charAt((int) index));
    }
  }

  /**
   * sets the character at an index
   */
  @Override
  public Value setCharValueAt(long index, Value value)
  {
    //XXX: need to double-check this for non-string values
    
    int len = length();

    if (index < 0 || len <= index)
      return this;
    else {
      return (createStringBuilder()
              .append(this, 0, (int) index)
              .append(value)
              .append(this, (int) (index + 1), length()));
    }
  }

  /**
   * Increment the following value.
   */
  @Override
  public Value increment(int incr)
  {
    // php/03i6
    if (length() == 0) {
      if (incr == 1)
        return createStringBuilder().append("1");
      else
        return LongValue.MINUS_ONE;
    }

    if (incr > 0) {
      StringBuilder tail = new StringBuilder();

      for (int i = length() - 1; i >= 0; i--) {
        char ch = charAt(i);

        if (ch == 'z') {
          if (i == 0)
            return createStringBuilder().append("aa").append(tail);
          else
            tail.insert(0, 'a');
        }
        else if ('a' <= ch && ch < 'z') {
          return (createStringBuilder()
		  .append(this, 0, i)
		  .append((char) (ch + 1))
		  .append(tail));
        }
        else if (ch == 'Z') {
          if (i == 0)
            return createStringBuilder().append("AA").append(tail);
          else
            tail.insert(0, 'A');
        }
        else if ('A' <= ch && ch < 'Z') {
          return (createStringBuilder()
		  .append(this, 0, i)
		  .append((char) (ch + 1))
		  .append(tail));
        }
        else if ('0' <= ch && ch <= '9' && i == length() - 1) {
          return LongValue.create(toLong() + incr);
        }
      }

      return createStringBuilder().append(tail.toString());
    }
    else if (getValueType().isLongAdd()) {
      return LongValue.create(toLong() + incr);
    }
    else {
      return this;
    }
  }
  
  /**
   * Adds to the following value.
   */
  public Value add(long rValue)
  {
    if (getValueType().isLongAdd())
      return LongValue.create(toLong() + rValue);
    
    return DoubleValue.create(toDouble() + rValue);
  }
  
  /**
   * Adds to the following value.
   */
  public Value sub(long rValue)
  {
    if (getValueType().isLongAdd())
      return LongValue.create(toLong() - rValue);
    
    return DoubleValue.create(toDouble() - rValue);
  }
  
  /*
   * Bit and.
   */
  @Override
  public Value bitAnd(Value rValue)
  {
    if (rValue.isString()) {
      StringValue rStr = (StringValue) rValue;
      
      int len = Math.min(length(), rValue.length());
      StringValue sb = createStringBuilder();
      
      for (int i = 0; i < len; i++) {
        char l = charAt(i);
        char r = rStr.charAt(i);
        
        sb.appendByte(l & r);
      }

      return sb;
    }
    else
      return LongValue.create(toLong() & rValue.toLong());
  }
  
  /*
   * Bit or.
   */
  @Override
  public Value bitOr(Value rValue)
  {
    if (rValue.isString()) {
      StringValue rStr = (StringValue) rValue;
      
      int len = Math.min(length(), rValue.length());
      StringValue sb = createStringBuilder();
      
      for (int i = 0; i < len; i++) {
        char l = charAt(i);
        char r = rStr.charAt(i);
        
        sb.appendByte(l | r);
      }
     
      if (len != length())
        sb.append(substring(len));
      else if (len != rStr.length())
        sb.append(rStr.substring(len));

      return sb;
    }
    else
      return LongValue.create(toLong() | rValue.toLong());
  }
  
  /*
   * Bit xor.
   */
  @Override
  public Value bitXor(Value rValue)
  {
    if (rValue.isString()) {
      StringValue rStr = (StringValue) rValue;
      
      int len = Math.min(length(), rValue.length());
      StringValue sb = createStringBuilder();
      
      for (int i = 0; i < len; i++) {
        char l = charAt(i);
        char r = rStr.charAt(i);
        
        sb.appendByte(l ^ r);
      }

      return sb;
    }
    else
      return LongValue.create(toLong() ^ rValue.toLong());
  }
  
  /**
   * Serializes the value.
   */
  @Override
  public void serialize(Env env, StringBuilder sb)
  {
    sb.append("s:");
    sb.append(length());
    sb.append(":\"");
    sb.append(toString());
    sb.append("\";");
  }
  
  /*
   * Returns a value to be used as a key for the deserialize cache.
   */
  /*
  public StringValue toSerializeKey()
  {
    if (length() <= 4096)
      return this;

    try {
      MessageDigest md = MessageDigest.getInstance("SHA1");

      byte []buffer = toBytes();

      md.update(buffer, 0, buffer.length);

      //XXX: create a special serialize type?
      return new StringBuilderValue(md.digest());

    } catch (NoSuchAlgorithmException e) {
      throw new QuercusException(e);
    }
  }
  */

  //
  // append code
  //

  /**
   * Append a Java string to the value.
   */
  public StringValue append(String s)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Append a Java string to the value.
   */
  public StringValue append(String s, int start, int end)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Append a Java buffer to the value.
   */
  public StringValue append(char []buf, int offset, int length)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Append a Java double to the value.
   */
  public StringValue append(char []buf)
  {
    return append(buf, 0, buf.length);
  }

  /**
   * Append a Java buffer to the value.
   */
  public StringValue append(CharSequence buf, int head, int tail)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Append a Java buffer to the value.
   */
  public StringValue append(UnicodeBuilderValue sb, int head, int tail)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /*
   * Appends a Unicode string to the value.
   * 
   * @param str should be a Unicode string
   * @param charset to decode string from
   */
  public StringValue append(Env env, StringValue unicodeStr, String charset)
  {
    if (! unicodeStr.isUnicode())
      return append(unicodeStr);

    try {
      byte []bytes = unicodeStr.toString().getBytes(charset);
      
      append(bytes);
      return this;
      
    }
    catch (UnsupportedEncodingException e) {
      env.warning(e);
      
      return append(unicodeStr);
    }
  }

  /**
   * Append a Java char to the value.
   */
  public StringValue append(char v)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Append a Java boolean to the value.
   */
  public StringValue append(boolean v)
  {
    return append(v ? "true" : "false");
  }

  /**
   * Append a Java long to the value.
   */
  public StringValue append(long v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java double to the value.
   */
  public StringValue append(double v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java value to the value.
   */
  public StringValue append(Object v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java value to the value.
   */
  public StringValue append(Value v)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Ensure enough append capacity.
   */
  public void ensureAppendCapacity(int size)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Append a byte buffer to the value.
   */
  public StringValue append(byte []buf, int offset, int length)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Append a byte buffer to the value.
   */
  public StringValue append(byte []buf)
  {
    return append(buf, 0, buf.length);
  }

  /**
   * Append a byte buffer to the value.
   */
  public StringValue appendUtf8(byte []buf, int offset, int length)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Append a byte buffer to the value.
   */
  public StringValue appendUtf8(byte []buf)
  {
    return appendUtf8(buf, 0, buf.length);
  }
  
  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(UnicodeBuilderValue sb)
  {
    int length = length();

    for (int i = 0; i < length; i++)
      sb.append(charAt(i));

    return this;
  }

  /**
   * Append a Java boolean to the value.
   */
  public StringValue appendUnicode(boolean v)
  {
    return append(v ? "true" : "false");
  }

  /**
   * Append a Java long to the value.
   */
  public StringValue appendUnicode(long v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java double to the value.
   */
  public StringValue appendUnicode(double v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java value to the value.
   */
  public StringValue appendUnicode(Object v)
  {
    return append(String.valueOf(v));
  }

  /**
   * Append a Java char, possibly converting to a unicode string
   */
  public StringValue appendUnicode(char v)
  {
    return append(v);
  }

  /**
   * Append a Java char buffer, possibly converting to a unicode string
   */
  public StringValue appendUnicode(char []buffer, int offset, int length)
  {
    return append(buffer, offset, length);
  }

  /**
   * Append a Java char buffer, possibly converting to a unicode string
   */
  public StringValue appendUnicode(char []buffer)
  {
    return append(buffer);
  }

  /**
   * Append a Java char buffer, possibly converting to a unicode string
   */
  public StringValue appendUnicode(String value)
  {
    return append(value);
  }

  /**
   * Append a Java char buffer, possibly converting to a unicode string
   */
  public StringValue appendUnicode(String value, int offset, int length)
  {
    return append(value, offset, length);
  }

  /**
   * Append a Java char buffer, possibly converting to a unicode string
   */
  public StringValue appendUnicode(Value value)
  {
    return append(value);
  }

  /**
   * Append a Java char buffer, possibly converting to a unicode string
   */
  public StringValue appendUnicode(Value v1, Value v2)
  {
    return append(v1).append(v2);
  }

  /**
   * Append a Java byte to the value without conversions.
   */
  public StringValue appendByte(int v)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Append a Java String to the value without conversions.
   */
  public StringValue appendBytes(String s)
  {
    StringValue sb = this;
    
    for (int i = 0; i < s.length(); i++) {
      sb = sb.appendByte(s.charAt(i));
    }
    
    return sb;
  }
  
  /**
   * Append a Java String to the value without conversions.
   */
  public StringValue appendBytes(StringValue s)
  {
    StringValue sb = this;
    
    for (int i = 0; i < s.length(); i++) {
      sb = sb.appendByte(s.charAt(i));
    }
    
    return sb;
  }
  
  /**
   * Append a Java char[] to the value without conversions.
   */
  public StringValue appendBytes(char []buf, int offset, int length)
  {
    StringValue sb = this;
    int end = Math.min(buf.length, offset + length);
    
    while (offset < end) {
      sb = sb.appendByte(buf[offset++]);
    }
    
    return sb;
  }
  
  /**
   * Append Java bytes to the value without conversions.
   */
  public StringValue appendBytes(byte []bytes, int offset, int end)
  {
    StringValue sb = this;
    
    while (offset < end) {
      sb = sb.appendByte(bytes[offset++]);
    }
    
    return sb;
  }

  /**
   * Append from a read stream
   */
  public StringValue append(Reader reader)
    throws IOException
  {
    int ch;
    
    while ((ch = reader.read()) >= 0) {
      append((char) ch);
    }

    return this;
  }

  /**
   * Append from a read stream
   */
  public StringValue append(Reader reader, long length)
    throws IOException
  {
    int ch;
    
    while (length-- > 0 && (ch = reader.read()) >= 0) {
      append((char) ch);
    }

    return this;
  }

  /**
   * Append from an input stream, using InputStream.read semantics,
   * i.e. just call is.read once even if more data is available.
   */
  public int appendRead(InputStream is, long length)
  {
    TempBuffer tBuf = TempBuffer.allocate();

    try {
      byte []buffer = tBuf.getBuffer();
      int sublen = buffer.length;
      if (length < sublen)
        sublen = (int) length;

      sublen = is.read(buffer, 0, sublen);

      if (sublen > 0)
        append(buffer, 0, sublen);

      return sublen;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    } finally {
      TempBuffer.free(tBuf);
    }
  }

  /**
   * Append from an input stream, reading from the input stream until
   * end of file or the length is reached.
   */
  public int appendReadAll(InputStream is, long length)
  {
    TempBuffer tBuf = TempBuffer.allocate();

    try {
      byte []buffer = tBuf.getBuffer();
      int readLength = 0;
      
      while (length > 0) {
	int sublen = buffer.length;
	if (length < sublen)
	  sublen = (int) length;

	sublen = is.read(buffer, 0, sublen);

	if (sublen > 0) {
	  append(buffer, 0, sublen);
	  length -= sublen;
	  readLength += sublen;
	}
	else
	  return readLength > 0 ? readLength : -1;
      }

      return readLength;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    } finally {
      TempBuffer.free(tBuf);
    }
  }

  /**
   * Append from an input stream, using InputStream semantics, i.e
   * call is.read() only once.
   */
  public int appendRead(BinaryInput is, long length)
  {
    TempBuffer tBuf = TempBuffer.allocate();

    try {
      byte []buffer = tBuf.getBuffer();
      int sublen = buffer.length;
      if (length < sublen)
        sublen = (int) length;
      else if (length > sublen) {
        buffer = new byte[(int) length];
        sublen = (int) length;
      }

      sublen = is.read(buffer, 0, sublen);

      if (sublen > 0)
        append(buffer, 0, sublen);

      return sublen;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    } finally {
      TempBuffer.free(tBuf);
    }
  }

  /**
   * Append from an input stream, reading all available data from the
   * stream.
   */
  public int appendReadAll(BinaryInput is, long length)
  {
    TempBuffer tBuf = TempBuffer.allocate();

    try {
      byte []buffer = tBuf.getBuffer();
      int readLength = 0;
      
      while (length > 0) {
	int sublen = buffer.length;
	if (length < sublen)
	  sublen = (int) length;

	sublen = is.read(buffer, 0, sublen);

	if (sublen > 0) {
	  append(buffer, 0, sublen);
	  length -= sublen;
	  readLength += sublen;
	}
	else
	  return readLength > 0 ? readLength : -1;
      }

      return readLength;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    } finally {
      TempBuffer.free(tBuf);
    }
  }

  /**
   * Exports the value.
   */
  @Override
  public void varExport(StringBuilder sb)
  {
    sb.append("'");

    String value = toString();
    int len = value.length();
    for (int i = 0; i < len; i++) {
      char ch = value.charAt(i);

      switch (ch) {
      case '\'':
        sb.append("\\'");
        break;
      case '\\':
        sb.append("\\\\");
        break;
      default:
        sb.append(ch);
      }
    }
    sb.append("'");
  }

  /**
   * Interns the string.
   */
  /*
  public StringValue intern(Quercus quercus)
  {
    return quercus.intern(toString());
  }
  */

  //
  // CharSequence
  //

  /**
   * Returns the length of the string.
   */
  public int length()
  {
    return toString().length();
  }
  
  /**
   * Returns the character at a particular location
   */
  public char charAt(int index)
  {
    return toString().charAt(index);
  }

  /**
   * Returns a subsequence
   */
  public CharSequence subSequence(int start, int end)
  {
    return new StringBuilderValue(toString().substring(start, end));
  }

  //
  // java.lang.String methods
  //

  /**
   * Returns the first index of the match string, starting from the head.
   */
  public final int indexOf(CharSequence match)
  {
    return indexOf(match, 0);
  }
    
  /**
   * Returns the first index of the match string, starting from the head.
   */
  public int indexOf(CharSequence match, int head)
  {
    int length = length();
    int matchLength = match.length();

    if (matchLength <= 0)
      return -1;
    else if (head < 0)
      return -1;
    
    int end = length - matchLength;
    char first = match.charAt(0);

    loop:
    for (; head <= end; head++) {
      if (charAt(head) != first)
	continue;

      for (int i = 1; i < matchLength; i++) {
	if (charAt(head + i) != match.charAt(i))
	  continue loop;
      }

      return head;
    }

    return -1;
  }
    
  /**
   * Returns the last index of the match string, starting from the head.
   */
  public int indexOf(char match)
  {
    return indexOf(match, 0);
  }
    
  /**
   * Returns the last index of the match string, starting from the head.
   */
  public int indexOf(char match, int head)
  {
    int length = length();
    
    for (; head < length; head++) {
      if (charAt(head) == match)
	return head;
    }

    return -1;
  }
    
  /**
   * Returns the last index of the match string, starting from the head.
   */
  public final int lastIndexOf(char match)
  {
    return lastIndexOf(match, Integer.MAX_VALUE);
  }
    
  /**
   * Returns the last index of the match string, starting from the head.
   */
  public int lastIndexOf(char match, int tail)
  {
    int length = length();

    if (tail >= length)
      tail = length - 1;
    
    for (; tail >= 0; tail--) {
      if (charAt(tail) == match)
	return tail;
    }

    return -1;
  }

  /**
   * Returns the last index of the match string, starting from the tail.
   */
  public int lastIndexOf(CharSequence match)
  {
    return lastIndexOf(match, Integer.MAX_VALUE);
  }

  /**
   * Returns the last index of the match string, starting from the tail.
   */
  public int lastIndexOf(CharSequence match, int tail)
  {
    int length = length();
    int matchLength = match.length();

    if (matchLength <= 0)
      return -1;
    if (tail < 0)
      return -1;

    if (tail > length - matchLength)
      tail = length - matchLength;

    char first = match.charAt(0);

    loop:
    for (; tail >= 0; tail--) {
      if (charAt(tail) != first)
        continue;

      for (int i = 1; i < matchLength; i++) {
        if (charAt(tail + i) != match.charAt(i))
	      continue loop;
      }

      return tail;
    }

    return -1;
  }
    
  /**
   * Returns true if the region matches
   */
  public boolean regionMatches(int offset,
			       char []mBuffer, int mOffset, int mLength)
  {
    int length = length();

    if (length < offset + mLength)
      return false;

    for (int i = 0; i < mLength; i++) {
      if (charAt(offset + i) != mBuffer[mOffset + i])
	return false;
    }

    return true;
  }
    
  /**
   * Returns true if the region matches
   */
  public boolean regionMatches(int offset,
			       StringValue match, int mOffset, int mLength)
  {
    int length = length();

    if (length < offset + mLength)
      return false;

    for (int i = 0; i < mLength; i++) {
      if (charAt(offset + i) != match.charAt(mOffset + i))
	return false;
    }

    return true;
  }
    
  /**
   * Returns true if the region matches
   */
  public boolean
    regionMatchesIgnoreCase(int offset,
			    char []match, int mOffset, int mLength)
  {
    int length = length();

    if (length < offset + mLength)
      return false;

    for (int i = 0; i < mLength; i++) {
      char a = Character.toLowerCase(charAt(offset + i));
      char b = Character.toLowerCase(match[mOffset + i]);
      
      if (a != b)
	return false;
    }

    return true;
  }

  /**
   * Returns true if the string ends with another string.
   */
  public boolean endsWith(StringValue tail)
  {
    int len = length();
    int tailLen = tail.length();

    int offset = len - tailLen;
    
    if (offset < 0)
      return false;

    for (int i = 0; i < tailLen; i++) {
      if (charAt(offset + i) != tail.charAt(i))
	return false;
    }

    return true;
  }

  /**
   * Returns a StringValue substring.
   */
  public StringValue substring(int head)
  {
    return (StringValue) subSequence(head, length());
  }

  /**
   * Returns a StringValue substring.
   */
  public StringValue substring(int begin, int end)
  {
    return (StringValue) subSequence(begin, end);
  }

  /**
   * Returns a String substring
   */
  public String stringSubstring(int begin, int end)
  {
    return substring(begin, end).toString();
  }

  /**
   * Returns a character array
   */
  public char []toCharArray()
  {
    int length = length();
    
    char []array = new char[length()];

    getChars(0, array, 0, length);

    return array;
  }

  public char []getRawCharArray()
  {
    return toCharArray();
  }

  /**
   * Copies the chars
   */
  public void getChars(int stringOffset, char []buffer, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      buffer[offset + i] = charAt(stringOffset + i);
  }

  /**
   * Convert to lower case.
   */
  public StringValue toLowerCase()
  {
    int length = length();
    
    UnicodeBuilderValue string = new UnicodeBuilderValue(length);
    
    char []buffer = string.getBuffer();
    getChars(0, buffer, 0, length);

    for (int i = 0; i < length; i++) {
      char ch = buffer[i];
      
      if ('A' <= ch && ch <= 'Z')
	buffer[i] = (char) (ch + 'a' - 'A');
      else if (ch < 0x80) {
      }
      else if (Character.isUpperCase(ch))
	buffer[i] = Character.toLowerCase(ch);
    }

    string.setOffset(length);

    return string;
  }
  
  /**
   * Convert to lower case.
   */
  public StringValue toUpperCase()
  {
    int length = length();
    
    UnicodeBuilderValue string = new UnicodeBuilderValue(length);

    char []buffer = string.getBuffer();
    getChars(0, buffer, 0, length);

    for (int i = 0; i < length; i++) {
      char ch = buffer[i];
      
      if ('a' <= ch && ch <= 'z')
        buffer[i] = (char) (ch + 'A' - 'a');
      else if (ch < 0x80) {
      }
      else if (Character.isLowerCase(ch))
        buffer[i] = Character.toUpperCase(ch);
    }

    string.setOffset(length);

    return string;
  }

  /**
   * Returns a byteArrayInputStream for the value.
   * See TempBufferStringValue for how this can be overriden
   *
   * @return InputStream
   */
  public InputStream toInputStream()
  {
    try {
      //XXX: refactor so that env is passed in
      return toInputStream(Env.getInstance().getRuntimeEncoding());
    }
    catch (UnsupportedEncodingException e) {
      throw new QuercusRuntimeException(e);
    }
    //return new StringValueInputStream();
  }

  /**
   * Returns a byte stream of chars.
   * @param charset to encode chars to
   */
  public InputStream toInputStream(String charset)
    throws UnsupportedEncodingException
  {
    return new ByteArrayInputStream(toString().getBytes(charset));
  }

  /**
   * Returns a char stream.
   * XXX: when decoding fails
   *
   * @param charset to decode bytes by
   */
  public Reader toReader(String charset)
    throws UnsupportedEncodingException
  {
    byte []bytes = toBytes();
    
    return new InputStreamReader(new ByteArrayInputStream(bytes), charset);
  }

  public byte []toBytes()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Decodes from charset and returns UnicodeValue.
   *
   * @param env
   * @param charset
   */
  public StringValue toUnicodeValue(Env env, String charset)
  {
    StringValue sb = env.createUnicodeBuilder();

    Decoder decoder = Decoder.create(charset);
    
    sb.append(decoder.decode(env, this));

    return sb;
  }

  /**
   * Decodes from charset and returns UnicodeValue.
   *
   * @param env
   * @param charset
   */
  public StringValue convertToUnicode(Env env, String charset)
  {
    UnicodeBuilderValue sb = new UnicodeBuilderValue();

    Decoder decoder = Decoder.create(charset);
    decoder.setAllowMalformedOut(true);
    
    sb.append(decoder.decode(env, this));
    
    return sb;
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    return createStringBuilder().append(this);
  }

  /**
   * Writes to a stream
   */
  public void writeTo(OutputStream os)
  {
    try {
      int len = length();

      for (int i = 0; i < len; i++)
        os.write(charAt(i));
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  //
  // java.lang.Object methods
  //

  /**
   * Returns the hash code.
   */
  public int hashCode()
  {
    int hash = 37;

    int length = length();

    for (int i = 0; i < length; i++) {
      hash = 65521 * hash + charAt(i);
    }

    return hash;
  }

  /**
   * Test for equality
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof StringValue))
      return false;

    StringValue s = (StringValue) o;

    if (s.isUnicode() != isUnicode())
      return false;
    
    int aLength = length();
    int bLength = s.length();

    if (aLength != bLength)
      return false;

    for (int i = aLength - 1; i >= 0; i--) {
      if (charAt(i) != s.charAt(i))
	return false;
    }

    return true;
  }
  
  //
  // Java generator code
  //

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  @Override
  public void generate(PrintWriter out)
    throws IOException
  {
    // max JVM constant string length
    int maxSublen = 0xFFFE;
    
    int len = length();
    
    String className = getClass().getSimpleName();
    
    if (len == 1) {
      out.print(className + ".create('");
      printJavaChar(out, charAt(0));
      out.print("')");
    }
    else if (len < maxSublen) {
      out.print("new " + className + "(\"");
      printJavaString(out, this);
      out.print("\")");
    }
    else {
      out.print("((" + className + ") (new " + className + "(\"");
      
      // php/313u
      for (int i = 0; i < len; i += maxSublen) {
        if (i != 0)
          out.print("\").append(\"");
        
        printJavaString(out, substring(i, Math.min(i + maxSublen, len)));
      }
      
      out.print("\")))");
    }
  }

  @Override
  abstract public String toDebugString();

  @Override
  abstract public void varDumpImpl(Env env,
                                   WriteStream out,
                                   int depth,
                                   IdentityHashMap<Value, String> valueSet)
    throws IOException;

  class StringValueInputStream extends java.io.InputStream {
    private final int _length;
    private int _index;

    StringValueInputStream()
    {
      _length = length();
    }
    
    /**
     * Reads the next byte.
     */
    public int read()
    {
      if (_index < _length)
        return charAt(_index++);
      else
        return -1;
    }

    /**
     * Reads into a buffer.
     */
    public int read(byte []buffer, int offset, int length)
    {
      int sublen = _length - _index;

      if (length < sublen)
        sublen = length;

      if (sublen <= 0)
        return -1;

      int index = _index;

      for (int i = 0; i < sublen; i++)
        buffer[offset + i] = (byte) charAt(index + i);

      _index += sublen;

      return sublen;
    }
  }
}

