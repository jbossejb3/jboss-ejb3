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
package org.jboss.ejb3.timerservice.integration.test.autotimer;

import java.util.Date;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.ejb.Timer;

import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.logging.Logger;

/**
 * SimpleSLSBAutoTimer
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Stateless
@Remote(SLSBAutoTimer.class)
@RemoteBinding(jndiBinding = SimpleSLSBAutoTimerBean.JNDI_NAME)
public class SimpleSLSBAutoTimerBean implements SLSBAutoTimer
{

   private static Logger logger = Logger.getLogger(SimpleSLSBAutoTimerBean.class);

   public static final String JNDI_NAME = "SLSBAutoTimer";

   @EJB
   private TimeoutTrackerForSLSB timeoutTracker;

   @Schedule(second = "*/5", minute = "*", hour = "*")
   public void scheduleEvery5Second()
   {
      Date now = new Date();
      this.timeoutTracker.trackTimeout("scheduleEvery5Second", now);

   }

   @Schedule(second = "0, 4, 8, 12, 16, 20, 24, 28, 32, 36,40, 44, 48, 52, 56", minute = "*", hour = "*")
   public void scheduleEvery4Seconds(Timer timer)
   {
      Date now = new Date();
      this.timeoutTracker.trackTimeout("scheduleEvery4Seconds", now);
      int numTimeouts = this.timeoutTracker.getNumberOfTimeouts("scheduleEvery4Seconds");
      if (numTimeouts == 2)
      {
         logger.info("Cancelling timer " + timer);
         timer.cancel();
      }
   }

   @Override
   public int getNumTimeoutsForEvery4SecTimer()
   {
      return this.timeoutTracker.getNumberOfTimeouts("scheduleEvery4Seconds");
   }

   @Override
   public int getNumTimeoutsForEvery5SecTimer()
   {
      return this.timeoutTracker.getNumberOfTimeouts("scheduleEvery5Second");
   }
   
   
}
