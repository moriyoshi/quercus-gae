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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Localization
 */
public class L10N {
  private static Logger _log;
  
  private static HashMap<String,HashMap<String,String>> l10nMap
    = new HashMap<String,HashMap<String,String>>();
  
  HashMap<String,String> messages;
  
  public L10N(Class cl)
  {
    String name = cl.getName().replace('.', '/');
    int p = name.lastIndexOf('/');

    if (p > 0)
      init(name.substring(0, p) + "/messages");
    else
      init("/messages");
  }
/*  
  public L10N(String path)
  {
    init(path);
  }
*/

  private void init(String path)
  {
    if (! path.startsWith("/"))
      path = "/" + path;

    messages = l10nMap.get(path);
    
    InputStream is = null;
    Locale locale = Locale.getDefault();
    
    try {
      String language = locale.getLanguage();

      String xmlName = path + "_" + language + ".xml";

      /* XXX: punt for now
      is = getClass().getResourceAsStream(xmlName);
      
      if (is != null) {
	RegistryNode registry = null;

	try {
	  ReadStream rs = Vfs.openRead(is);
        
	  Registry root = Registry.parse(rs);
	  registry = root.getTop();

	  rs.close();
	} finally {
	  is.close();
	}

        messages = new HashMap<String,String>();
        
        l10nMap.put(path, messages);

        RegistryNode localization = registry.lookup("localization");
        Iterator iter = localization.select("message");
        while (iter.hasNext()) {
          RegistryNode msg = (RegistryNode) iter.next();

          String key = msg.getString("key", null);
          String value = msg.getString("value", null);

          if (key != null && value != null)
            messages.put(key, value);
        }
      }
      */
    } catch (Exception e) {
      log().log(Level.FINE, e.toString(), e);
    }
  }
  
  public String l(String msg)
  {
    msg = getTranslated(msg);
    
    return msg;
  }
  
  public String l(String msg, long l)
  {
    return l(msg, String.valueOf(l));
  }
  
  public String l(String msg, Object o)
  {
    msg = getTranslated(msg);

    CharBuffer cb = CharBuffer.allocate();

    int length = msg.length();
    int i = 0;

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '{' || i + 2 >= length) {
        cb.append(ch);
        i++;
      }
      else {
        ch = msg.charAt(i + 1);
        
        if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else if (msg.charAt(i + 2) != '}') {
          cb.append('{');
          i++;
        }
        else if (ch == '0') {
          cb.append(o);
          i += 3;
        }
        else {
          cb.append('{');
          i++;
        }
      }
    }
    
    return cb.close();
  }
  
  public String l(String msg, Object o1, Object o2)
  {
    msg = getTranslated(msg);
    
    CharBuffer cb = CharBuffer.allocate();

    int length = msg.length();
    int i = 0;

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '{' || i + 2 >= length) {
        cb.append(ch);
        i++;
      }
      else {
        ch = msg.charAt(i + 1);
        
        if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else if (msg.charAt(i + 2) != '}') {
          cb.append('{');
          i++;
        }
        else if (ch == '0') {
          cb.append(o1);
          i += 3;
        }
        else if (ch == '1') {
          cb.append(o2);
          i += 3;
        }
        else {
          cb.append('{');
          i++;
        }
      }
    }
    
    return cb.close();
  }

  public String l(String msg, Object o1, int i2)
  {
    return l(msg, o1, String.valueOf(i2));
  }

  public String l(String msg, int i1, int i2)
  {
    return l(msg, String.valueOf(i1), String.valueOf(i2));
  }
  
  public String l(String msg, Object o1, Object o2, Object o3)
  {
    msg = getTranslated(msg);
    
    CharBuffer cb = CharBuffer.allocate();

    int length = msg.length();
    int i = 0;

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '{' || i + 2 >= length) {
        cb.append(ch);
        i++;
      }
      else {
        ch = msg.charAt(i + 1);
        
        if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else if (msg.charAt(i + 2) != '}') {
          cb.append('{');
          i++;
        }
        else if (ch == '0') {
          cb.append(o1);
          i += 3;
        }
        else if (ch == '1') {
          cb.append(o2);
          i += 3;
        }
        else if (ch == '2') {
          cb.append(o3);
          i += 3;
        }
        else {
          cb.append('{');
          i++;
        }
      }
    }
    
    return cb.close();
  }
  
  public String l(String msg, Object o1, Object o2, Object o3, Object o4)
  {
    msg = getTranslated(msg);
    
    CharBuffer cb = CharBuffer.allocate();

    int length = msg.length();
    int i = 0;

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '{' || i + 2 >= length) {
        cb.append(ch);
        i++;
      }
      else {
        ch = msg.charAt(i + 1);
        
        if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else if (msg.charAt(i + 2) != '}') {
          cb.append('{');
          i++;
        }
        else if (ch == '0') {
          cb.append(o1);
          i += 3;
        }
        else if (ch == '1') {
          cb.append(o2);
          i += 3;
        }
        else if (ch == '2') {
          cb.append(o3);
          i += 3;
        }
        else if (ch == '3') {
          cb.append(o4);
          i += 3;
        }
        else {
          cb.append('{');
          i++;
        }
      }
    }
    
    return cb.close();
  }
  
  public String l(String msg, Object o1, Object o2,
                  Object o3, Object o4, Object o5)
  {
    msg = getTranslated(msg);
    
    CharBuffer cb = CharBuffer.allocate();

    int length = msg.length();
    int i = 0;

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '{' || i + 2 >= length) {
        cb.append(ch);
        i++;
      }
      else {
        ch = msg.charAt(i + 1);
        
        if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else if (msg.charAt(i + 2) != '}') {
          cb.append('{');
          i++;
        }
        else if (ch == '0') {
          cb.append(o1);
          i += 3;
        }
        else if (ch == '1') {
          cb.append(o2);
          i += 3;
        }
        else if (ch == '2') {
          cb.append(o3);
          i += 3;
        }
        else if (ch == '3') {
          cb.append(o4);
          i += 3;
        }
        else if (ch == '4') {
          cb.append(o5);
          i += 3;
        }
        else {
          cb.append('{');
          i++;
        }
      }
    }
    
    return cb.close();
  }
  
  public String l(String msg, Object o1, Object o2,
                  Object o3, Object o4, Object o5, Object o6)
  {
    msg = getTranslated(msg);
    
    CharBuffer cb = CharBuffer.allocate();

    int length = msg.length();
    int i = 0;

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '{' || i + 2 >= length) {
        cb.append(ch);
        i++;
      }
      else {
        ch = msg.charAt(i + 1);
        
        if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else if (msg.charAt(i + 2) != '}') {
          cb.append('{');
          i++;
        }
        else if (ch == '0') {
          cb.append(o1);
          i += 3;
        }
        else if (ch == '1') {
          cb.append(o2);
          i += 3;
        }
        else if (ch == '2') {
          cb.append(o3);
          i += 3;
        }
        else if (ch == '3') {
          cb.append(o4);
          i += 3;
        }
        else if (ch == '4') {
          cb.append(o5);
          i += 3;
        }
        else if (ch == '5') {
          cb.append(o6);
          i += 3;
        }
        else {
          cb.append('{');
          i++;
        }
      }
    }
    
    return cb.close();
  }

  private String getTranslated(String msg)
  {
    if (messages == null)
      return msg;
    
    String translated = (String) messages.get(msg);

    if (translated == null) {
      return msg;
    }
    else
      return translated;
  }

  private Logger log()
  {
    if (_log == null)
      _log = Log.open(L10N.class);

    return _log;
  }
}
