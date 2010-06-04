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
import java.util.UUID;

import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;

import org.jboss.ejb3.timer.schedule.CalendarBasedTimeout;
import org.jboss.ejb3.timerservice.mk2.persistence.CalendarTimerEntity;
import org.jboss.ejb3.timerservice.mk2.persistence.TimerEntity;
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
   
   public CalendarTimer(CalendarTimerEntity persistedCalendarTimer, TimerServiceImpl timerService)
   {
      super(persistedCalendarTimer, timerService);
      this.calendarTimeout = persistedCalendarTimer.getCalendarTimeout();
   }

   @Override
   public ScheduleExpression getSchedule() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      this.assertTimerState();
      return this.calendarTimeout.getScheduleExpression();
   }

   @Override
   public boolean isCalendarTimer() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      this.assertTimerState();
      return true;
   }

   
   @Override
   protected TimerEntity createPersistentState()
   {
      return new CalendarTimerEntity(this);
   }
   
   public CalendarBasedTimeout getCalendarTimeout()
   {
      return this.calendarTimeout;
   }
   
}
