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

package com.caucho.quercus.lib.file;

import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempCharBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Information and actions for about sockets
 */
public class SocketModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(SocketModule.class);
  private static final Logger log
    = Logger.getLogger(SocketModule.class.getName());

  private static final int AF_UNIX = 1;
  private static final int AF_INET = 2;
  private static final int AF_INET6 = 10;
  private static final int SOCK_STREAM = 1;
  private static final int SOCK_DGRAM = 2;
  private static final int SOCK_RAW = 3;
  private static final int SOCK_SEQPACKET = 5;
  private static final int SOCK_RDM = 4;
  private static final int MSG_OOB = 1;
  private static final int MSG_WAITALL = 256;
  private static final int MSG_PEEK = 2;
  private static final int MSG_DONTROUTE = 4;
  private static final int SO_DEBUG = 1;
  private static final int SO_REUSEADDR = 2;
  private static final int SO_KEEPALIVE = 9;
  private static final int SO_DONTROUTE = 5;
  private static final int SO_LINGER = 13;
  private static final int SO_BROADCAST = 6;
  private static final int SO_OOBINLINE = 10;
  private static final int SO_SNDBUF = 7;
  private static final int SO_RCVBUF = 8;
  private static final int SO_SNDLOWAT = 19;
  private static final int SO_RCVLOWAT = 18;
  private static final int SO_SNDTIMEO = 21;
  private static final int SO_RCVTIMEO = 20;
  private static final int SO_TYPE = 3;
  private static final int SO_ERROR = 4;
  private static final int SOL_SOCKET = 1;
  private static final int SOMAXCONN = 128;
  private static final int PHP_NORMAL_READ = 1;
  private static final int PHP_BINARY_READ = 2;
  private static final int SOCKET_EPERM = 1;
  private static final int SOCKET_ENOENT = 2;
  private static final int SOCKET_EINTR = 4;
  private static final int SOCKET_EIO = 5;
  private static final int SOCKET_ENXIO = 6;
  private static final int SOCKET_E2BIG = 7;
  private static final int SOCKET_EBADF = 9;
  private static final int SOCKET_EAGAIN = 11;
  private static final int SOCKET_ENOMEM = 12;
  private static final int SOCKET_EACCES = 13;
  private static final int SOCKET_EFAULT = 14;
  private static final int SOCKET_ENOTBLK = 15;
  private static final int SOCKET_EBUSY = 16;
  private static final int SOCKET_EEXIST = 17;
  private static final int SOCKET_EXDEV = 18;
  private static final int SOCKET_ENODEV = 19;
  private static final int SOCKET_ENOTDIR = 20;
  private static final int SOCKET_EISDIR = 21;
  private static final int SOCKET_EINVAL = 22;
  private static final int SOCKET_ENFILE = 23;
  private static final int SOCKET_EMFILE = 24;
  private static final int SOCKET_ENOTTY = 25;
  private static final int SOCKET_ENOSPC = 28;
  private static final int SOCKET_ESPIPE = 29;
  private static final int SOCKET_EROFS = 30;
  private static final int SOCKET_EMLINK = 31;
  private static final int SOCKET_EPIPE = 32;
  private static final int SOCKET_ENAMETOOLONG = 36;
  private static final int SOCKET_ENOLCK = 37;
  private static final int SOCKET_ENOSYS = 38;
  private static final int SOCKET_ENOTEMPTY = 39;
  private static final int SOCKET_ELOOP = 40;
  private static final int SOCKET_EWOULDBLOCK = 11;
  private static final int SOCKET_ENOMSG = 42;
  private static final int SOCKET_EIDRM = 43;
  private static final int SOCKET_ECHRNG = 44;
  private static final int SOCKET_EL2NSYNC = 45;
  private static final int SOCKET_EL3HLT = 46;
  private static final int SOCKET_EL3RST = 47;
  private static final int SOCKET_ELNRNG = 48;
  private static final int SOCKET_EUNATCH = 49;
  private static final int SOCKET_ENOCSI = 50;
  private static final int SOCKET_EL2HLT = 51;
  private static final int SOCKET_EBADE = 52;
  private static final int SOCKET_EBADR = 53;
  private static final int SOCKET_EXFULL = 54;
  private static final int SOCKET_ENOANO = 55;
  private static final int SOCKET_EBADRQC = 56;
  private static final int SOCKET_EBADSLT = 57;
  private static final int SOCKET_ENOSTR = 60;
  private static final int SOCKET_ENODATA = 61;
  private static final int SOCKET_ETIME = 62;
  private static final int SOCKET_ENOSR = 63;
  private static final int SOCKET_ENONET = 64;
  private static final int SOCKET_EREMOTE = 66;
  private static final int SOCKET_ENOLINK = 67;
  private static final int SOCKET_EADV = 68;
  private static final int SOCKET_ESRMNT = 69;
  private static final int SOCKET_ECOMM = 70;
  private static final int SOCKET_EPROTO = 71;
  private static final int SOCKET_EMULTIHOP = 72;
  private static final int SOCKET_EBADMSG = 74;
  private static final int SOCKET_ENOTUNIQ = 76;
  private static final int SOCKET_EBADFD = 77;
  private static final int SOCKET_EREMCHG = 78;
  private static final int SOCKET_ERESTART = 85;
  private static final int SOCKET_ESTRPIPE = 86;
  private static final int SOCKET_EUSERS = 87;
  private static final int SOCKET_ENOTSOCK = 88;
  private static final int SOCKET_EDESTADDRREQ = 89;
  private static final int SOCKET_EMSGSIZE = 90;
  private static final int SOCKET_EPROTOTYPE = 91;
  private static final int SOCKET_ENOPROTOOPT = 92;
  private static final int SOCKET_EPROTONOSUPPORT = 93;
  private static final int SOCKET_ESOCKTNOSUPPORT = 94;
  private static final int SOCKET_EOPNOTSUPP = 95;
  private static final int SOCKET_EPFNOSUPPORT = 96;
  private static final int SOCKET_EAFNOSUPPORT = 97;
  private static final int SOCKET_EADDRINUSE = 98;
  private static final int SOCKET_EADDRNOTAVAIL = 99;
  private static final int SOCKET_ENETDOWN = 100;
  private static final int SOCKET_ENETUNREACH = 101;
  private static final int SOCKET_ENETRESET = 102;
  private static final int SOCKET_ECONNABORTED = 103;
  private static final int SOCKET_ECONNRESET = 104;
  private static final int SOCKET_ENOBUFS = 105;
  private static final int SOCKET_EISCONN = 106;
  private static final int SOCKET_ENOTCONN = 107;
  private static final int SOCKET_ESHUTDOWN = 108;
  private static final int SOCKET_ETOOMANYREFS = 109;
  private static final int SOCKET_ETIMEDOUT = 110;
  private static final int SOCKET_ECONNREFUSED = 111;
  private static final int SOCKET_EHOSTDOWN = 112;
  private static final int SOCKET_EHOSTUNREACH = 113;
  private static final int SOCKET_EALREADY = 114;
  private static final int SOCKET_EINPROGRESS = 115;
  private static final int SOCKET_EISNAM = 120;
  private static final int SOCKET_EREMOTEIO = 121;
  private static final int SOCKET_EDQUOT = 122;
  private static final int SOCKET_ENOMEDIUM = 123;
  private static final int SOCKET_EMEDIUMTYPE = 124;
  private static final int SOL_TCP = 6;
  private static final int SOL_UDP = 17;

  private static final HashMap<String,Value> _constMap
    = new HashMap<String,Value>();

  /**
   * Returns the constants defined by this module.
   */
  public Map<String,Value> getConstMap()
  {
    return _constMap;
  }
 
  @ReturnNullAsFalse
  public static SocketInputOutput socket_create(Env env,
                                                int domain, 
                                                int type, 
                                                int protocol)
  {
    try {
      SocketInputOutput.Domain socketDomain = SocketInputOutput.Domain.AF_INET;

      switch (domain) {
        case AF_INET:
          socketDomain = SocketInputOutput.Domain.AF_INET;
          break;
        case AF_INET6:
          socketDomain = SocketInputOutput.Domain.AF_INET6;
          break;
        case AF_UNIX:
          env.warning(L.l("Unix sockets not supported"));
          return null;
        default:
          env.warning(L.l("Unknown domain: {0}", domain));
          return null;
      }

      switch (type) {
        case SOCK_STREAM:
          return new TcpInputOutput(env, new Socket(), socketDomain);
        case SOCK_DGRAM:
          return new UdpInputOutput(env, new DatagramSocket(), socketDomain);
        default:
          env.warning(L.l("socket stream not socked"));
          return null;
      }

    } catch (Exception e) {
      env.warning(e);
      return null;
    }
  }

  public static boolean socket_bind(Env env,
                                    @NotNull SocketInputOutput socket,
                                    StringValue address, 
                                    @Optional("0") int port)
  {
    try {
      InetAddress []addresses = InetAddress.getAllByName(address.toString());

      if (addresses == null || addresses.length < 1) {
        //XXX: socket.setError();
        return false;
      }

      InetSocketAddress socketAddress = 
        new InetSocketAddress(addresses[0], port);

      socket.bind(socketAddress);

      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public static void socket_close(Env env, @NotNull SocketInputOutput socket)
  {
    socket.close();
  }

  public static boolean socket_connect(Env env,
                                       @NotNull SocketInputOutput socket,
                                       StringValue address, @Optional int port)
  {
    try {
      InetAddress []addresses = InetAddress.getAllByName(address.toString());

      if (addresses == null || addresses.length < 1) {
        //XXX: socket.setError();
        return false;
      }

      InetSocketAddress socketAddress = 
        new InetSocketAddress(addresses[0], port);

      socket.connect(socketAddress);

      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public static Value socket_get_status(Env env, BinaryStream stream)
  {
    return StreamModule.stream_get_meta_data(env, stream);
  }

  public static Value socket_read(Env env,
                                  @NotNull SocketInputOutput socket,
                                  int length, @Optional int type)
  {
    TempBuffer tempBuffer = null;
    TempCharBuffer tempCharBuffer = null;

    try {
      if (type == PHP_NORMAL_READ) {
        return socket.readLine(length);
      } else {
        tempBuffer = TempBuffer.allocate();

        if (length > tempBuffer.getCapacity())
          length = tempBuffer.getCapacity();

        byte []buffer = tempBuffer.getBuffer();

        length = socket.read(buffer, 0, length);

        if (length > 0) {
          StringValue sb = env.createBinaryBuilder(buffer, 0, length);
          return sb;
        } else
          return BooleanValue.FALSE;
      }
    } catch (IOException e) {
      env.warning(e);

      return BooleanValue.FALSE;
    } finally {
      if (tempCharBuffer != null)
        TempCharBuffer.free(tempCharBuffer);

      if (tempBuffer != null)
        TempBuffer.free(tempBuffer);
    }
  }

  public static boolean socket_set_timeout(Env env,
                                           @NotNull Value stream,
                                           int seconds,
                                           @Optional("-1") int milliseconds)
  {
    return StreamModule.stream_set_timeout(env, stream, seconds, milliseconds);
  }

  public static Value socket_write(Env env,
                                   @NotNull SocketInputOutput socket,
                                   @NotNull InputStream is, 
                                   @Optional("-1") int length)
  {
    if (is == null)
      return BooleanValue.FALSE;

    // php/4800
    if (length < 0)
      length = Integer.MAX_VALUE;

    try {
      int result = socket.write(is, length);

      if (result < 0)
        return BooleanValue.FALSE;
      else
        return LongValue.create(result);
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
      
      return BooleanValue.FALSE;
    }
  }

  /**
   * Closes a socket.
   *
   * @param how 0 = read, 1 = write, 2 = both
   */
  public boolean socket_shutdown(Env env,
                                 @NotNull SocketInputOutput file,
                                 int how)
  {
    if (file == null)
      return false;

    switch (how) {
    case 0:
      file.closeRead();
      return true;
      
    case 1:
      file.closeWrite();
      return true;
      
    case 2:
      file.close();
      return true;
      
    default:
      return false;
    }
  }

  static {
    _constMap.put("AF_UNIX", LongValue.create(AF_UNIX));
    _constMap.put("AF_INET", LongValue.create(AF_INET));
    _constMap.put("AF_INET6", LongValue.create(AF_INET6));

    _constMap.put("SOCK_STREAM", LongValue.create(SOCK_STREAM));
    _constMap.put("SOCK_DGRAM", LongValue.create(SOCK_DGRAM));
    _constMap.put("SOCK_RAW", LongValue.create(SOCK_RAW));
    _constMap.put("SOCK_SEQPACKET", LongValue.create(SOCK_SEQPACKET));
    _constMap.put("SOCK_RDM", LongValue.create(SOCK_RDM));

    _constMap.put("MSG_OOB", LongValue.create(MSG_OOB));
    _constMap.put("MSG_WAITALL", LongValue.create(MSG_WAITALL));
    _constMap.put("MSG_PEEK", LongValue.create(MSG_PEEK));
    _constMap.put("MSG_DONTROUTE", LongValue.create(MSG_DONTROUTE));

    _constMap.put("SO_DEBUG", LongValue.create(SO_DEBUG));
    _constMap.put("SO_REUSEADDR", LongValue.create(SO_REUSEADDR));
    _constMap.put("SO_KEEPALIVE", LongValue.create(SO_KEEPALIVE));
    _constMap.put("SO_DONTROUTE", LongValue.create(SO_DONTROUTE));
    _constMap.put("SO_LINGER", LongValue.create(SO_LINGER));
    _constMap.put("SO_BROADCAST", LongValue.create(SO_BROADCAST));
    _constMap.put("SO_OOBINLINE", LongValue.create(SO_OOBINLINE));
    _constMap.put("SO_SNDBUF", LongValue.create(SO_SNDBUF));
    _constMap.put("SO_RCVBUF", LongValue.create(SO_RCVBUF));
    _constMap.put("SO_SNDLOWAT", LongValue.create(SO_SNDLOWAT));
    _constMap.put("SO_RCVLOWAT", LongValue.create(SO_RCVLOWAT));
    _constMap.put("SO_SNDTIMEO", LongValue.create(SO_SNDTIMEO));
    _constMap.put("SO_RCVTIMEO", LongValue.create(SO_RCVTIMEO));
    _constMap.put("SO_TYPE", LongValue.create(SO_TYPE));
    _constMap.put("SO_ERROR", LongValue.create(SO_ERROR));

    _constMap.put("SOL_SOCKET", LongValue.create(SOL_SOCKET));

    _constMap.put("SOMAXCONN", LongValue.create(SOMAXCONN));

    _constMap.put("PHP_NORMAL_READ", LongValue.create(PHP_NORMAL_READ));
    _constMap.put("PHP_BINARY_READ", LongValue.create(PHP_BINARY_READ));
    
    _constMap.put("SOCKET_EPERM", LongValue.create(SOCKET_EPERM));
    _constMap.put("SOCKET_ENOENT", LongValue.create(SOCKET_ENOENT));
    _constMap.put("SOCKET_EINTR", LongValue.create(SOCKET_EINTR));
    _constMap.put("SOCKET_EIO", LongValue.create(SOCKET_EIO));
    _constMap.put("SOCKET_ENXIO", LongValue.create(SOCKET_ENXIO));
    _constMap.put("SOCKET_E2BIG", LongValue.create(SOCKET_E2BIG));
    _constMap.put("SOCKET_EBADF", LongValue.create(SOCKET_EBADF));
    _constMap.put("SOCKET_EAGAIN", LongValue.create(SOCKET_EAGAIN));
    _constMap.put("SOCKET_ENOMEM", LongValue.create(SOCKET_ENOMEM));
    _constMap.put("SOCKET_EACCES", LongValue.create(SOCKET_EACCES));
    _constMap.put("SOCKET_EFAULT", LongValue.create(SOCKET_EFAULT));
    _constMap.put("SOCKET_ENOTBLK", LongValue.create(SOCKET_ENOTBLK));
    _constMap.put("SOCKET_EBUSY", LongValue.create(SOCKET_EBUSY));
    _constMap.put("SOCKET_EEXIST", LongValue.create(SOCKET_EEXIST));
    _constMap.put("SOCKET_EXDEV", LongValue.create(SOCKET_EXDEV));
    _constMap.put("SOCKET_ENODEV", LongValue.create(SOCKET_ENODEV));
    _constMap.put("SOCKET_ENOTDIR", LongValue.create(SOCKET_ENOTDIR));
    _constMap.put("SOCKET_EISDIR", LongValue.create(SOCKET_EISDIR));
    _constMap.put("SOCKET_EINVAL", LongValue.create(SOCKET_EINVAL));
    _constMap.put("SOCKET_ENFILE", LongValue.create(SOCKET_ENFILE));
    _constMap.put("SOCKET_EMFILE", LongValue.create(SOCKET_EMFILE));
    _constMap.put("SOCKET_ENOTTY", LongValue.create(SOCKET_ENOTTY));
    _constMap.put("SOCKET_ENOSPC", LongValue.create(SOCKET_ENOSPC));
    _constMap.put("SOCKET_ESPIPE", LongValue.create(SOCKET_ESPIPE));
    _constMap.put("SOCKET_EROFS", LongValue.create(SOCKET_EROFS));
    _constMap.put("SOCKET_EMLINK", LongValue.create(SOCKET_EMLINK));
    _constMap.put("SOCKET_EPIPE", LongValue.create(SOCKET_EPIPE));
    _constMap.put("SOCKET_ENAMETOOLONG", LongValue.create(SOCKET_ENAMETOOLONG));
    _constMap.put("SOCKET_ENOLCK", LongValue.create(SOCKET_ENOLCK));
    _constMap.put("SOCKET_ENOSYS", LongValue.create(SOCKET_ENOSYS));
    _constMap.put("SOCKET_ENOTEMPTY", LongValue.create(SOCKET_ENOTEMPTY));
    _constMap.put("SOCKET_ELOOP", LongValue.create(SOCKET_ELOOP));
    _constMap.put("SOCKET_EWOULDBLOCK", LongValue.create(SOCKET_EWOULDBLOCK));
    _constMap.put("SOCKET_ENOMSG", LongValue.create(SOCKET_ENOMSG));
    _constMap.put("SOCKET_EIDRM", LongValue.create(SOCKET_EIDRM));
    _constMap.put("SOCKET_ECHRNG", LongValue.create(SOCKET_ECHRNG));
    _constMap.put("SOCKET_EL2NSYNC", LongValue.create(SOCKET_EL2NSYNC));
    _constMap.put("SOCKET_EL3HLT", LongValue.create(SOCKET_EL3HLT));
    _constMap.put("SOCKET_EL3RST", LongValue.create(SOCKET_EL3RST));
    _constMap.put("SOCKET_ELNRNG", LongValue.create(SOCKET_ELNRNG));
    _constMap.put("SOCKET_EUNATCH", LongValue.create(SOCKET_EUNATCH));
    _constMap.put("SOCKET_ENOCSI", LongValue.create(SOCKET_ENOCSI));
    _constMap.put("SOCKET_EL2HLT", LongValue.create(SOCKET_EL2HLT));
    _constMap.put("SOCKET_EBADE", LongValue.create(SOCKET_EBADE));
    _constMap.put("SOCKET_EBADR", LongValue.create(SOCKET_EBADR));
    _constMap.put("SOCKET_EXFULL", LongValue.create(SOCKET_EXFULL));
    _constMap.put("SOCKET_ENOANO", LongValue.create(SOCKET_ENOANO));
    _constMap.put("SOCKET_EBADRQC", LongValue.create(SOCKET_EBADRQC));
    _constMap.put("SOCKET_EBADSLT", LongValue.create(SOCKET_EBADSLT));
    _constMap.put("SOCKET_ENOSTR", LongValue.create(SOCKET_ENOSTR));
    _constMap.put("SOCKET_ENODATA", LongValue.create(SOCKET_ENODATA));
    _constMap.put("SOCKET_ETIME", LongValue.create(SOCKET_ETIME));
    _constMap.put("SOCKET_ENOSR", LongValue.create(SOCKET_ENOSR));
    _constMap.put("SOCKET_ENONET", LongValue.create(SOCKET_ENONET));
    _constMap.put("SOCKET_EREMOTE", LongValue.create(SOCKET_EREMOTE));
    _constMap.put("SOCKET_ENOLINK", LongValue.create(SOCKET_ENOLINK));
    _constMap.put("SOCKET_EADV", LongValue.create(SOCKET_EADV));
    _constMap.put("SOCKET_ESRMNT", LongValue.create(SOCKET_ESRMNT));
    _constMap.put("SOCKET_ECOMM", LongValue.create(SOCKET_ECOMM));
    _constMap.put("SOCKET_EPROTO", LongValue.create(SOCKET_EPROTO));
    _constMap.put("SOCKET_EMULTIHOP", LongValue.create(SOCKET_EMULTIHOP));
    _constMap.put("SOCKET_EBADMSG", LongValue.create(SOCKET_EBADMSG));
    _constMap.put("SOCKET_ENOTUNIQ", LongValue.create(SOCKET_ENOTUNIQ));
    _constMap.put("SOCKET_EBADFD", LongValue.create(SOCKET_EBADFD));
    _constMap.put("SOCKET_EREMCHG", LongValue.create(SOCKET_EREMCHG));
    _constMap.put("SOCKET_ERESTART", LongValue.create(SOCKET_ERESTART));
    _constMap.put("SOCKET_ESTRPIPE", LongValue.create(SOCKET_ESTRPIPE));
    _constMap.put("SOCKET_EUSERS", LongValue.create(SOCKET_EUSERS));
    _constMap.put("SOCKET_ENOTSOCK", LongValue.create(SOCKET_ENOTSOCK));
    _constMap.put("SOCKET_EDESTADDRREQ", LongValue.create(SOCKET_EDESTADDRREQ));
    _constMap.put("SOCKET_EMSGSIZE", LongValue.create(SOCKET_EMSGSIZE));
    _constMap.put("SOCKET_EPROTOTYPE", LongValue.create(SOCKET_EPROTOTYPE));
    _constMap.put("SOCKET_ENOPROTOOPT", LongValue.create(SOCKET_ENOPROTOOPT));
    _constMap.put("SOCKET_EPROTONOSUPPORT", 
        LongValue.create(SOCKET_EPROTONOSUPPORT));
    _constMap.put("SOCKET_ESOCKTNOSUPPORT", 
        LongValue.create(SOCKET_ESOCKTNOSUPPORT));
    _constMap.put("SOCKET_EOPNOTSUPP", LongValue.create(SOCKET_EOPNOTSUPP));
    _constMap.put("SOCKET_EPFNOSUPPORT", LongValue.create(SOCKET_EPFNOSUPPORT));
    _constMap.put("SOCKET_EAFNOSUPPORT", LongValue.create(SOCKET_EAFNOSUPPORT));
    _constMap.put("SOCKET_EADDRINUSE", LongValue.create(SOCKET_EADDRINUSE));
    _constMap.put("SOCKET_EADDRNOTAVAIL", 
        LongValue.create(SOCKET_EADDRNOTAVAIL));
    _constMap.put("SOCKET_ENETDOWN", LongValue.create(SOCKET_ENETDOWN));
    _constMap.put("SOCKET_ENETUNREACH", LongValue.create(SOCKET_ENETUNREACH));
    _constMap.put("SOCKET_ENETRESET", LongValue.create(SOCKET_ENETRESET));
    _constMap.put("SOCKET_ECONNABORTED", LongValue.create(SOCKET_ECONNABORTED));
    _constMap.put("SOCKET_ECONNRESET", LongValue.create(SOCKET_ECONNRESET));
    _constMap.put("SOCKET_ENOBUFS", LongValue.create(SOCKET_ENOBUFS));
    _constMap.put("SOCKET_EISCONN", LongValue.create(SOCKET_EISCONN));
    _constMap.put("SOCKET_ENOTCONN", LongValue.create(SOCKET_ENOTCONN));
    _constMap.put("SOCKET_ESHUTDOWN", LongValue.create(SOCKET_ESHUTDOWN));
    _constMap.put("SOCKET_ETOOMANYREFS", LongValue.create(SOCKET_ETOOMANYREFS));
    _constMap.put("SOCKET_ETIMEDOUT", LongValue.create(SOCKET_ETIMEDOUT));
    _constMap.put("SOCKET_ECONNREFUSED", LongValue.create(SOCKET_ECONNREFUSED));
    _constMap.put("SOCKET_EHOSTDOWN", LongValue.create(SOCKET_EHOSTDOWN));
    _constMap.put("SOCKET_EHOSTUNREACH", LongValue.create(SOCKET_EHOSTUNREACH));
    _constMap.put("SOCKET_EALREADY", LongValue.create(SOCKET_EALREADY));
    _constMap.put("SOCKET_EINPROGRESS", LongValue.create(SOCKET_EINPROGRESS));
    _constMap.put("SOCKET_EISNAM", LongValue.create(SOCKET_EISNAM));
    _constMap.put("SOCKET_EREMOTEIO", LongValue.create(SOCKET_EREMOTEIO));
    _constMap.put("SOCKET_EDQUOT", LongValue.create(SOCKET_EDQUOT));
    _constMap.put("SOCKET_ENOMEDIUM", LongValue.create(SOCKET_ENOMEDIUM));
    _constMap.put("SOCKET_EMEDIUMTYPE", LongValue.create(SOCKET_EMEDIUMTYPE));

    _constMap.put("SOL_TCP", LongValue.create(SOL_TCP));
    _constMap.put("SOL_UDP", LongValue.create(SOL_UDP));
  }
}

