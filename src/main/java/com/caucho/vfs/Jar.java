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

package com.caucho.vfs;

import com.caucho.util.CacheListener;
import com.caucho.util.Log;
import com.caucho.util.LruCache;
import com.caucho.util.L10N;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.security.cert.Certificate;
import java.util.*;
import java.util.jar.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Jar is a cache around a jar file to avoid scanning through the whole
 * file on each request.
 *
 * <p>When the Jar is created, it scans the file and builds a directory
 * of the Jar entries.
 */
public class Jar implements CacheListener {
  private static final Logger log = Log.open(Jar.class);
  private static final L10N L = new L10N(Jar.class);
  
  private static LruCache<Path,Jar> _jarCache;

  private Path _backing;
  private boolean _backingIsFile;

  // saved last modified time
  private long _lastModified;
  // saved length
  private long _length;

  // cached zip file to read jar entries
  private SoftReference<JarFile> _jarFileRef;
  // last time the zip file was modified
  private long _jarLastModified;
  private long _jarLength;
  private Boolean _isSigned;

  // file to be closed
  private SoftReference<JarFile> _closeJarFileRef;

  /**
   * Creates a new Jar.
   *
   * @param path canonical path
   */
  private Jar(Path backing)
  {
    if (backing instanceof JarPath)
      throw new IllegalStateException();
    
    _backing = backing;
    
    _backingIsFile = (_backing.getScheme().equals("file")
                      && _backing.canRead());
  }

  /**
   * Return a Jar for the path.  If the backing already exists, return
   * the old jar.
   */
  static Jar create(Path backing)
  {
    if (_jarCache == null) {
      _jarCache = new LruCache<Path,Jar>(256);
    }
    
    Jar jar = _jarCache.get(backing);
    if (jar == null) {
      jar = new Jar(backing);
      jar = _jarCache.putIfNew(backing, jar);
    }
    
    return jar;
  }

  /**
   * Return a Jar for the path.  If the backing already exists, return
   * the old jar.
   */
  static Jar getJar(Path backing)
  {
    if (_jarCache != null) {
      Jar jar = _jarCache.get(backing);

      return jar;
    }

    return null;
  }

  /**
   * Returns the backing path.
   */
  Path getBacking()
  {
    return _backing;
  }

