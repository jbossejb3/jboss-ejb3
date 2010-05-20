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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

import org.jboss.ejb3.timer.schedule.CalendarBasedTimeout;
import org.jboss.ejb3.timerservice.mk2.persistence.TimerEntity;
import org.jboss.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class TimerServiceImpl implements TimerService
{
   /**
    * Logger
    */
   private static Logger log = Logger.getLogger(TimerServiceImpl.class);

   /**
    * The {@link TimedObjectInvoker} which is responsible for invoking the timeout
    * method
    */
   private TimedObjectInvoker invoker;

   /**
    * Used for persistent timers
    */
   private EntityManager em;

   /**
    * Transaction manager
    */
   private TransactionManager transactionManager;

   /**
    * For scheduling timeout tasks
    */
   private ScheduledExecutorService executor;

   /**
    * All available timers which were created by this {@link TimerService} 
    */
   private Map<TimerHandle, TimerImpl> timers = new HashMap<TimerHandle, TimerImpl>();

   /**
    * Creates a {@link TimerServiceImpl}
    * @param invoker
    * @param em
    * @param transactionManager
    * @param executor
    */
   public TimerServiceImpl(TimedObjectInvoker invoker, EntityManager em, TransactionManager transactionManager,
         ScheduledExecutorService executor)
   {
      this.invoker = invoker;
      this.em = em;
      this.transactionManager = transactionManager;
      this.executor = executor;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Timer createCalendarTimer(ScheduleExpression schedule) throws IllegalArgumentException,
         IllegalStateException, EJBException
   {
      return this.createCalendarTimer(schedule, null);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Timer createCalendarTimer(ScheduleExpression schedule, TimerConfig timerConfig)
         throws IllegalArgumentException, IllegalStateException, EJBException
   {
      Serializable info = timerConfig == null ? null : timerConfig.getInfo();
      boolean persistent = timerConfig == null ? true : timerConfig.isPersistent();
      return this.createCalendarTimer(schedule, info, persistent);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Timer createIntervalTimer(Date initialExpiration, long intervalDuration, TimerConfig timerConfig)
         throws IllegalArgumentException, IllegalStateException, EJBException
   {
      if (initialExpiration == null)
      {
         throw new IllegalArgumentException("initialExpiration cannot be null while creating a timer");
      }
      if (initialExpiration.getTime() < 0)
      {
         throw new IllegalArgumentException("initialExpiration.getTime() cannot be negative while creating a timer");
      }
      if (intervalDuration < 0)
      {
         throw new IllegalArgumentException("intervalDuration cannot be negative while creating a timer");
      }
      return this.createTimer(initialExpiration, intervalDuration, timerConfig.getInfo(), timerConfig.isPersistent());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Timer createIntervalTimer(long initialDuration, long intervalDuration, TimerConfig timerConfig)
         throws IllegalArgumentException, IllegalStateException, EJBException
   {
      return this.createIntervalTimer(new Date(initialDuration), intervalDuration, timerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Timer createSingleActionTimer(Date expiration, TimerConfig timerConfig) throws IllegalArgumentException,
         IllegalStateException, EJBException
   {
      if (expiration == null)
      {
         throw new IllegalArgumentException("expiration cannot be null while creating a single action timer");
      }
      if (expiration.getTime() < 0)
      {
         throw new IllegalArgumentException(
               "expiration.getTime() cannot be negative while creating a single action timer");
      }
      return this.createTimer(expiration, 0, timerConfig.getInfo(), timerConfig.isPersistent());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Timer createSingleActionTimer(long duration, TimerConfig timerConfig) throws IllegalArgumentException,
         IllegalStateException, EJBException
   {
      if (duration < 0)
         throw new IllegalArgumentException("duration cannot be negative while creating single action timer");

      return createTimer(new Date(System.currentTimeMillis() + duration), 0, timerConfig.getInfo(), timerConfig
            .isPersistent());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Timer createTimer(long duration, Serializable info) throws IllegalArgumentException, IllegalStateException,
         EJBException
   {
      if (duration < 0)
         throw new IllegalArgumentException("Duration cannot negative while creating the timer");

      return createTimer(new Date(System.currentTimeMillis() + duration), 0, info, true);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Timer createTimer(Date expiration, Serializable info) throws IllegalArgumentException, IllegalStateException,
         EJBException
   {
      if (expiration == null)
      {
         throw new IllegalArgumentException("Expiration date cannot be null while creating a timer");
      }
      if (expiration.getTime() < 0)
      {
         throw new IllegalArgumentException("expiration.getTime() cannot be negative while creating a timer");
      }
      return this.createTimer(expiration, 0, info, true);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Timer createTimer(long initialDuration, long intervalDuration, Serializable info)
         throws IllegalArgumentException, IllegalStateException, EJBException
   {
      if (initialDuration < 0)
      {
         throw new IllegalArgumentException("Initial duration cannot be negative while creating timer");
      }
      if (intervalDuration < 0)
      {
         throw new IllegalArgumentException("Interval cannot be negative while creating timer");
      }
      return this.createTimer(new Date(System.currentTimeMillis() + initialDuration), intervalDuration, info, true);

   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Timer createTimer(Date initialExpiration, long intervalDuration, Serializable info)
         throws IllegalArgumentException, IllegalStateException, EJBException
   {
      if (initialExpiration == null)
      {
         throw new IllegalArgumentException("intial expiration date cannot be null while creating a timer");
      }
      if (initialExpiration.getTime() < 0)
      {
         throw new IllegalArgumentException("expiration.getTime() cannot be negative while creating a timer");
      }
      if (intervalDuration < 0)
      {
         throw new IllegalArgumentException("interval duration cannot be negative while creating timer");
      }
      return this.createTimer(initialExpiration, intervalDuration, info, true);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<Timer> getTimers() throws IllegalStateException, EJBException
   {
      Set<Timer> activeTimers = new HashSet<Timer>();
      for (TimerImpl timer : this.timers.values())
      {
         if (timer != null && timer.isActive())
         {
            activeTimers.add(timer);
         }
      }
      return activeTimers;
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
      TimerImpl timer = new TimerImpl(uuid, this, initialExpiration, intervalDuration, info, persistent);
      if (persistent)
      {
         TimerEntity entity = new TimerEntity();
         entity.setId(uuid);
         entity.setInfo(info);
         entity.setInitialDate(initialExpiration);
         entity.setInterval(intervalDuration);
         entity.setTargetId(invoker.getTimedObjectId());
         em.persist(entity);
      }
      timer.startTimer();
      return timer;
   }

   private Timer createCalendarTimer(ScheduleExpression schedule, Serializable info, boolean persistent)
   {
      if (schedule == null)
         throw new IllegalArgumentException("schedule is null");

      CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(schedule);
      if (calendarTimeout.getFirstTimeout() == null)
      {
         // TODO: Think about this. It might be possible that a timer creation request
         // was issued for a schedule which is in past (i.e. doesn't have any future timeouts)
         // For ex: through the use of a @Schedule on a method. How should we handle such timers?
         throw new IllegalStateException("The schedule " + schedule + " doesn't have a timeout in future from now "
               + new Date());
      }

      UUID uuid = UUID.randomUUID();

      TimerImpl timer = new CalendarTimer(uuid, this, calendarTimeout, info, persistent);
      if (persistent)
      {
         TimerEntity entity = new TimerEntity();
         entity.setId(uuid);
         entity.setInfo(info);
         entity.setTargetId(invoker.getTimedObjectId());
         em.persist(entity);
      }
      timer.startTimer();
      return timer;
   }

   protected void addTimer(TimerImpl timer)
   {
      synchronized (timers)
      {
         timers.put(timer.getHandle(), timer);
      }
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
            if (currentTx == null)
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
               if (currentTx == null)
                  transactionManager.commit();
            }
         }
         catch (HeuristicMixedException e)
         {
            throw new EJBException(e);
         }
         catch (HeuristicRollbackException e)
         {
            throw new EJBException(e);
         }
         catch (NotSupportedException e)
         {
            throw new EJBException(e);
         }
         catch (RollbackException e)
         {
            // TODO: what now?
            throw new EJBException(e);
         }
         catch (SystemException e)
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

   public void persistTimer(TimerImpl timer)
   {
      if (timer == null || timer.isPersistent() == false)
      {
         return;
      }
      UUID id = timer.getId();

      TimerEntity timerEntity = this.em.find(TimerEntity.class, id);
      timerEntity.setNextDate(timer.getNextTimeout());
      timerEntity.setPreviousRun(timer.getPreviousRun());
      timerEntity.setTimerState(timer.getState());
      // persist in a new tx
      this.persistInNewTx(timerEntity);

   }

   private void persistInNewTx(TimerEntity timer)
   {
      //TODO: Implement this
   }

}
