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

package com.caucho.quercus.lib;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.file.BinaryInput;
import com.caucho.quercus.lib.file.BinaryStream;
import com.caucho.quercus.lib.file.FileModule;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.Base64;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.TempBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * PHP URL
 */
public class UrlModule
  extends AbstractQuercusModule
{
  private static final L10N L = new L10N(UrlModule.class);
  private static final Logger log
    = Logger.getLogger(UrlModule.class.getName());

  public static final int PHP_URL_SCHEME = 0;
  public static final int PHP_URL_HOST = 1;
  public static final int PHP_URL_PORT = 2;
  public static final int PHP_URL_USER = 3;
  public static final int PHP_URL_PASS = 4;
  public static final int PHP_URL_PATH = 5;
  public static final int PHP_URL_QUERY = 6;
  public static final int PHP_URL_FRAGMENT = 7;
  
  /**
   * Encodes base64
   */
  public static String base64_encode(InputStream is)
  {
    CharBuffer cb = new CharBuffer();

    TempBuffer tb = TempBuffer.allocate();
    byte []buffer = tb.getBuffer();

    int len;
    int offset = 0;
    
    try {
      while ((len = is.read(buffer, offset, buffer.length - offset)) >= 0) {
        int tail = len % 3;

        Base64.encode(cb, buffer, 0, len - tail);

        System.arraycopy(buffer, len - tail, buffer, 0, tail);
        offset = tail;
      }

      if (offset > 0)
        Base64.encode(cb, buffer, 0, offset);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      TempBuffer.free(tb);
    }

    return cb.toString();
  }

  /**
   * Decodes base64
   */
  public static String base64_decode(String str)
  {
    if (str == null)
      return "";

    return Base64.decode(str);
  }

  /**
   * Connects to the given URL using a HEAD request to retreive
   * the headers sent in the response.
   */
  public static Value get_headers(Env env, String urlString,
                                  @Optional Value format)
  {
    Socket socket = null;

    try {
      URL url = new URL(urlString);

      if (! url.getProtocol().equals("http") &&
          ! url.getProtocol().equals("https")) {
        env.warning(L.l("Not an HTTP URL"));
        return null;
      }

      int port = 80;

      if (url.getPort() < 0) {
        if (url.getProtocol().equals("http"))
          port = 80;
        else if (url.getProtocol().equals("https"))
          port = 443;
      } else {
        port = url.getPort();
      }

      socket = new Socket(url.getHost(), port);

      OutputStream out = socket.getOutputStream();
      InputStream in = socket.getInputStream();

      StringBuilder request = new StringBuilder();

      request.append("HEAD ");

      if (url.getPath() != null)
        request.append(url.getPath());

      if (url.getQuery() != null)
        request.append("?" + url.getQuery());

      if (url.getRef() != null)
        request.append("#" + url.getRef());

      request.append(" HTTP/1.0\r\n");

      if (url.getHost() != null)
        request.append("Host: " + url.getHost() + "\r\n");

      request.append("\r\n");

      OutputStreamWriter writer = new OutputStreamWriter(out);
      writer.write(request.toString());
      writer.flush();

      LineNumberReader reader = new LineNumberReader(new InputStreamReader(in));

      ArrayValue result = new ArrayValueImpl();

      if (format.toBoolean()) {
        for (String line = reader.readLine();
             line != null;
             line = reader.readLine()) {
          line = line.trim();

          if (line.length() == 0)
            continue;

          int colon = line.indexOf(':');

          ArrayValue values;

          if (colon < 0)
            result.put(env.createString(line.trim()));
          else {
            StringValue key =
              env.createString(line.substring(0, colon).trim());

            StringValue value;

            if (colon < line.length())
              value = env.createString(line.substring(colon + 1).trim());
            else
              value = env.getEmptyString();


            if (result.get(key) != UnsetValue.UNSET)
              values = (ArrayValue)result.get(key);
            else {
              values = new ArrayValueImpl();

              result.put(key, values);
            }

            values.put(value);
          }
        }

        // collapse single entries
        for (Value key : result.keySet()) {
          Value value = result.get(key);

          if (value.isArray() && ((ArrayValue)value).getSize() == 1)
            result.put(key, ((ArrayValue)value).get(LongValue.ZERO));
        }
      } else {
        for (String line = reader.readLine();
             line != null;
             line = reader.readLine()) {
          line = line.trim();

          if (line.length() == 0)
            continue;

          result.put(env.createString(line.trim()));
        }
      }

      return result;
    } catch (Exception e) {
      env.warning(e);

      return BooleanValue.FALSE;
    } finally {
      try {
        if (socket != null)
          socket.close();
      } catch (IOException e) {
        env.warning(e);
      }
    }
  }

  /**
   * Extracts the meta tags from a file and returns them as an array.
   */
  public static Value get_meta_tags(Env env, StringValue filename,
                                    @Optional("false") boolean use_include_path)
  {
    InputStream in = null;

    ArrayValue result = new ArrayValueImpl();

    try {
      BinaryStream stream
	= FileModule.fopen(env, filename, "r", use_include_path, null);

      if (stream == null || ! (stream instanceof BinaryInput))
        return result;

      BinaryInput input = (BinaryInput) stream;

      while (! input.isEOF()) {
        String tag = getNextTag(input);

        if (tag.equalsIgnoreCase("meta")) {
          String name = null;
          String content = null;

          String [] attr;

          while ((attr = getNextAttribute(input)) != null) {
            if (name == null && attr[0].equalsIgnoreCase("name")) {
              if (attr.length > 1)
                name = attr[1];
            } else if (content == null && attr[0].equalsIgnoreCase("content")) {
              if (attr.length > 1)
                content = attr[1];
            }

            if (name != null && content != null) {
              result.put(env.createString(name),
                         env.createString(content));
              break;
            }
          }
        } else if (tag.equalsIgnoreCase("/head"))
          break;
      }
    } catch (IOException e) {
      env.warning(e);
    } finally {
      try {
        if (in != null)
          in.close();
      } catch (IOException e) {
        env.warning(e);
      }
    }

    return result;
  }
  
  public static Value http_build_query(Env env,
                                       Value formdata, 
		                               @Optional StringValue numeric_prefix,
		                               @Optional("'&'") StringValue separator)
  {
    StringValue result = env.createUnicodeBuilder();

    httpBuildQueryImpl(env,
                       result,
                       formdata,
                       env.getEmptyString(),
                       numeric_prefix,
                       separator);

    return result;
  }
  
  private static void httpBuildQueryImpl(Env env,
                                         StringValue result,
                                         Value formdata,
                                         StringValue path,
                                         StringValue numeric_prefix,
                                         StringValue separator)
  {
    Set<Map.Entry<Value,Value>> entrySet;

    if (formdata.isArray())
      entrySet = ((ArrayValue)formdata).entrySet();
    else if (formdata.isObject()) {
      Set<? extends Map.Entry<Value,Value>> stringEntrySet
        = ((ObjectValue)formdata).entrySet();

      LinkedHashMap<Value,Value> valueMap = new LinkedHashMap<Value,Value>();

      for (Map.Entry<Value,Value> entry : stringEntrySet)
        valueMap.put(entry.getKey(), entry.getValue());

      entrySet = valueMap.entrySet();
    } else {
      env.warning(L.l("formdata must be an array or object"));

      return;
    }

    boolean isFirst = true;
    for (Map.Entry<Value,Value> entry : entrySet) {
      if (! isFirst) {
        if (separator != null)
          result.append(separator);
        else
          result.append("&");
      }
      isFirst = false;
      
      StringValue newPath = makeNewPath(path, entry.getKey(), numeric_prefix);
      Value entryValue = entry.getValue();

      if (entryValue.isArray() || entryValue.isObject()) {
        // can always throw away the numeric prefix on recursive calls
        httpBuildQueryImpl(env, result, entryValue, newPath, null, separator);

      } else {
        result.append(newPath);
        result.append("=");
        result.append(urlencode(entry.getValue().toStringValue()));
      }
    }
  }

  private static StringValue makeNewPath(StringValue oldPath,
                                         Value key,
                                         StringValue numeric_prefix)
  {
    StringValue path = oldPath.createStringBuilder();
    
    if (oldPath.length() != 0) {
      path.append(oldPath);
      //path.append('[');
      path.append("%5B");
      urlencode(path, key.toStringValue());
      //path.append(']');
      path.append("%5D");

      return path;
    }
    else if (key.isLongConvertible() && numeric_prefix != null) {
      urlencode(path, numeric_prefix);
      urlencode(path, key.toStringValue());
      
      return path;
    }
    else {
      urlencode(path, key.toStringValue());
      
      return path;
    }
  }

  /**
   * Creates a http string.
   */
  /*
  public String http_build_query(Value value,
                                 @Optional String prefix)
  {
    StringBuilder sb = new StringBuilder();

    int index = 0;
    if (value instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) value;

      for (Map.Entry<Value,Value> entry : array.entrySet()) {
        Value keyValue = entry.getKey();
        Value v = entry.getValue();

        String key;

        if (keyValue.isLongConvertible())
          key = prefix + keyValue;
        else
          key = keyValue.toString();

        if (v instanceof ArrayValue)
          http_build_query(sb, key, (ArrayValue) v);
        else {
          if (sb.length() > 0)
            sb.append('&');

          sb.append(key);
          sb.append('=');
          urlencode(sb, v.toString());
        }
      }
    }

    return sb.toString();
  }
  */

  /**
   * Creates a http string.
   */
  /*
  private void http_build_query(StringBuilder sb,
                                String prefix,
                                ArrayValue array)
  {
    for (Map.Entry<Value,Value> entry : array.entrySet()) {
      Value keyValue = entry.getKey();
      Value v = entry.getValue();

      String key = prefix + '[' + keyValue + ']';

      if (v instanceof ArrayValue)
        http_build_query(sb, key, (ArrayValue) v);
      else {
        if (sb.length() > 0)
          sb.append('&');

        sb.append(key);
        sb.append('=');
        urlencode(sb, v.toString());
      }
    }
  }
  */

  /**
   * Parses the URL into an array.
   */
  public static Value parse_url(Env env,
                                StringValue str,
                                @Optional("-1") int component)
  {
    int i = 0;
    int length = str.length();

    StringValue sb = str.createStringBuilder();

    ArrayValueImpl value = new ArrayValueImpl();

    // XXX: php/1i04.qa contradicts:
    // value.put("path", "");

    ParseUrlState state = ParseUrlState.INIT;

    StringValue user = null;

    for (; i < length; i++) {
      char ch = str.charAt(i);

      switch (ch) {
      case ':':
        if (state == ParseUrlState.INIT) {
          value.put(env.createString("scheme"), sb);
          sb = env.createUnicodeBuilder();

          if (length <= i + 1 || str.charAt(i + 1) != '/') {
            state = ParseUrlState.PATH;
          }
          else if (length <= i + 2 || str.charAt(i + 2) != '/') {
            state = ParseUrlState.PATH;
          }
          else if (length <= i + 3 || str.charAt(i + 3) != '/') {
            i += 2;
            state = ParseUrlState.USER;
          }
          else {
            // file:///foo

            i += 2;
            state = ParseUrlState.PATH;
          }
        }
        else if (state == ParseUrlState.USER) {
          user = sb;
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.PASS;
        }
        else if (state == ParseUrlState.HOST) {
          value.put(env.createString("host"), sb);
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.PORT;
        }
        else
          sb.append(ch);
        break;

      case '@':
        if (state == ParseUrlState.USER) {
          value.put(env.createString("user"), sb);
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.HOST;
        }
        else if (state == ParseUrlState.PASS) {
          value.put(env.createString("user"), user);
          value.put(env.createString("pass"), sb);
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.HOST;
        }
        else
          sb.append(ch);
        break;

      case '/':
        if (state == ParseUrlState.USER || state == ParseUrlState.HOST) {
          value.put(env.createString("host"), sb);
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.PATH;
          sb.append(ch);
        }
        else if (state == ParseUrlState.PASS) {
          value.put(env.createString("host"), user);
          value.put(env.createString("port"), LongValue.create(sb.toLong()));
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.PATH;
          sb.append(ch);
        }
        else if (state == ParseUrlState.PORT) {
          value.put(env.createString("port"), LongValue.create(sb.toLong()));
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.PATH;
          sb.append(ch);
        }
        else
          sb.append(ch);
        break;

      case '?':
        if (state == ParseUrlState.USER || state == ParseUrlState.HOST) {
          value.put(env.createString("host"), sb);
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.QUERY;
        }
        else if (state == ParseUrlState.PASS) {
          value.put(env.createString("host"), user);
          value.put(env.createString("port"), LongValue.create(sb.toLong()));
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.QUERY;
        }
        else if (state == ParseUrlState.PORT) {
          value.put(env.createString("port"), LongValue.create(sb.toLong()));
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.QUERY;
        }
        else if (state == ParseUrlState.PATH) {
          if (sb.length() > 0)
            value.put(env.createString("path"), sb);
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.QUERY;
        }
        else
          sb.append(ch);
        break;

      case '#':
        if (state == ParseUrlState.USER || state == ParseUrlState.HOST) {
          value.put(env.createString("host"), sb);
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.FRAGMENT;
        }
        else if (state == ParseUrlState.PASS) {
          value.put(env.createString("host"), user);
          value.put(env.createString("port"), LongValue.create(sb.toLong()));
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.FRAGMENT;
        }
        else if (state == ParseUrlState.PORT) {
          value.put(env.createString("port"), LongValue.create(sb.toLong()));
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.FRAGMENT;
        }
        else if (state == ParseUrlState.PATH) {
          if (sb.length() > 0)
            value.put(env.createString("path"), sb);
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.FRAGMENT;
        }
        else if (state == ParseUrlState.QUERY) {
          if (sb.length() > 0)
            value.put(env.createString("query"), sb);
          sb = env.createUnicodeBuilder();
          state = ParseUrlState.FRAGMENT;
        }
        else
          sb.append(ch);
        break;

      default:
        sb.append((char) ch);
        break;
      }
    }

    if (sb.length() == 0) {
    }
    else if (state == ParseUrlState.USER
	     || state == ParseUrlState.HOST)
      value.put(env.createString("host"), sb);
    else if (state == ParseUrlState.PASS) {
      value.put(env.createString("host"), user);
      value.put(env.createString("port"), LongValue.create(sb.toLong()));
    }
    else if (state == ParseUrlState.PORT) {
      value.put(env.createString("port"), LongValue.create(sb.toLong()));
    }
    else if (state == ParseUrlState.QUERY)
      value.put(env.createString("query"), sb);
    else if (state == ParseUrlState.FRAGMENT)
      value.put(env.createString("fragment"), sb);
    else
      value.put(env.createString("path"), sb);

    switch (component) {
    case PHP_URL_SCHEME:
      return value.get(env.createString("scheme"));
    case PHP_URL_HOST:
      return value.get(env.createString("host"));
    case PHP_URL_PORT:
      return value.get(env.createString("port"));
    case PHP_URL_USER:
      return value.get(env.createString("user"));
    case PHP_URL_PASS:
      return value.get(env.createString("pass"));
    case PHP_URL_PATH:
      return value.get(env.createString("path"));
    case PHP_URL_QUERY:
      return value.get(env.createString("query"));
    case PHP_URL_FRAGMENT:
      return value.get(env.createString("fragment"));
    }
    
    return value;
  }


  /**
   * Returns the decoded string.
   */
  public static String rawurldecode(String s)
  {
    if (s == null)
      return "";

    int len = s.length();
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      if (ch == '%' && i + 2 < len) {
        int d1 = s.charAt(i + 1);
        int d2 = s.charAt(i + 2);

        int v = 0;

        if ('0' <= d1 && d1 <= '9')
          v = 16 * (d1 - '0');
        else if ('a' <= d1 && d1 <= 'f')
          v = 16 * (d1 - 'a' + 10);
        else if ('A' <= d1 && d1 <= 'F')
          v = 16 * (d1 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        if ('0' <= d2 && d2 <= '9')
          v += (d2 - '0');
        else if ('a' <= d2 && d2 <= 'f')
          v += (d2 - 'a' + 10);
        else if ('A' <= d2 && d2 <= 'F')
          v += (d2 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        i += 2;
        sb.append((char) v);
      }
      else
        sb.append(ch);
    }

    return sb.toString();
  }

  /**
   * Encodes the url
   */
  public static String rawurlencode(String str)
  {
    if (str == null)
      return "";
    
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);

      if ('a' <= ch && ch <= 'z' ||
          'A' <= ch && ch <= 'Z' ||
          '0' <= ch && ch <= '9' ||
          ch == '-' || ch == '_' || ch == '.') {
        sb.append(ch);
      }
      else {
        sb.append('%');
        sb.append(toHexDigit(ch >> 4));
        sb.append(toHexDigit(ch));
      }
    }

    return sb.toString();
  }

  enum ParseUrlState {
    INIT, USER, PASS, HOST, PORT, PATH, QUERY, FRAGMENT
  };

  /**
   * Gets the magic quotes value.
   */
  public static StringValue urlencode(StringValue str)
  {
    StringValue sb = str.createStringBuilder();

    urlencode(sb, str);

    return sb;
  }

  /**
   * Gets the magic quotes value.
   */
  private static void urlencode(StringValue sb, StringValue str)
  {
    int len = str.length();

    for (int i = 0; i < len; i++) {
      char ch = str.charAt(i);

      if ('a' <= ch && ch <= 'z')
        sb.append(ch);
      else if ('A' <= ch && ch <= 'Z')
        sb.append(ch);
      else if ('0' <= ch && ch <= '9')
        sb.append(ch);
      else if (ch == '-' || ch == '_' || ch == '.')
        sb.append(ch);
      else if (ch == ' ')
        sb.append('+');
      else {
        sb.append('%');
        sb.append(toHexDigit(ch / 16));
        sb.append(toHexDigit(ch));
      }
    }
  }

  /**
   * Returns the decoded string.
   */
  public static String urldecode(String s)
  {
    if (s == null)
      return "";
  
    int len = s.length();
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      if (ch == '%' && i + 2 < len) {
        int d1 = s.charAt(i + 1);
        int d2 = s.charAt(i + 2);

        int v = 0;

        if ('0' <= d1 && d1 <= '9')
          v = 16 * (d1 - '0');
        else if ('a' <= d1 && d1 <= 'f')
          v = 16 * (d1 - 'a' + 10);
        else if ('A' <= d1 && d1 <= 'F')
          v = 16 * (d1 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        if ('0' <= d2 && d2 <= '9')
          v += (d2 - '0');
        else if ('a' <= d2 && d2 <= 'f')
          v += (d2 - 'a' + 10);
        else if ('A' <= d2 && d2 <= 'F')
          v += (d2 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        i += 2;
        sb.append((char) v);
      }
      else if (ch == '+')
        sb.append(' ');
      else
        sb.append(ch);
    }

    return sb.toString();
  }

  private static String getNextTag(BinaryInput input)
    throws IOException
  {
    StringBuilder tag = new StringBuilder();

    for (int ch = 0; ! input.isEOF() && ch != '<'; ch = input.read()) {}

    while (! input.isEOF()) {
      int ch = input.read();

      if (Character.isWhitespace(ch))
        break;

      tag.append((char) ch);
    }

    return tag.toString();
  }

  /**
   * Finds the next attribute in the stream and return the key and value
   * as an array.
   */
  private static String [] getNextAttribute(BinaryInput input)
    throws IOException
  {
    int ch;

    consumeWhiteSpace(input);

    StringBuilder attribute = new StringBuilder();

    while (! input.isEOF()) {
      ch = input.read();

      if (isValidAttributeCharacter(ch))
        attribute.append((char) ch);
      else {
        input.unread();
        break;
      }
    }

    if (attribute.length() == 0)
      return null;

    consumeWhiteSpace(input);

    if (input.isEOF())
      return new String[] { attribute.toString() };

    ch = input.read();
    if (ch != '=') {
      input.unread();

      return new String[] { attribute.toString() };
    }

    consumeWhiteSpace(input);

    // check for quoting
    int quote = ' ';
    boolean quoted = false;

    if (input.isEOF())
      return new String[] { attribute.toString() };

    ch = input.read();

    if (ch == '"' || ch == '\'') {
      quoted = true;
      quote = ch;
    } else
      input.unread();

    StringBuilder value = new StringBuilder();

    while (! input.isEOF()) {
      ch = input.read();

      // mimics PHP behavior
      if ((quoted && ch == quote) ||
          (! quoted && Character.isWhitespace(ch)) || ch == '>')
        break;

      value.append((char) ch);
    }

    return new String[] { attribute.toString(), value.toString() };
  }

  private static void consumeWhiteSpace(BinaryInput input)
    throws IOException
  {
    int ch = 0;

    while (! input.isEOF() && Character.isWhitespace(ch = input.read())) {}

    if (! Character.isWhitespace(ch))
      input.unread();
  }

  private static boolean isValidAttributeCharacter(int ch)
  {
    return Character.isLetterOrDigit(ch) ||
           (ch == '-') || (ch == '.') || (ch == '_') || (ch == ':');
  }

  private static char toHexDigit(int d)
  {
    d = d & 0xf;

    if (d < 10)
      return (char) ('0' + d);
    else
      return (char) ('A' + d - 10);
  }
}

