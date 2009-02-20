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
package org.jboss.ejb3.timerservice.mk2;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.ejb.TimerService;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.TransactionManager;

import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.ejb3.timerservice.spi.TimerServiceFactory;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class TimerServiceFactoryImpl implements TimerServiceFactory
{
   private EntityManagerFactory emf;
   private TransactionManager transactionManager;
   private ScheduledExecutorService executor;
   
   public TimerService createTimerService(TimedObjectInvoker invoker)
   {
      // TODO: inject
      executor = Executors.newScheduledThreadPool(10);
      
      return new TimerServiceImpl(invoker, emf.createEntityManager(), transactionManager, executor);
   }

   /* (non-Javadoc)
    * @see org.jboss.ejb3.timerservice.spi.TimerServiceFactory#restoreTimerService(javax.ejb.TimerService)
    */
   public void restoreTimerService(TimerService timerService)
   {
      // TODO Auto-generated method stub
      //
      throw new RuntimeException("NYI");
   }

   @PersistenceUnit(unitName="timerdb")
   public void setEntityManagerFactory(EntityManagerFactory emf)
   {
      this.emf = emf;
   }
   
   @Inject
   public void setTransactionManager(TransactionManager tm)
   {
      this.transactionManager = tm;
   }
   
   /* (non-Javadoc)
    * @see org.jboss.ejb3.timerservice.spi.TimerServiceFactory#suspendTimerService(javax.ejb.TimerService)
    */
   public void suspendTimerService(TimerService timerService)
   {
      // TODO Auto-generated method stub
      //
      throw new RuntimeException("NYI");
   }

}
