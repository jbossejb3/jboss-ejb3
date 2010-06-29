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
 * Represents in the year value part constructed out of a {@link ScheduleExpression#getYear()}
 *
 * <p>
 *  A {@link Year} can hold only {@link Integer} as its value. The only exception to this being the wildcard (*)
 *  value. The various ways in which a 
 *  {@link Year} value can be represented are:
 *  <ul>
 *      <li>Wildcard. For example, year = "*"</li>
 *      <li>Range. For example, year = "2009-2011"</li>
 *      <li>List. For example, year = "2008, 2010, 2011"</li>
 *      <li>Single value. For example, year = "2009"</li>
 *  </ul>
 * </p>
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class Year extends IntegerBasedExpression
{

   // The EJB3. timer service spec says that the year
   // value can be any 4 digit value. 
   // Hence the max value 9999 
   public static final Integer MAX_YEAR = 9999;

   // TODO: think about this min value. The EJB3.1 timerservice spec
   // says, that the year value can be any 4 digit value.
   // That's the reason we have set it to 1000 here.
   public static final Integer MIN_YEAR = 1000;

   /**
    * A sorted set of valid values for years, created out
    * of a {@link ScheduleExpression#getYear()} 
    */
   private SortedSet<Integer> years = new TreeSet<Integer>();

   /**
    * The type of the expression, from which this {@link Year} was 
    * constructed.
    * 
    * @see ScheduleExpressionType
    */
   private ScheduleExpressionType expressionType;

   /**
    * Creates a {@link Year} by parsing the passed {@link String} <code>value</code>
    * <p>
    *   Valid values are of type {@link ScheduleExpressionType#WILDCARD}, {@link ScheduleExpressionType#RANGE},
    *   {@link ScheduleExpressionType#LIST} or {@link ScheduleExpressionType#SINGLE_VALUE}
    * </p>
    * @param value The value to be parsed
    * 
    * @throws IllegalArgumentException If the passed <code>value</code> is neither a {@link ScheduleExpressionType#WILDCARD}, 
    *                               {@link ScheduleExpressionType#RANGE}, {@link ScheduleExpressionType#LIST} 
    *                               nor {@link ScheduleExpressionType#SINGLE_VALUE}
    */
   public Year(String value)
   {
      // check the type of value 
      this.expressionType = ScheduleExpressionTypeUtil.getType(value);
      Set<Integer> yrs = null;
      switch (this.expressionType)
      {
         case RANGE :
            RangeValue range = new RangeValue(value);
            // process the range value to get integer values
            // out of it
            yrs = this.processRangeValue(range);
            // store in our sorted set
            this.years.addAll(yrs);
            break;

         case LIST :
            ListValue list = new ListValue(value);
            // process the list value and get integer values
            // out of it
            yrs = this.processListValue(list);
            // add it to our sorted set 
            this.years.addAll(yrs);
            break;

         case SINGLE_VALUE :
            SingleValue singleValue = new SingleValue(value);
            // process the single value and get the integer value
            // out of it
            Integer year = this.processSingleValue(singleValue);
            // add this to our sorted set
            this.years.add(year);
            break;

         case WILDCARD :
            // a wildcard is equivalent to "all possible" values, so 
            // do nothing
            break;
         case INCREMENT :
            throw new IllegalArgumentException(
                  "Increment type expression is not allowed for year value. Invalid value: " + value);
         default :
            throw new IllegalArgumentException("Invalid value for year: " + value);
      }
   }

   /**
    * Returns the maximum possible value for a {@link Year}
    * 
    * @see Year#MAX_YEAR
    */
   @Override
   protected Integer getMaxValue()
   {
      return MAX_YEAR;
   }

   /**
    * Returns the minimum possible value for a {@link Year}
    * 
    * @see Year#MIN_YEAR
    */
   @Override
   protected Integer getMinValue()
   {
      return MIN_YEAR;
   }

   public Calendar getNextYear(Calendar current)
   {
      boolean isFeb29 = this.isFeb29(current); 
      if (this.expressionType == ScheduleExpressionType.WILDCARD)
      {
         if (isFeb29)
         {
            if (isLeapYear(current.get(Calendar.YEAR)))
            {
               return current;
            }
            else
            {
               int nextLeapYear = this.getNextLeapYear(current.get(Calendar.YEAR));
               current.set(Calendar.YEAR, nextLeapYear);
               return current;
            }
         }
         return current;
      }

      Calendar next = new GregorianCalendar(current.getTimeZone());
      next.setTime(current.getTime());

      Integer currentYear = current.get(Calendar.YEAR);

      Integer nextYear = this.years.first();
      for (Integer year : this.years)
      {
         if (currentYear.equals(year))
         {
            if (isFeb29 && this.isLeapYear(year) == false)
            {
               continue;
            }
               
            nextYear = currentYear;
            break;
         }
         if (year.intValue() > currentYear.intValue())
         {
            if (isFeb29 && this.isLeapYear(year) == false)
            {
               continue;
            }

            nextYear = year;
            break;
         }
      }
      if (nextYear < currentYear)
      {
         // no more years
         return null;
      }
      if (isFeb29 && this.isLeapYear(nextYear) == false)
      {
         return null;
      }
      next.set(Calendar.YEAR, nextYear);

      return next;
   }

   private boolean isFeb29(Calendar cal)
   {
      int date = cal.get(Calendar.DATE);
      int month = cal.get(Calendar.MONTH);
      if (date == 29 && month == Calendar.FEBRUARY)
      {
         return true;
      }
      return false;
   }

   private boolean isLeapYear(int year)
   {
      if (isDivisibleBy4(year))
      {
         if (isDivisibleBy100(year))
         {
            if (isDivisibleBy400(year))
            {
               return true;
            }
            return false;
         }
         return true;
      }
      return false;
      
   }
   
   private boolean isDivisibleBy4(int num)
   {
      return num % 4 == 0;
   }
   
   private boolean isDivisibleBy100(int num)
   {
      return num % 100 == 0;
   }
   
   private boolean isDivisibleBy400(int num)
   {
      return num % 400 == 0;
   }
   
   private int getNextLeapYear(int year)
   {
      while (this.isLeapYear(year) == false)
      {
         year ++;
      }
      return year;
   }

}
