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
package org.jboss.ejb3.timerservice.mk2.test.simple.unit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Semaphore;

import javax.ejb.Timer;
import javax.ejb.TimerService;

import org.jboss.ejb3.timerservice.mk2.test.common.AbstractTimerTestCase;
import org.jboss.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.ejb3.timerservice.spi.TimerServiceFactory;
import org.junit.Test;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class SimpleTimerTestCase extends AbstractTimerTestCase
{
   @Test
   public void test1() throws Exception
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
      
      TimerServiceFactory factory = getBean("TimerServiceFactory", TimerServiceFactory.class);
      TimerService service = factory.createTimerService(invoker);
      service.createTimer(500, null);
      
      boolean success = semaphore.tryAcquire(5, SECONDS);
      
      assertTrue("timeout failed", success);
   }
}
