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
 * DayOfWeek
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class DayOfWeek extends MixedValueTypeExpression
{

   public static final Integer MAX_DAY_OF_WEEK = 7;

   public static final Integer MIN_DAY_OF_WEEK = 0;

   private static final Map<String, Integer> DAY_OF_WEEK_ALIAS = new HashMap<String, Integer>();

   static
   {
      DAY_OF_WEEK_ALIAS.put("Sun", 0);
      DAY_OF_WEEK_ALIAS.put("Mon", 1);
      DAY_OF_WEEK_ALIAS.put("Tue", 2);
      DAY_OF_WEEK_ALIAS.put("Wed", 3);
      DAY_OF_WEEK_ALIAS.put("Thu", 4);
      DAY_OF_WEEK_ALIAS.put("Fri", 5);
      DAY_OF_WEEK_ALIAS.put("Sat", 6);
   }

   private static final int OFFSET = DAY_OF_WEEK_ALIAS.get("Sun") - Calendar.SUNDAY;

   private SortedSet<Integer> daysOfWeek = new TreeSet<Integer>();

   private SortedSet<Integer> offsetAdjustedDaysOfWeek = new TreeSet<Integer>();

   private ScheduleExpressionType expressionType;

   public DayOfWeek(String value)
   {
      this.expressionType = ScheduleExpressionTypeUtil.getType(value);
      Set<Integer> days = null;
      switch (this.expressionType)
      {
         case RANGE :
            RangeValue range = new RangeValue(value);
            days = this.processRangeValue(range);
            this.daysOfWeek.addAll(days);
            break;

         case LIST :
            ListValue list = new ListValue(value);
            days = this.processListValue(list);
            this.daysOfWeek.addAll(days);
            break;

         case SINGLE_VALUE :
            SingleValue singleValue = new SingleValue(value);
            // process the single value and get the integer value
            // out of it
            Integer day = this.processSingleValue(singleValue);
            // add it to our sorted set
            this.daysOfWeek.add(day);
            break;
         case WILDCARD :
            break;
         case INCREMENT :
            throw new IllegalArgumentException(
                  "Increment type expression is not allowed for day-of-week value. Invalid value: " + value);

      }
      for (Integer dayOfWeek : this.daysOfWeek)
      {
         if (dayOfWeek == 7)
         {
            this.daysOfWeek.remove(dayOfWeek);
            this.daysOfWeek.add(new Integer(0));
         }
      }
      if (OFFSET != 0)
      {
         for (Integer dayOfWeek : this.daysOfWeek)
         {
            this.offsetAdjustedDaysOfWeek.add(dayOfWeek - OFFSET);
         }
      }
      else
      {
         this.offsetAdjustedDaysOfWeek = this.daysOfWeek;
      }
   }

   @Override
   protected Map<String, Integer> getAliases()
   {
      return DAY_OF_WEEK_ALIAS;
   }

   @Override
   protected Integer getMaxValue()
   {
      return MAX_DAY_OF_WEEK;
   }

   @Override
   protected Integer getMinValue()
   {
      return MIN_DAY_OF_WEEK;
   }

   public Calendar getNextDayOfWeek(Calendar current)
   {
      if (this.expressionType == ScheduleExpressionType.WILDCARD)
      {
         return current;
      }

      Calendar next = new GregorianCalendar(current.getTimeZone());
      next.setTime(current.getTime());
      next.setFirstDayOfWeek(current.getFirstDayOfWeek());

      Integer currentDayOfWeek = current.get(Calendar.DAY_OF_WEEK);

      Integer nextDayOfWeek = this.offsetAdjustedDaysOfWeek.first();
      for (Integer dayOfWeek : this.offsetAdjustedDaysOfWeek)
      {
         if (currentDayOfWeek.equals(dayOfWeek))
         {
            nextDayOfWeek = currentDayOfWeek;
            break;
         }
         if (dayOfWeek.intValue() > currentDayOfWeek.intValue())
         {
            nextDayOfWeek = dayOfWeek;
            break;
         }
      }
      if (nextDayOfWeek < currentDayOfWeek)
      {
         // advance to next week
         next.add(Calendar.WEEK_OF_MONTH, 1);
      }
      int maximumPossibleDateForTheMonth = next.getActualMaximum(Calendar.DAY_OF_MONTH);
      int date = next.get(Calendar.DAY_OF_MONTH);
      while (date > maximumPossibleDateForTheMonth)
      {
         //
         next.add(Calendar.MONTH, 1);
         maximumPossibleDateForTheMonth = next.getActualMaximum(Calendar.DAY_OF_MONTH);
      }

      next.set(Calendar.DAY_OF_WEEK, nextDayOfWeek);

      return next;
   }
   
  
}
