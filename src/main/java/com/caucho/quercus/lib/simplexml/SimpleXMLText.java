/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.simplexml;

import com.caucho.quercus.env.*;

/**
 * SimpleXMLElement object oriented API facade.
 * Also acts as the DOM document.
 */
public class SimpleXMLText extends SimpleXMLElement
{
  protected SimpleXMLText(Env env,
                          QuercusClass cls)
  {
    super(env, cls, null, "#text");
  }
  
  protected SimpleXMLText(Env env,
                          QuercusClass cls,
                          StringValue text)
  {
    super(env, cls, null, "#text");

    _text = text;
  }

  protected boolean isElement()
  {
    return false;
  }

  protected boolean isText()
  {
    return true;
  }
  
  protected void toXMLImpl(StringValue sb)
  {
    sb.append(_text);
  }
  
  public StringValue __toString()
  {
    return _text;
  }
}
