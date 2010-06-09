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
package org.jboss.ejb3.timerservice.integration.test.calendar;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.ScheduleExpression;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.logging.Logger;

/**
 * CalendarSchedulerImpl
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Stateless
@Remote(CalendarScheduler.class)
@RemoteBinding (jndiBinding = CalendarSchedulerImpl.JNDI_NAME)
public class CalendarSchedulerImpl implements CalendarScheduler
{

   private static Logger logger = Logger.getLogger(CalendarSchedulerImpl.class);

   public static final String JNDI_NAME = "CalendarSchedulerTestBean";

   @Resource
   private TimerService timerService;

   @EJB
   private TimeoutTracker timeoutTracker;

   @Override
   public void schedule(ScheduleExpression schedule)
   {
      this.timerService.createCalendarTimer(schedule);

   }

   @Override
   public void schedule(ScheduleExpression schedule, int maxTimeouts)
   {
      TimerConfig timerConfig = new TimerConfig();
      timerConfig.setInfo(maxTimeouts);
      timerConfig.setPersistent(true);
      this.timerService.createCalendarTimer(schedule, timerConfig);

   }

   @Timeout
   public void timeout(Timer timer)
   {
      Date now = new Date();
      logger.info("Timeout called on bean " + this + " for timer " + timer + " at " + now);
      this.timeoutTracker.trackTimeout(timer, now);

      int numberOfTimeouts = this.timeoutTracker.getTimeoutCount();
      Integer maxTimeouts = (Integer) timer.getInfo();
      logger.debug("Number of timeouts for timer " + timer + " on bean " + this + " is " + numberOfTimeouts
            + ", max allowed = " + maxTimeouts);
      if (maxTimeouts != null && numberOfTimeouts == maxTimeouts)
      {
         logger.info("Cancelling timer " + timer + " on bean " + this);
         timer.cancel();
      }
   }
   
   @Override
   public int getTimeoutCount()
   {
      return this.timeoutTracker.getTimeoutCount();
   }
   
   @Override
   public List<Date> getTimeouts()
   {
      return this.timeoutTracker.getTimeouts();
   }

}
