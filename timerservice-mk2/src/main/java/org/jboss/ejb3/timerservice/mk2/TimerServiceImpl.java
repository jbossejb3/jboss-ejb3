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
import javax.persistence.Query;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

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
    * 
    */
   // TODO: Do we really need this?
   private Map<TimerHandle, TimerImpl> timers = new HashMap<TimerHandle, TimerImpl>();

   /**
    * Creates a {@link TimerServiceImpl}
    * 
    * @param invoker The {@link TimedObjectInvoker} responsible for invoking the timeout method
    * @param em Entity manager responsible for JPA persistence management
    * @param transactionManager Transaction manager responsible for managing the transactional timer service
    * @param executor Executor service responsible for creating scheduled timer tasks
    * @throws IllegalArgumentException If either of the passed param is null
    */
   public TimerServiceImpl(TimedObjectInvoker invoker, EntityManager em, TransactionManager transactionManager,
         ScheduledExecutorService executor)
   {
      if (invoker == null)
      {
         throw new IllegalArgumentException("Invoker cannot be null");
      }
      if (em == null)
      {
         throw new IllegalArgumentException("EntityManager cannot be null");
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
      this.em = em;
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
      return this.createCalendarTimer(schedule, info, persistent, null, null);
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
   public org.jboss.ejb3.timerservice.extension.Timer getAutoTimer(ScheduleExpression schedule,
         String timeoutMethodName, String[] methodParams)
   {
      return this.createCalendarTimer(schedule, null, true, timeoutMethodName, methodParams);
   }

   @Override
   public org.jboss.ejb3.timerservice.extension.Timer getAutoTimer(ScheduleExpression schedule,
         TimerConfig timerConfig, String timeoutMethodName, String[] methodParams)
   {
      return this.createCalendarTimer(schedule, timerConfig.getInfo(), timerConfig.isPersistent(), timeoutMethodName,
            methodParams);
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
    */
   private Timer createTimer(Date initialExpiration, long intervalDuration, Serializable info, boolean persistent)
   {
      if (initialExpiration == null)
         throw new IllegalArgumentException("initial expiration is null");
      if (intervalDuration < 0)
         throw new IllegalArgumentException("interval duration is negative");

      // create an id for the new timer instance
      UUID uuid = UUID.randomUUID();
      // create the timer
      TimerImpl timer = new TimerImpl(uuid, this, initialExpiration, intervalDuration, info, persistent);
      // if it's persistent, then save it
      if (persistent)
      {
         TimerEntity entity = timer.getPersistentState();
         em.persist(entity);
      }
      // now "start" the timer. This involves, moving the timer to an ACTIVE state
      // and scheduling the timer task
      this.startTimer(timer);

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
    */
   private org.jboss.ejb3.timerservice.extension.Timer createCalendarTimer(ScheduleExpression schedule,
         Serializable info, boolean persistent, String timeoutMethod, String[] methodParams)
   {
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
         throw new IllegalStateException("The schedule " + schedule + " doesn't have a timeout in future from now "
               + new Date());
      }

      // generate a id for the timer
      UUID uuid = UUID.randomUUID();
      // create the timer
      TimerImpl timer = new CalendarTimer(uuid, this, calendarTimeout, info, persistent, timeoutMethod, methodParams);
      // persist it if it's persistent
      if (persistent)
      {
         TimerEntity entity = timer.getPersistentState();
         em.persist(entity);
      }

      // now "start" the timer. This involves, moving the timer to an ACTIVE state
      // and scheduling the timer task
      this.startTimer(timer);

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
      synchronized (timers)
      {
         timers.put(timer.getHandle(), timer);
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
   public Timer getTimer(TimerHandle handle)
   {
      TimerImpl timer = timers.get(handle);
      if (timer != null && timer.isActive())
         return timer;
      else
         return null;
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
   void removeTimer(TimerImpl txtimer)
   {
      synchronized (timers)
      {
         timers.remove(txtimer.getHandle());
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
      if (timer == null || timer.isPersistent() == false)
      {
         return;
      }

      // get the persistent entity from the timer
      TimerEntity timerEntity = timer.getPersistentState();

      // TODO: Now all that bolierplate for tx management (which
      // needs to go once we have the timer service "managed")
      Transaction previousTx = null;
      try
      {
         previousTx = this.transactionManager.getTransaction();
         // we persist in a separate tx (REQUIRES_NEW), so suspend
         // any current tx
         if (previousTx != null)
         {
            this.transactionManager.suspend();
         }
         // start new tx
         this.transactionManager.begin();

         // let the entity manager (which has been created outside a tx context) join the transaction
         this.em.joinTransaction();
         // merge the state
         TimerEntity mergedTimerEntity = this.em.merge(timerEntity);

         // do the actual persistence
         this.em.persist(mergedTimerEntity);
      }
      catch (Throwable t)
      {
         // TODO: Again the boilerplate tx management code
         this.setRollbackOnly();
         throw new RuntimeException(t);
      }
      finally
      {
         // since we started a new tx (for REQUIRES_NEW) semantics,
         // resume any previously suspended tx
         this.restorePreviousTx(previousTx);
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
      // TODO: Is this map the definitive place to find 
      // all currently active timers?
      Collection<TimerImpl> timers = this.timers.values();
      for (TimerImpl timer : timers)
      {
         // suspend each timer
         timer.suspend();
      }
   }

   /**
    * Restores persisted timers, corresponding to this timerservice, which are eligible for any new timeouts. 
    * <p>
    * This includes timers whose {@link TimerState} is <b>neither</b> of the following:
    * <ul>
    *   <li>{@link TimerState#CANCELED}</li>
    *   <li>{@link TimerState#CANCELED_IN_TX}</li>
    *   <li>{@link TimerState#EXPIRED}</li>
    * </ul>
    * </p>
    * <p>
    *   All such restored timers will be schedule for their next timeouts.
    * </p>    
    */
   public void restoreTimers()
   {
      // we need to restore only those timers which correspond to the 
      // timed object invoker to which this timer service belongs. So
      // first get hold of the timed object id
      String timedObjectId = this.getInvoker().getTimedObjectId();

      // TODO: Again the boilerplate transaction management code
      // (which will go, once the timer service is "managed")
      boolean thisMethodStartedTx = this.startTxIfNone();

      // timers which are eligible for being restored (see javadoc of this
      // method for details)
      List<TimerImpl> restorableTimers = new ArrayList<TimerImpl>();
      try
      {
         // timer states which we are *not* interested in, during restore
         Set<TimerState> ineligibleTimerStates = new HashSet<TimerState>();
         ineligibleTimerStates.add(TimerState.CANCELED);
         ineligibleTimerStates.add(TimerState.EXPIRED);
         ineligibleTimerStates.add(TimerState.CANCELED_IN_TX);

         // join the transaction, since the entity manager was created
         // outside the transaction context
         this.em.joinTransaction();

         Query restorableTimersQuery = this.em
               .createQuery("from TimerEntity t where t.timedObjectId = :timedObjectId and t.timerState not in (:timerStates)");
         restorableTimersQuery.setParameter("timedObjectId", timedObjectId);
         restorableTimersQuery.setParameter("timerStates", ineligibleTimerStates);

         List<TimerEntity> persistedTimers = restorableTimersQuery.getResultList();
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
            restorableTimers.add(activeTimer);
         }
      }
      catch (Throwable t)
      {
         // TODO: Again the tx management boilerplate
         this.setRollbackOnly();
         throw new RuntimeException(t);
      }
      finally
      {
         // TODO: Remove this once the timer service implementation
         // becomes "managed"
         if (thisMethodStartedTx)
         {
            this.endTransaction();
         }
      }
      logger.debug("Found " + restorableTimers.size() + " active timers for timedObjectId: " + timedObjectId);
      // now "start" each of the restorable timer. This involves, moving the timer to an ACTIVE state
      // and scheduling the timer task
      for (TimerImpl activeTimer : restorableTimers)
      {
         this.startTimer(activeTimer);
         logger.debug("Started timer: " + activeTimer);
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
            tx.registerSynchronization(new TimerTransactionSynchronization(timer));
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
      if (this.getTransaction() != null)
      {
         // don't schedule the timeout yet
         timer.setTimerState(TimerState.STARTED_IN_TX);
      }
      else
      {
         // create and schedule a timer task
         timer.scheduleTimeout();
         timer.setTimerState(TimerState.ACTIVE);
      }
      // persist changes
      this.persistTimer(timer);

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
    * Starts a new tx if not already started.
    * 
    * NOTE: This method will soon be removed, once this timer service
    * implementation becomes "managed"
    *  
    * @return Returns true if a new transaction was created
    * 
    */
   private boolean startTxIfNone()
   {
      try
      {
         Transaction currentTx = this.transactionManager.getTransaction();
         if (currentTx == null)
         {
            this.transactionManager.begin();
            return true;
         }
         return false;
      }
      catch (Throwable t)
      {
         throw new RuntimeException("Could not start transaction", t);
      }
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
         logger.error("Could set transaction to rollback only", ise);
      }
      catch (SystemException se)
      {
         logger.error("Could set transaction to rollback only", se);
      }
   }

   /**
    * Ends (either commits or rolls back, depending on the tx state) the current
    * transaction
    * NOTE: This method will soon be removed, once this timer service
    * implementation becomes "managed"
    *  
    */
   private void endTransaction()
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
         else
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

   /**
    * Resumes a previously suspended transaction. Before doing that,
    *  this method ends (either commits or rolls back, depending on tx state) the current
    *  tx.
    * @param previousTx The previous transaction which was suspended and which now
    *           needs to be resumed
    * NOTE: This method will soon be removed, once this timer service
    * implementation becomes "managed"
    *             
    */
   private void restorePreviousTx(Transaction previousTx)
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
         else
         {
            // Commit tx
            this.transactionManager.commit();
         }
      }
      catch (Exception e)
      {
         throw new RuntimeException("Could not end transaction", e);
      }
      finally
      {
         try
         {
            this.transactionManager.resume(previousTx);
         }
         catch (Exception e)
         {
            throw new RuntimeException("Could not resume previous transaction " + previousTx, e);
         }
      }
   }

   private class TimerTransactionSynchronization implements Synchronization
   {
      /**
       * The timer being managed in the transaction
       */
      private TimerImpl timer;

      public TimerTransactionSynchronization(TimerImpl timer)
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
         if (status == Status.STATUS_COMMITTED)
         {
            logger.debug("commit: " + this);

            TimerState timerState = this.timer.getState();
            switch (timerState)
            {
               case STARTED_IN_TX :
                  this.timer.scheduleTimeout();
                  // set the timer state 
                  this.timer.setTimerState(TimerState.ACTIVE);
                  // persist changes
                  TimerServiceImpl.this.persistTimer(this.timer);
                  break;

               case CANCELED_IN_TX :
                  this.timer.cancel();
                  break;

               case IN_TIMEOUT :
               case RETRY_TIMEOUT :
                  if (this.timer.getInterval() == 0)
                  {
                     this.timer.setTimerState(TimerState.EXPIRED);
                     this.timer.cancel();
                  }
                  else
                  {
                     this.timer.setTimerState(TimerState.ACTIVE);
                     // persist changes
                     TimerServiceImpl.this.persistTimer(this.timer);
                  }
                  break;
            }
         }
         else if (status == Status.STATUS_ROLLEDBACK)
         {
            logger.debug("rollback: " + this);
            TimerState timerState = this.timer.getState();
            switch (timerState)
            {
               case STARTED_IN_TX :
                  this.timer.cancel();
                  break;

               case CANCELED_IN_TX :
                  this.timer.setTimerState(TimerState.ACTIVE);
                  // persist changes
                  TimerServiceImpl.this.persistTimer(this.timer);
                  break;

               case IN_TIMEOUT :
                  this.timer.setTimerState(TimerState.RETRY_TIMEOUT);
                  // persist changes
                  TimerServiceImpl.this.persistTimer(this.timer);
                  logger.debug("retry: " + this.timer);
                  TimerServiceImpl.this.retryTimeout(this.timer);
                  break;

               case RETRY_TIMEOUT :
                  if (this.timer.getInterval() == 0)
                  {
                     this.timer.setTimerState(TimerState.EXPIRED);
                     this.timer.cancel();
                  }
                  else
                  {
                     this.timer.setTimerState(TimerState.ACTIVE);
                     // persist changes
                     TimerServiceImpl.this.persistTimer(this.timer);
                  }
                  break;
            }
         }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void beforeCompletion()
      {
         TimerState timerState = this.timer.getState();
         switch (timerState)
         {
            case CANCELED_IN_TX :
               TimerServiceImpl.this.removeTimer(this.timer);
               break;

            case IN_TIMEOUT :
            case RETRY_TIMEOUT :
               if (this.timer.getInterval() == 0)
               {
                  TimerServiceImpl.this.removeTimer(this.timer);
               }
               break;
         }
      }
   }

}
