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
 * Represents the value of a hour constructed out of a {@link ScheduleExpression#getHour()}
 *
 * <p>
 *  A {@link Hour} can hold only {@link Integer} as its value. The only exception to this being the wildcard (*)
 *  value. The various ways in which a 
 *  {@link Hour} value can be represented are:
 *  <ul>
 *      <li>Wildcard. For example, hour = "*"</li>
 *      <li>Range. For example, hour = "0-23"</li>
 *      <li>List. For example, hour = "1, 12, 20"</li>
 *      <li>Single value. For example, hour = "5"</li>
 *      <li>Increment. For example, hour = "0 &#47; 3"</li>
 *  </ul>
 * </p>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class Hour extends IntegerBasedExpression
{
   /**
    * Maximum allowed value for a {@link Hour}
    */
   public static final Integer MAX_HOUR = 23;

   /**
    * Minimum allowed value for a {@link Hour}
    */
   public static final Integer MIN_HOUR = 0;

   /**
    * A sorted set of valid values for hours, created out
    * of a {@link ScheduleExpression#getHour()} 
    */
   private SortedSet<Integer> hours = new TreeSet<Integer>();

   /**
    * The type of the expression, from which this {@link Hour} was 
    * constructed.
    * 
    * @see ScheduleExpressionType
    */
   private ScheduleExpressionType expressionType;

   /**
    * 
    * Creates a {@link Hour} by parsing the passed {@link String} <code>value</code>
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
   public Hour(String value)
   {
      // check the type of value
      this.expressionType = ScheduleExpressionTypeUtil.getType(value);

      Set<Integer> hrs = null;
      switch (this.expressionType)
      {
         case RANGE :
            RangeValue range = new RangeValue(value);
            // process the range value and get the integer
            // values out of it
            hrs = this.processRangeValue(range);
            // add those to our sorted set
            this.hours.addAll(hrs);
            break;

         case LIST :
            ListValue list = new ListValue(value);
            // process the list value and get the integer
            // values out of it
            hrs = this.processListValue(list);
            // add them to our sorted set
            this.hours.addAll(hrs);
            break;

         case SINGLE_VALUE :
            SingleValue singleValue = new SingleValue(value);
            // process the single value and get the integer value
            // out of it
            Integer hour = this.processSingleValue(singleValue);
            // add it to our sorted set
            this.hours.add(hour);
            break;

         case INCREMENT :
            IncrementValue incrValue = new IncrementValue(value);
            // process the increment value and get integer values
            // out of it
            hrs = this.processIncrement(incrValue);
            // add them to our sorted set
            this.hours.addAll(hrs);
            break;

         case WILDCARD :
            // a wildcard is equivalent to "all possible" values. So
            // nothing to do here.
            break;

         default :
            throw new IllegalArgumentException("Invalid value for hour: " + value);
      }
   }

   public Calendar getNextHour(Calendar current)
   {
      Calendar next = new GregorianCalendar(current.getTimeZone());
      next.setTime(current.getTime());

      // HOUR_OF_DAY is 24 hour based unlike HOUR which is 12 hour based
      // http://java.sun.com/j2se/1.5.0/docs/api/java/util/Calendar.html#HOUR_OF_DAY
      Integer currentHour = current.get(Calendar.HOUR_OF_DAY);
      if (this.expressionType == ScheduleExpressionType.WILDCARD)
      {
         return current;
      }
      Integer nextHour = hours.first();
      for (Integer hour : hours)
      {
         if (currentHour.equals(hour))
         {
            nextHour = currentHour;
            break;
         }
         if (hour.intValue() > currentHour.intValue())
         {
            nextHour = hour;
            break;
         }
      }
      if (nextHour < currentHour)
      {
         // advance to next day
         next.add(Calendar.DATE, 1);
      }
      // HOUR_OF_DAY is 24 hour based unlike HOUR which is 12 hour based
      // http://java.sun.com/j2se/1.5.0/docs/api/java/util/Calendar.html#HOUR_OF_DAY
      next.set(Calendar.HOUR_OF_DAY, nextHour);

      return next;
   }

   public int getFirst()
   {
      if (this.expressionType == ScheduleExpressionType.WILDCARD)
      {
         return new GregorianCalendar().get(Calendar.HOUR_OF_DAY);
      }
      return this.hours.first();
   }

   
   /**
    * Returns the maximum allowed value for a {@link Hour}
    * 
    * @see Hour#MAX_HOUR
    */
   @Override
   protected Integer getMaxValue()
   {
      return MAX_HOUR;
   }

   /**
    * Returns the minimum allowed value for a {@link Hour}
    * 
    * @see Hour#MIN_HOUR
    */
   @Override
   protected Integer getMinValue()
   {
      return MIN_HOUR;
   }

}
