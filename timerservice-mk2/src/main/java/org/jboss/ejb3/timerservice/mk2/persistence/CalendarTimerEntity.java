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
package org.jboss.ejb3.timerservice.mk2.persistence;

import java.lang.reflect.Method;
import java.util.Date;

import javax.ejb.ScheduleExpression;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.jboss.ejb3.timer.schedule.CalendarBasedTimeout;
import org.jboss.ejb3.timerservice.mk2.CalendarTimer;

/**
 * CalendarTimerEntity
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Entity
@Table(name = "Calendar_Timer")
@Inheritance(strategy = InheritanceType.JOINED)
public class CalendarTimerEntity extends TimerEntity
{

   @Transient
   private ScheduleExpression scheduleExpression;

   @Transient
   private CalendarBasedTimeout calendarTimeout;

   private String second;

   private String minute;

   private String hour;

   private String dayOfWeek;

   private String dayOfMonth;

   private String month;

   private String year;

   private Date startDate;

   private Date endDate;

   private String timezone;

   private boolean autoTimer;

   @OneToOne(cascade = CascadeType.ALL)
   private TimeoutMethod timeoutMethod;

   public CalendarTimerEntity()
   {

   }

   public CalendarTimerEntity(CalendarTimer calendarTimer)
   {
      super(calendarTimer);
      this.scheduleExpression = calendarTimer.getScheduleExpression();
      this.autoTimer = calendarTimer.isAutoTimer();
      if (calendarTimer.isAutoTimer())
      {
         Method method = calendarTimer.getTimeoutMethod();
         Class<?>[] methodParams = method.getParameterTypes();
         String[] params = new String[methodParams.length];
         for (int i = 0; i < methodParams.length; i++)
         {
            params[i] = methodParams[i].getName();
         }
         this.timeoutMethod = new TimeoutMethod(method.getDeclaringClass().getName(), method.getName(), params);
      }

      this.second = this.scheduleExpression.getSecond();
      this.minute = this.scheduleExpression.getMinute();
      this.hour = this.scheduleExpression.getHour();
      this.dayOfMonth = this.scheduleExpression.getDayOfMonth();
      this.month = this.scheduleExpression.getMonth();
      this.dayOfWeek = this.scheduleExpression.getDayOfWeek();
      this.year = this.scheduleExpression.getYear();
      this.startDate = this.scheduleExpression.getStart();
      this.endDate = this.scheduleExpression.getEnd();
      this.timezone = this.scheduleExpression.getTimezone();

   }

   @Override
   public boolean isCalendarTimer()
   {
      return true;
   }

   public ScheduleExpression getScheduleExpression()
   {
      if (this.scheduleExpression == null)
      {
         this.scheduleExpression = new ScheduleExpression();
         this.scheduleExpression.second(this.second).minute(this.minute).hour(this.hour).dayOfWeek(this.dayOfWeek)
               .dayOfMonth(this.dayOfMonth).month(this.month).year(this.year).timezone(this.timezone);

      }
      return scheduleExpression;
   }

   public CalendarBasedTimeout getCalendarTimeout()
   {
      if (this.calendarTimeout == null)
      {
         this.calendarTimeout = new CalendarBasedTimeout(this.getScheduleExpression());
      }
      return this.calendarTimeout;
   }

   public String getSecond()
   {
      return second;
   }

   public String getMinute()
   {
      return minute;
   }

   public String getHour()
   {
      return hour;
   }

   public String getDayOfWeek()
   {
      return dayOfWeek;
   }

   public String getDayOfMonth()
   {
      return dayOfMonth;
   }

   public String getMonth()
   {
      return month;
   }

   public String getYear()
   {
      return year;
   }

   public Date getStartDate()
   {
      return startDate;
   }

   public void setStartDate(Date start)
   {
      this.startDate = start;
   }

   public Date getEndDate()
   {
      return endDate;
   }

   public void setEndDate(Date end)
   {
      this.endDate = end;
   }

   public TimeoutMethod getTimeoutMethod()
   {
      return timeoutMethod;
   }

   public void setTimeoutMethod(TimeoutMethod timeoutMethod)
   {
      this.timeoutMethod = timeoutMethod;
   }

   public boolean isAutoTimer()
   {
      return autoTimer;
   }

   public void setAutoTimer(boolean autoTimer)
   {
      this.autoTimer = autoTimer;
   }

   public String getTimezone()
   {
      return timezone;
   }

   public void setTimezone(String timezone)
   {
      this.timezone = timezone;
   }
   
   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
      {
         return false;
      }
      if (obj instanceof CalendarTimerEntity == false)
      {
         return false;
      }
      CalendarTimerEntity other = (CalendarTimerEntity) obj;
      if (this.id == null)
      {
         return false;
      }
      return this.id.equals(other.id);
   }
   
   @Override
   public int hashCode()
   {
      if (this.id == null)
      {
         return super.hashCode();
      }
      return this.id.hashCode();
   }

}
