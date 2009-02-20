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

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.ejb3.timerservice.mk2.persistence.TimerEntity;
import org.jboss.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class TimerServiceImpl implements TimerService
{
   private static Logger log = Logger.getLogger(TimerServiceImpl.class);
   
   private TimedObjectInvoker invoker;
   private EntityManager em;
   private TransactionManager transactionManager;
   private ScheduledExecutorService executor;

   private Map<TimerHandle, TimerImpl> timers = new HashMap<TimerHandle, TimerImpl>();

   protected TimerServiceImpl(TimedObjectInvoker invoker, EntityManager em, TransactionManager transactionManager, ScheduledExecutorService executor)
   {
      this.invoker = invoker;
      this.em = em;
      this.transactionManager = transactionManager;
      this.executor = executor;
   }
   
   public Timer createCalendarTimer(ScheduleExpression schedule, Serializable info) 
      throws IllegalArgumentException, IllegalStateException, EJBException
   {
      throw new RuntimeException("NYI");
   }

   public Timer createCalendarTimer(ScheduleExpression schedule, TimerConfig timerConfig)
      throws IllegalArgumentException, IllegalStateException, EJBException
   {
      throw new RuntimeException("NYI");
   }

   public Timer createIntervalTimer(Date initialExpiration, long intervalDuration, TimerConfig timerConfig)
      throws IllegalArgumentException, IllegalStateException, EJBException
   {
      throw new RuntimeException("NYI");
   }

   public Timer createIntervalTimer(long initialDuration, long intervalDuration, TimerConfig timerConfig)
      throws IllegalArgumentException, IllegalStateException, EJBException
   {
      throw new RuntimeException("NYI");
   }

   public Timer createSingleActionTimer(Date expiration, TimerConfig timerConfig)
      throws IllegalArgumentException, IllegalStateException, EJBException
   {
      throw new RuntimeException("NYI");
   }

   public Timer createSingleActionTimer(long duration, TimerConfig timerConfig)
      throws IllegalArgumentException, IllegalStateException, EJBException
   {
      if(duration < 0)
         throw new IllegalArgumentException("duration is negative");
      
      return createTimer(new Date(System.currentTimeMillis() + duration), 0, timerConfig.getInfo(), timerConfig.isPersistent());
   }

   /**
    * @param initialExpiration
    * @param intervalDuration
    * @param info
    * @param persistent
    * @return
    */
   private Timer createTimer(Date initialExpiration, long intervalDuration, Serializable info, boolean persistent)
   {
      if (initialExpiration == null)
         throw new IllegalArgumentException("initial expiration is null");
      if (intervalDuration < 0)
         throw new IllegalArgumentException("interval duration is negative");
      
      UUID uuid = UUID.randomUUID();
      TimerImpl timer = new TimerImpl(this, uuid, info);
      if(persistent)
      {
         TimerEntity entity = new TimerEntity();
         entity.setId(uuid);
         entity.setInfo(info);
         entity.setInitialDate(initialExpiration);
         entity.setInterval(intervalDuration);
         entity.setTargetId(invoker.getTimedObjectId());
         em.persist(entity);
      }
      timer.startTimer(initialExpiration, intervalDuration);
      return timer;
   }

   protected void addTimer(TimerImpl timer)
   {
      synchronized (timers)
      {
         timers.put(timer.getHandle(), timer);
      }
   }
   
   public Timer createTimer(long duration, Serializable info)
      throws IllegalArgumentException, IllegalStateException, EJBException
   {
      if(duration < 0)
         throw new IllegalArgumentException("duration is negative");
      
      return createTimer(new Date(System.currentTimeMillis() + duration), 0, info, true);
   }

   public Timer createTimer(Date expiration, Serializable info)
      throws IllegalArgumentException, IllegalStateException, EJBException
   {
      throw new RuntimeException("NYI");
   }

   public Timer createTimer(long initialDuration, long intervalDuration, Serializable info)
      throws IllegalArgumentException, IllegalStateException, EJBException
   {
      throw new RuntimeException("NYI");
   }

   public Timer createTimer(Date initialExpiration, long intervalDuration, Serializable info)
      throws IllegalArgumentException, IllegalStateException, EJBException
   {
      throw new RuntimeException("NYI");
   }

   protected ScheduledExecutorService getExecutor()
   {
      return executor;
   }
   
   protected TimedObjectInvoker getInvoker()
   {
      return invoker;
   }
   
   public Timer getTimer(TimerHandle handle)
   {
      TimerImpl timer = timers.get(handle);
      if (timer != null && timer.isActive())
         return timer;
      else
         return null;
   }
   
   public Collection<Timer> getTimers() throws IllegalStateException, EJBException
   {
      throw new RuntimeException("NYI");
   }
   
   protected Transaction getTransaction()
   {
      try
      {
         return transactionManager.getTransaction();
      }
      catch (SystemException e)
      {
         throw new EJBException(e);
      }
   }
   
   /**
    * Remove a txtimer from the list of active timers
    */
   void removeTimer(TimerImpl txtimer)
   {
      synchronized (timers)
      {
         try
         {
            Transaction currentTx = transactionManager.getTransaction();
            if(currentTx == null)
               transactionManager.begin();
            try
            {
               Query query = em.createQuery("DELETE FROM TimerEntity WHERE id = ?");
               query.setParameter(1, txtimer.getId());
               query.executeUpdate();
               timers.remove(txtimer.getHandle());
            }
            finally
            {
               if(currentTx == null)
                  transactionManager.commit();
            }
         }
         catch(HeuristicMixedException e)
         {
            throw new EJBException(e);
         }
         catch(HeuristicRollbackException e)
         {
            throw new EJBException(e);
         }
         catch(NotSupportedException e)
         {
            throw new EJBException(e);
         }
         catch(RollbackException e)
         {
            // TODO: what now?
            throw new EJBException(e);
         }
         catch(SystemException e)
         {
            throw new EJBException(e);
         }
      }
   }
   
   void retryTimeout(TimerImpl txtimer)
   {
      try
      {
         //retryPolicy.retryTimeout(timedObjectInvoker, txtimer);
         log.warn("retryTimeout is NYI");
         throw new RuntimeException("NYI");
      }
      catch (Exception e)
      {
         log.error("Retry timeout failed for timer: " + txtimer, e);
      }
   }
}
