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
package org.jboss.ejb3.timerservice.integration.test.disallowed;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Remote;
import javax.ejb.ScheduleExpression;
import javax.ejb.Stateless;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.jboss.ejb3.annotation.RemoteBinding;

/**
 * SimpleSLSB
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Stateless
@Remote(Echo.class)
@RemoteBinding(jndiBinding = SimpleSLSB.JNDI_NAME)
public class SimpleSLSB implements Echo
{

   public static final String JNDI_NAME = "UselessBean";

   @Resource
   private TimerService timerService;

   @PostConstruct
   public void postConstruct()
   {
      Date now = new Date();
      ScheduleExpression schedule = new ScheduleExpression();

      // All these method invocation on timerservice aren't allowed in lifecycle
      // callback on SLSB. They are expected to throw IllegalStateException, which
      // is what we are testing

      try
      {
         this.timerService.createCalendarTimer(schedule);
         throw new EJBException("TimerService.createCalendarTimer was allowed in @Postconstruct of "
               + this.getClass().getName() + " bean");
      }
      catch (IllegalStateException ise)
      {
         // expected
      }

      try
      {
         this.timerService.createCalendarTimer(schedule, new TimerConfig());
         throw new EJBException("TimerService.createCalendarTimer was allowed in @Postconstruct of "
               + this.getClass().getName() + " bean");
      }
      catch (IllegalStateException ise)
      {
         // expected
      }

      try
      {
         this.timerService.createIntervalTimer(now, 0, new TimerConfig());
         throw new EJBException("TimerService.createIntervalTimer was allowed in @Postconstruct of "
               + this.getClass().getName() + " bean");
      }
      catch (IllegalStateException ise)
      {
         // expected
      }

      try
      {
         this.timerService.createIntervalTimer(10, 0, new TimerConfig());
         throw new EJBException("TimerService.createIntervalTimer was allowed in @Postconstruct of "
               + this.getClass().getName() + " bean");
      }
      catch (IllegalStateException ise)
      {
         // expected
      }

      try
      {
         this.timerService.createSingleActionTimer(now, new TimerConfig());
         throw new EJBException("TimerService.createSingleActionTimer was allowed in @Postconstruct of "
               + this.getClass().getName() + " bean");
      }
      catch (IllegalStateException ise)
      {
         // expected
      }

      try
      {
         this.timerService.createSingleActionTimer(10, new TimerConfig());
         throw new EJBException("TimerService.createSingleActionTimer was allowed in @Postconstruct of "
               + this.getClass().getName() + " bean");
      }
      catch (IllegalStateException ise)
      {
         // expected
      }

      try
      {
         this.timerService.createTimer(10, null);
         throw new EJBException("TimerService.createTimer was allowed in @Postconstruct of "
               + this.getClass().getName() + " bean");
      }
      catch (IllegalStateException ise)
      {
         // expected
      }

      try
      {
         this.timerService.createTimer(now, 0, null);
         throw new EJBException("TimerService.createTimer was allowed in @Postconstruct of "
               + this.getClass().getName() + " bean");
      }
      catch (IllegalStateException ise)
      {
         // expected
      }

      try
      {
         this.timerService.createTimer(10, 0, null);
         throw new EJBException("TimerService.createTimer was allowed in @Postconstruct of "
               + this.getClass().getName() + " bean");
      }
      catch (IllegalStateException ise)
      {
         // expected
      }

      try
      {
         this.timerService.createTimer(now, null);
         throw new EJBException("TimerService.createTimer was allowed in @Postconstruct of "
               + this.getClass().getName() + " bean");
      }
      catch (IllegalStateException ise)
      {
         // expected
      }

      try
      {
         this.timerService.getTimers();
         throw new EJBException("TimerService.getTimers was allowed in @Postconstruct of " + this.getClass().getName()
               + " bean");
      }
      catch (IllegalStateException ise)
      {
         // expected
      }
   }

   @Override
   public String echo(String msg)
   {
      return msg;
   }

}
