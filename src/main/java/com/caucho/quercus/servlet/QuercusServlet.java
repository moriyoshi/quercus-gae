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

package com.caucho.quercus.servlet;

import com.caucho.config.ConfigException;
import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.module.QuercusModule;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Enumeration;

/**
 * Servlet to call PHP through javax.script.
 */
public class QuercusServlet
  extends HttpServlet
{
  private static final L10N L = new L10N(QuercusServlet.class);
  private static final Logger log
    = Logger.getLogger(QuercusServlet.class.getName());

  private Quercus _quercus;
  private QuercusServletImpl _impl;

  private boolean _isCompileSet;

  public QuercusServlet()
  {
    checkJavaVersion();

    if (_impl == null) {
      try {
	Class cl = Class.forName("com.caucho.quercus.servlet.ProQuercusServlet");
	_impl = (QuercusServletImpl) cl.newInstance();
      } catch (ConfigException e) {
	log.log(Level.FINEST, e.toString(), e);
	log.info("Quercus compiled mode requires Resin personal or professional licenses");
	log.info(e.getMessage());
      } catch (Exception e) {
	log.log(Level.FINEST, e.toString(), e);
      }
    }
    
    if (_impl == null) {
      try {
	Class cl = Class.forName("com.caucho.quercus.servlet.ResinQuercusServlet");
	_impl = (QuercusServletImpl) cl.newInstance();
      } catch (Exception e) {
	log.log(Level.FINEST, e.toString(), e);
      }
    }
    
    if (_impl == null)
      _impl = new QuercusServletImpl();
  }

  /**
   * Make sure Quercus is running on JDK 1.5+.
   */
  private static void checkJavaVersion()
  {
    String version = System.getProperty("java.version");

    if (version.startsWith("1.3.") || version.startsWith("1.4."))
      throw new QuercusRuntimeException(L.l("Quercus requires JDK 1.5 or newer."));

/*
    int major = 0;
    int minor = 0;

    int i = 0;
    int length = version.length();
    while(i < length) {
      char ch = version.charAt(i++);

      if (ch == '.')
        break;

      major = major * 10 + ch - '0';
    }

    while(i < length) {
      char ch = version.charAt(i++);

      if (ch == '.')
        break;

      minor = minor * 10 + ch - '0';
    }

    if (major == 1 && minor < 5)
      throw new QuercusRuntimeException(L.l("Quercus requires JDK 1.5 or newer."));
*/
  }

  /**
   * Set true if quercus should be compiled into Java.
   */
  public void setCompile(String isCompile)
    throws ConfigException
  {
    _isCompileSet = true;

    Quercus quercus = getQuercus();

    if ("true".equals(isCompile) || "".equals(isCompile)) {
      quercus.setCompile(true);
      quercus.setLazyCompile(false);
    } else if ("false".equals(isCompile)) {
      quercus.setCompile(false);
      quercus.setLazyCompile(false);
    } else if ("lazy".equals(isCompile)) {
      quercus.setLazyCompile(true);
    } else
      throw new ConfigException(L.l(
        "'{0}' is an unknown compile value.  Values are 'true', 'false', or 'lazy'.",
        isCompile));
  }
  
  /**
   * Set true interpreted pages should be used for pages that fail to compile.
   */
  public void setCompileFailover(String isCompileFailover)
    throws ConfigException
  {
    Quercus quercus = getQuercus();

    if ("true".equals(isCompileFailover) || "".equals(isCompileFailover)) {
      quercus.setCompileFailover(true);
    } else if ("false".equals(isCompileFailover)) {
      quercus.setCompileFailover(false);
    } else
      throw new ConfigException(L.l(
        "'{0}' is an unknown compile-failover value.  Values are 'true', 'false', or 'lazy'.",
        isCompileFailover));
  }

  /**
   * Sets the frequency of profiling, expressed as a probability.
   */
  public void setProfileProbability(double probability)
    throws ConfigException
  {
    _impl.setProfileProbability(probability);
  }

  /**
   * Set true if the source php is required
   */
  public void setRequireSource(boolean isRequireSource)
  {
    getQuercus().setRequireSource(isRequireSource);
  }

  /**
   * Set the default data source.
   */
  public void setDatabase(DataSource database)
    throws ConfigException
  {
    if (database == null)
      throw new ConfigException(L.l("invalid database"));

    getQuercus().setDatabase(database);
  }

  /**
   * Sets the strict mode.
   */
  public void setStrict(boolean isStrict)
  {
    getQuercus().setStrict(isStrict);
  }
  
  /*
   * Sets the max size of the page cache.
   */
  public void setPageCacheEntries(int entries)
  {
    getQuercus().setPageCacheSize(entries);
  }
  
  /*
   * Sets the max size of the page cache.
   */
  public void setPageCacheSize(int size)
  {
    getQuercus().setPageCacheSize(size);
  }
  
  /*
   * Sets the max size of the regexp cache.
   */
  public void setRegexpCacheSize(int size)
  {
    getQuercus().setRegexpCacheSize(size);
  }
  
  /*
   * Turns connection pooling on or off.
   */
  public void setConnectionPool(boolean isEnable)
  {
    getQuercus().setConnectionPool(isEnable);
  }

  /**
   * Adds a quercus module.
   */
  public void addModule(QuercusModule module)
    throws ConfigException
  {
    getQuercus().addModule(module);
  }

  /**
   * Adds a quercus class.
   */
  public void addClass(PhpClassConfig classConfig)
    throws ConfigException
  {
    getQuercus().addJavaClass(classConfig.getName(), classConfig.getType());
  }

  /**
   * Adds a quercus class.
   */
  public void addImplClass(PhpClassConfig classConfig)
    throws ConfigException
  {
    getQuercus().addImplClass(classConfig.getName(), classConfig.getType());
  }

  /**
   * Adds a quercus.ini configuration
   */
  public PhpIni createPhpIni()
    throws ConfigException
  {
    return new PhpIni(getQuercus());
  }

  /**
   * Adds a $_SERVER configuration
   */
  public ServerEnv createServerEnv()
    throws ConfigException
  {
    return new ServerEnv(getQuercus());
  }

  /**
   * Adds a quercus.ini configuration
   */
  public void setIniFile(Path path)
  {
    getQuercus().setIniFile(path);
  }

  /**
   * Sets the script encoding.
   */
  public void setScriptEncoding(String encoding)
    throws ConfigException
  {
    getQuercus().setScriptEncoding(encoding);
  }

  /**
   * Sets the version of the client php library.
   */
  public void setMysqlVersion(String version)
  {
    getQuercus().setMysqlVersion(version);
  }
  
  /**
   * Sets the php version that Quercus is implementing.
   */
  public void setPhpVersion(String version)
  {
    getQuercus().setPhpVersion(version);
  }

  /**
   * Initializes the servlet.
   */
  public void init(ServletConfig config)
    throws ServletException
  {
    super.init(config);

    Enumeration paramNames = config.getInitParameterNames();

    while (paramNames.hasMoreElements()) {
      String paramName = String.valueOf(paramNames.nextElement());
      String paramValue = config.getInitParameter(paramName);

      setInitParam(paramName, paramValue);
    }

    initImpl(config);
  }

  /**
   * Sets a named init-param to the passed value.
   *
   * @throws ServletException if the init-param is not recognized
   */
  protected void setInitParam(String paramName, String paramValue)
    throws ServletException
  {
    if ("compile".equals(paramName)) {
      setCompile(paramValue);
    }
    else if ("database".equals(paramName)) {
      try {
        Context ic = new InitialContext();
        DataSource ds;

        if (! paramValue.startsWith("java:comp")) {
          try {
            ds = (DataSource) ic.lookup("java:comp/env/" + paramValue);
          }
          catch (Exception e) {
            // for glassfish
            ds = (DataSource) ic.lookup(paramValue);
          }
        }
        else {
          ds = (DataSource) ic.lookup(paramValue);
        }

        if (ds == null)
          throw new ServletException(L.l("database '{0}' is not valid", paramValue));

        getQuercus().setDatabase(ds);
      } catch (Exception e) {
        throw new ServletException(e);
      }
    }
    else if ("ini-file".equals(paramName)) {
      Quercus quercus = getQuercus();

      String realPath = getServletContext().getRealPath(paramValue);

      Path path = quercus.getPwd().lookup(realPath);

      setIniFile(path);
    }
    else if ("mysql-version".equals(paramName)) {
      setMysqlVersion(paramValue);
    }
    else if ("php-version".equals(paramName)) {
      setPhpVersion(paramValue);
    }
    else if ("script-encoding".equals(paramName)) {
      setScriptEncoding(paramValue);
    }
    else if ("strict".equals(paramName)) {
      setStrict("true".equals(paramValue));
    }
    else if ("page-cache-entries".equals(paramName)
             || "page-cache-size".equals(paramName)) {
      setPageCacheSize(Integer.parseInt(paramValue));
    }
    else if ("regexp-cache-size".equals(paramName)) {
      setRegexpCacheSize(Integer.parseInt(paramValue));
    }
    else if ("connection-pool".equals(paramName)) {
      setConnectionPool("true".equals(paramValue));
    }
    else
      throw new ServletException(L.l("'{0}' is not a recognized init-param", paramName));
  }

  private void initImpl(ServletConfig config)
    throws ServletException
  {
    getQuercus();

    if (! _isCompileSet) {
      getQuercus().setLazyCompile(true);
    }

    _impl.init(config);
  }

  /**
   * Service.
   */
  public void service(HttpServletRequest request,
                      HttpServletResponse response)
    throws ServletException, IOException
  {
    _impl.service(request, response);
  }

  /**
   * Returns the Quercus instance.
   */
  private Quercus getQuercus()
  {
    if (_quercus == null)
      _quercus = _impl.getQuercus();

    return _quercus;
  }

  /**
   * Gets the script manager.
   */
  public void destroy()
  {
    _quercus.close();
    _impl.destroy();
  }

  public static class PhpIni {
    private Quercus _quercus;

    PhpIni(Quercus quercus)
    {
      _quercus = quercus;
    }

    /**
     * Sets an arbitrary property.
     */
    public void setProperty(String key, String value)
    {
      _quercus.setIni(key, value);
    }
  }

  public static class ServerEnv {
    private Quercus _quercus;

    ServerEnv(Quercus quercus)
    {
      _quercus = quercus;
    }

    /**
     * Sets an arbitrary property.
     */
    public void setProperty(String key, String value)
    {
      _quercus.setServerEnv(key, value);
    }
  }
}

