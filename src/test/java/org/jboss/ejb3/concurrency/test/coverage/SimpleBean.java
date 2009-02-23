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
package org.jboss.ejb3.concurrency.test.coverage;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ejb.LockType.READ;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ejb.AccessTimeout;
import javax.ejb.Lock;

import org.jboss.ejb3.interceptors.container.BeanContext;
import org.jboss.ejb3.interceptors.direct.DirectContainer;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
@AccessTimeout(5000)
public class SimpleBean
{
   // Note this is an atomic because without the interceptor it'll be accessed by multiple threads
   private AtomicInteger accessCount = new AtomicInteger(0);
   
   private CyclicBarrier barrier = new CyclicBarrier(2);
   private Semaphore semaphore = new Semaphore(0);
   
   @AccessTimeout(1)
   public int accessImmediately()
   {
      return accessCount.incrementAndGet();
   }
   
   public CyclicBarrier getBarrier()
   {
      return barrier;
   }
   
   public Semaphore getSemaphore()
   {
      return semaphore;
   }
   
   @Lock(READ)
   public int read()
   {
      return accessCount.incrementAndGet();
   }
   
   @AccessTimeout(1)
   @Lock(READ)
   public int readImmediately()
   {
      return accessCount.incrementAndGet();
   }
   
   @Lock(READ)
   public int readInto(DirectContainer<SimpleBean> container, BeanContext<SimpleBean> bean, String methodName) throws Throwable
   {
      assert bean.getInstance() == this;
      // normally: ctx.getBusinessObject(Simple.class).method
      Integer count = container.invoke(bean, methodName);
      return count;
   }
   
   public String sayHello(String name)
   {
      return "Hi " + name;
   }
   
   public int waitOnSemaphore() throws BrokenBarrierException, InterruptedException, TimeoutException
   {
      barrier.await(5, SECONDS);
      boolean success = semaphore.tryAcquire(5, SECONDS);
      if(!success)
         throw new RuntimeException("timeout");
      return accessCount.incrementAndGet();
   }
   
   @Lock(READ)
   public int waitRead() throws BrokenBarrierException, InterruptedException, TimeoutException
   {
      barrier.await(5, SECONDS);
      boolean success = semaphore.tryAcquire(5, SECONDS);
      if(!success)
         throw new RuntimeException("timeout");
      return accessCount.incrementAndGet();
   }
   
   public int write()
   {
      return accessCount.incrementAndGet();
   }
   
}
