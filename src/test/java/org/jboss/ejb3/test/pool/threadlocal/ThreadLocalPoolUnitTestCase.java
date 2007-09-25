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
package org.jboss.ejb3.test.pool.threadlocal;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.ejb3.pool.StatelessObjectFactory;
import org.jboss.ejb3.pool.threadlocal.ThreadLocalPool;
import org.jboss.ejb3.test.pool.common.MockBean;
import org.jboss.ejb3.test.pool.common.MockFactory;

import junit.framework.TestCase;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class ThreadLocalPoolUnitTestCase extends TestCase
{
   private static class MyUncaughtExceptionHandler implements UncaughtExceptionHandler
   {
      private Throwable uncaught;
      
      Throwable getUncaughtException()
      {
         return uncaught;
      }
      
      public void uncaughtException(Thread t, Throwable e)
      {
         this.uncaught = e;
      }
   }
   
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
   
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      MockBean.reset();
   }
   
   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      
      gc();
   }
   
   /**
    * The basic assumption is that we only create
    * one object per thread and keep that one pooled.
    */
   public void testBasic()
   {
      StatelessObjectFactory<MockBean> factory = new MockFactory();
      ThreadLocalPool<MockBean> pool = new ThreadLocalPool<MockBean>(factory);
      pool.start();
      
      for(int i = 0; i < 10; i++)
      {
         MockBean bean = pool.get();
         
         pool.release(bean);
         
         bean = null;
      }
      
      assertEquals(1, MockBean.getPostConstructs());
   }
   
   public void testBasicMultiThreaded() throws Exception
   {
      StatelessObjectFactory<MockBean> factory = new MockFactory();
      final ThreadLocalPool<MockBean> pool = new ThreadLocalPool<MockBean>(factory);
      pool.start();
      
      Callable<?> task = new Callable<Void>()
      {
         public Void call() throws Exception
         {
            for(int i = 0; i < 10; i++)
            {
               MockBean bean = pool.get();
               
               pool.release(bean);
               
               bean = null;
            }
            return null;
         }
      };
      
      ExecutorService service = Executors.newFixedThreadPool(10);
      Future<?> results[] = new Future<?>[10];
      for(int i = 0; i < results.length; i++)
      {
         results[i] = service.submit(task);
      }
      
      for(Future<?> result : results)
      {
         result.get(5, TimeUnit.SECONDS);
      }
      
      service.shutdown();
      boolean terminated = service.awaitTermination(5, TimeUnit.SECONDS);
      assertTrue(terminated);
      
      assertEquals(10, MockBean.getPostConstructs());
   }
   
   /**
    * Check testMemLeak first.
    * 
    * The problem with WeakThreadLocal is that beans
    * can be gc'd while in the pool (EJBTHREE-1031).
    * @see testMemLeak
    */
   public void testGCTooSoon() throws Exception
   {
      StatelessObjectFactory<MockBean> factory = new MockFactory();
      ThreadLocalPool<MockBean> pool = new ThreadLocalPool<MockBean>(factory);
      pool.start();
      
      MockBean bean = pool.get();
      
      pool.release(bean);
      
      bean = null;
      
      gc();
      
      assertEquals(0, MockBean.getFinalized());
   }
   
   /**
    * Test the old EJBTHREE-840 mem leak.
    * @throws Exception 
    */
   public void testMemLeak() throws Exception
   {
      assertEquals(0, MockBean.getFinalized());
      
      StatelessObjectFactory<MockBean> factory = new MockFactory();
      final ThreadLocalPool<MockBean> pool = new ThreadLocalPool<MockBean>(factory);
      pool.start();
      
      Runnable test = new Runnable()
      {
         public void run()
         {
            MockBean bean = pool.get();
            
            pool.release(bean);
            
            bean = null;
            
            while(!Thread.currentThread().isInterrupted())
            {
               try
               {
                  Thread.sleep(1000);
               }
               catch (InterruptedException e)
               {
                  return;
               }
            }
         }
      };
      
      MyUncaughtExceptionHandler eh = new MyUncaughtExceptionHandler();
      
      Thread threads[] = new Thread[10];
      for(int i = 0; i < threads.length; i++)
      {
         threads[i] = new Thread(test);
         threads[i].setUncaughtExceptionHandler(eh);
         threads[i].start();
      }
      
      assertNull(eh.getUncaughtException());

      Thread.sleep(500);
      
      // We stop the pool (undeploy / redeploy). The threads remain alive, because
      // they're managed by the remote connection pool.
      pool.stop();
      
      gc();
      
      assertNull(eh.getUncaughtException());
      
      assertEquals(10, MockBean.getPostConstructs());
      assertEquals(10, MockBean.getFinalized());
      // Note: that before the Infinite delegate we didn't call preDestroy
      assertEquals(10, MockBean.getPreDestroys());

      for(Thread thread : threads)
      {
         thread.interrupt();
         thread.join(5000);
         assertFalse(thread.isAlive());
      }
      
      assertNull(eh.getUncaughtException());
   }
}
