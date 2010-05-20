/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.ejb3.timer.schedule;

import java.util.StringTokenizer;

import javax.ejb.ScheduleExpression;

/**
 * Represents a value for a {@link ScheduleExpression} which is expressed as a range type. An
 * {@link RangeValue} comprises of a start and an end value for the range, separated by a "-"
 * 
 * <p>
 *  Each side of the range must be an individual attribute value. Members of a range <b>cannot</b> themselves 
 *  be lists, wild-cards, ranges, or increments. In range ”x-y”, if x is larger than y, the range is equivalent 
 *  to “x-max, min-y”, where max is the largest value of the corresponding attribute and min is the
 *  smallest. The range “x-x”, where both range values are the same, evaluates to the single value x.
 * </p>
 *
 * @see ScheduleExpressionType#RANGE
 * 
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class RangeValue
{
   /**
    * The separator which is used for parsing a {@link String} which
    * represents a {@link RangeValue} 
    */
   public static final String RANGE_SEPARATOR = "-";

   /**
    * The start value of the range
    */
   private String rangeStart;

   /**
    * The end value of the range
    */
   private String rangeEnd;

   /**
    * Creates a {@link RangeValue} by parsing the passed <code>value</code>.
    * <p>
    *   Upon successfully parsing the passed <code>value</code>, this constructor
    *   sets the start and the end value of this {@link RangeValue} 
    * </p>
    * @param range The value to be parsed
    * @throws IllegalArgumentException If the passed <code>value</code> cannot be 
    *           represented as an {@link RangeValue}
    * 
    */
   public RangeValue(String range)
   {
      StringTokenizer tokenizer = new StringTokenizer(range, RANGE_SEPARATOR);
      int numberOfTokens = tokenizer.countTokens();
      if (numberOfTokens != 2)
      {
         throw new IllegalArgumentException("Invalid range value: " + range);
      }

      this.rangeStart = tokenizer.nextToken();
      this.rangeEnd = tokenizer.nextToken();
   }

   /**
    * Returns the start value of this {@link RangeValue}
    * @return
    */
   public String getStart()
   {
      return this.rangeStart;
   }

   /**
    * Returns the end value of this {@link RangeValue}
    * @return
    */
   public String getEnd()
   {
      return this.rangeEnd;
   }
}
