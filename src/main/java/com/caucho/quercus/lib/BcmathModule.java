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
 * @author Sam
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.IniDefinitions;
import com.caucho.quercus.module.IniDefinition;
import com.caucho.util.L10N;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * PHP math routines.
 */
public class BcmathModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(BcmathModule.class);

  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final BigDecimal ONE = BigDecimal.ONE;
  private static final BigDecimal TWO = new BigDecimal(2);
  private static final int SQRT_MAX_ITERATIONS = 50;

  private static final IniDefinitions _iniDefinitions = new IniDefinitions();

  public String []getLoadedExtensions()
  {
    return new String[] {  "bcmath" };
  }

  /**
   * Returns the default php.ini values.
   */
  public IniDefinitions getIniDefinitions()
  {
    return _iniDefinitions;
  }

  private static BigDecimal toBigDecimal(Value value)
  {
    try {
      if (value instanceof StringValue)
        return new BigDecimal(value.toString());
      if (value instanceof DoubleValue)
        return new BigDecimal(value.toDouble());
      else if (value instanceof LongValue)
        return new BigDecimal(value.toLong());
      else
        return new BigDecimal(value.toString());
    }
    catch (NumberFormatException ex) {
      return ZERO;
    }
    catch (IllegalArgumentException ex) {
      return ZERO;
    }
  }

  private static int calculateScale(Env env, int scale)
  {
    if (scale < 0) {
      Value iniValue = env.getIni("bcmath.scale");

      if (iniValue != null)
        scale = iniValue.toInt();
    }

    if (scale < 0)
      scale = 0;

    return scale;
  }

  /**
   * Add two arbitrary precision numbers.
   *
   * The optional scale indicates the number of decimal digits to include in
   * the result, the default is the value of a previous call to {@link #bcscale}
   * or the value of the ini variable "bcmath.scale".
   */
  public static String bcadd(Env env, Value value1, Value value2, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    BigDecimal bd1 = toBigDecimal(value1);
    BigDecimal bd2 = toBigDecimal(value2);

    BigDecimal bd = bd1.add(bd2);

    bd = bd.setScale(scale, RoundingMode.DOWN);

    return bd.toPlainString();
  }

  /**
   * Compare two arbitrary precision numbers, return -1 if value 1 < value2,
   * 0 if value1 == value2, 1 if value1 > value2.
   *
   * The optional scale indicates the number of decimal digits to include in
   * comparing the values, the default is the value of a previous call to
   * {@link #bcscale} or the value of the ini variable "bcmath.scale".
   */
  public static int bccomp(Env env, Value value1, Value value2, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    BigDecimal bd1 = toBigDecimal(value1);
    BigDecimal bd2 = toBigDecimal(value2);

    bd1 = bd1.setScale(scale, RoundingMode.DOWN);
    bd2 = bd2.setScale(scale, RoundingMode.DOWN);

    return bd1.compareTo(bd2);
  }

  /**
   * Divide one arbitrary precision number (value1) by another (value2).
   *
   * A division by zero results in a warning message and a return value of null.
   *
   * The optional scale indicates the number of decimal digits to include in
   * the result, the default is the value of a previous call to {@link #bcscale}
   * or the value of the ini variable "bcmath.scale".
   */
  public static String bcdiv(Env env, Value value1, Value value2, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    BigDecimal bd1 = toBigDecimal(value1);
    BigDecimal bd2 = toBigDecimal(value2);

    if (bd2.compareTo(ZERO) == 0) {
      env.warning(L.l("division by zero"));
      return null;
    }

    BigDecimal result;

    if (scale > 0) {
      result = bd1.divide(bd2, scale + 2, RoundingMode.DOWN);
    }
    else {
      result = bd1.divide(bd2, 2, RoundingMode.DOWN);
    }

    result = result.setScale(scale, RoundingMode.DOWN);

    return result.toPlainString();
  }

  /**
   * Return the modulus of an aribtrary precison number.
   * The returned number is always a whole number.
   *
   * A modulus of 0 results in a division by zero warning message and a
   * return value of null.
   */
  public static String bcmod(Env env, Value value, Value modulus)
  {
    BigDecimal bd1 = toBigDecimal(value).setScale(0, RoundingMode.DOWN);
    BigDecimal bd2 = toBigDecimal(modulus).setScale(0, RoundingMode.DOWN);

    if (bd2.compareTo(ZERO) == 0) {
      env.warning(L.l("division by zero"));
      return null;
    }

    BigDecimal bd = bd1.remainder(bd2, MathContext.DECIMAL128);

    // scale is always 0 in php
    bd = bd.setScale(0, RoundingMode.DOWN);

    return bd.toPlainString();
  }

  /**
   * Multiply two arbitrary precision numbers.
   *
   * The optional scale indicates the number of decimal digits to include in
   * the result, the default is the value of a previous call to {@link #bcscale}
   * or the value of the ini variable "bcmath.scale".
   */
  public static String bcmul(Env env, Value value1, Value value2, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    BigDecimal bd1 = toBigDecimal(value1);
    BigDecimal bd2 = toBigDecimal(value2);

    BigDecimal bd = bd1.multiply(bd2);

    // odd php special case for 0, scale is ignored:
    if (bd.compareTo(ZERO) == 0) {
      if (scale > 0)
        return "0.0";
      else
        return "0";
    }

    bd = bd.setScale(scale, RoundingMode.DOWN);
    bd = bd.stripTrailingZeros();

    return bd.toPlainString();
  }

  /**
   * Raise one arbitrary precision number (base) to the power of another (exp).
   *
   * exp must be a whole number. Negative exp is supported.
   *
   * The optional scale indicates the number of decimal digits to include in
   * the result, the default is the value of a previous call to {@link #bcscale}
   * or the value of the ini variable "bcmath.scale".
   */
  public static String bcpow(Env env, Value base, Value exp, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    BigDecimal bd1 = toBigDecimal(base);
    BigDecimal bd2 = toBigDecimal(exp);

    if (bd2.scale() > 0)
      env.warning("fractional exponent not supported");

    int exponent = bd2.toBigInteger().intValue();

    if (exponent == 0)
      return "1";

    boolean isNeg;

    if (exponent < 0)  {
      isNeg = true;
      exponent *= -1;
    }
    else
      isNeg = false;

    BigDecimal bd = bd1.pow(exponent);

    if (isNeg)
      bd = ONE.divide(bd, scale + 2, RoundingMode.DOWN);

    bd = bd.setScale(scale, RoundingMode.DOWN);

    if (bd.compareTo(BigDecimal.ZERO) == 0)
      return "0";

    bd = bd.stripTrailingZeros();

    return bd.toPlainString();
  }

  /**
   * Raise one arbitrary precision number (base) to the power of another (exp),
   * and then return the modulus.
   * The returned number is always a whole number.
   *
   * exp must be a whole number. Negative exp is supported.
   *
   * The optional scale indicates the number of decimal digits to include in
   * the pow calculation, the default is the value of a previous call to {@link #bcscale}
   * or the value of the ini variable "bcmath.scale".
   */
  public static String bcpowmod(Env env, Value base, Value exp, Value modulus, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    // XXX: this is inefficient, s/b fast-exponentiation
    String pow = bcpow(env, base, exp, scale);

    if (pow == null)
      return null;

    return bcmod(env, env.createString(pow), modulus);
  }


  /**
   * Set the default scale to use for subsequent calls to bcmath functions.
   * The scale is the number of decimal points to include in the string that
   * results from bcmath calculations.
   *
   * A default scale set with this function overrides the value of the
   * "bcmath.scale" ini variable.
   */
  public static boolean bcscale(Env env, int scale)
  {
    env.setIni("bcmath.scale", String.valueOf(scale));

    return true;
  }

  /**
   * Return the square root of an arbitrary precision number.
   *
   * A negative operand results in a warning message and a return value of null.
   *
   * The optional scale indicates the number of decimal digits to include in
   * the result, the default is the value of a previous call to {@link #bcscale}
   * or the value of the ini variable "bcmath.scale".
   */
  public static String bcsqrt(Env env, Value operand, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    BigDecimal value = toBigDecimal(operand);

    int compareToZero = value.compareTo(ZERO);

    if (compareToZero < 0) {
      env.warning(L.l("square root of negative number"));
      return null;
    }
    else if (compareToZero == 0) {
      return "0";
    }

    int compareToOne = value.compareTo(ONE);

    if (compareToOne == 0)
      return "1";

    // newton's algorithm

    int cscale;

    // initial guess

    BigDecimal initialGuess;

    if (compareToOne < 1) {
      initialGuess = ONE;
      cscale = value.scale();
    }
    else {
      BigInteger integerPart = value.toBigInteger();

      int length = integerPart.toString().length();

      if ((length % 2) == 0)
        length--;

      length /= 2;

      initialGuess = ONE.movePointRight(length);

      cscale = Math.max(scale, value.scale()) + 2;
    }

    // iterate

    BigDecimal guess = initialGuess;

    BigDecimal lastGuess;

    for (int iteration = 0; iteration < SQRT_MAX_ITERATIONS; iteration++) {
      lastGuess = guess;
      guess = value.divide(guess, cscale, RoundingMode.DOWN);
      guess = guess.add(lastGuess);
      guess = guess.divide(TWO, cscale, RoundingMode.DOWN);

      if (lastGuess.equals(guess)) {
          break;
      }
    }

    value = guess;

    value = value.setScale(scale, RoundingMode.DOWN);

    return value.toPlainString();
  }

  /**
   * Subtract arbitrary precision number (value2) from another (value1).
   *
   * The optional scale indicates the number of decimal digits to include in
   * the result, the default is the value of a previous call to {@link #bcscale}
   * or the value of the ini variable "bcmath.scale".
   */
  public static String bcsub(Env env, Value value1, Value value2, @Optional("-1") int scale)
  {
    scale = calculateScale(env, scale);

    BigDecimal bd1 = toBigDecimal(value1);
    BigDecimal bd2 = toBigDecimal(value2);

    BigDecimal bd = bd1.subtract(bd2);

    bd = bd.setScale(scale, RoundingMode.DOWN);

    return bd.toPlainString();
  }

  public static final IniDefinition INI_BCMATH_SCALE = _iniDefinitions.add("bcmath.scale", 0, PHP_INI_ALL);
}
