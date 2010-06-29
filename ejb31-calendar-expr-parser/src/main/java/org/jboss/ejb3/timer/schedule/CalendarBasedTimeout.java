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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.ejb.ScheduleExpression;

import org.jboss.logging.Logger;

/**
 * CalendarBasedTimeout
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class CalendarBasedTimeout
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(CalendarBasedTimeout.class);

   /**
    * The {@link ScheduleExpression} from which this {@link CalendarBasedTimeout}
    * was created
    */
   private ScheduleExpression scheduleExpression;

   /**
    * The {@link Second} created out of the {@link ScheduleExpression#getSecond()} value
    */
   private Second second;

   /**
    * The {@link Minute} created out of the {@link ScheduleExpression#getMinute()} value
    */
   private Minute minute;

   /**
    * The {@link Hour} created out of the {@link ScheduleExpression#getHour()} value
    */
   private Hour hour;

   /**
    * The {@link DayOfWeek} created out of the {@link ScheduleExpression#getDayOfWeek()} value
    */
   private DayOfWeek dayOfWeek;

   /**
    * The {@link DayOfMonth} created out of the {@link ScheduleExpression#getDayOfMonth()} value
    */
   private DayOfMonth dayOfMonth;

   /**
    * The {@link Month} created out of the {@link ScheduleExpression#getMonth()} value
    */
   private Month month;

   /**
    * The {@link Year} created out of the {@link ScheduleExpression#getYear()} value
    */
   private Year year;

   /**
    * The first timeout relative to the time when this {@link CalendarBasedTimeout} was created
    * from a {@link ScheduleExpression} 
    */
   private Calendar firstTimeout;

   /**
    * The timezone being used for this {@link CalendarBasedTimeout}
    */
   private TimeZone timezone;

   /**
    * Creates a {@link CalendarBasedTimeout} from the passed <code>schedule</code>.
    * <p>
    *   This constructor parses the passed {@link ScheduleExpression} and sets up
    *   its internal representation of the same.
    * </p>
    * @param schedule The schedule 
    */
   public CalendarBasedTimeout(ScheduleExpression schedule)
   {
      if (schedule == null)
      {
         throw new IllegalArgumentException("Cannot create " + this.getClass().getName()
               + " from a null schedule expression");
      }
      // make sure that the schedule doesn't have null values for its various attributes
      this.nullCheckScheduleAttributes(schedule);
      
      // store the original expression from which this
      // CalendarBasedTimeout was created. Since the ScheduleExpression
      // is mutable, we will have to store a clone copy of the schedule,
      // so that any subsequent changes after the CalendarBasedTimeout construction,
      // do not affect this internal schedule expression.
      this.scheduleExpression = this.clone(schedule);

      // Start parsing the values in the ScheduleExpression
      this.second = new Second(schedule.getSecond());
      this.minute = new Minute(schedule.getMinute());
      this.hour = new Hour(schedule.getHour());
      this.dayOfWeek = new DayOfWeek(schedule.getDayOfWeek());
      this.dayOfMonth = new DayOfMonth(schedule.getDayOfMonth());
      this.month = new Month(schedule.getMonth());
      this.year = new Year(schedule.getYear());
      if (schedule.getTimezone() != null && schedule.getTimezone().trim().isEmpty() == false)
      {
         // If the timezone ID wasn't valid, then Timezone.getTimeZone returns
         // GMT, which may not always be desirable.
         // So we first check to see if the timezone id specified is available in
         // timezone ids in the system. If it's available then we log a WARN message
         // and fallback on the server's timezone.
         String timezoneId = schedule.getTimezone();
         String[] availableTimeZoneIDs = TimeZone.getAvailableIDs();
         if (availableTimeZoneIDs != null && Arrays.asList(availableTimeZoneIDs).contains(timezoneId))
         {
            this.timezone = TimeZone.getTimeZone(timezoneId);
         }
         else
         {
            logger.warn("Unknown timezone id: " + timezoneId
                  + " found in schedule expression. Ignoring it and using server's timezone: "
                  + TimeZone.getDefault().getID());

            // use server's timezone
            this.timezone = TimeZone.getDefault();
         }
      }
      else
      {
         this.timezone = TimeZone.getDefault();
      }

      // Now that we have parsed the values from the ScheduleExpression,
      // determine and set the first timeout (relative to the current time)
      // of this CalendarBasedTimeout
      this.setFirstTimeout();
   }

   public Calendar getNextTimeout(Calendar current)
   {
      Calendar next = new GregorianCalendar(this.timezone);
      Date start = this.scheduleExpression.getStart();
      if (start != null && current.getTime().before(start))
      {
         next.setTime(start);
      }
      else
      {
         next.setTime(current.getTime());
         // increment the current second by 1
         next.add(Calendar.SECOND, 1);
         
      }
      
      next.setFirstDayOfWeek(Calendar.SUNDAY);


      next = this.second.getNextSecond(next);
      next = this.minute.getNextMinute(next);
      next = this.hour.getNextHour(next);
      next = this.dayOfWeek.getNextDayOfWeek(next);
      next = this.dayOfMonth.getNextDayOfMonth(next);
      next = this.month.getNextMonth(next);
      if (next == null)
      {
         return null;
      }
      next = this.year.getNextYear(next);
      Date end = this.scheduleExpression.getEnd();
      if (next != null && end != null && next.after(end))
      {
         return null;
      }
      return next;
   }

   public Calendar getNextTimeout()
   {
      Calendar now = new GregorianCalendar(this.timezone);
      now.setTime(new Date());

      return this.getNextTimeout(now);
   }

   /**
    * 
    * @return
    */
   public Calendar getFirstTimeout()
   {
      return this.firstTimeout;
   }

   private void setFirstTimeout()
   {
      this.firstTimeout = new GregorianCalendar(this.timezone);
      Date start = this.scheduleExpression.getStart();
      if (start != null)
      {
         this.firstTimeout.setTime(start);
      }
      else
      {
         this.firstTimeout.set(Calendar.SECOND, this.second.getFirst());
         this.firstTimeout.set(Calendar.MINUTE, this.minute.getFirst());
         this.firstTimeout.set(Calendar.HOUR_OF_DAY, this.hour.getFirst());
         //      this.firstTimeout.set(Calendar.DAY_OF_WEEK, this.dayOfWeek.getFirst());
         //      this.firstTimeout.set(Calendar.DAY_OF_MONTH, this.dayOfMonth.getFirst());
         //      this.firstTimeout.set(Calendar.MONTH, this.month.getFirst());
         //      this.firstTimeout.set(Calendar.YEAR, this.year.getFirst());
      }
      this.firstTimeout.setFirstDayOfWeek(Calendar.SUNDAY);

      this.firstTimeout = this.second.getNextSecond(this.firstTimeout);
      this.firstTimeout = this.minute.getNextMinute(this.firstTimeout);
      this.firstTimeout = this.hour.getNextHour(this.firstTimeout);
      this.firstTimeout = this.dayOfWeek.getNextDayOfWeek(this.firstTimeout);
      this.firstTimeout = this.dayOfMonth.getNextDayOfMonth(this.firstTimeout);
      this.firstTimeout = this.month.getNextMonth(this.firstTimeout);
      if (this.firstTimeout != null)
      {
         this.firstTimeout = this.year.getNextYear(this.firstTimeout);
      }

   }

   /**
    * Returns the original {@link ScheduleExpression} from which this {@link CalendarBasedTimeout}
    * was created.
    * 
    * @return
    */
   public ScheduleExpression getScheduleExpression()
   {
      return this.scheduleExpression;
   }

   private void nullCheckScheduleAttributes(ScheduleExpression schedule)
   {
      if (schedule.getSecond() == null)
      {
         throw new IllegalArgumentException("Second cannot be null in schedule expression " + schedule);
      }
      if (schedule.getMinute() == null)
      {
         throw new IllegalArgumentException("Minute cannot be null in schedule expression " + schedule);
      }
      if (schedule.getHour() == null)
      {
         throw new IllegalArgumentException("Hour cannot be null in schedule expression " + schedule);
      }
      if (schedule.getDayOfMonth() == null)
      {
         throw new IllegalArgumentException("day-of-month cannot be null in schedule expression " + schedule);
      }
      if (schedule.getDayOfWeek() == null)
      {
         throw new IllegalArgumentException("day-of-week cannot be null in schedule expression " + schedule);
      }
      if (schedule.getMonth() == null)
      {
         throw new IllegalArgumentException("Month cannot be null in schedule expression " + schedule);
      }
      if (schedule.getYear() == null)
      {
         throw new IllegalArgumentException("Year cannot be null in schedule expression " + schedule);
      }
   }

   private ScheduleExpression clone(ScheduleExpression schedule)
   {
      // clone the schedule 
      ScheduleExpression clonedSchedule = new ScheduleExpression();
      clonedSchedule.second(schedule.getSecond());
      clonedSchedule.minute(schedule.getMinute());
      clonedSchedule.hour(schedule.getHour());
      clonedSchedule.dayOfWeek(schedule.getDayOfWeek());
      clonedSchedule.dayOfMonth(schedule.getDayOfMonth());
      clonedSchedule.month(schedule.getMonth());
      clonedSchedule.year(schedule.getYear());
      clonedSchedule.timezone(schedule.getTimezone());
      clonedSchedule.start(schedule.getStart());
      clonedSchedule.end(schedule.getEnd());

      return clonedSchedule;
   }

}
