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
package org.jboss.ejb3.timerservice.mk2.task;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.ejb.Timer;

import org.jboss.ejb3.timerservice.mk2.CalendarTimer;
import org.jboss.ejb3.timerservice.mk2.TimerImpl;
import org.jboss.ejb3.timerservice.mk2.TimerServiceImpl;
import org.jboss.ejb3.timerservice.mk2.TimerState;
import org.jboss.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.logging.Logger;

/**
 * A timer task which will be invoked at appropriate intervals based on a {@link Timer}
 * schedule.
 * 
 * <p>
 *  A {@link TimerTask} is responsible for invoking the timeout method on the target, through
 *  the use of {@link TimedObjectInvoker} 
 * </p>
 * <p>
 *  For calendar timers, this {@link TimerTask} is additionally responsible for creating and
 *  scheduling the next round of timer task.
 * </p>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class TimerTask<T extends TimerImpl> implements Runnable
{

   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(TimerTask.class);

   /**
    * The timer to which this {@link TimerTask} belongs
    */
   protected T timer;

   /**
    * {@link TimerServiceImpl} to which this {@link TimerTask} belongs
    */
   protected TimerServiceImpl timerService;

   /**
    * Creates a {@link TimerTask} for the timer
    * @param timer The timer for which this task is being created. 
    * @throws IllegalArgumentException If the passed timer is null
    */
   public TimerTask(T timer)
   {
      if (timer == null)
      {
         throw new IllegalArgumentException("Timer cannot be null");
      }

      this.timer = timer;
      this.timerService = timer.getTimerService();
   }

   /**
    * Invokes the timeout method through the {@link TimedObjectInvoker} corresponding
    * to the {@link TimerImpl} to which this {@link TimerTask} belongs.
    * <p>
    *   This method also sets other attributes on the {@link TimerImpl} including the 
    *   next timeout of the timer and the timer state. 
    * </p>
    * <p>
    *   Additionally, for calendar timers, this method even schedules the next timeout timer task
    *   before calling the timeout method for the current timeout.
    * </p>
    */
   @Override
   public void run()
   {
      logger.debug("run: " + this.timer);
      // set the current date as the "previous run" of the timer. 
      this.timer.setPreviousRun(new Date());
      long intervalDuration = this.timer.getInterval();
      if (this.timer.isActive())
      {
         // if it's a calendar timer or a timer for repeated intervals,
         // then compute the next timeout date
         if (this.timer.isCalendarTimer())
         {
            Date nextExpiration = this.timer.getNextTimeout();
            Calendar cal = new GregorianCalendar();
            cal.setTime(nextExpiration);
            // compute the next timeout date
            Calendar nextTimeout = ((CalendarTimer) this.timer).getCalendarTimeout().getNextTimeout(cal);
            this.timer.setNextTimeout(nextTimeout.getTime());
         }
         else if (intervalDuration > 0)
         {
            Date nextExpiration = this.timer.getNextTimeout();
            // compute the next timeout date
            nextExpiration = new Date(nextExpiration.getTime() + intervalDuration);
            this.timer.setNextTimeout(nextExpiration);
         }
      }
      // persist changes
      this.timerService.persistTimer(this.timer);

      // If a retry thread is in progress, we don't want to allow another
      // interval to execute until the retry is complete. See JIRA-1926.
      if (this.timer.isInRetry())
      {
         logger.debug("Timer in retry mode, skipping this scheduled execution");
         return;
      }

      if (this.timer.isActive())
      {
         try
         {
            // change the state to mark it as in timeout method
            this.timer.setTimerState(TimerState.IN_TIMEOUT);
            // persist changes
            this.timerService.persistTimer(this.timer);
            // invoke timeout
            this.handleTimeout();
         }
         catch (Exception e)
         {
            logger.error("Error invoking ejbTimeout", e);
         }
         finally
         {
            TimerState timerState = this.timer.getState();
            if (timerState == TimerState.IN_TIMEOUT)
            {
               // if it's not a calendar timer and it's not scheduled at
               // repeated intervals, then expire the timer. 
               if (intervalDuration == 0 && this.timer.isCalendarTimer() == false)
               {
                  this.timer.expireTimer();
               }
               else
               {
                  this.timer.setTimerState(TimerState.ACTIVE);
                  // persist changes
                  timerService.persistTimer(this.timer);
               }
            }
         }
      }
   }

   protected void handleTimeout() throws Exception
   {
      this.timerService.getInvoker().callTimeout(this.timer);
   }

   protected T getTimer()
   {
      return this.timer;
   }
}
