/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.test.pool;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;

import org.jboss.ejb3.annotation.RemoteBinding;

@Stateless
@Remote(ExampleTimer.class)
@RemoteBinding(jndiBinding="ExampleTimer")
public class ExampleTimerBean implements ExampleTimer
{
   private static int instanceCount = 0;

   private @Resource SessionContext ctx;

   public void scheduleTimer(long milliseconds)
   {
      ctx.getTimerService().createTimer(
            new Date(new Date().getTime() + milliseconds), "Hello World");
   }

   @Timeout
   public void timeoutHandler(Timer timer)
   {
      timer.cancel();

      ctx.getTimerService().createTimer(new Date(new Date().getTime() + 50),
            "Hello World");
   }

   @PostConstruct
   public void postConstruct()
   {
      instanceCount++;
      System.out.println("New instance: " + instanceCount);
   }

   @PreDestroy
   public void preDestroy()
   {
      instanceCount++;
      System.out.println("Instance removed: " + instanceCount);
   }
}
