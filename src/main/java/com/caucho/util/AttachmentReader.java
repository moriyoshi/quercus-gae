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
 * @author Emil Ong
 */

package com.caucho.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reads attachments from a stream.
 **/
public class AttachmentReader {
  private static final Logger log = 
    Logger.getLogger(AttachmentReader.class.getName());
  private static final L10N L = new L10N(AttachmentReader.class);

  public static List<Attachment> read(InputStream is, String contentType)
    throws IOException
  {
    String[] tokens = contentType.split(";");

    String boundary = null;

    for (int i = 0; i < tokens.length; i++) {
      int start = tokens[i].indexOf("boundary=");
      
      if (start >= 0) {
        boundary = tokens[i].substring(start + "boundary=".length() + 1,
                                       tokens[i].lastIndexOf('"'));
        boundary = boundary + "\r\n";
        break;
      }
    }

    if (boundary == null)
      return null; // XXX throw something about malformed header

    List<Attachment> attachments = new ArrayList<Attachment>();

    Attachment attachment = new Attachment();
    BoundaryBuffer buffer = new BoundaryBuffer();
    boolean starting = true;
    boolean inBody = false;
    int lastCh = -1;

    for (int ch = is.read(); ch >= 0; ch = is.read()) {
      buffer.write(ch);

      // found a boundary
      if (buffer.endsWith(boundary)) {
        if (! starting) {
          attachment.setContents(buffer.getContents(boundary));
          attachments.add(attachment);
          attachment = new Attachment();
        }

        buffer.reset();

        starting = false;
        inBody = false;
      }
      else if (! inBody && ! starting && lastCh == '\r' && ch == '\n') {
        if (buffer.size() == 2)
          inBody = true;
        
        else {
          String header = buffer.toString("US-ASCII");

          tokens = header.split(":", 2);

          if (tokens.length == 2)
            attachment.addHeader(tokens[0].trim(), tokens[1].trim());

          else
            log.fine(L.l("Header in attachment does not contain a ':': '{0}'",
                         header));
        }

        buffer.reset();
      }

      lastCh = ch;
    }

    attachment.setContents(buffer.toByteArray());
    attachments.add(attachment);

    return attachments;
  }

  private static class BoundaryBuffer extends ByteArrayOutputStream {
    public boolean endsWith(String boundary)
      throws java.io.UnsupportedEncodingException
    {
      if (count < boundary.length())
        return false;

      String end = new String(buf, count-boundary.length(), boundary.length(), 
                              "US-ASCII");

      return end.equals(boundary);
    }

    public byte[] getContents(String boundary)
    {
      byte[] contents = new byte[count-boundary.length()-"\r\n--".length()];

      System.arraycopy(buf, 0, contents, 0, contents.length);

      return contents;
    }
  }
}
