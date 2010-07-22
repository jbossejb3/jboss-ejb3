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
package org.jboss.ejb3.timerservice.integration.test.cancel;

import java.util.Collection;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;

import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.logging.Logger;

/**
 * SimpleTimerSLSB
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Stateless
@Remote(SimpleTimer.class)
@RemoteBinding(jndiBinding = SimpleTimerSLSB.JNDI_NAME)
public class SimpleTimerSLSB implements SimpleTimer
{

   private static Logger logger = Logger.getLogger(SimpleTimerSLSB.class);

   public static final String JNDI_NAME = "CancelTimerTestCaseBean";

   @Resource
   private TimerService timerService;

   @EJB
   private TimeoutTracker timeoutTracker;

   @Override
   public void createTimer(long intialDuration, long intervalDurationMillis)
   {
      // cancel existing timers
      cancelTimers();
      logger.info("Creating timer starting at " + intialDuration
            + " milli. sec from now and with a recurring duration  of " + intervalDurationMillis + " milli. sec");
      timerService.createTimer(intialDuration, intervalDurationMillis, "Test Timer");

   }

   @Override
   public void stopTimers()
   {
      cancelTimers();
   }

   @Override
   public boolean timersCreated()
   {
      Collection<Timer> timers = timerService.getTimers();
      logger.info("Number of timers for bean: " + this.getClass().getSimpleName() + " =  " + timers.size());
      return (timers.size() > 0);
   }

   @Override
   public int getTimeoutCount()
   {
      return this.timeoutTracker.getTimeoutCount();
   }

   private void cancelTimers()
   {
      logger.info("Canceling all existing timers: ");
      Collection<Timer> timers = timerService.getTimers();
      for (Timer timer : timers)
      {
         logger.info("Canceling timer: " + timer);
         timer.cancel();

      }
   }

   @Timeout
   public void handleTimeout(Timer timer)
   {
      logger.info("Timeout called on bean: " + this.getClass().getSimpleName() + " for timer " + timer);
      this.timeoutTracker.trackTimeout(timer);
   }

}
