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

import javax.ejb.Remote;
import javax.ejb.Schedule;
import javax.ejb.Schedules;
import javax.ejb.Singleton;
import javax.ejb.Timer;

import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.logging.Logger;

/**
 * SingletonAutoTimerBean
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
@Remote(AutoTimerTacker.class)
@RemoteBinding(jndiBinding = SingletonAutoTimerBean.JNDI_NAME)
public class SingletonAutoTimerBean implements AutoTimerTacker
{

   private static Logger logger = Logger.getLogger(SingletonAutoTimerBean.class);

   public static final String JNDI_NAME = "AutoTimerTestBean";

   public static final String INFO_EVERY_10_SEC = "Every 10 Sec";

   public static final String INFO_EVERY_MINUTE = "Every Minute";

   private int numTimeoutsForEvery10SecTimer;

   private int numTimeoutsForEveryMinute;

   @Schedules(
   {@Schedule(second = "*/10", minute = "*", hour = "*", info = INFO_EVERY_10_SEC),
         @Schedule(minute = "*/1", hour = "*", info = INFO_EVERY_MINUTE)})
   public void timeout(Timer timer)
   {
      Date now = new Date();
      logger.info("Timeout method invoked for timer " + timer + " at " + now);
      if (timer.getInfo().equals(INFO_EVERY_10_SEC))
      {
         logger.info("Timer " + timer + " is every 10 second timer");
         numTimeoutsForEvery10SecTimer++;
         if (numTimeoutsForEvery10SecTimer == 3)
         {
            logger.info("Cancelling  every 10 second timer: " + timer);
            timer.cancel();
         }
      }
      else if (timer.getInfo().equals(INFO_EVERY_MINUTE))
      {
         logger.info("Timer " + timer + " is every 1 minute timer");
         numTimeoutsForEveryMinute++;
         if (numTimeoutsForEveryMinute == 1)
         {
            logger.info("Cancelling  every 1 minute timer: " + timer);
            timer.cancel();
         }
      }
   }

   @Override
   public int getNumTimeoutsForEvery10SecTimer()
   {
      return this.numTimeoutsForEvery10SecTimer;
   }

   @Override
   public int getNumTimeoutsForEveryMinuteTimer()
   {
      return this.numTimeoutsForEveryMinute;
   }
}
