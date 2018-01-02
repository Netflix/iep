/*
 * Copyright 2014-2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.iep.config;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;

public class Strings {
  private Strings() {}

  /**
   * URL query parameter.
   */
  private static final Pattern QueryParam = Pattern.compile("^([^=]+)=(.*)$");

  /**
   * Simple variable syntax with $varname.
   */
  private static final Pattern SimpleVar = Pattern.compile("\\$([-_.a-zA-Z0-9]+)");

  /**
   * Simple variable syntax where variable name is enclosed in parenthesis,
   * e.g., $(varname).
   */
  private static final Pattern ParenVar = Pattern.compile("\\$\\(([^\\(\\)]+)\\)");

  /**
   * Period following conventions of unix `at` command.
   */
  private static final Pattern AtPeriod = Pattern.compile("^(\\d+)([a-z]+)$");

  /**
   * Period following the ISO8601 conventions.
   */
  private static final Pattern IsoPeriod = Pattern.compile("^(P.*)$");

  /**
   * Date following the ISO8601 conventions.
   */
  private static final Pattern IsoDate = Pattern.compile("^(\\d{4}-.*)$");

  /**
   * Date relative to a given reference point.
   */
  private static final Pattern RelativeDate = Pattern.compile("^([a-z]+)([\\-+])(.+)$");

  /**
   * Named date such as `epoch` or `now`.
   */
  private static final Pattern NamedDate = Pattern.compile("^([a-z]+)$");

  /**
   * Unix data in seconds since the epoch.
   */
  private static final Pattern UnixDate = Pattern.compile("^([0-9]+)$");

  /**
   * Constant for the unix epoch.
   */
  private static final DateTime epoch = new DateTime(0L, DateTimeZone.UTC);

  private static final DateTimeFormatter isoDateFmt = ISODateTimeFormat.dateTimeParser().withZone(DateTimeZone.UTC);
  private static final PeriodFormatter isoPeriodFmt = ISOPeriodFormat.standard();

  public static String join(java.util.List<String> list, String separator) {
    StringBuilder sb = new StringBuilder();
    String sep = "";
    for (String s : list) {
        sb.append(sep).append(s);
        sep = separator;
    }
    return sb.toString();
  }

  public static boolean conversionExists(Class c) {
    return c == String.class
        || c == boolean.class
        || c == byte.class
        || c == short.class
        || c == int.class
        || c == long.class
        || c == float.class
        || c == double.class
        || c == Boolean.class
        || c == Byte.class
        || c == Short.class
        || c == Integer.class
        || c == Long.class
        || c == Float.class
        || c == Double.class
        || c == DateTime.class
        || c == DateTimeZone.class
        || c == Duration.class
        || c == Period.class
        || c == ZonedDateTime.class
        || c == ZoneId.class
        || c == java.time.Duration.class
        || c == Pattern.class;
  }

  @SuppressWarnings("unchecked")
  public static <T> T cast(Class<T> c, String v) {
    if (c == String.class) return (T) v;
    if (c == boolean.class) return (T) java.lang.Boolean.valueOf(v);
    if (c == byte.class) return (T) java.lang.Byte.valueOf(v);
    if (c == short.class) return (T) java.lang.Short.valueOf(v);
    if (c == int.class) return (T) java.lang.Integer.valueOf(v);
    if (c == long.class) return (T) java.lang.Long.valueOf(v);
    if (c == float.class) return (T) java.lang.Float.valueOf(v);
    if (c == double.class) return (T) java.lang.Double.valueOf(v);
    if (c == Boolean.class) return (T) java.lang.Boolean.valueOf(v);
    if (c == Byte.class) return (T) java.lang.Byte.valueOf(v);
    if (c == Short.class) return (T) java.lang.Short.valueOf(v);
    if (c == Integer.class) return (T) java.lang.Integer.valueOf(v);
    if (c == Long.class) return (T) java.lang.Long.valueOf(v);
    if (c == Float.class) return (T) java.lang.Float.valueOf(v);
    if (c == Double.class) return (T) java.lang.Double.valueOf(v);
    if (c == DateTime.class) return (T) parseDate(v);
    if (c == DateTimeZone.class) return (T) DateTimeZone.forID(v);
    if (c == Duration.class) return (T) parseDuration(v);
    if (c == Period.class) return (T) parsePeriod(v);
    if (c == ZonedDateTime.class) return (T) parseJavaDate(v);
    if (c == ZoneId.class) return (T) ZoneId.of(v);
    if (c == java.time.Duration.class) return (T) parseJavaDuration(v);
    if (c == Pattern.class) return (T) Pattern.compile(v);
    throw new IllegalArgumentException("unsupported property type " + c.getName());
  }

  private static ZonedDateTime parseJavaDate(String v) {
    DateTime dt = parseDate(v);
    long time = dt.toDate().getTime();
    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneOffset.UTC);
  }

  public static DateTime parseDate(String v) {
    return parseDate(v, DateTimeZone.UTC);
  }

  public static DateTime parseDate(String v, DateTimeZone tz) {
    return parseDate(new DateTime(tz), v, tz);
  }

  public static DateTime parseDate(DateTime ref, String v, DateTimeZone tz) {
    Matcher m = IsoDate.matcher(v);
    if (m.matches()) return isoDateFmt.withZone(tz).parseDateTime(m.group(1));
    m = RelativeDate.matcher(v);
    if (m.matches()) {
      String r = m.group(1);
      String op = m.group(2);
      String p = m.group(3);
      if (op.equals("-")) return parseRefVar(ref, r).minus(parsePeriod(p));
      else if (op.equals("+")) return parseRefVar(ref, r).plus(parsePeriod(p));
      else throw new IllegalArgumentException("invalid operation " + op);
    }
    m = NamedDate.matcher(v);
    if (m.matches()) return parseRefVar(ref, m.group(1));
    m = UnixDate.matcher(v);
    if (m.matches()) return new DateTime(Long.valueOf(m.group(1)) * 1000, DateTimeZone.UTC);
    throw new IllegalArgumentException("invalid date " + v);
  }

  private static DateTime parseRefVar(DateTime ref, String v) {
    if (v.equals("now")) return new DateTime();
    else if (v.equals("epoch")) return epoch;
    else return ref;
  }

  private static java.time.Duration parseJavaDuration(String v) {
    Duration d = parseDuration(v);
    return java.time.Duration.ofMillis(d.getMillis());
  }

  public static Duration parseDuration(String v) {
    return parsePeriod(v).toStandardDuration();
  }

  public static Period parsePeriod(String v) {
    Matcher m = AtPeriod.matcher(v);
    if (m.matches()) return parseAtPeriod(m.group(1), m.group(2));
    m = IsoPeriod.matcher(v);
    if (m.matches()) return isoPeriodFmt.parsePeriod(m.group(1));
    throw new IllegalArgumentException("invalid period " + v);
  }

  private static Period parseAtPeriod(String amt, String unit) {
    int v = Integer.valueOf(amt);
    if (unit.equals("s") || unit.equals("second") || unit.equals("seconds"))
      return Period.seconds(v);
    if (unit.equals("m") || unit.equals("min") || unit.equals("minute") || unit.equals("minutes"))
      return Period.minutes(v);
    if (unit.equals("h") || unit.equals("hour") || unit.equals("hours"))
      return Period.hours(v);
    if (unit.equals("d") || unit.equals("day") || unit.equals("days"))
      return Period.days(v);
    if (unit.equals("w") || unit.equals("wk") || unit.equals("week") || unit.equals("weeks"))
      return Period.weeks(v);
    if (unit.equals("month") || unit.equals("months"))
      return Period.months(v);
    if (unit.equals("y") || unit.equals("year") || unit.equals("years"))
      return Period.years(v);
    throw new IllegalArgumentException("unknown unit " + unit);
  }
}
