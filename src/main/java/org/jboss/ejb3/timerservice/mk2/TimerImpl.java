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

import static org.jboss.ejb3.timerservice.mk2.TimerImpl.State.ACTIVE;
import static org.jboss.ejb3.timerservice.mk2.TimerImpl.State.CANCELED;
import static org.jboss.ejb3.timerservice.mk2.TimerImpl.State.CANCELED_IN_TX;
import static org.jboss.ejb3.timerservice.mk2.TimerImpl.State.CREATED;
import static org.jboss.ejb3.timerservice.mk2.TimerImpl.State.EXPIRED;
import static org.jboss.ejb3.timerservice.mk2.TimerImpl.State.IN_TIMEOUT;
import static org.jboss.ejb3.timerservice.mk2.TimerImpl.State.RETRY_TIMEOUT;
import static org.jboss.ejb3.timerservice.mk2.TimerImpl.State.STARTED_IN_TX;

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
   
   private State timerState;
   private TimerServiceImpl timerService;
   private TimedObjectInvoker timedObjectInvoker;
   private UUID id;
   private Serializable info;
   private TimerHandleImpl handle;

   private Date initialExpiration;
   private long intervalDuration;
   private long nextExpiration;

   private ScheduledFuture<?> future;


   /**
    * Timer states and their allowed transitions
    * <p/>
    * CREATED  - on create
    * CREATED -> STARTED_IN_TX - when strated with Tx
    * CREATED -> ACTIVE  - when started without Tx
    * STARTED_IN_TX -> ACTIVE - on Tx commit
    * STARTED_IN_TX -> CANCELED - on Tx rollback
    * ACTIVE -> CANCELED_IN_TX - on cancel() with Tx
    * ACTIVE -> CANCELED - on cancel() without Tx
    * CANCELED_IN_TX -> CANCELED - on Tx commit
    * CANCELED_IN_TX -> ACTIVE - on Tx rollback
    * ACTIVE -> IN_TIMEOUT - on TimerTask run
    * IN_TIMEOUT -> ACTIVE - on Tx commit if intervalDuration > 0
    * IN_TIMEOUT -> EXPIRED -> on Tx commit if intervalDuration == 0
    * IN_TIMEOUT -> RETRY_TIMEOUT -> on Tx rollback
    * RETRY_TIMEOUT -> ACTIVE -> on Tx commit/rollback if intervalDuration > 0
    * RETRY_TIMEOUT -> EXPIRED -> on Tx commit/rollback if intervalDuration == 0
    */
   static enum State {
      CREATED,
      STARTED_IN_TX,
      ACTIVE,
      CANCELED_IN_TX,
      CANCELED,
      EXPIRED,
      IN_TIMEOUT,
      RETRY_TIMEOUT
   };
   
   class TimerSynchronization implements Synchronization
   {
      public void afterCompletion(int status)
      {
         if (status == Status.STATUS_COMMITTED)
         {
            log.debug("commit: " + this);

            switch (timerState)
            {
               case STARTED_IN_TX:
                  scheduleTimeout();
                  setTimerState(ACTIVE);
                  break;

               case CANCELED_IN_TX:
                  cancelTimer();
                  break;

               case IN_TIMEOUT:
               case RETRY_TIMEOUT:
                  if(intervalDuration == 0)
                  {
                     setTimerState(EXPIRED);
                     cancelTimer();
                  }
                  else
                  {
                     setTimerState(ACTIVE);
                  }
                  break;
            }
         }
         else if (status == Status.STATUS_ROLLEDBACK)
         {
            log.debug("rollback: " + this);

            switch (timerState)
            {
               case STARTED_IN_TX:
                  cancelTimer();
                  break;
                  
               case CANCELED_IN_TX:
                  setTimerState(ACTIVE);
                  break;
                  
               case IN_TIMEOUT:
                  setTimerState(RETRY_TIMEOUT);
                  log.debug("retry: " + TimerImpl.this);
                  timerService.retryTimeout(TimerImpl.this);
                  break;
                  
               case RETRY_TIMEOUT:
                  if (intervalDuration == 0)
                  {
                     setTimerState(EXPIRED);
                     cancelTimer();
                  }
                  else
                  {
                     setTimerState(ACTIVE);
                  }
                  break;
            }
         }
      }

      public void beforeCompletion()
      {
         switch(timerState)
         {
            case CANCELED_IN_TX:
               timerService.removeTimer(TimerImpl.this);
               break;

            case IN_TIMEOUT:
            case RETRY_TIMEOUT:
               if(intervalDuration == 0)
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

         // Set next scheduled execution attempt. This is used only
         // for reporting (getTimeRemaining()/getNextTimeout())
         // and not from the underlying jdk timer implementation.
         if (isActive() && intervalDuration > 0)
         {
            nextExpiration += intervalDuration;
         }
         
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
               setTimerState(IN_TIMEOUT);
               timedObjectInvoker.callTimeout(TimerImpl.this);
            }
            catch (Exception e)
            {
               log.error("Error invoking ejbTimeout", e);
            }
            finally
            {
               if (timerState == IN_TIMEOUT)
               {
                  log.debug("Timer was not registered with Tx, resetting state: " + TimerImpl.this);
                  if (intervalDuration == 0)
                  {
                     setTimerState(EXPIRED);
                     killTimer();
                  }
                  else
                  {
                     setTimerState(ACTIVE);
                  }
               }
            }
         }
      }
   }
   
   TimerImpl(TimerServiceImpl service, UUID id, Serializable info)
   {
      assert service != null : "service is null";
      assert id != null : "id is null";
      
      this.timerService = service;
      this.timedObjectInvoker = service.getInvoker();
      this.id = id;
      this.info = info;
      
      this.handle = new TimerHandleImpl(id, service);
      
      setTimerState(CREATED);
   }
   
   /**
    * @throws NoSuchObjectLocalException if the txtimer was canceled or has expired
    */
   private void assertTimedOut()
   {
      if (timerState == EXPIRED)
         throw new NoSuchObjectLocalException("Timer has expired");
      if (timerState == CANCELED_IN_TX || timerState == CANCELED)
         throw new NoSuchObjectLocalException("Timer was canceled");
   }
   
   /* (non-Javadoc)
    * @see javax.ejb.Timer#cancel()
    */
   public void cancel() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      // TODO Auto-generated method stub
      //
      throw new RuntimeException("NYI");
   }

   /**
    * killTimer w/o persistence work
    */
   protected void cancelTimer()
   {
      if (timerState != EXPIRED)
         setTimerState(CANCELED);
      future.cancel(false);
   }
   
   public TimerHandle getHandle() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      return handle;
   }

   protected UUID getId()
   {
      return id;
   }
   
   /* (non-Javadoc)
    * @see javax.ejb.Timer#getInfo()
    */
   public Serializable getInfo() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      // TODO Auto-generated method stub
      //return null;
      throw new RuntimeException("NYI");
   }

   /* (non-Javadoc)
    * @see javax.ejb.Timer#getNextTimeout()
    */
   public Date getNextTimeout() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      // TODO Auto-generated method stub
      //return null;
      throw new RuntimeException("NYI");
   }

   /* (non-Javadoc)
    * @see javax.ejb.Timer#getSchedule()
    */
   public ScheduleExpression getSchedule() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      // TODO Auto-generated method stub
      //return null;
      throw new RuntimeException("NYI");
   }
   
   /* (non-Javadoc)
    * @see javax.ejb.Timer#getTimeRemaining()
    */
   public long getTimeRemaining() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      // TODO Auto-generated method stub
      //return 0;
      throw new RuntimeException("NYI");
   }
   
   protected boolean isActive()
   {
      return !isCanceled() && !isExpired();
   }

   protected boolean isCanceled()
   {
      return timerState == CANCELED_IN_TX || timerState == CANCELED;
   }

   protected boolean isExpired()
   {
      return timerState == EXPIRED;
   }
   
   protected boolean isInRetry() {
      return timerState == RETRY_TIMEOUT;
   }

   /* (non-Javadoc)
    * @see javax.ejb.Timer#isPersistent()
    */
   public boolean isPersistent() throws IllegalStateException, NoSuchObjectLocalException, EJBException
   {
      // TODO Auto-generated method stub
      //return false;
      throw new RuntimeException("NYI");
   }
   
   /**
    * Kill the timer, and remove it from the timer service
    */
   protected void killTimer()
   {
      log.debug("killTimer: " + this);
      if (timerState != EXPIRED)
         setTimerState(CANCELED);
      timerService.removeTimer(this);
      future.cancel(false);
   }
   
   protected void registerTimerWithTx()
   {
      Transaction tx = timerService.getTransaction();
      if(tx != null)
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
      long delay = nextExpiration - System.currentTimeMillis();
      if(delay < 0) delay = 0;
      if (intervalDuration > 0)
         future = timerService.getExecutor().scheduleAtFixedRate(command, delay, intervalDuration, TimeUnit.MILLISECONDS);
      else
         future = timerService.getExecutor().schedule(command, delay, TimeUnit.MILLISECONDS);
   }
   
   private void setTimerState(State state)
   {
      log.debug(this + " set state " + state);
      this.timerState = state;
   }
   
   protected void startInTx()
   {
      if (timerService.getTransaction() != null)
      {
         // don't schedule the timeout yet
         setTimerState(STARTED_IN_TX);
      }
      else
      {
         scheduleTimeout();
         setTimerState(ACTIVE);
      }
   }
   
   protected void startTimer(Date initialExpiration, long intervalDuration)
   {
      this.initialExpiration = initialExpiration;
      this.nextExpiration = initialExpiration.getTime();
      this.intervalDuration = intervalDuration;
      
      timerService.addTimer(this);
      registerTimerWithTx();
      
      // the timer will actually go ACTIVE on tx commit
      startInTx();
   }
   
   @Override
   public String toString()
   {
      long remaining = nextExpiration - System.currentTimeMillis();
      String retStr = "[id=" + id + ",service=" + timerService + ",remaining=" + remaining + ",intervalDuration=" + intervalDuration + "," + timerState + "]";
      return retStr;
   }
}