  private boolean isSigned()
  {
    Boolean isSigned = _isSigned;

    if (isSigned != null)
      return isSigned;

    try {
      Manifest manifest = getManifest();

      if (manifest == null) {
        _isSigned = Boolean.FALSE;
        return false;
      }

      Map<String,Attributes> entries = manifest.getEntries();

      if (entries == null) {
        _isSigned = Boolean.FALSE;
        return false;
      }
      
      for (Attributes attr : entries.values()) {
        for (Object key : attr.keySet()) {
          String keyString = String.valueOf(key);

          if (keyString.contains("Digest")) {
            _isSigned = Boolean.TRUE;

            return true;
          }
        }
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    _isSigned = Boolean.FALSE;
    
    return false;
  }

  /**
   * Returns true if the entry is a file in the jar.
   *
   * @param path the path name inside the jar.
   */
  public Manifest getManifest()
    throws IOException
  {
    Manifest manifest;

    synchronized (this) {
      JarFile jarFile = getJarFile();

      if (jarFile == null)
	manifest = null;
      else
	manifest = jarFile.getManifest();
    }

    closeJarFile();

    return manifest;
  }

  /**
   * Returns any certificates.
   */
  public Certificate []getCertificates(String path)
  {
    if (! isSigned())
      return null;
    
    if (path.length() > 0 && path.charAt(0) == '/')
      path = path.substring(1);

    try {
      if (! _backing.canRead())
	return null;
      
      JarFile jarFile = new JarFile(_backing.getNativePath());
      JarEntry entry;
      InputStream is = null;

      try {
	entry = jarFile.getJarEntry(path);

	if (entry != null) {
	  is = jarFile.getInputStream(entry);

	  while (is.skip(65536) > 0) {
	  }

	  is.close();

	  return entry.getCertificates();
	}
      } finally {
	jarFile.close();
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }

    return null;
  }

  /**
   * Returns true if the entry exists in the jar.
   *
   * @param path the path name inside the jar.
   */
  public boolean exists(String path)
  {
    // server/249f, server/249g
    // XXX: facelets vs issue of meta-inf (i.e. lower case)

    boolean result = false;
    
    synchronized (this) {
      try {
	ZipEntry entry = getJarEntry(path);

	result = entry != null;
      } catch (IOException e) {
	log.log(Level.FINE, e.toString(), e);
      }
    }

    closeJarFile();

    return result;
  }

  /**
   * Returns true if the entry is a directory in the jar.
   *
   * @param path the path name inside the jar.
   */
  public boolean isDirectory(String path)
  {
    boolean result = false;
    
    synchronized (this) {
      try {
	ZipEntry entry = getJarEntry(path);

	result = entry != null && entry.isDirectory();
      } catch (IOException e) {
	log.log(Level.FINE, e.toString(), e);
      }
    }

    closeJarFile();

    return result;
  }

  /**
   * Returns true if the entry is a file in the jar.
   *
   * @param path the path name inside the jar.
   */
  public boolean isFile(String path)
  {
    boolean result = false;
    
    synchronized (this) {
      try {
	ZipEntry entry = getJarEntry(path);

	result = entry != null && ! entry.isDirectory();
      } catch (IOException e) {
	log.log(Level.FINE, e.toString(), e);
      }
    }

    closeJarFile();

    return result;
  }

  /**
   * Returns the length of the entry in the jar file.
   *
   * @param path full path to the jar entry
   * @return the length of the entry
   */
  public long getLastModified(String path)
  {
    long result = -1;
    synchronized (this) {
      try {
	ZipEntry entry = getJarEntry(path);

	result = entry != null ? entry.getTime() : -1;
      } catch (IOException e) {
	log.log(Level.FINE, e.toString(), e);
      }
    }

    closeJarFile();

    return result;
  }

  /**
   * Returns the length of the entry in the jar file.
   *
   * @param path full path to the jar entry
   * @return the length of the entry
   */
  public long getLength(String path)
  {
    long result = -1;
    
    synchronized (this) {
      try {
	ZipEntry entry = getJarEntry(path);

	result = entry != null ? entry.getSize() : -1;
      } catch (IOException e) {
	log.log(Level.FINE, e.toString(), e);
      }
    }

    closeJarFile();

    return result;
  }

  /**
   * Readable if the jar is readable and the path refers to a file.
   */
  public boolean canRead(String path)
  {
    boolean result = false;
    
    synchronized (this) {
      try {
	ZipEntry entry = getJarEntry(path);

	result = entry != null && ! entry.isDirectory();
      } catch (IOException e) {
	log.log(Level.FINE, e.toString(), e);
	
	return false;
      }
    }

    closeJarFile();

    return result;
  }

  /**
   * Can't write to jars.
   */
  public boolean canWrite(String path)
  {
    return false;
  }

  /**
   * Lists all the files in this directory.
   */
  public String []list(String path) throws IOException
  {
    // XXX:
    
    return new String[0];
  }

  /**
   * Opens a stream to an entry in the jar.
   *
   * @param path relative path into the jar.
   */
  public StreamImpl openReadImpl(Path path) throws IOException
  {
    String pathName = path.getPath();

    if (pathName.length() > 0 && pathName.charAt(0) == '/')
      pathName = pathName.substring(1);

    ZipFile zipFile = new ZipFile(_backing.getNativePath());
    ZipEntry entry;
    InputStream is = null;

    try {
      entry = zipFile.getEntry(pathName);
      if (entry != null) {
	is = zipFile.getInputStream(entry);

	return new ZipStreamImpl(zipFile, is, null, path);
      }
      else {
	throw new FileNotFoundException(path.toString());
      }
    } finally {
      if (is == null) {
	zipFile.close();
      }
    }
  }

  public String toString()
  {
    return _backing.toString();
  }

  /**
   * Clears any cached JarFile.
   */
  public void clearCache()
  {
    JarFile jarFile = null;

    synchronized (this) {
      SoftReference<JarFile> jarFileRef = _jarFileRef;
      _jarFileRef = null;
      
      if (jarFileRef != null)
	jarFile = jarFileRef.get();
    }
      
    try {
      if (jarFile != null)
	jarFile.close();
    } catch (Exception e) {
    }
  }

  private ZipEntry getJarEntry(String path)
    throws IOException
  {
    if (path.startsWith("/"))
      path = path.substring(1);

    JarFile jarFile = getJarFile();

    if (jarFile != null)
      return jarFile.getJarEntry(path);
    else
      return null;
  }

  /**
   * Returns the Java ZipFile for this Jar.  Accessing the entries with
   * the ZipFile is faster than scanning through them.
   *
   * getJarFile is not thread safe.
   */
  private JarFile getJarFile()
    throws IOException
  {
    JarFile jarFile = null;

    isCacheValid();
    
    SoftReference<JarFile> jarFileRef = _jarFileRef;

    if (jarFileRef != null) {
      jarFile = jarFileRef.get();
	
      if (jarFile != null)
        return jarFile;
    }

    SoftReference<JarFile> oldJarRef = _jarFileRef;
    _jarFileRef = null;

    JarFile oldFile = null;
    if (oldJarRef == null) {
    }
    else if (_closeJarFileRef == null)
      _closeJarFileRef = oldJarRef;
    else
      oldFile = oldJarRef.get();

    if (oldFile != null) {
      try {
	oldFile.close();
      } catch (Throwable e) {
	e.printStackTrace();
      }
    }

    if (_backingIsFile) {
      try {
        jarFile = new JarFile(_backing.getNativePath());
      }
      catch (IOException ex) {
        if (log.isLoggable(Level.FINE))
          log.log(Level.FINE, L.l("Error opening jar file '{0}'", _backing.getNativePath()));

        throw ex;
      }

      _jarFileRef = new SoftReference<JarFile>(jarFile);
      _jarLastModified = getLastModifiedImpl();
    }

    return jarFile;
  }
  
  /**
   * Returns the last modified time for the path.
   *
   * @param path path into the jar.
   *
   * @return the last modified time of the jar in milliseconds.
   */
  private long getLastModifiedImpl()
  {
    isCacheValid();
    
    return _lastModified;
  }
  
  /**
   * Returns the last modified time for the path.
   *
   * @param path path into the jar.
   *
   * @return the last modified time of the jar in milliseconds.
   */
  private boolean isCacheValid()
  {
    long oldLastModified = _lastModified;
    long oldLength = _length;
    
    _lastModified = _backing.getLastModified();
    _length = _backing.getLength();

    if (_lastModified == oldLastModified && _length == oldLength)
      return true;
    else {
      // If the file has changed, close the old file
      SoftReference<JarFile> oldFileRef = _jarFileRef;
	
      _jarFileRef = null;
      _jarLastModified = 0;
      _isSigned = null;

      JarFile oldCloseFile = null;
      if (_closeJarFileRef != null)
	oldCloseFile = _closeJarFileRef.get();

      _closeJarFileRef = oldFileRef;

      if (oldCloseFile != null) {
	try {
	  oldCloseFile.close();
	} catch (Throwable e) {
	}
      }

      return false;
    }
  }

  /**
   * Closes any old jar waiting for close.
   */
  private void closeJarFile()
  {
    JarFile jarFile = null;
    
    synchronized (this) {
      if (_closeJarFileRef != null)
	jarFile = _closeJarFileRef.get();
      _closeJarFileRef = null;
    }
    
    if (jarFile != null) {
      try {
	jarFile.close();
      } catch (IOException e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  public void close()
  {
    removeEvent();
  }
  
  public void removeEvent()
  {
    JarFile jarFile = null;
    
    synchronized (this) {
      if (_jarFileRef != null)
	jarFile = _jarFileRef.get();

      _jarFileRef = null;
    }

    try {
      if (jarFile != null)
	jarFile.close();
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
    
    closeJarFile();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (o == null || ! getClass().equals(o.getClass()))
      return false;

    Jar jar = (Jar) o;

    return _backing.equals(jar._backing);
  }

  /**
   * Clears all the cached files in the jar.  Needed to avoid some
   * windows NT issues.
   */
  public static void clearJarCache()
  {
    LruCache<Path,Jar> jarCache = _jarCache;
    
    if (jarCache == null)
      return;

    ArrayList<Jar> jars = new ArrayList<Jar>();
    
    synchronized (jarCache) {
      Iterator<Jar> iter = jarCache.values();
      
      while (iter.hasNext())
	jars.add(iter.next());
    }

    for (int i = 0; i < jars.size(); i++) {
      Jar jar = jars.get(i);
        
      if (jar != null)
	jar.clearCache();
    }
  }

  /**
   * StreamImpl to read from a ZIP file.
   */
  static class ZipStreamImpl extends StreamImpl {
    private ZipFile _zipFile;
    private InputStream _zis;
    private InputStream _is;

    /**
     * Create the new stream  impl.
     *
     * @param zis the underlying zip stream.
     * @param is the backing stream.
     * @param path the path to the jar entry.
     */
    ZipStreamImpl(ZipFile file, InputStream zis, InputStream is, Path path)
    {
      _zipFile = file;
      _zis = zis;
      _is = is;
      
      setPath(path);
    }

    /**
     * Returns true since this is a read stream.
     */
    public boolean canRead() { return true; }
 
    public int getAvailable() throws IOException
    {
      if (_zis == null)
        return -1;
      else
        return _zis.available();
    }
 
    public int read(byte []buf, int off, int len) throws IOException
    {
      int readLen = _zis.read(buf, off, len);
 
      return readLen;
    }
 
    public void close() throws IOException
    {
      ZipFile zipFile = _zipFile;
      _zipFile = null;
      
      InputStream zis = _zis;
      _zis = null;
      
      InputStream is = _is;
      _is = null;
      
      try {
	if (zis != null)
	  zis.close();
      } catch (Throwable e) {
      }

      try {
	if (zipFile != null)
	  zipFile.close();
      } catch (Throwable e) {
      }

      if (is != null)
        is.close();
    }

    protected void finalize()
      throws IOException
    {
      close();
    }
  }
}
