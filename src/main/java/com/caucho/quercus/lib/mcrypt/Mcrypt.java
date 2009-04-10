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

package com.caucho.quercus.lib.mcrypt;

import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.logging.Logger;

/**
 * Encryption class
 */
public class Mcrypt {
  private static final L10N L = new L10N(Mcrypt.class);

  private static final Logger log =
    Logger.getLogger(Mcrypt.class.getName());

  private final String _algorithm;
  private final String _mode;

  private final Cipher _cipher;

  private Key _key;
  private IvParameterSpec _iv;

  Mcrypt(Env env, String algorithm, String mode)
    throws Exception
  {
    _algorithm = algorithm;
    _mode = mode.toUpperCase();

    String transformation = getTransformation(algorithm, mode);

    if (transformation == null)
      throw new QuercusRuntimeException(L.l("'{0}' is an unknown algorithm",
					    algorithm));


    _cipher = Cipher.getInstance(transformation);
  }

  /**
   * Uninitialize data.
   */
  public boolean deinit()
  {
    return false;
  }

  /**
   * Encrypts data.
   */
  public byte []decrypt(byte []data)
  {
    try {
      _cipher.init(Cipher.DECRYPT_MODE, _key, _iv);

      int blockSize = _cipher.getBlockSize();

      return _cipher.doFinal(data);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Encrypts data.
   */
  public byte []encrypt(byte []data)
  {
    try {
      _cipher.init(Cipher.ENCRYPT_MODE, _key, _iv);

      if (isPadded())
        data = pad(data);

      return _cipher.doFinal(data);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the block size.
   */
  public int get_block_size()
  {
    return _cipher.getBlockSize();
  }

  /**
   * Returns the initialization vector size.
   */
  public String get_algorithms_name()
  {
    return _algorithm;
  }

  /**
   * Returns the initialization vector size.
   */
  public int get_iv_size()
  {
    if (_mode.equals("OFB"))
      return _cipher.getBlockSize();
    else if (_mode.equals("CFB"))
      return _cipher.getBlockSize();
    else if (_mode.equals("CBC"))
      return _cipher.getBlockSize();
    else if (_mode.equals("ECB"))
      return _cipher.getBlockSize();
    else
      return 0;
  }

  /**
   * Returns the supported key sizes
   */
  public Value get_supported_key_sizes()
  {
    ArrayValue value = new ArrayValueImpl();

    if (McryptModule.MCRYPT_RIJNDAEL_128.equals(_algorithm)) {
      value.put(128 / 8);
    }
    else if (McryptModule.MCRYPT_RIJNDAEL_192.equals(_algorithm)) {
      value.put(128 / 8);
      value.put(192 / 8);
    }
    else if (McryptModule.MCRYPT_RIJNDAEL_256.equals(_algorithm)) {
      value.put(128 / 8);
      value.put(192 / 8);
      value.put(256 / 8);
    }
    else if (McryptModule.MCRYPT_3DES.equals(_algorithm)) {
      value.put(24);
    }
    else if (McryptModule.MCRYPT_DES.equals(_algorithm)) {
      value.put(8);
    }

    return value;
  }

  /**
   * Returns the key size in bytes.
   */
  public int get_key_size()
  {
    if (McryptModule.MCRYPT_RIJNDAEL_128.equals(_algorithm))
      return 128 / 8;
    else if (McryptModule.MCRYPT_RIJNDAEL_192.equals(_algorithm))
      return 192 / 8;
    else if (McryptModule.MCRYPT_RIJNDAEL_256.equals(_algorithm))
      return 256 / 8;
    else if (McryptModule.MCRYPT_3DES.equals(_algorithm))
      return 24;
    else if (McryptModule.MCRYPT_BLOWFISH.equals(_algorithm))
      return 56;
    else
      return get_block_size();
  }

  private boolean isPadKey()
  {
    if (McryptModule.MCRYPT_BLOWFISH.equals(_algorithm))
      return false;
    else
      return true;
  }

  /**
   * Returns the initialization vector size.
   */
  public String get_modes_name()
  {
    return _mode;
  }

  /**
   * Initialize the crypt.
   */
  public int init(byte []keyBytesArg, byte []iv)
  {
    byte []keyBytes;

    if (isPadKey()) {
      keyBytes = new byte[get_key_size()];
    
      int length = Math.min(keyBytesArg.length, keyBytes.length);
    
      System.arraycopy(keyBytesArg, 0, keyBytes, 0, length);
    }
    else
      keyBytes = keyBytesArg;

    _key = new SecretKeySpec(keyBytes, getAlgorithm(_algorithm));

    if (iv == null)
      _iv = null;
    else if (_mode.equals("CBC") || _mode.equals("CFB") || _mode.equals("OFB"))
      _iv = new IvParameterSpec(iv);
    else
      _iv = null;

    return 0;
  }

  /**
   * Returns true for block algorithms
   */
  public boolean is_block_algorithm()
  {
    if (_algorithm.equals(McryptModule.MCRYPT_BLOWFISH))
      return false;
    else
      return true;
  }

  /**
   * Returns true for block algorithms
   */
  public boolean is_block_algorithm_mode()
  {
    return _mode.equals("CBC") || _mode.equals("CFB") || _mode.equals("OFB");
  }

  /**
   * Returns true for block algorithms
   */
  public boolean is_block_mode()
  {
    return _mode.equals("CBC") || _mode.equals("ECB");
  }

  private byte []pad(byte []data)
  {
    int blockSize = get_block_size();

    int len = data.length;
    int offset = len % blockSize;

    if (offset == 0)
      return data;
    else {
      byte []pad = new byte[len + blockSize - offset];

      System.arraycopy(data, 0, pad, 0, data.length);

      return pad;
    }
  }

  private boolean isPadded()
  {
    return (McryptModule.MCRYPT_DES.equals(_algorithm)
            || McryptModule.MCRYPT_3DES.equals(_algorithm)
            || McryptModule.MCRYPT_RIJNDAEL_128.equals(_algorithm)
            || McryptModule.MCRYPT_RIJNDAEL_192.equals(_algorithm)
            || McryptModule.MCRYPT_RIJNDAEL_256.equals(_algorithm));
  }

  /**
   * Closes the mcrypt.
   */
  public void close()
  {
  }

  private static String getTransformation(String algorithm, String mode)
    throws Exception
  {
    mode = mode.toUpperCase();
    if (McryptModule.MCRYPT_RIJNDAEL_128.equals(algorithm))
      return "AES/" + mode + "/NoPadding";
    else if (McryptModule.MCRYPT_RIJNDAEL_192.equals(algorithm))
      return "AES/" + mode + "192/NoPadding";
    else if (McryptModule.MCRYPT_RIJNDAEL_256.equals(algorithm))
      return "AES/" + mode + "256/NoPadding";
    else if (McryptModule.MCRYPT_DES.equals(algorithm))
      return "DES/" + mode + "/NoPadding";
    else if (McryptModule.MCRYPT_3DES.equals(algorithm))
      return "DESede/" + mode + "/NoPadding";
    else if (McryptModule.MCRYPT_BLOWFISH.equals(algorithm)) {
      // php/1q0t, #2561
      return "Blowfish/" + mode + "/PKCS5Padding";
    }
    else if (McryptModule.MCRYPT_ARCFOUR.equals(algorithm)
	     || McryptModule.MCRYPT_RC4.equals(algorithm))
      return "ARCFOUR/" + mode + "/NoPadding";
    else
      return algorithm + '/' + mode + "/NoPadding";
  }

  private static String getAlgorithm(String algorithm)
  {
    if (McryptModule.MCRYPT_RIJNDAEL_128.equals(algorithm) ||
	McryptModule.MCRYPT_RIJNDAEL_192.equals(algorithm) ||
	McryptModule.MCRYPT_RIJNDAEL_256.equals(algorithm))
      return "AES";
    else if (McryptModule.MCRYPT_3DES.equals(algorithm))
      return "DESede";
    else
      return algorithm;
  }

  public String toString()
  {
    return "Mcrypt[" + _algorithm + ", " + _mode + "]";
  }
}
