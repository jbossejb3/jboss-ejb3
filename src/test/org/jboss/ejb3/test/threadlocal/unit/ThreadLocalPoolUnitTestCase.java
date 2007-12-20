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
package org.jboss.ejb3.test.threadlocal.unit;

import junit.framework.TestCase;

import org.jboss.ejb3.BeanContext;
import org.jboss.ejb3.Container;
import org.jboss.ejb3.pool.ThreadlocalPool;
import org.jboss.ejb3.test.threadlocal.MockBean;
import org.jboss.ejb3.test.threadlocal.MockContainer;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: 67042 $
 */
public class ThreadLocalPoolUnitTestCase extends TestCase
{
   int used = 0;
   
   private static void gc()
   {
      for(int i = 0; i < 3; i++)
      {
         System.gc();
         try
         {
            Thread.sleep(100);
         }
         catch (InterruptedException e)
         {
            // ignore
         }
         System.runFinalization();
      }
   }
   
   public void test1()
   {
      ThreadlocalPool pool = new ThreadlocalPool();
      Container container = new MockContainer();
      int maxSize = -1;
      int timeout = -1;
      pool.initialize(container, maxSize, timeout);
      BeanContext ctx = pool.get();
      pool.release(ctx);
      
      ctx = null;
      
      gc();
      assertEquals(0, pool.getRemoveCount());
      assertEquals(0, MockBean.finalizers);
      
      pool.destroy();
      
      gc();
      assertEquals(1, pool.getRemoveCount());
      assertEquals(1, MockBean.finalizers);
   }
   
   public void testWithThreads() throws Exception
   {
      final ThreadlocalPool pool = new ThreadlocalPool();
      Container container = new MockContainer();
      int maxSize = -1;
      int timeout = -1;
      pool.initialize(container, maxSize, timeout);
      
      Runnable r = new Runnable()
      {
         public void run()
         {
            BeanContext ctx = pool.get();
            pool.release(ctx);
            
            ctx = null;
            used++;
         }
      };
      
      Thread threads[] = new Thread[20];
      for(int i = 0; i < threads.length; i++)
      {
         threads[i] = new Thread(r);
         threads[i].start();
      }
      
      for(Thread t : threads)
      {
         t.join(1000);
      }
      
      gc();
      assertEquals(0, pool.getRemoveCount());
      assertEquals(0, MockBean.finalizers);
      
      pool.destroy();
      
      gc();
      assertEquals(20, pool.getRemoveCount());
      assertEquals(20, MockBean.finalizers);
      
      assertEquals(20, used);
   }
   
   public void testMultipleWithThreads() throws Exception
   {
      final ThreadlocalPool pool = new ThreadlocalPool();
      Container container = new MockContainer();
      int maxSize = -1;
      int timeout = -1;
      pool.initialize(container, maxSize, timeout);
      
      Runnable r = new Runnable()
      {
         public void run()
         {
            for(int i = 0; i < 10; i++)
            {
               BeanContext ctx = pool.get();
               pool.release(ctx);
               
               ctx = null;
               used++;
            }
         }
      };
      
      Thread threads[] = new Thread[20];
      for(int i = 0; i < threads.length; i++)
      {
         threads[i] = new Thread(r);
         threads[i].start();
      }
      
      for(Thread t : threads)
      {
         t.join(1000);
      }
      
      gc();
      assertEquals(0, pool.getRemoveCount());
      assertEquals(0, MockBean.finalizers);
      
      pool.destroy();
      
      gc();
      assertEquals(20, pool.getRemoveCount());
      assertEquals(20, MockBean.finalizers);
      
      assertEquals(200, used);
   }
   
   public void testMultipleRecursiveWithThreads() throws Exception
   {
      final ThreadlocalPool pool = new ThreadlocalPool();
      Container container = new MockContainer();
      int maxSize = -1;
      int timeout = -1;
      pool.initialize(container, maxSize, timeout);
      
      Runnable r = new Runnable()
      {
         public void run()
         {
            for(int i = 0; i < 10; i++)
            {
               BeanContext ctx = pool.get();
               BeanContext ctx2 = pool.get();
               
               pool.release(ctx2);
               ctx2 = null;
               used ++;
               
               pool.release(ctx);
               ctx = null;
               used ++;
            }
         }
      };
      
      Thread threads[] = new Thread[20];
      for(int i = 0; i < threads.length; i++)
      {
         threads[i] = new Thread(r);
         threads[i].start();
      }
      
      for(Thread t : threads)
      {
         t.join(1000);
      }
      
      gc();
      assertEquals(200, pool.getRemoveCount());
      assertEquals(200, MockBean.finalizers);
      
      pool.destroy();
      
      gc();
      assertEquals(220, pool.getRemoveCount());
      assertEquals(220, MockBean.finalizers);
      
      assertEquals(400, used);
   }
   
   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      
      MockBean.finalizers = 0;
      used = 0;
   }
}
