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
package org.jboss.ejb3.timerservice.mk2;

import java.io.Serializable;
import java.util.Calendar;
import java.util.UUID;

import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;

import org.jboss.ejb3.timer.schedule.CalendarBasedTimeout;
import org.jboss.ejb3.timerservice.mk2.persistence.CalendarTimerEntity;
import org.jboss.ejb3.timerservice.mk2.persistence.TimeoutMethod;
import org.jboss.ejb3.timerservice.mk2.persistence.TimerEntity;
import org.jboss.ejb3.timerservice.mk2.task.CalendarTimerTask;
import org.jboss.ejb3.timerservice.mk2.task.TimerTask;
import org.jboss.logging.Logger;

/**
 * Represents a {@link Timer} which is created out a calendar expression
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class CalendarTimer extends TimerImpl
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(CalendarTimer.class);

   /**
    * The calendar based timeout for this timer
    */
   private CalendarBasedTimeout calendarTimeout;

   /**
    * Represents whether this is an auto-timer or a normal
    * programatically created timer
    */
   private boolean autoTimer;

   /**
    * This represents the timeout method name for auto-timers.
    * <p>
    *   If this isn't an auto-timer, then this {@link #timeoutMethodName} will 
    *   be null 
    * </p>
    */
   private String timeoutMethodName;

   /**
    * Represents the timeout method parameters for an auto-timer.
    * 
    * <p>
    *   If this isn't an auto-timer, then this {@link #timeoutMethodParams} will 
    *   be null
    * </p>
    */
   private String[] timeoutMethodParams;

   /**
    * Constructs a {@link CalendarTimer}
    * 
    * @param id The id of this timer
    * @param timerService The timer service to which this timer belongs
    * @param calendarTimeout The {@link CalendarBasedTimeout} from which this {@link CalendarTimer} is being created
    */
   public CalendarTimer(UUID id, TimerServiceImpl timerService, CalendarBasedTimeout calendarTimeout)
   {
      this(id, timerService, calendarTimeout, null, true);
   }

   /**
    * Constructs a {@link CalendarTimer}
    * 
    * @param id The id of this timer
    * @param timerService The timer service to which this timer belongs
    * @param calendarTimeout The {@link CalendarBasedTimeout} from which this {@link CalendarTimer} is being created
    * @param info The serializable info which will be made available through {@link Timer#getInfo()}
    * @param persistent True if this timer is persistent. False otherwise
    */
   public CalendarTimer(UUID id, TimerServiceImpl timerService, CalendarBasedTimeout calendarTimeout,
         Serializable info, boolean persistent)
   {
      this(id, timerService, calendarTimeout, info, persistent, null, null);
   }

   /**
    * Constructs a {@link CalendarTimer}
    * 
    * @param id The id of this timer
    * @param timerService The timer service to which this timer belongs
    * @param calendarTimeout The {@link CalendarBasedTimeout} from which this {@link CalendarTimer} is being created
    * @param info The serializable info which will be made available through {@link Timer#getInfo()}
    * @param persistent True if this timer is persistent. False otherwise
    * @param timeoutMethodName If this is a non-null value, then this {@link CalendarTimer} is marked as an auto-timer.
    *               This <code>timeoutMethodName</code> is then considered as the name of the timeout method which has to
    *               be invoked when this timer times out.
    * @param timeoutMethodParams The timeout method params. Can be null. This param value will only be used if the
    *           <code>timeoutMethodName</code> is not null
    */
   public CalendarTimer(UUID id, TimerServiceImpl timerService, CalendarBasedTimeout calendarTimeout,
         Serializable info, boolean persistent, String timeoutMethodName, String[] timeoutMethodParams)
   {
      super(id, timerService, calendarTimeout.getFirstTimeout().getTime(), 0, info, persistent);
      this.calendarTimeout = calendarTimeout;

      // compute the next timeout (from "now")
      Calendar nextTimeout = this.calendarTimeout.getNextTimeout();
      if (nextTimeout != null)
      {
         this.nextExpiration = nextTimeout.getTime();
      }
      // set this as an auto-timer if the passed timeout method name 
      // is not null
      if (timeoutMethodName != null)
      {
         this.autoTimer = true;
         this.timeoutMethodName = timeoutMethodName;
         this.timeoutMethodParams = timeoutMethodParams;
      }
   }

   /**
    * Constructs a {@link CalendarTimer} from a persistent state 
    * 
    * @param persistedCalendarTimer The persistent state of the calendar timer
    * @param timerService The timer service to which this timer belongs
    */
   public CalendarTimer(CalendarTimerEntity persistedCalendarTimer, TimerServiceImpl timerService)
   {
      super(persistedCalendarTimer, timerService);
      this.calendarTimeout = persistedCalendarTimer.getCalendarTimeout();
      // set the next expiration (which will be available in the persistent state)
      this.nextExpiration = persistedCalendarTimer.getNextDate();
      // auto-timer related attributes
      if (persistedCalendarTimer.isAutoTimer())
      {
         this.autoTimer = true;
         TimeoutMethod timeoutMethod = persistedCalendarTimer.getTimeoutMethod();
         this.timeoutMethodName = timeoutMethod.getMethodName();
         this.timeoutMethodParams = timeoutMethod.getMethodParams();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ScheduleExpression getSchedule() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      this.assertTimerState();
      return this.calendarTimeout.getScheduleExpression();
   }

   /**
    * {@inheritDoc}
    * 
    */
   @Override
   public boolean isCalendarTimer() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      this.assertTimerState();
      return true;
   }

   /**
    * Creates and return a new persistent state of this timer
    */
   @Override
   protected TimerEntity createPersistentState()
   {
      return new CalendarTimerEntity(this);
   }

   /**
    * Returns the {@link CalendarBasedTimeout} corresponding to this 
    * {@link CalendarTimer} 
    * @return
    */
   public CalendarBasedTimeout getCalendarTimeout()
   {
      return this.calendarTimeout;
   }

   /**
    * Returns true if this is an auto-timer. Else returns false.
    */
   @Override
   public boolean isAutoTimer()
   {
      return autoTimer;
   }

   /**
    * Returns the timeout method if this is an auto-timer.
    * @return
    * @throws IllegalStateException If this is not an auto-timer
    */
   public String getTimeoutMethod()
   {
      if (this.autoTimer == false)
      {
         throw new IllegalStateException("Cannot invoke getTimeoutMethod on a timer which is not an auto-timer");
      }
      return this.timeoutMethodName;
   }

   /**
    * Returns the timeout method params, if this is an auto-timer
    * @return
    * @throws IllegalStateException If this is not an auto-timer
    */
   public String[] getTimeoutMethodParams()
   {
      if (this.autoTimer == false)
      {
         throw new IllegalStateException("Cannot invoke getTimeoutMethodParams on a timer which is not an auto-timer");
      }
      return this.timeoutMethodParams;
   }

   /**
    * Returns the task which handles the timeouts on this {@link CalendarTimer}
    * 
    * @see CalendarTimerTask
    */
   @Override
   protected TimerTask<?> getTimerTask()
   {
      return new CalendarTimerTask(this);
   }

}
