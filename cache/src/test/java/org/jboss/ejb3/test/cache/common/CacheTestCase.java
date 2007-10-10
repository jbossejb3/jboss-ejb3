/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.test.cache.common;

import junit.framework.TestCase;

/**
 * A TestCase helper.
 * 
 * You can either extend it or call the static helper methods directly.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public abstract class CacheTestCase extends TestCase
{
   /**
    * Sleep for the specified number of milliseconds, ignoring
    * any resulting InterruptedException. The method execution
    * is cut short if an InterruptedException occurs.
    *  
    * @param micros the length of time to sleep in milliseconds.    
    */
   public static void sleep(long millis)
   {
      try
      {
         Thread.sleep(millis);
      }
      catch (InterruptedException e)
      {
         // ignore
      }
   }
    
   /**
    * Wait for notification on the object for 5 seconds.
    * 
    * @param obj                    the object to wait on.
    * @throws InterruptedException  if the wait is interrupted.
    */
   public static void wait(Object obj) throws InterruptedException
   {
      synchronized (obj)
      {
         obj.wait(5000);
      }
   }
}
