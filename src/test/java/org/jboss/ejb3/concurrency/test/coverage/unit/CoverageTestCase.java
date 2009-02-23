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
package org.jboss.ejb3.concurrency.test.coverage.unit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.IllegalLoopbackException;

import org.jboss.ejb3.concurrency.test.common.AbstractBootstrapTestCase;
import org.jboss.ejb3.concurrency.test.coverage.SimpleBean;
import org.jboss.ejb3.interceptors.container.BeanContext;
import org.jboss.ejb3.interceptors.direct.DirectContainer;
import org.junit.Test;

/**
 * Try to touch as much code as possible. Don't check spec, if it's tested it exists.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class CoverageTestCase extends AbstractBootstrapTestCase
{
   static class ContainerInvocationTask<B, T> implements Callable<T>
   {
      private DirectContainer<B> container;
      private BeanContext<B> bean;
      private String methodName;
      
      ContainerInvocationTask(DirectContainer<B> container, BeanContext<B> bean, String methodName)
      {
         this.container = container;
         this.bean = bean;
         this.methodName = methodName;
      }
      
      @Override
      public T call() throws Exception
      {
         try
         {
            Object o = container.invoke(bean, methodName);
            return (T) o;
         }
         catch(Error e)
         {
            throw e;
         }
         catch(RuntimeException e)
         {
            throw e;
         }
         catch(Exception e)
         {
            throw e;
         }
         catch(Throwable t)
         {
            throw new Error(t);
         }
      }
   }
   
   /**
    * 4.8.5.5.1 Concurrent access timeouts
    */
   @Test
   public void testAccessTimeout() throws Throwable
   {
      ExecutorService service = Executors.newCachedThreadPool();
      
      final DirectContainer<SimpleBean> container = new DirectContainer<SimpleBean>("SimpleBean", "Singleton Container", SimpleBean.class);
      
      final BeanContext<SimpleBean> bean = container.construct();
      
      Callable<Integer> task = new ContainerInvocationTask<SimpleBean, Integer>(container, bean, "waitOnSemaphore");
      Future<Integer> future = service.submit(task);
      bean.getInstance().getBarrier().await(5, SECONDS);
      
      try
      {
         container.invoke(bean, "accessImmediately");
         fail("Should have thrown " + ConcurrentAccessTimeoutException.class.getSimpleName());
      }
      catch(ConcurrentAccessTimeoutException e)
      {
         // good
      }
      
      bean.getInstance().getSemaphore().release();
      
      int accessCount = future.get(5, SECONDS);
      assertEquals(1, accessCount);
   }
   
   @Test
   public void testMultipleRead() throws Throwable
   {
      ExecutorService service = Executors.newCachedThreadPool();
      
      final DirectContainer<SimpleBean> container = new DirectContainer<SimpleBean>("SimpleBean", "Singleton Container", SimpleBean.class);
      
      final BeanContext<SimpleBean> bean = container.construct();
      
      Callable<Integer> task = new ContainerInvocationTask<SimpleBean, Integer>(container, bean, "waitRead");
      Future<Integer> future = service.submit(task);
      bean.getInstance().getBarrier().await(5, SECONDS);
      
      Integer accessCount = container.invoke(bean, "readImmediately");
      assertEquals(1, accessCount.intValue());
      
      bean.getInstance().getSemaphore().release();
      
      accessCount = future.get(5, SECONDS);
      assertEquals(2, accessCount.intValue());
   }
   
   /**
    * Can we invoke a method at all?
    */
   @Test
   public void testSanity() throws Throwable
   {
      DirectContainer<SimpleBean> container = new DirectContainer<SimpleBean>("SimpleBean", "Singleton Container", SimpleBean.class);
      
      BeanContext<SimpleBean> bean = container.construct();
      
      String arg = new Date().toString();
      String actual = container.invoke(bean, "sayHello", arg);
      
      assertEquals("Hi " + arg, actual);
   }
   
   @Test
   public void testWriteAfterRead() throws Throwable
   {
      ExecutorService executor = Executors.newCachedThreadPool();
      
      final DirectContainer<SimpleBean> container = new DirectContainer<SimpleBean>("SimpleBean", "Singleton Container", SimpleBean.class);
      
      final BeanContext<SimpleBean> bean = container.construct();
      
      Callable<Integer> task = new ContainerInvocationTask<SimpleBean, Integer>(container, bean, "waitRead");
      Future<Integer> future = executor.submit(task);
      bean.getInstance().getBarrier().await(5, SECONDS);
      
      Future<Integer> futureWrite = executor.submit(new ContainerInvocationTask<SimpleBean, Integer>(container, bean, "write"));
      
      /* TODO: improve performance of the locking
      // F I X M E: wait for the write to settle into the writeLock
      Thread.sleep(1000);
      int accessCount = container.invoke(bean, "readImmediately");
      assertEquals(1, accessCount);
      */
      
      bean.getInstance().getSemaphore().release();
      
      int accessCount = future.get(5, SECONDS);
      assertEquals(1, accessCount);
      
      accessCount = futureWrite.get(5, SECONDS);
      assertEquals(2, accessCount);
   }
   
   @Test
   public void testReadIntoRead() throws Throwable
   {
      DirectContainer<SimpleBean> container = new DirectContainer<SimpleBean>("SimpleBean", "Singleton Container", SimpleBean.class);
      
      BeanContext<SimpleBean> bean = container.construct();
      
      Integer accessCount = container.invoke(bean, "readInto", container, bean, "read");
      assertEquals(1, accessCount.intValue());
   }
   
   @Test
   public void testReadIntoWrite() throws Throwable
   {
      DirectContainer<SimpleBean> container = new DirectContainer<SimpleBean>("SimpleBean", "Singleton Container", SimpleBean.class);
      
      BeanContext<SimpleBean> bean = container.construct();
      
      try
      {
         container.invoke(bean, "readInto", container, bean, "write");
         fail("Should have thrown " + IllegalLoopbackException.class.getSimpleName());
      }
      catch(IllegalLoopbackException e)
      {
         // good
      }
   }
}
