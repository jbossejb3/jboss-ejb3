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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ejb.ScheduleExpression;

/**
 * Represents the value of a day in a month, constructed out of a {@link ScheduleExpression#getDayOfMonth()}
 *
 * <p>
 *  A {@link DayOfMonth} can hold an {@link Integer} or a {@link String} as its value. 
 *  value. The various ways in which a 
 *  {@link DayOfMonth} value can be represented are:
 *  <ul>
 *      <li>Wildcard. For example, dayOfMonth = "*"</li>
 *      <li>Range. Examples:
 *          <ul>
 *              <li>dayOfMonth = "1-10"</li>
 *              <li>dayOfMonth = "Sun-Tue"</li>
 *              <li>dayOfMonth = "1st-5th"</li>
 *          </ul>
 *       </li>   
 *      <li>List. Examples:
 *          <ul>
 *              <li>dayOfMonth = "1, 12, 20"</li>
 *              <li>dayOfMonth = "Mon, Fri, Sun"</li>
 *              <li>dayOfMonth = "3rd, 1st, Last"</li>
 *          </ul>
 *       </li>   
 *      <li>Single value. Examples:
 *          <ul>
 *              <li>dayOfMonth = "Fri"</li>
 *              <li>dayOfMonth = "Last"</li>
 *              <li>dayOfMonth = "10"</li>
 *          </ul>
 *      </li>        
 *  </ul>
 * </p>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class DayOfMonth extends MixedValueTypeExpression
{

   /**
    * The maximum allowed value for the {@link DayOfMonth}
    */
   public static final Integer MAX_DAY_OF_MONTH = 31;

   /**
    * The minimum allowed value for the {@link DayOfMonth}
    */
   public static final Integer MIN_DAY_OF_MONTH = -7;

   /**
    * A {@link DayOfMonth} can be represented as a {@link String} too (for example "1st", "Sun" etc...).
    * Internally, we map all allowed {@link String} values to their {@link Integer} equivalents.
    * This map holds the {@link String} name to {@link Integer} value mapping. 
    */
   private static final Map<String, Integer> DAY_OF_MONTH_ALIAS = new HashMap<String, Integer>();

   static
   {
      DAY_OF_MONTH_ALIAS.put("1st", 1);
      DAY_OF_MONTH_ALIAS.put("2nd", 2);
      DAY_OF_MONTH_ALIAS.put("3rd", 3);
      DAY_OF_MONTH_ALIAS.put("4th", 4);
      DAY_OF_MONTH_ALIAS.put("5th", 5);

      DAY_OF_MONTH_ALIAS.put("Last", 31);

      DAY_OF_MONTH_ALIAS.put("Sun", Calendar.SUNDAY);
      DAY_OF_MONTH_ALIAS.put("Mon", Calendar.MONDAY);
      DAY_OF_MONTH_ALIAS.put("Tue", Calendar.TUESDAY);
      DAY_OF_MONTH_ALIAS.put("Wed", Calendar.WEDNESDAY);
      DAY_OF_MONTH_ALIAS.put("Thu", Calendar.THURSDAY);
      DAY_OF_MONTH_ALIAS.put("Fri", Calendar.FRIDAY);
      DAY_OF_MONTH_ALIAS.put("Sat", Calendar.SATURDAY);

   }

   /**
    * A sorted set of valid values for days of month, created out
    * of a {@link ScheduleExpression#getDayOfMonth()} 
    */
   private SortedSet<Integer> daysOfMonth = new TreeSet<Integer>();

   /**
    * The type of the expression, from which this {@link DayOfMonth} was 
    * constructed.
    * 
    * @see ScheduleExpressionType
    */
   private ScheduleExpressionType expressionType;

   /**
    * Creates a {@link DayOfMonth} by parsing the passed {@link String} <code>value</code>
    * <p>
    *   Valid values are of type {@link ScheduleExpressionType#WILDCARD}, {@link ScheduleExpressionType#RANGE},
    *   {@link ScheduleExpressionType#LIST} or {@link ScheduleExpressionType#SINGLE_VALUE}
    * </p>
    * @param value The value to be parsed
    * 
    * @throws IllegalArgumentException If the passed <code>value</code> is neither a {@link ScheduleExpressionType#WILDCARD}, 
    *                               {@link ScheduleExpressionType#RANGE}, {@link ScheduleExpressionType#LIST}, 
    *                               nor {@link ScheduleExpressionType#SINGLE_VALUE}.
    */
   public DayOfMonth(String value)
   {
      // check the type of value
      this.expressionType = ScheduleExpressionTypeUtil.getType(value);

      Set<Integer> days = null;
      switch (this.expressionType)
      {
         case RANGE :
            RangeValue range = new RangeValue(value);
            // process the range value and get the integer
            // values out of it
            days = this.processRangeValue(range);
            // add them to our sorted set
            this.daysOfMonth.addAll(days);
            break;

         case LIST :
            ListValue list = new ListValue(value);
            // process a list value and get the integer
            // values out of it
            days = this.processListValue(list);
            // add them to our sorted set
            this.daysOfMonth.addAll(days);
            break;

         case SINGLE_VALUE :
            SingleValue singleValue = new SingleValue(value);
            // process the single value and get the integer value
            // out of it
            Integer day = this.processSingleValue(singleValue);
            // add this to our sorted set
            this.daysOfMonth.add(day);
            break;

         case WILDCARD :
            // a wildcard is equivalent to "all possible" values. So
            // nothing to do here.
            break;

         case INCREMENT :
            throw new IllegalArgumentException(
                  "Increment type expression is not allowed for day-of-month value. Invalid value: " + value);

         default :
            throw new IllegalArgumentException("Invalid value for day of month: " + value);

      }
   }

   /**
    * A {@link DayOfMonth} like any other {@link MixedValueTypeExpression} can 
    * hold both {@link String} and {@link Integer} values. Ultimately, a {@link String}
    * value (like "Sun") is mapped back to an {@link Integer} value. This method
    * return a {@link Map} which contains that mapping. 
    * 
    * {@inheritDoc}
    */
   @Override
   protected Map<String, Integer> getAliases()
   {
      return DAY_OF_MONTH_ALIAS;
   }

   /**
    * Returns the maximum allowed value for a {@link DayOfMonth}
    * 
    * @see DayOfMonth#MAX_DAY_OF_MONTH
    */
   @Override
   protected Integer getMaxValue()
   {
      return MAX_DAY_OF_MONTH;
   }

   /**
    * Returns the minimum allowed value for a {@link DayOfMonth}
    * 
    * @see DayOfMonth#MIN_DAY_OF_MONTH
    */
   @Override
   protected Integer getMinValue()
   {
      return MIN_DAY_OF_MONTH;
   }

   public Calendar getNextDayOfMonth(Calendar current)
   {
      if (this.expressionType == ScheduleExpressionType.WILDCARD)
      {
         return current;
      }

      Calendar next = new GregorianCalendar(current.getTimeZone());
      next.setTime(current.getTime());
      next.setFirstDayOfWeek(current.getFirstDayOfWeek());

      Integer currentDayOfMonth = current.get(Calendar.DAY_OF_MONTH);

      Integer nextDayOfMonth = this.daysOfMonth.first();
      for (Integer dayOfMonth : this.daysOfMonth)
      {
         if (currentDayOfMonth.equals(dayOfMonth))
         {
            nextDayOfMonth = currentDayOfMonth;
            break;
         }
         if (dayOfMonth.intValue() > currentDayOfMonth.intValue())
         {
            nextDayOfMonth = dayOfMonth;
            break;
         }
      }
      if (nextDayOfMonth < currentDayOfMonth)
      {
         // advance to next month
         next.add(Calendar.MONTH, 1);
      }
      int maximumPossibleDateForTheMonth = next.getActualMaximum(Calendar.DAY_OF_MONTH);
      while (nextDayOfMonth > maximumPossibleDateForTheMonth)
      {
         //
         next.add(Calendar.MONTH, 1);
         maximumPossibleDateForTheMonth = next.getActualMaximum(Calendar.DAY_OF_MONTH);
      }
      next.set(Calendar.DAY_OF_MONTH, nextDayOfMonth);

      return next;
   }
 

   @Override
   protected void assertValid(Integer value) throws IllegalArgumentException
   {
      if (value != null && value == 0)
      {
         throw new IllegalArgumentException("Invalid value for day-of-month: " + value);
      }
      super.assertValid(value);
   }

}
