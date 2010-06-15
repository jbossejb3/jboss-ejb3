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
package org.jboss.ejb3.timerservice.mk2.cancel.unit;

import java.util.Date;
import java.util.UUID;

import javax.ejb.Timer;

import org.jboss.ejb3.timerservice.mk2.TimerImpl;
import org.jboss.ejb3.timerservice.mk2.TimerServiceImpl;
import org.jboss.ejb3.timerservice.spi.TimedObjectInvoker;
import org.junit.Test;

import static org.mockito.Mockito.*;


/**
 * Tests that the cancel operation on {@link Timer} works as expected
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class CancelTimerTestCase
{

   /**
    * Tests that invoking a {@link Timer#cancel()} which has no future schedules,
    * doesn't throw any exceptions
    */
   @Test
   public void cancelUnScheduledTimer()
   {
      // create the mocks
      TimerServiceImpl mockTimerService = mock(TimerServiceImpl.class);
      TimedObjectInvoker mockInvoker = mock(TimedObjectInvoker.class);
      when(mockInvoker.getTimedObjectId()).thenReturn("Dummy");
      when(mockTimerService.getInvoker()).thenReturn(mockInvoker);
      
      // now create the real timer
      TimerImpl timer = new TimerImpl(UUID.randomUUID(), mockTimerService, new Date(), 0, null, false);
      // cancel a timer without starting it (i.e. there are no scheduled tasks) 
      timer.cancel();
   }
}
