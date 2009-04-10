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

import java.util.logging.*;
import java.net.InetAddress;

/**
 * Represents an internet network mask.
 */
public class InetNetwork {
  private static final Logger log
    = Logger.getLogger(InetNetwork.class.getName());
  
  private long _hiAddress;
  private long _address;
  private long _mask;
  private int _maskIndex;

  /**
   * Create a internet mask.
   *
   * @param inetAddress the main address
   * @param maskIndex the number of bits to match.
   */
  public InetNetwork(InetAddress inetAddress, int maskIndex)
  {
    byte []bytes = inetAddress.getAddress();

    long address = 0;
    for (int i = 0; i < bytes.length; i++)
      address = 256 * address + (bytes[i] & 0xff);

    _address = address;
    _maskIndex = maskIndex;
    _mask = -1L << (8 * bytes.length - maskIndex);
  }
  
  /**
   * Creates an inet network with a mask.
   */
  public InetNetwork(long address, int maskIndex)
  {
    _address = address;
    _maskIndex = maskIndex;
    _mask = -1L << (32 - maskIndex);
  }
  
  /**
   * Creates an inet network with a mask.
   */
  public InetNetwork(long hiAddress, long loAddress, int maskIndex)
  {
    _hiAddress = hiAddress;
    _address = loAddress;
    _maskIndex = maskIndex;
    _mask = -1L << (64 - maskIndex);
  }

  public static InetNetwork valueOf(String network)
  {
    return create(network);
  }
  
  public static InetNetwork create(String network)
  {
    if (network == null)
      return null;
    
    int i = 0;
    int len = network.length();

    if (network.indexOf(':') >= 0)
      return createIPv6(network);
    
    long address = 0;
    int digits = 0;

    int ch = 0;
    while (i < len) {
      if (network.charAt(i) == '/')
        break;

      int digit = 0;
      for (; i < len && '0' <= (ch = network.charAt(i)) && ch <= '9'; i++)
        digit = 10 * digit + ch - '0';

      address = 256 * address + digit;

      digits++;

      if (i < len && ch == '.')
        i++;
      else if (i < len)
	break;
    }

    int mask = 8 * digits;
    
    for (; digits < 4; digits++) {
      address *= 256;
    }

    if (i < len && network.charAt(i) == '/') {
      mask = 0;
      for (i++; i < len && '0' <= (ch = network.charAt(i)) && ch <= '9'; i++)
        mask = 10 * mask + ch - '0';
    }

    return new InetNetwork(address, mask);
  }

  public static InetNetwork createIPv6(String network)
  {
    
    int i = 0;
    int len = network.length();
    
    long hi = 0;
    long lo = 0;
    long topHi = 0;
    long topLo = 0;
    int digits = 0;
    boolean isIPv4 = false;

    int ch = 0;
    while (i < len) {
      ch = network.charAt(i);
      
      if (ch == '/')
        break;

      int digit = 0;
      for (; i < len; i++) {
	ch = network.charAt(i);
	int v = 0;

	if ('0' <= ch && ch <= '9')
	  v = ch - '0';
	else if ('a' <= ch && ch <= 'f')
	  v = ch - 'a';
	else if ('A' <= ch && ch <= 'F')
	  v = ch - 'A';
	else
	  break;

	if (isIPv4)
	  digit = 10 * digit + v;
	else
	  digit = 16 * digit + v;
      }

      if (ch == '.' && ! isIPv4) {
	digit = (digit / 256) * 100 + (digit / 16 % 16) * 10 + digit % 16;
	isIPv4 = true;
      }

      if (isIPv4) {
	hi = (hi << 8) + (lo >> 56);
	lo = (lo << 8) + digit;
	digits++;
      }
      else {
	hi = (hi << 16) + (lo >> 48);
	lo = (lo << 16) + digit;
	
	digits += 2;
      }

      if (len <= i)
	break;
      else if (ch == '/')
        break;
      else if (ch == '.')
        i++;
      else if (ch == ':' && i + 1 < len && network.charAt(i + 1) == ':') {
	i += 2;

	int shift = 16 - digits;

	topHi += hi << (8 * shift);

	if (shift < 8)
	  topHi += lo >> (8 * (8 - shift));
	else
	  topHi += lo << 8 * (shift - 8);

	hi = 0;
	lo = 0;
      }
      else if (ch == ':')
	i++;
      else
	break;
    }

    hi += topHi;
    lo += topLo;

    int mask = 64;

    if (i < len && network.charAt(i) == '/') {
      mask = 0;
      for (i++; i < len && (ch = network.charAt(i)) >= '0' && ch <= '9'; i++)
        mask = 10 * mask + ch - '0';
    }

    return new InetNetwork(hi, lo, mask);
  }

  /**
   * Returns true if the address is in this network.
   */
  public boolean isMatch(InetAddress inetAddress)
  {
    byte []bytes = inetAddress.getAddress();

    long address = 0;
    for (int i = 0; i < bytes.length; i++)
      address = 256 * address + (bytes[i] & 0xff);

    return (_address & _mask) == (address & _mask);
  }

  /**
   * Returns true if the address is in this network.
   */
  public boolean isMatch(byte []bytes)
  {
    long address = 0;
    for (int i = 0; i < bytes.length; i++)
      address = 256 * address + (bytes[i] & 0xff);

    return (_address & _mask) == (address & _mask);
  }

  /**
   * Returns true if the address is in this network.
   */
  public boolean isMatch(String address)
  {
    try {
      return isMatch(InetAddress.getByName(address));
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);

      return false;
    }
  }

  /**
   * Returns true if the address is in this network.
   */
  public boolean isMatch(long address)
  {
    return (_address & _mask) == (address & _mask);
  }

  /**
   * Return a readable string.
   */
  public String toString()
  {
    StringBuilder cb = new StringBuilder();

    for (int i = 0; i < 4; i++) {
      if (i != 0)
        cb.append('.');

      cb.append((_address >> (3 - i) * 8) & 0xff);
    }

    cb.append('/');
    cb.append(_maskIndex);

    return cb.toString();
  }
}
