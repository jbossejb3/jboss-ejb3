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
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.jboss.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class TimerImpl implements Timer
{
   private static final Logger log = Logger.getLogger(TimerImpl.class);

   protected TimerState timerState;

   protected TimerServiceImpl timerService;

   protected TimedObjectInvoker timedObjectInvoker;

   protected UUID id;

   protected Serializable info;

   protected boolean persistent;

   protected TimerHandleImpl handle;

   protected Date initialExpiration;

   protected long intervalDuration;

   protected Date nextExpiration;

   protected ScheduledFuture<?> future;

   protected Date previousRun; 
   
   class TimerSynchronization implements Synchronization
   {
      public void afterCompletion(int status)
      {
         if (status == Status.STATUS_COMMITTED)
         {
            log.debug("commit: " + this);

            switch (timerState)
            {
               case STARTED_IN_TX :
                  scheduleTimeout();
                  setTimerState(TimerState.ACTIVE);
                  // persist changes
                  timerService.persistTimer(TimerImpl.this);
                  break;

               case CANCELED_IN_TX :
                  cancelTimer();
                  break;

               case IN_TIMEOUT :
               case RETRY_TIMEOUT :
                  if (intervalDuration == 0)
                  {
                     setTimerState(TimerState.EXPIRED);
                     cancelTimer();
                  }
                  else
                  {
                     setTimerState(TimerState.ACTIVE);
                     // persist changes
                     timerService.persistTimer(TimerImpl.this);
                  }
                  break;
            }
         }
         else if (status == Status.STATUS_ROLLEDBACK)
         {
            log.debug("rollback: " + this);

            switch (timerState)
            {
               case STARTED_IN_TX :
                  cancelTimer();
                  break;

               case CANCELED_IN_TX :
                  setTimerState(TimerState.ACTIVE);
                  // persist changes
                  timerService.persistTimer(TimerImpl.this);
                  break;

               case IN_TIMEOUT :
                  setTimerState(TimerState.RETRY_TIMEOUT);
                  // persist changes
                  timerService.persistTimer(TimerImpl.this);
                  log.debug("retry: " + TimerImpl.this);
                  timerService.retryTimeout(TimerImpl.this);
                  break;

               case RETRY_TIMEOUT :
                  if (intervalDuration == 0)
                  {
                     setTimerState(TimerState.EXPIRED);
                     cancelTimer();
                  }
                  else
                  {
                     setTimerState(TimerState.ACTIVE);
                     // persist changes
                     timerService.persistTimer(TimerImpl.this);
                  }
                  break;
            }
         }
      }

      public void beforeCompletion()
      {
         switch (timerState)
         {
            case CANCELED_IN_TX :
               timerService.removeTimer(TimerImpl.this);
               break;

            case IN_TIMEOUT :
            case RETRY_TIMEOUT :
               if (intervalDuration == 0)
               {
                  timerService.removeTimer(TimerImpl.this);
               }
               break;
         }
      }

   };

   class TimerTaskImpl implements Runnable
   {
      public void run()
      {
         log.debug("run: " + TimerImpl.this);
         TimerImpl.this.previousRun = new Date();
         // Set next scheduled execution attempt. This is used only
         // for reporting (getTimeRemaining()/getNextTimeout())
         // and not from the underlying jdk timer implementation.
         if (isActive() && intervalDuration > 0)
         {
            nextExpiration = new Date(nextExpiration.getTime() + intervalDuration);
         }
         // persist changes
         timerService.persistTimer(TimerImpl.this);

         // If a retry thread is in progress, we don't want to allow another
         // interval to execute until the retry is complete. See JIRA-1926.
         if (isInRetry())
         {
            log.debug("Timer in retry mode, skipping this scheduled execution");
            return;
         }

         if (isActive())
         {
            try
            {
               setTimerState(TimerState.IN_TIMEOUT);
               // persist changes
               timerService.persistTimer(TimerImpl.this);
               timedObjectInvoker.callTimeout(TimerImpl.this);
            }
            catch (Exception e)
            {
               log.error("Error invoking ejbTimeout", e);
            }
            finally
            {
               if (timerState == TimerState.IN_TIMEOUT)
               {
                  log.debug("Timer was not registered with Tx, resetting state: " + TimerImpl.this);
                  if (intervalDuration == 0)
                  {
                     setTimerState(TimerState.EXPIRED);
                     killTimer();
                  }
                  else
                  {
                     setTimerState(TimerState.ACTIVE);
                     // persist changes
                     timerService.persistTimer(TimerImpl.this);
                  }
               }
            }
         }
      }
   }

   public TimerImpl(UUID id, TimerServiceImpl service, Date initialExpiry, long intervalDuration, Serializable info,
         boolean persistent)
   {
      assert service != null : "service is null";
      assert id != null : "id is null";

      this.timerService = service;
      this.timerService.addTimer(this);

      this.timedObjectInvoker = service.getInvoker();
      this.id = id;
      this.info = info;
      this.persistent = persistent;
      this.initialExpiration = initialExpiry;
      this.intervalDuration = intervalDuration;
      this.nextExpiration = initialExpiry;

      this.handle = new TimerHandleImpl(id, service);

      setTimerState(TimerState.CREATED);
   }

   /**
    * @throws NoSuchObjectLocalException if the txtimer was canceled or has expired
    */
   protected void assertTimerState()
   {
      if (timerState == TimerState.EXPIRED)
         throw new NoSuchObjectLocalException("Timer has expired");
      if (timerState == TimerState.CANCELED_IN_TX || timerState == TimerState.CANCELED)
         throw new NoSuchObjectLocalException("Timer was canceled");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isCalendarTimer() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      // first check whether this timer has expired or cancelled
      this.assertTimerState();

      return false;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void cancel() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      // first check whether the timer has expired or has been cancelled
      this.assertTimerState();
      this.cancelTimer();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public TimerHandle getHandle() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      return this.handle;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isPersistent() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      return this.persistent;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Serializable getInfo() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      return this.info;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Date getNextTimeout() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      // first check the validity of the timer state
      this.assertTimerState();
      return this.nextExpiration;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ScheduleExpression getSchedule() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      this.assertTimerState();
      throw new IllegalStateException("Timer " + this + " is not a calendar based timer");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long getTimeRemaining() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      //      // first check the validity of the timer state
      //      this.assertTimerState();
      //      long currentTimeInMillis = System.currentTimeMillis();
      //      // if the next expiration is *not* in future and the repeat interval isn't
      //      // a positive number (i.e. no repeats) then there won't be any more timeouts.
      //      // So throw a NoMoreTimeoutsException.
      //      // NOTE: We check for intervalDuration and not just nextExpiration because,
      //      // it's a valid case where the nextExpiration is in past (maybe the server was
      //      // down when the timeout was expected) 
      //      if (this.nextExpiration < currentTimeInMillis && this.intervalDuration <= 0)
      //      {
      //         throw new NoMoreTimeoutsException("No more timeouts for timer " + this);
      //      }
      return 0;//this.nextExpiration - currentTimeInMillis;
   }

   protected UUID getId()
   {
      return this.id;
   }

   /**
    * killTimer w/o persistence work
    */
   protected void cancelTimer()
   {
      if (timerState != TimerState.EXPIRED)
         setTimerState(TimerState.CANCELED);
      future.cancel(false);

      // persist changes
      timerService.persistTimer(this);
   }

   public boolean isActive()
   {
      return !isCanceled() && !isExpired();
   }

   public boolean isCanceled()
   {
      return timerState == TimerState.CANCELED_IN_TX || timerState == TimerState.CANCELED;
   }

   public boolean isExpired()
   {
      return timerState == TimerState.EXPIRED;
   }

   protected boolean isInRetry()
   {
      return timerState == TimerState.RETRY_TIMEOUT;
   }

   public Date getPreviousRun()
   {
      return this.previousRun;
   }
   
   public TimerState getState()
   {
      return this.timerState;
   }
   
   /**
    * Kill the timer, and remove it from the timer service
    */
   protected void killTimer()
   {
      log.debug("killTimer: " + this);
      if (timerState != TimerState.EXPIRED)
         setTimerState(TimerState.CANCELED);
      timerService.removeTimer(this);
      future.cancel(false);

      // persist changes
      timerService.persistTimer(this);
   }

   protected void registerTimerWithTx()
   {
      Transaction tx = timerService.getTransaction();
      if (tx != null)
      {
         try
         {
            tx.registerSynchronization(new TimerSynchronization());
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

   protected void scheduleTimeout()
   {
      Runnable command = new TimerTaskImpl();
      long delay = this.nextExpiration.getTime() - System.currentTimeMillis();
      if (delay < 0)
         delay = 0;
      if (intervalDuration > 0)
         future = timerService.getExecutor().scheduleAtFixedRate(command, delay, intervalDuration,
               TimeUnit.MILLISECONDS);
      else
         future = timerService.getExecutor().schedule(command, delay, TimeUnit.MILLISECONDS);
   }

   protected void setTimerState(TimerState state)
   {
      log.debug(this + " set state " + state);
      this.timerState = state;
   }

   protected void startInTx()
   {
      if (timerService.getTransaction() != null)
      {
         // don't schedule the timeout yet
         setTimerState(TimerState.STARTED_IN_TX);
      }
      else
      {
         scheduleTimeout();
         setTimerState(TimerState.ACTIVE);
      }
      // persist changes
      timerService.persistTimer(this);

   }

   public void startTimer()
   {
      registerTimerWithTx();

      // the timer will actually go ACTIVE on tx commit
      startInTx();
   }

   @Override
   public String toString()
   {
      long remaining = this.nextExpiration.getTime() - System.currentTimeMillis();
      String retStr = "[id=" + id + ",service=" + timerService + ",remaining=" + remaining + ",intervalDuration="
            + intervalDuration + "," + timerState + "]";
      return retStr;
   }

}
