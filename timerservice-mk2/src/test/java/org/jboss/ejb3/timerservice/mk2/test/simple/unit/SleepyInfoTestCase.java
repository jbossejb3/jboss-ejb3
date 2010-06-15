/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.ejb3.timerservice.mk2.test.simple.unit;

import org.jboss.ejb3.timerservice.mk2.test.common.AbstractTimerTestCase;
import org.jboss.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.ejb3.timerservice.spi.TimerServiceFactory;
import org.junit.Test;

import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.transaction.Status;
import javax.transaction.TransactionManager;
import java.io.Serializable;
import java.util.concurrent.Semaphore;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SleepyInfoTestCase extends AbstractTimerTestCase
{
   static class Sleepy implements Serializable
   {
      private boolean asleep = true;

      public void awake()
      {
         asleep = false;
      }

      @Override
      public boolean equals(Object o)
      {
         if (asleep) throw new IllegalStateException("still asleep");
         
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         Sleepy sleepy = (Sleepy) o;

         if (asleep != sleepy.asleep) return false;

         return true;
      }

      @Override
      public int hashCode()
      {
         return (asleep ? 1 : 0);
      }
   }

   @Test
   public void testSleepy() throws Exception
   {
      final Semaphore semaphore = new Semaphore(0);
      TimedObjectInvoker invoker = new TimedObjectInvoker() {
         public void callTimeout(Timer timer) throws Exception
         {
            semaphore.release();
         }

         public String getTimedObjectId()
         {
            return "test";
         }
      };

      TransactionManager tm = getBeanByType(TransactionManager.class);
      
      TimerServiceFactory factory = getBeanByType(TimerServiceFactory.class);
      TimerService service = factory.createTimerService(invoker);

      tm.begin();
      try
      {
         Sleepy sleepy = new Sleepy();
         service.createTimer(500, sleepy);
         sleepy.awake();
         tm.commit();
      }
      finally
      {
         if(tm.getStatus() == Status.STATUS_ACTIVE)
            tm.rollback();
      }

      boolean success = semaphore.tryAcquire(5, SECONDS);

      assertTrue("timeout failed", success);
   }
}
