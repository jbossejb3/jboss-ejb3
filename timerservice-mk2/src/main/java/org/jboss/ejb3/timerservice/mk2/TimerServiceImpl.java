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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerHandle;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.ejb3.context.CurrentInvocationContext;
import org.jboss.ejb3.context.spi.InvocationContext;
import org.jboss.ejb3.timer.schedule.CalendarBasedTimeout;
import org.jboss.ejb3.timerservice.extension.TimerService;
import org.jboss.ejb3.timerservice.mk2.persistence.CalendarTimerEntity;
import org.jboss.ejb3.timerservice.mk2.persistence.TimeoutMethod;
import org.jboss.ejb3.timerservice.mk2.persistence.TimerEntity;
import org.jboss.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.logging.Logger;

/**
 * MK2 implementation of EJB3.1 {@link TimerService}
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class TimerServiceImpl implements TimerService
{
   /**
    * Logger
    */
   private static Logger logger = Logger.getLogger(TimerServiceImpl.class);

   /**
    * The {@link TimedObjectInvoker} which is responsible for invoking the timeout
    * method
    */
   private TimedObjectInvoker invoker;

   /**
    * Used for persistent timers
    */
   private EntityManagerFactory emf;

   /**
    * Transaction manager
    */
   private TransactionManager transactionManager;

   /**
    * For scheduling timeout tasks
    */
   private ScheduledExecutorService executor;

   /**
    * All non-persistent timers which were created by this {@link TimerService} 
    * 
    */
   private Map<TimerHandle, TimerImpl> nonPersistentTimers = new HashMap<TimerHandle, TimerImpl>();

   private Map<TimerHandle, TimerImpl> persistentWaitingOnTxCompletionTimers = new HashMap<TimerHandle, TimerImpl>();

   private ThreadLocal<EntityManager> transactionScopedEntityManager = new ThreadLocal<EntityManager>();

   /**
    * Creates a {@link TimerServiceImpl}
    * 
    * @param invoker The {@link TimedObjectInvoker} responsible for invoking the timeout method
    * @param emf Entity manager factory responsible for JPA persistence management
    * @param transactionManager Transaction manager responsible for managing the transactional timer service
    * @param executor Executor service responsible for creating scheduled timer tasks
    * @throws IllegalArgumentException If either of the passed param is null
    */
   public TimerServiceImpl(TimedObjectInvoker invoker, EntityManagerFactory emf, TransactionManager transactionManager,
         ScheduledExecutorService executor)
   {
      if (invoker == null)
      {
         throw new IllegalArgumentException("Invoker cannot be null");
      }
      if (emf == null)
      {
         throw new IllegalArgumentException("EntityManagerFactory cannot be null");
      }
      if (transactionManager == null)
      {
         throw new IllegalArgumentException("Transaction manager cannot be null");
      }
      if (executor == null)
      {
         throw new IllegalArgumentException("Executor cannot be null");
      }

      this.invoker = invoker;
      this.emf = emf;
      this.transactionManager = transactionManager;
      this.executor = executor;
   }

   /**
    * 
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
      return this.createCalendarTimer(schedule, info, persistent, null);
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

   @Override
   public org.jboss.ejb3.timerservice.extension.Timer getAutoTimer(ScheduleExpression schedule, Method timeoutMethod)
   {
      return this.createCalendarTimer(schedule, null, true, timeoutMethod);
   }

   @Override
   public org.jboss.ejb3.timerservice.extension.Timer getAutoTimer(ScheduleExpression schedule,
         TimerConfig timerConfig, Method timeoutMethod)
   {
      return this.createCalendarTimer(schedule, timerConfig.getInfo(), timerConfig.isPersistent(), timeoutMethod);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<Timer> getTimers() throws IllegalStateException, EJBException
   {
      if (this.isLifecycleCallbackInvocation())
      {
         throw new IllegalStateException(
               "getTimers() method invocation is not allowed during lifecycle callback of EJBs");
      }

      Set<Timer> activeTimers = new HashSet<Timer>();
      // get all active non-persistent timers for this timerservice
      for (TimerImpl timer : this.nonPersistentTimers.values())
      {
         if (timer != null && timer.isActive())
         {
            activeTimers.add(timer);
         }
      }
      // get all active timers which are persistent, but haven't yet been
      // persisted (waiting for tx to complete)
      for (TimerImpl timer : this.persistentWaitingOnTxCompletionTimers.values())
      {
         if (timer != null && timer.isActive())
         {
            activeTimers.add(timer);
         }
      }

      // now get all active persistent timers for this timerservice
      activeTimers.addAll(this.getActiveTimers());
      return activeTimers;
   }

   /**
    * Create a {@link Timer} 
    * 
    * @param initialExpiration The {@link Date} at which the first timeout should occur. 
    *                       <p>If the date is in the past, then the timeout is triggered immediately 
    *                       when the timer moves to {@link TimerState#ACTIVE}</p> 
    * @param intervalDuration The interval (in milli seconds) between consecutive timeouts for the newly created timer.
    *                           <p>Cannot be a negative value. A value of 0 indicates a single timeout action</p>
    * @param info {@link Serializable} info that will be made available through the newly created timer's {@link Timer#getInfo()} method
    * @param persistent True if the newly created timer has to be persistent
    * @return Returns the newly created timer
    * @throws IllegalArgumentException If <code>initialExpiration</code> is null or <code>intervalDuration</code> is negative
    * @throws IllegalStateException If this method was invoked during a lifecycle callback on the EJB
    */
   private Timer createTimer(Date initialExpiration, long intervalDuration, Serializable info, boolean persistent)
   {
      if (this.isLifecycleCallbackInvocation())
      {
         throw new IllegalStateException("Creation of timers is not allowed during lifecycle callback of EJBs");
      }
      if (initialExpiration == null)
      {
         throw new IllegalArgumentException("initial expiration is null");
      }
      if (intervalDuration < 0)
      {
         throw new IllegalArgumentException("interval duration is negative");
      }

      // create an id for the new timer instance
      UUID uuid = UUID.randomUUID();
      // create the timer
      TimerImpl timer = new TimerImpl(uuid, this, initialExpiration, intervalDuration, info, persistent);
      // if it's persistent, then save it
      if (persistent)
      {
         this.persistTimer(timer);
      }
      // now "start" the timer. This involves, moving the timer to an ACTIVE state
      // and scheduling the timer task
      this.startTimer(timer);

      this.addTimer(timer);
      // return the newly created timer
      return timer;
   }

   /**
    * Creates a calendar based {@link Timer}
    * 
    * @param schedule The {@link ScheduleExpression} which will be used for creating scheduled timer tasks
    *               for a calendar based timer
    * @param info {@link Serializable} info that will be made available through the newly created timer's {@link Timer#getInfo()} method
    * @param persistent True if the newly created timer has to be persistent
    * @return Returns the newly created timer
    * @throws IllegalArgumentException If the passed <code>schedule</code> is null
    * @throws IllegalStateException If this method was invoked during a lifecycle callback on the EJB
    */
   private org.jboss.ejb3.timerservice.extension.Timer createCalendarTimer(ScheduleExpression schedule,
         Serializable info, boolean persistent, Method timeoutMethod)
   {
      if (this.isLifecycleCallbackInvocation())
      {
         throw new IllegalStateException("Creation of timers is not allowed during lifecycle callback of EJBs");
      }
      if (schedule == null)
      {
         throw new IllegalArgumentException("schedule is null");
      }
      // parse the passed schedule and create the calendar based timeout 
      CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(schedule);
      // no schedules for this timer?
      if (calendarTimeout.getFirstTimeout() == null)
      {
         // TODO: Think about this. It might be possible that a timer creation request
         // was issued for a schedule which is in past (i.e. doesn't have any future timeouts)
         // For ex: through the use of a @Schedule on a method. How should we handle such timers?
         logger.warn("The schedule " + schedule + " doesn't have a timeout in future from now " + new Date());
         throw new IllegalArgumentException("Invalid schedule expression: " + schedule);
      }

      // generate a id for the timer
      UUID uuid = UUID.randomUUID();
      // create the timer
      TimerImpl timer = new CalendarTimer(uuid, this, calendarTimeout, info, persistent, timeoutMethod);

      if (persistent)
      {
         this.persistTimer(timer);
      }
      
      // now "start" the timer. This involves, moving the timer to an ACTIVE state
      // and scheduling the timer task
      this.startTimer(timer);

      this.addTimer(timer);
      // return the timer
      return timer;
   }

   /**
    * TODO: Rethink about this method. Do we really need this?
    * Adds the timer instance to an internal {@link TimerHandle} to {@link TimerImpl} map.
    * 
    * @param timer Timer instance
    */
   protected void addTimer(TimerImpl timer)
   {
      if (timer.persistent == false)
      {
         synchronized (nonPersistentTimers)
         {
            nonPersistentTimers.put(timer.getTimerHandle(), timer);
         }
      }
      else
      {
         synchronized (this.persistentWaitingOnTxCompletionTimers)
         {
            this.persistentWaitingOnTxCompletionTimers.put(timer.getTimerHandle(), timer);
         }
      }
   }

   /**
    * Returns the {@link ScheduledExecutorService} used for scheduling timer tasks
    * @return
    */
   protected ScheduledExecutorService getExecutor()
   {
      return executor;
   }

   /**
    * Returns the {@link TimedObjectInvoker} to which this timer service belongs
    * @return
    */
   public TimedObjectInvoker getInvoker()
   {
      return invoker;
   }

   /**
    * Returns the {@link Timer} corresponding to the passed {@link TimerHandle}
    * 
    * @param handle The {@link TimerHandle} for which the {@link Timer} is being looked for
    * @return
    * TODO: Rethink about this method. Looks brittle.
    */
   public org.jboss.ejb3.timerservice.extension.Timer getTimer(TimerHandle handle)
   {
      // TODO: Why do we even have to maintain a map of timers.
      // Looks needless and could perhaps lead to inconsistencies. 
      // Let's instead just rely on DB? But then again what about non-persistent timers.
      // Overall, needs a bit of thinking
      TimerImpl timer = nonPersistentTimers.get(handle);
      if (timer != null)
      {
         return timer;
      }
      timer = this.persistentWaitingOnTxCompletionTimers.get(handle);
      if (timer != null)
      {
         return timer;
      }
      TimerHandleImpl timerHandle = (TimerHandleImpl) handle;
      return this.getPersistedTimer(timerHandle);

   }

   /**
    * TODO: Rethink this method. 
    * @return Returns the current transaction, if any. Else returns null.
    * @throws EJBException If there is any system level exception 
    */
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
   void removeTimer(TimerImpl timer)
   {
      if (timer.persistent == false)
      {
         synchronized (nonPersistentTimers)
         {
            nonPersistentTimers.remove(timer.getTimerHandle());
         }
      }
      else
      {
         synchronized (this.persistentWaitingOnTxCompletionTimers)
         {
            this.persistentWaitingOnTxCompletionTimers.remove(timer.getTimerHandle());
         }
      }
      // TODO: I don't really like the idea of removing/deleting
      // persisted timers from the persistent store. This method
      // is currently called when the timer expires or is cancelled.
      // In either case, the state of the timer is changed appropriately
      // and persisted. I guess that should be enough. 
      // If there's a good reason for deleting the entries, then uncomment 
      // the following (untested) code

      //         try
      //         {
      //            Transaction currentTx = transactionManager.getTransaction();
      //            if (currentTx == null)
      //               transactionManager.begin();
      //            try
      //            {
      //               Query query = em.createQuery("DELETE FROM TimerEntity WHERE id = ?");
      //               query.setParameter(1, txtimer.getId());
      //               query.executeUpdate();
      //               timers.remove(txtimer.getHandle());
      //            }
      //            finally
      //            {
      //               if (currentTx == null)
      //                  transactionManager.commit();
      //            }
      //         }
      //         catch (HeuristicMixedException e)
      //         {
      //            throw new EJBException(e);
      //         }
      //         catch (HeuristicRollbackException e)
      //         {
      //            throw new EJBException(e);
      //         }
      //         catch (NotSupportedException e)
      //         {
      //            throw new EJBException(e);
      //         }
      //         catch (RollbackException e)
      //         {
      //            // TODO: what now?
      //            throw new EJBException(e);
      //         }
      //         catch (SystemException e)
      //         {
      //            throw new EJBException(e);
      //         }

   }

   /**
    * TODO: Not yet implemented
    * @param txtimer
    */
   void retryTimeout(TimerImpl txtimer)
   {
      try
      {
         //retryPolicy.retryTimeout(timedObjectInvoker, txtimer);
         logger.warn("retryTimeout is NYI");
         throw new RuntimeException("NYI");
      }
      catch (Exception e)
      {
         logger.error("Retry timeout failed for timer: " + txtimer, e);
      }
   }

   /**
    * Persists the passed <code>timer</code>.
    * 
    * <p>
    *   If the passed timer is null or is non-persistent (i.e. {@link Timer#isPersistent()} returns false),
    *   then this method acts as a no-op 
    * </p>
    * @param timer
    */
   public void persistTimer(TimerImpl timer)
   {
      // if not persistent, then do nothing
      if (timer == null || timer.persistent == false)
      {
         return;
      }

      // get the persistent entity from the timer
      TimerEntity timerEntity = timer.getPersistentState();

      // TODO: Now all that bolierplate for tx management (which
      // needs to go once we have the timer service "managed")
      Transaction previousTx = null;
      boolean newTxStarted = false;
      try
      {
         previousTx = this.transactionManager.getTransaction();
         // we persist with REQUIRED tx semantics
         // if there's no current tx in progress, then create a new one
         if (previousTx == null)
         {
            this.startNewTx();
            newTxStarted = true;
         }

         EntityManager em = this.getCurrentEntityManager();
         // merge the state
         TimerEntity mergedTimerEntity = em.merge(timerEntity);

         // do the actual persistence
         em.persist(mergedTimerEntity);

      }
      catch (Throwable t)
      {
         // TODO: Again the boilerplate tx management code
         this.setRollbackOnly();
         throw new RuntimeException(t);
      }
      finally
      {
         // since we started a new tx, end it (either commit or rollback) ourselves
         if (newTxStarted)
         {
            this.endTx();
         }
      }

   }

   /**
    * Suspends any currently scheduled tasks for {@link Timer}s
    * <p>
    *   Note that, suspend does <b>not</b> cancel the {@link Timer}. Instead,
    *   it just cancels the <b>next scheduled timeout</b>. So once the {@link Timer}
    *   is restored (whenever that happens), the {@link Timer} will continue to 
    *   timeout at appropriate times. 
    * </p>
    */
   public void suspendTimers()
   {
      // get all active timers (persistent/non-persistent inclusive)
      Collection<Timer> timers = this.getTimers();
      for (Timer timer : timers)
      {
         if (timer instanceof TimerImpl == false)
         {
            continue;
         }
         // suspend the timer
         ((TimerImpl) timer).suspend();
      }
   }

   /**
    * Restores persisted timers, corresponding to this timerservice, which are eligible for any new timeouts. 
    * <p>
    * This includes timers whose {@link TimerState} is <b>neither</b> of the following:
    * <ul>
    *   <li>{@link TimerState#CANCELED}</li>
    *   <li>{@link TimerState#EXPIRED}</li>
    * </ul>
    * </p>
    * <p>
    *   All such restored timers will be schedule for their next timeouts.
    * </p>    
    */
   public void restoreTimers()
   {
      // get the persisted timers which are considered active
      List<TimerImpl> restorableTimers = this.getActiveTimers();

      logger.debug("Found " + restorableTimers.size() + " active timers for timedObjectId: "
            + this.invoker.getTimedObjectId());
      // now "start" each of the restorable timer. This involves, moving the timer to an ACTIVE state
      // and scheduling the timer task
      for (TimerImpl activeTimer : restorableTimers)
      {
         this.startTimer(activeTimer);
         logger.debug("Started timer: " + activeTimer);
         // save any changes to the state (that will have happened on call to startTimer) 
         this.persistTimer(activeTimer);
      }

   }

   /**
    * Registers a timer with a transaction (if any in progress) and then moves
    * the timer to a active state, so that it becomes eligible for timeouts
    */
   protected void startTimer(TimerImpl timer)
   {
      registerTimerWithTx(timer);

      // the timer will actually go ACTIVE on tx commit
      startInTx(timer);
   }

   /**
    * Registers the timer with any active transaction so that appropriate action on the timer can be 
    * carried out on transaction lifecycle events, through the use of {@link Synchronization}
    * callbacks.
    * <p>
    *   If there is no transaction in progress, when this method is called, then
    *   this method is effectively a no-op.
    * </p>
    * @param timer 
    */
   protected void registerTimerWithTx(TimerImpl timer)
   {
      // get the current transaction
      Transaction tx = this.getTransaction();
      if (tx != null)
      {
         try
         {
            // register for lifecycle events of transaction
            tx.registerSynchronization(new TimerCreationTransactionSynchronization(timer));
         }
         catch (RollbackException e)
         {
            // TODO: throw the right exception
            throw new EJBException(e);
         }
         catch (SystemException e)
         {
            throw new EJBException(e);
         }
      }
   }

   /**
    * Moves the timer to either {@link TimerState#STARTED_IN_TX} or {@link TimerState#ACTIVE}
    * depending on whether there's any transaction active currently.
    * <p>
    *   If there's no transaction currently active, then this method creates and schedules a timer task.
    *   Else, it just changes the state of the timer to {@link TimerState#STARTED_IN_TX} and waits
    *   for the transaction to commit, to schedule the timer task. 
    * </p>
    * @param timer
    */
   protected void startInTx(TimerImpl timer)
   {
      timer.setTimerState(TimerState.ACTIVE);
      this.persistTimer(timer);
      
      // if there's no transaction, then trigger a schedule immidiately.
      // Else, the timer will be scheduled on tx synchronization callback
      if (this.getTransaction() == null)
      {
         // create and schedule a timer task
         timer.scheduleTimeout();
      }

   }

   /**
    * Returns true if the {@link CurrentInvocationContext} represents a lifecycle 
    * callback invocation. Else returns false.
    * <p>
    *   This method internally relies on {@link CurrentInvocationContext#get()} to obtain
    *   the current invocation context.
    *   <ul>
    *       <li>If the context is available then it looks for the method that was invoked. 
    *       The absence of a method indicates a lifecycle callback.</li>
    *       <li>If the context is <i>not</i> available, then this method returns false (i.e.
    *       it doesn't consider the current invocation as a lifecycle callback). This is 
    *       for convenience, to allow the invocation of {@link javax.ejb.TimerService} methods
    *       in the absence of {@link CurrentInvocationContext}</li>
    *   </ul>    
    *       
    * </p>
    * @return
    */
   protected boolean isLifecycleCallbackInvocation()
   {
      InvocationContext currentInvocationContext = null;
      try
      {
         currentInvocationContext = CurrentInvocationContext.get();
      }
      catch (IllegalStateException ise)
      {
         // no context info available so return false
         return false;
      }
      // If the method in current invocation context is null,
      // then it represents a lifecycle callback invocation
      Method invokedMethod = currentInvocationContext.getMethod();
      if (invokedMethod == null)
      {
         // it's a lifecycle callback
         return true;
      }
      // not an lifecycle callback
      return false;
   }

   private TimerImpl getPersistedTimer(TimerHandleImpl timerHandle)
   {
      UUID id = timerHandle.getId();
      String timedObjectId = timerHandle.getTimedObjectId();
      EntityManager em = this.emf.createEntityManager();
      Query query = em.createQuery("from TimerEntity t where t.id = :id and t.timedObjectId = :timedObjectId");
      query.setParameter("id", id);
      query.setParameter("timedObjectId", timedObjectId);

      List<TimerEntity> timers = query.getResultList();
      if (timers == null || timers.isEmpty())
      {
         return null;
      }
      if (timers.size() > 1)
      {
         throw new EJBException("More than one timer found for TimerHandle: " + timerHandle);
      }
      TimerEntity timerEntity = timers.get(0);
      if (timerEntity.isCalendarTimer())
      {
         return new CalendarTimer((CalendarTimerEntity) timerEntity, this);
      }
      return new TimerImpl(timerEntity, this);

   }

   private List<TimerImpl> getActiveTimers()
   {
      // we need only those timers which correspond to the 
      // timed object invoker to which this timer service belongs. So
      // first get hold of the timed object id
      String timedObjectId = this.getInvoker().getTimedObjectId();

      // timer states which do *not* represent an active timer
      Set<TimerState> ineligibleTimerStates = new HashSet<TimerState>();
      ineligibleTimerStates.add(TimerState.CANCELED);
      ineligibleTimerStates.add(TimerState.EXPIRED);

      EntityManager em = this.emf.createEntityManager();

      Query activeTimersQuery = em
            .createQuery("from TimerEntity t where t.timedObjectId = :timedObjectId and t.timerState not in (:timerStates)");
      activeTimersQuery.setParameter("timedObjectId", timedObjectId);
      activeTimersQuery.setParameter("timerStates", ineligibleTimerStates);

      List<TimerEntity> persistedTimers = activeTimersQuery.getResultList();
      List<TimerImpl> activeTimers = new ArrayList<TimerImpl>();
      for (TimerEntity persistedTimer : persistedTimers)
      {
         TimerImpl activeTimer = null;
         if (persistedTimer.isCalendarTimer())
         {
            CalendarTimerEntity calendarTimerEntity = (CalendarTimerEntity) persistedTimer;
            // create a timer instance from the persisted calendar timer
            activeTimer = new CalendarTimer(calendarTimerEntity, this);
         }
         else
         {
            // create the timer instance from the persisted state
            activeTimer = new TimerImpl(persistedTimer, this);
         }
         // add it to the list of timers which will be restored
         activeTimers.add(activeTimer);
      }

      return activeTimers;
   }

   private Serializable clone(Serializable info) throws Exception
   {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
      objectOutputStream.writeObject(info);

      ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
      ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
      Object clonedInfo = objectInputStream.readObject();

      return (Serializable) clonedInfo;
   }

   private org.jboss.ejb3.timerservice.extension.Timer getExistingAutoTimer(ScheduleExpression schedule,
         TimerConfig timerConfig, String timeoutMethodName, String[] methodParams)
   {
      //      if (timerConfig != null && timerConfig.isPersistent() == false)
      //      {
      //         return null;
      //      }
      //      // we need to restore only those timers which correspond to the 
      //      // timed object invoker to which this timer service belongs. So
      //      // first get hold of the timed object id
      //      String timedObjectId = this.getInvoker().getTimedObjectId();
      //
      //      // TODO: Again the boilerplate transaction management code
      //      // (which will go, once the timer service is "managed")
      //      boolean thisMethodStartedTx = this.startTxIfNone();
      //
      //      try
      //      {
      //
      //         // join the transaction, since the entity manager was created
      //         // outside the transaction context
      //         this.em.joinTransaction();
      //
      //         Query autoTimersQuery = this.em
      //               .createQuery("from CalendarTimerEntity t where t.timedObjectId = :timedObjectId and t.autoTimer is true");
      //         autoTimersQuery.setParameter("timedObjectId", timedObjectId);
      //
      //         List<CalendarTimerEntity> autoTimers = autoTimersQuery.getResultList();
      //         for (CalendarTimerEntity autoTimer : autoTimers)
      //         {
      //            TimeoutMethod timeoutMethod = autoTimer.getTimeoutMethod();
      //            if (this.doesTimeoutMethodMatch(timeoutMethod, timeoutMethodName, methodParams) == false)
      //            {
      //               continue;
      //            }
      //            if (timerConfig != null)
      //            {
      //               Serializable info = timerConfig.getInfo();
      //               if (info != null)
      //               {
      //                  if (info.equals(autoTimer.getInfo()) == false)
      //                  {
      //                     continue;
      //                  }
      //               }
      //               else
      //               {
      //                  if (autoTimer.getInfo() != null)
      //                  {
      //                     continue;
      //                  }
      //               }
      //            }
      //            // now onto schedule
      //            ScheduleExpression autoTimerSchedule = autoTimer.getScheduleExpression();
      //            if (this.doSchedulesMatch(autoTimerSchedule, schedule))
      //            {
      //               return new CalendarTimer(autoTimer, this);
      //            }
      //            
      //         }
      //      }
      //      catch (Throwable t)
      //      {
      //         // TODO: Again the tx management boilerplate
      //         this.setRollbackOnly();
      //         throw new RuntimeException(t);
      //      }
      //      finally
      //      {
      //         // TODO: Remove this once the timer service implementation
      //         // becomes "managed"
      //         if (thisMethodStartedTx)
      //         {
      //            this.endTransaction();
      //         }
      //      }
      return null;
   }

   private boolean doSchedulesMatch(ScheduleExpression schedule, ScheduleExpression otherScheduleExpression)
   {
      return true;
   }

   private boolean doesTimeoutMethodMatch(TimeoutMethod timeoutMethod, String timeoutMethodName, String[] methodParams)
   {
      if (timeoutMethod.getMethodName().equals(timeoutMethodName) == false)
      {
         return false;
      }
      String[] timeoutMethodParams = timeoutMethod.getMethodParams();
      if (timeoutMethodParams == null && methodParams == null)
      {
         return true;
      }
      return this.methodParamsMatch(timeoutMethodParams, methodParams);
   }

   private boolean doesTimerConfigMatch(TimerConfig timerConfig)
   {
      return true;
   }

   private boolean isEitherParamNull(Object param1, Object param2)
   {
      if (param1 != null && param2 == null)
      {
         return true;
      }
      if (param2 != null && param1 == null)
      {
         return true;
      }
      return false;
   }

   private boolean methodParamsMatch(String[] methodParams, String[] otherMethodParams)
   {
      if (this.isEitherParamNull(methodParams, otherMethodParams))
      {
         return false;
      }

      if (methodParams.length != otherMethodParams.length)
      {
         return false;
      }
      for (int i = 0; i < methodParams.length; i++)
      {
         if (methodParams[i].equals(otherMethodParams[i]) == false)
         {
            return false;
         }
      }
      return true;
   }

   /**
    * Marks the transaction for rollback
    * NOTE: This method will soon be removed, once this timer service
    * implementation becomes "managed"
    *  
    */
   private void setRollbackOnly()
   {
      try
      {
         Transaction tx = this.transactionManager.getTransaction();
         if (tx != null)
         {
            tx.setRollbackOnly();
         }
      }
      catch (IllegalStateException ise)
      {
         logger.error("Ignoring exception during setRollbackOnly: ", ise);
      }
      catch (SystemException se)
      {
         logger.error("Ignoring exception during setRollbackOnly: ", se);
      }
   }

   private void startNewTx()
   {
      try
      {
         this.transactionManager.begin();
      }
      catch (Throwable t)
      {
         throw new RuntimeException("Could not start transaction", t);
      }
   }

   private void endTx()
   {
      try
      {
         Transaction tx = this.transactionManager.getTransaction();
         if (tx == null)
         {
            throw new IllegalStateException("Transaction cannot be ended since no transaction is in progress");
         }
         if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK)
         {
            this.transactionManager.rollback();
         }
         else if (tx.getStatus() == Status.STATUS_ACTIVE)
         {
            // Commit tx
            this.transactionManager.commit();
         }
      }
      catch (Exception e)
      {
         throw new RuntimeException("Could not end transaction", e);
      }
   }

   private class TimerCreationTransactionSynchronization implements Synchronization
   {
      /**
       * The timer being managed in the transaction
       */
      private TimerImpl timer;

      public TimerCreationTransactionSynchronization(TimerImpl timer)
      {
         if (timer == null)
         {
            throw new IllegalStateException("Timer cannot be null");
         }
         this.timer = timer;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void afterCompletion(int status)
      {
         if (this.timer.persistent)
         {
            synchronized (TimerServiceImpl.this.persistentWaitingOnTxCompletionTimers)
            {
               TimerServiceImpl.this.persistentWaitingOnTxCompletionTimers.remove(this.timer.getTimerHandle());
            }
         }
         if (status == Status.STATUS_COMMITTED)
         {
            logger.debug("commit timer creation: " + this.timer);

            TimerState timerState = this.timer.getState();
            switch (timerState)
            {
               case ACTIVE :
                  // the timer was started/activated in a tx.
                  // now it's time to schedule the task
                  this.timer.scheduleTimeout();
                  break;
            }
         }
         else if (status == Status.STATUS_ROLLEDBACK)
         {
            logger.debug("Rolling back timer creation: " + this.timer);

            TimerState timerState = this.timer.getState();
            switch (timerState)
            {
               case ACTIVE:
                  // TODO: I think the timer state should be FAILED instead of
                  // CANCELLED. Let's rethink this later
                  this.timer.setTimerState(TimerState.CANCELED);
                  break;
            }
            
         }
         
      }

      @Override
      public void beforeCompletion()
      {
         // TODO Auto-generated method stub

      }

   }
   


   private class EntityManagerTransactionSynchronization implements Synchronization
   {

      @Override
      public void afterCompletion(int status)
      {
         EntityManager em = TimerServiceImpl.this.transactionScopedEntityManager.get();
         TimerServiceImpl.this.transactionScopedEntityManager.remove();
         if (em != null)
         {
            try
            {
               em.close();
            }
            catch (Exception e)
            {
               logger.debug("Ignoring exception during entity manager close: ", e);
            }
         }

      }

      @Override
      public void beforeCompletion()
      {
         // TODO Auto-generated method stub

      }

   }

   private EntityManager getCurrentEntityManager() throws Exception
   {
      EntityManager em = this.transactionScopedEntityManager.get();
      if (em != null)
      {
         return em;
      }
      Transaction tx = this.transactionManager.getTransaction();
      if (tx == null)
      {
         throw new IllegalStateException("No transaction in progress. Cannot create an entity manager");
      }
      em = this.emf.createEntityManager();
      em.joinTransaction();
      this.transactionScopedEntityManager.set(em);
      tx.registerSynchronization(new EntityManagerTransactionSynchronization());

      return em;
   }
}
