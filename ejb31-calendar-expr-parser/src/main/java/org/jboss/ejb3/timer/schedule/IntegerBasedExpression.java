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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.ScheduleExpression;

/**
 * Represents a {@link Integer} type value in a {@link ScheduleExpression}.
 * 
 * <p>
 *  Examples for {@link IntegerBasedExpression} are the value of seconds, years, months etc...
 *  which allow {@link Integer}.
 * </p>
 *
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public abstract class IntegerBasedExpression
{

   protected abstract Integer getMaxValue();

   protected abstract Integer getMinValue();

   protected Set<Integer> processListValue(ListValue list)
   {
      Set<Integer> values = new HashSet<Integer>();
      List<String> listItems = list.getValues();
      for (String listItem : listItems)
      {
         values.addAll(this.processListItem(listItem));
      }
      return values;
   }

   protected Set<Integer> processListItem(String listItem)
   {
      // check what type of a value the list item is.
      // Each item in the list must be an individual attribute value or a range. 
      // List items can not themselves be lists, wild-cards, or increments.
      ScheduleExpressionType listItemType = ScheduleExpressionTypeUtil.getType(listItem);
      switch (listItemType)
      {
         case SINGLE_VALUE :
            SingleValue singleVal = new SingleValue(listItem);
            Integer value = this.processSingleValue(singleVal);
            Set<Integer> values = new HashSet<Integer>();
            values.add(value);
            return values;
         case RANGE :
            RangeValue range = new RangeValue(listItem);
            return this.processRangeValue(range);
         default :
            throw new IllegalArgumentException(
                  "A list value can only contain either a range or an individual value. Invalid value: " + listItem);
      }
   }

   protected Set<Integer> processRangeValue(RangeValue range)
   {
      Set<Integer> values = new HashSet<Integer>();
      String start = range.getStart();
      String end = range.getEnd();
      Integer rangeStart = this.get(start);
      Integer rangeEnd = this.get(end);

      // validations
      this.assertValid(rangeStart);
      this.assertValid(rangeEnd);

      // start and end are both the same. So it's just a single value
      if (rangeStart == rangeEnd)
      {
         values.add(rangeStart);

      }
      else if (rangeStart > rangeEnd)
      {
         // In range "x-y", if x is larger than y, the range is equivalent to
         // "x-max, min-y", where max is the largest value of the corresponding attribute 
         // and min is the smallest.
         for (int i = rangeStart; i <= this.getMaxValue(); i++)
         {
            values.add(i);
         }
         for (int i = this.getMinValue(); i <= rangeEnd; i++)
         {
            values.add(i);
         }
      }
      else
      {
         // just keep adding from range start to range end (both inclusive).
         for (int i = rangeStart; i <= rangeEnd; i++)
         {
            values.add(i);
         }
      }
      return values;
   }

   protected Set<Integer> processIncrement(IncrementValue incr)
   {
      String startValue = incr.getStart();
      Integer start = startValue.equals("*") ? 0 : this.get(startValue);
      // make sure it's a valid value
      this.assertValid(start);
      Integer interval = this.get(incr.getInterval());
      Set<Integer> values = new HashSet<Integer>();
      values.add(start);
      int next = start + interval;
      int maxValue = this.getMaxValue();
      while (next <= maxValue)
      {
         values.add(next);
         next = next + interval;
      }
      return values;
   }

   protected Integer processSingleValue(SingleValue singleValue)
   {
      Integer val = this.get(singleValue.getValue());
      this.assertValid(val);
      return val;
   }

   protected Integer get(String alias)
   {
      if (alias == null)
      {
         return null;
      }
      return Integer.parseInt(alias.trim());
   }

   protected void assertValid(Integer value) throws IllegalArgumentException
   {
      if (value == null)
      {
         throw new IllegalArgumentException("Null value in schedule expression");
      }
      int max = this.getMaxValue();
      int min = this.getMinValue();
      if (value > max || value < min)
      {
         throw new IllegalArgumentException("Invalid value: " + value + " Valid values are between " + min + " and "
               + max);
      }
   }
}
