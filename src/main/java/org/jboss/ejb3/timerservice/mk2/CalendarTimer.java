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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJBException;
import javax.ejb.NoMoreTimeoutsException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;

import org.jboss.ejb3.timer.schedule.CalendarBasedTimeout;
import org.jboss.logging.Logger;

/**
 * CalendarTimer
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class CalendarTimer extends TimerImpl
{

   private CalendarBasedTimeout calendarTimeout;

   private static Logger logger = Logger.getLogger(CalendarTimer.class);

   public CalendarTimer(UUID id, TimerServiceImpl timerService, CalendarBasedTimeout calendarTimeout)
   {
      this(id, timerService, calendarTimeout, null, true);
   }

   public CalendarTimer(UUID id, TimerServiceImpl timerService, CalendarBasedTimeout calendarTimeout,
         Serializable info, boolean persistent)
   {
      super(id, timerService, calendarTimeout.getFirstTimeout().getTime(), 0, info, persistent);
      this.calendarTimeout = calendarTimeout;
   }

   @Override
   public void cancel() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      // TODO Auto-generated method stub

   }

   @Override
   public Date getNextTimeout() throws IllegalStateException, NoMoreTimeoutsException, NoSuchObjectLocalException,
         EJBException
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public ScheduleExpression getSchedule() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      this.assertTimerState();
      return this.calendarTimeout.getScheduleExpression();
   }

   @Override
   public long getTimeRemaining() throws IllegalStateException, NoMoreTimeoutsException, NoSuchObjectLocalException,
         EJBException
   {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public boolean isCalendarTimer() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      this.assertTimerState();
      return true;
   }

   @Override
   protected void scheduleTimeout()
   {
      Runnable timeoutTask = new CalendarTimerTimeoutTask();
      long delay = this.nextExpiration.getTime() - System.currentTimeMillis();
      // if the timeout is in past, then trigger "now"
      if (delay < 0)
      {
         delay = 0;
      }
      // schedule a one-shot task
      future = timerService.getExecutor().schedule(timeoutTask, delay, TimeUnit.MILLISECONDS);
   }

   private class CalendarTimerTimeoutTask implements Runnable
   {

      @Override
      public void run()
      {

         logger.debug("run: " + CalendarTimer.this);
         CalendarTimer.this.previousRun = new Date();

         // If a retry thread is in progress, we don't want to allow another
         // interval to execute until the retry is complete. See JIRA-1926.
         if (isInRetry())
         {
            logger.debug("Timer in retry mode, skipping this scheduled execution");
            return;
         }

         if (isActive())
         {
            try
            {
               setTimerState(TimerState.IN_TIMEOUT);
               // calculate next timeout and schedule a new task for next timeout
               Calendar next = CalendarTimer.this.calendarTimeout.getNextTimeout(new GregorianCalendar());
               CalendarTimer.this.nextExpiration = next.getTime();
               // persist changes
               timerService.persistTimer(CalendarTimer.this);
               CalendarTimer.this.scheduleTimeout();
               // invoke the timeout method
               // TODO: This and the schedule of the next timeout should happen
               // independently (separate threads)
               timedObjectInvoker.callTimeout(CalendarTimer.this);
            }
            catch (Exception e)
            {
               logger.error("Error invoking ejbTimeout", e);
            }
            finally
            {
               if (timerState == TimerState.IN_TIMEOUT)
               {
                  logger.debug("Timer was not registered with Tx, resetting state: " + CalendarTimer.this);
                  if (intervalDuration == 0)
                  {
                     setTimerState(TimerState.EXPIRED);
                     killTimer();
                  }
                  else
                  {
                     setTimerState(TimerState.ACTIVE);
                     // persist changes
                     timerService.persistTimer(CalendarTimer.this);
                  }
               }
            }
         }

      }
   }

}
