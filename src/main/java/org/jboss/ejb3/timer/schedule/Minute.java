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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ejb.ScheduleExpression;

/**
 * Represents the value of a minute constructed out of a {@link ScheduleExpression#getMinute()}
 *
 * <p>
 *  A {@link Minute} can hold only {@link Integer} as its value. The only exception to this being the wildcard (*)
 *  value. The various ways in which a 
 *  {@link Minute} value can be represented are:
 *  <ul>
 *      <li>Wildcard. For example, minute = "*"</li>
 *      <li>Range. For example, minute = "0-20"</li>
 *      <li>List. For example, minute = "10, 30, 45"</li>
 *      <li>Single value. For example, minute = "8"</li>
 *      <li>Increment. For example, minute = "10 &#47; 15"</li>
 *  </ul>
 * </p>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class Minute extends IntegerBasedExpression
{

   /**
    * Maximum allowed value for a {@link Minute}
    */
   public static final Integer MAX_MINUTE = 59;

   /**
    * Minimum allowed value for a {@link Minute}
    */
   public static final Integer MIN_MINUTE = 0;

   /**
    * A sorted set of valid values for minutes, created out
    * of a {@link ScheduleExpression#getMinute()} 
    */
   private SortedSet<Integer> minutes = new TreeSet<Integer>();

   /**
    * The type of the expression, from which this {@link Minute} was 
    * constructed.
    * 
    * @see ScheduleExpressionType
    */
   private ScheduleExpressionType expressionType;

   /**
    * Creates a {@link Minute} by parsing the passed {@link String} <code>value</code>
    * <p>
    *   Valid values are of type {@link ScheduleExpressionType#WILDCARD}, {@link ScheduleExpressionType#RANGE},
    *   {@link ScheduleExpressionType#LIST} {@link ScheduleExpressionType#INCREMENT} or 
    *   {@link ScheduleExpressionType#SINGLE_VALUE}
    * </p>
    * @param value The value to be parsed
    * 
    * @throws IllegalArgumentException If the passed <code>value</code> is neither a {@link ScheduleExpressionType#WILDCARD}, 
    *                               {@link ScheduleExpressionType#RANGE}, {@link ScheduleExpressionType#LIST}, 
    *                               {@link ScheduleExpressionType#INCREMENT} nor {@link ScheduleExpressionType#SINGLE_VALUE}.
    */
   public Minute(String value)
   {
      // check the type of value
      this.expressionType = ScheduleExpressionTypeUtil.getType(value);

      Set<Integer> mins = null;
      switch (this.expressionType)
      {
         case RANGE :
            RangeValue range = new RangeValue(value);
            // process the range value and get integer values 
            // out of it
            mins = this.processRangeValue(range);
            // add them to our sorted set
            this.minutes.addAll(mins);
            break;

         case LIST :
            ListValue list = new ListValue(value);
            // process the list value and get integer values
            // out of it
            mins = this.processListValue(list);
            // add them to our sorted set
            this.minutes.addAll(mins);
            break;

         case INCREMENT :
            IncrementValue incrValue = new IncrementValue(value);
            // process the increment value and get integer values
            // out of it
            mins = this.processIncrement(incrValue);
            // add them to our sorted set
            this.minutes.addAll(mins);
            break;

         case SINGLE_VALUE :
            SingleValue singleValue = new SingleValue(value);
            // process the single value and get the integer value
            // out of it
            Integer minute = this.processSingleValue(singleValue);
            // add it to our sorted set
            this.minutes.add(minute);
            break;

         case WILDCARD :
            // a wildcard is equivalent to "all possible" values. So
            // nothing to do here.
            break;

         default :
            throw new IllegalArgumentException("Invalid value for minute: " + value);
      }
   }

   public Calendar getNextMinute(Calendar current)
   {
      Calendar next = new GregorianCalendar(current.getTimeZone());
      next.setTime(current.getTime());

      Integer currentMinute = current.get(Calendar.MINUTE);
      if (this.expressionType == ScheduleExpressionType.WILDCARD)
      {
         return current;
      }
      Integer nextMinute = minutes.first();
      for (Integer minute : minutes)
      {
         if (currentMinute.equals(minute))
         {
            nextMinute = currentMinute;
            break;
         }
         if (minute.intValue() > currentMinute.intValue())
         {
            nextMinute = minute;
            break;
         }
      }
      if (nextMinute < currentMinute)
      {
         // advance to next hour
         next.add(Calendar.HOUR, 1);
      }
      next.set(Calendar.MINUTE, nextMinute);

      return next;
   }

   /**
    * Returns the maximum allowed value for a {@link Minute}
    * 
    * @see Minute#MAX_MINUTE
    */
   @Override
   protected Integer getMaxValue()
   {
      return MAX_MINUTE;
   }

   /**
    * Returns the minimum allowed value for a {@link Minute}
    * 
    * @see Minute#MIN_MINUTE
    */
   @Override
   protected Integer getMinValue()
   {
      return MIN_MINUTE;
   }

}
