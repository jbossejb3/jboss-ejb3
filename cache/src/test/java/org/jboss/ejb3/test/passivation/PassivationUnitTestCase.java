/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
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
package org.jboss.ejb3.test.passivation;

import org.jboss.ejb3.cache.impl.FileObjectStore;
import org.jboss.ejb3.cache.impl.SimplePassivatingCache;

import junit.framework.TestCase;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class PassivationUnitTestCase extends TestCase
{
   private static void sleep(long micros)
   {
      try
      {
         Thread.sleep(micros);
      }
      catch (InterruptedException e)
      {
         // ignore
      }
   }
   
   public void test1() throws InterruptedException
   {
      MockBeanContainer container = new MockBeanContainer();
      FileObjectStore<MockBeanContext> store = new FileObjectStore<MockBeanContext>();
      store.setStorageDirectory("./target/tmp/passivation");
      store.start();
      SimplePassivatingCache<MockBeanContext> cache = new SimplePassivatingCache<MockBeanContext>(container, container, store);
      cache.setName("MockBeanContainer");
      cache.setSessionTimeout(1);
      cache.start();
      
      MockBeanContext obj = cache.create(null, null);
      Object key = obj.getId();
      
      cache.release(obj);
      obj = null;
      
      wait(container);
      
      assertEquals("MockBeanContext should have been passivated", 1, container.passivations);
      
      obj = cache.get(key);
      
      assertEquals("MockBeanContext should have been activated", 1, container.activations);
      
      sleep(3000);
      
      assertEquals("MockBeanContext should not have been passivated", 1, container.passivations);
      
      cache.release(obj);
      obj = null;
      
      wait(container);
      
      assertEquals("MockBeanContext should have been passivated", 2, container.passivations);
   }
   
   /**
    * Peek of an active object should not change it state.
    */
   public void testPeekActive()
   {
      MockBeanContainer container = new MockBeanContainer();
      FileObjectStore<MockBeanContext> store = new FileObjectStore<MockBeanContext>();
      store.setStorageDirectory("./target/tmp/passivation");
      store.start();
      SimplePassivatingCache<MockBeanContext> cache = new SimplePassivatingCache<MockBeanContext>(container, container, store);
      cache.setName("MockBeanContainer");
      cache.setSessionTimeout(1);
      cache.start();
      
      MockBeanContext obj = cache.create(null, null);
      Object key = obj.getId();
      
      cache.peek(key);
      
      cache.release(obj);
      obj = null;      
   }
   
   private static void wait(Object obj) throws InterruptedException
   {
      synchronized (obj)
      {
         obj.wait(5000);
      }
   }
}
