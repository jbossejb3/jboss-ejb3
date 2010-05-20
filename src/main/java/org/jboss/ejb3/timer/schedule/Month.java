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

/**
 * Month
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class Month extends MixedValueTypeExpression
{

   public static final Integer MAX_MONTH = 12;

   public static final Integer MIN_MONTH = 1;

   private static final Map<String, Integer> MONTH_ALIAS = new HashMap<String, Integer>();

   static
   {
      MONTH_ALIAS.put("Jan", 1);
      MONTH_ALIAS.put("Feb", 2);
      MONTH_ALIAS.put("Mar", 3);
      MONTH_ALIAS.put("Apr", 4);
      MONTH_ALIAS.put("May", 5);
      MONTH_ALIAS.put("Jun", 6);
      MONTH_ALIAS.put("Jul", 7);
      MONTH_ALIAS.put("Aug", 8);
      MONTH_ALIAS.put("Sep", 9);
      MONTH_ALIAS.put("Oct", 10);
      MONTH_ALIAS.put("Nov", 11);
      MONTH_ALIAS.put("Dec", 12);

   }

   private static final int OFFSET = MONTH_ALIAS.get("Jan") - Calendar.JANUARY;

   private SortedSet<Integer> months = new TreeSet<Integer>();

   private SortedSet<Integer> offsetAdjustedMonths = new TreeSet<Integer>();

   private ScheduleExpressionType expressionType;

   public Month(String value)
   {
      this.expressionType = ScheduleExpressionTypeUtil.getType(value);
      Set<Integer> mths = null;
      switch (this.expressionType)
      {
         case RANGE :
            RangeValue range = new RangeValue(value);
            mths = this.processRangeValue(range);
            this.months.addAll(mths);
            break;

         case LIST :
            ListValue list = new ListValue(value);
            mths = this.processListValue(list);
            this.months.addAll(mths);
            break;

         case SINGLE_VALUE :
            SingleValue singleValue = new SingleValue(value);
            // process the single value and get the integer value
            // out of it
            Integer month = this.processSingleValue(singleValue);
            // add it to our sorted set
            this.months.add(month);
            break;
         case WILDCARD :
            break;
         case INCREMENT :
            throw new IllegalArgumentException(
                  "Increment type expression is not allowed for month value. Invalid value: " + value);

      }
      if (OFFSET != 0)
      {
         for (Integer month : this.months)
         {
            this.offsetAdjustedMonths.add(month - OFFSET);
         }
      }
      else
      {
         this.offsetAdjustedMonths = this.months;
      }
   }

   @Override
   protected Map<String, Integer> getAliases()
   {
      return MONTH_ALIAS;
   }

   @Override
   protected Integer getMaxValue()
   {
      return MAX_MONTH;
   }

   @Override
   protected Integer getMinValue()
   {
      return MIN_MONTH;
   }

   public Calendar getNextMonth(Calendar current)
   {
      if (this.expressionType == ScheduleExpressionType.WILDCARD)
      {
         return current;
      }

      Calendar next = new GregorianCalendar(current.getTimeZone());
      next.setTime(current.getTime());

      // Calendar.JANUARY starts with 0 whereas our daysOfMonth is 1 based.
      // So increment the currentMonth by 1 to adjust the offset
      Integer currentMonth = current.get(Calendar.MONTH);
      Integer nextMonth = this.offsetAdjustedMonths.first();
      for (Integer month : this.offsetAdjustedMonths)
      {
         if (currentMonth.equals(month)
               && this.hasDateForMonth(next.get(Calendar.DAY_OF_MONTH), month, next.get(Calendar.YEAR)))
         {
            nextMonth = currentMonth;
         }
         if (month.intValue() > currentMonth.intValue()
               && this.hasDateForMonth(next.get(Calendar.DAY_OF_MONTH), month, next.get(Calendar.YEAR)))
         {
            nextMonth = month;
            break;
         }
      }
      if (nextMonth < currentMonth)
      {
         // advance to next year. But before doing that, make sure that the 
         // month can handle the date value

         int date = next.get(Calendar.DAY_OF_MONTH);
         int nextYear = current.get(Calendar.YEAR) + 1;
         if (nextMonth == Calendar.FEBRUARY && date == 29)
         {
            // special case
            for (int i = 0; i < 5; i++)
            {
               if (this.hasDateForMonth(date, nextMonth, nextYear))
               {
                  next.set(Calendar.MONTH, nextMonth);
                  next.set(Calendar.YEAR, nextYear);
                  return next;
               }
               nextYear++;
            }
            return null;
         }
         else
         {
            if (hasDateForMonth(date, nextMonth, nextYear))
            {
               next.set(Calendar.MONTH, nextMonth);
               next.set(Calendar.YEAR, nextYear);
               return next;

            }
            return null;
         }
         //         int nextYear = current.get(Calendar.YEAR) + 1;
         //         for (int i = 0; i < 5; i++)
         //         {
         //            if (this.hasDateForMonth(next.get(Calendar.DAY_OF_MONTH), nextMonth, nextYear))
         //            {
         //               next.set(Calendar.MONTH, nextMonth);
         //               next.set(Calendar.YEAR, nextYear);
         //               return next;
         //            }
         //            nextYear++;
         //         }
         //         return null;
      }
      else
      {
         next.set(Calendar.MONTH, nextMonth);
         return next;
      }
   }

   private boolean hasDateForMonth(int date, int month, int year)
   {
      Calendar cal = new GregorianCalendar();
      cal.set(Calendar.YEAR, year);
      cal.set(Calendar.MONTH, month);
      cal.set(Calendar.DAY_OF_MONTH, 1);
      int maximumPossibleDateForTheMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
      if (date > maximumPossibleDateForTheMonth)
      {
         return false;
      }
      return true;

   }

}
