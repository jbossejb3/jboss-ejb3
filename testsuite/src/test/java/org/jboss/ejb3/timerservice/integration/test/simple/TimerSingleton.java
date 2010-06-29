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
package org.jboss.ejb3.timerservice.integration.test.simple;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;

import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.logging.Logger;

/**
 * TimerSingleton
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
@Remote (TimerUtil.class)
@RemoteBinding (jndiBinding = TimerSingleton.JNDI_NAME)
public class TimerSingleton implements TimerUtil
{
   private static Logger logger = Logger.getLogger(TimerSingleton.class);

   public static final String JNDI_NAME = "TimerSFSBRemote";
   
   @Resource
   private TimerService timerService;
   
   private TimeoutTracker timeoutTracker = new TimeoutTracker();
   
   @Override
   public void createTimer(Date firstExpiration, long interval, int maxTimeouts)
   {
      ScheduleExpression expr = new ScheduleExpression();
      expr.second("*/5");
      expr.minute("*");
      expr.hour("*");
      this.timerService.createCalendarTimer(expr, new TimerConfig());
    
   }
   
   @Timeout
   private void timeout(Timer timer)
   {
      logger.info("Timeout method called for timer " + timer + " on bean " + this + " at " + new Date());
      logger.info("Timer service is " + this.timerService.toString());
      if (true)
      {
         throw new NullPointerException();
      }
      this.timeoutTracker.trackTimeout(timer);
      int numTimeouts = timeoutTracker.getTimeoutCount();
      Integer maxTimeouts = (Integer) timer.getInfo();
      logger.debug("Number of timeouts = " + numTimeouts + " max allowed = " + maxTimeouts);
      if (numTimeouts == maxTimeouts)
      {
         logger.info("Cancelling timer " + timer + " " + " for bean " + this);
         timer.cancel();
      }
   }
   
   @Override
   public TimeoutTracker getTimeoutTracker()
   {
      return this.timeoutTracker;
   }
}
