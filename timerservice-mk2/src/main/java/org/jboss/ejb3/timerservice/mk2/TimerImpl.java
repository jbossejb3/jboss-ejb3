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
import javax.ejb.NoMoreTimeoutsException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.TimerHandle;

import org.jboss.ejb3.timerservice.extension.Timer;
import org.jboss.ejb3.timerservice.extension.TimerService;
import org.jboss.ejb3.timerservice.mk2.persistence.TimerEntity;
import org.jboss.ejb3.timerservice.mk2.task.TimerTask;
import org.jboss.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.logging.Logger;

/**
 * Implementation of EJB3.1 {@link Timer}
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class TimerImpl implements Timer
{
   /**
    * Logger
    */
   private static final Logger logger = Logger.getLogger(TimerImpl.class);

   /**
    * Unique id for this timer instance
    */
   protected UUID id;

   /**
    * The timer state
    */
   protected TimerState timerState;

   /**
    * The {@link TimerService} through which this timer was created
    */
   protected TimerServiceImpl timerService;

   /**
    * The {@link TimedObjectInvoker} to which this timer corresponds
    */
   protected TimedObjectInvoker timedObjectInvoker;

   /**
    * The info which was passed while creating the timer. 
    */
   protected Serializable info;

   /**
    * Indicates whether the timer is persistent
    */
   protected boolean persistent;

   /**
    * A {@link TimerHandle} for this timer
    */
   protected TimerHandleImpl handle;

   /**
    * The initial (first) expiry date of this timer
    */
   protected Date initialExpiration;

   /**
    * The duration in milli sec. between timeouts
    */
   protected long intervalDuration;

   /**
    * Next expiry date of this timer
    */
   protected Date nextExpiration;

   /**
    * The {@link ScheduledFuture} for the next timeout of this timer
    */
   protected ScheduledFuture<?> future;

   /**
    * The date of the previous run of this timer 
    */
   protected Date previousRun;

   /**
    * If the timer is persistent, then this represents its persistent state.
    * 
    */
   protected TimerEntity persistentState;

   /**
    * Creates a {@link TimerImpl}
    * 
    * @param id The id of this timer
    * @param service The timer service through which this timer was created
    * @param initialExpiry The first expiry of this timer
    * @param intervalDuration The duration (in milli sec) between timeouts
    * @param info The info that will be passed on through the {@link Timer} and will be available through the {@link Timer#getInfo()} method
    * @param persistent True if this timer is persistent. False otherwise
    */
   public TimerImpl(UUID id, TimerServiceImpl service, Date initialExpiry, long intervalDuration, Serializable info,
         boolean persistent)
   {
      this(id, service, initialExpiry, intervalDuration, initialExpiry, info, persistent);
   }

   /**
    * Creates a {@link TimerImpl}
    * 
    * @param id The id of this timer
    * @param service The timer service through which this timer was created
    * @param initialExpiry The first expiry of this timer
    * @param intervalDuration The duration (in milli sec) between timeouts
    * @param nextEpiry The next expiry of this timer
    * @param info The info that will be passed on through the {@link Timer} and will be available through the {@link Timer#getInfo()} method
    * @param persistent True if this timer is persistent. False otherwise
    */
   public TimerImpl(UUID id, TimerServiceImpl service, Date initialExpiry, long intervalDuration, Date nextEpiry,
         Serializable info, boolean persistent)
   {
      assert service != null : "service is null";
      assert id != null : "id is null";

      this.id = id;
      this.timerService = service;
      this.timerService.addTimer(this);

      this.timedObjectInvoker = service.getInvoker();
      this.info = info;
      this.persistent = persistent;
      this.initialExpiration = initialExpiry;
      this.intervalDuration = intervalDuration;
      this.nextExpiration = nextEpiry;
      this.previousRun = null;

      // create a timer handle for this timer
      this.handle = new TimerHandleImpl(this.id, this.timedObjectInvoker.getTimedObjectId(), service);

      setTimerState(TimerState.CREATED);
      
   }

   /**
    * Creates a {@link TimerImpl} out of a persisted timer
    * 
    * @param persistedTimer The persisted state of the timer
    * @param service The timer service to which this timer belongs  
    */
   public TimerImpl(TimerEntity persistedTimer, TimerServiceImpl service)
   {
      this(persistedTimer.getId(), service, persistedTimer.getInitialDate(), persistedTimer.getInterval(),
            persistedTimer.getNextDate(), persistedTimer.getInfo(), true);
      this.previousRun = persistedTimer.getPreviousRun();
   }

   /**
    * Returns the id of this timer
    * @return
    */
   public UUID getId()
   {
      return this.id;
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
    * Sets the next timeout of this timer
    * @param next The next scheduled timeout of this timer
    */
   public void setNextTimeout(Date next)
   {
      this.nextExpiration = next;
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
      // TODO: Rethink this implementation

      // first check the validity of the timer state
      this.assertTimerState();
      if (this.nextExpiration == null)
      {
         throw new NoMoreTimeoutsException("No more timeouts for timer " + this);
      }
      long currentTimeInMillis = System.currentTimeMillis();
      long nextTimeoutInMillis = this.nextExpiration.getTime();

      // if the next expiration is *not* in future and the repeat interval isn't
      // a positive number (i.e. no repeats) then there won't be any more timeouts.
      // So throw a NoMoreTimeoutsException.
      // NOTE: We check for intervalDuration and not just nextExpiration because,
      // it's a valid case where the nextExpiration is in past (maybe the server was
      // down when the timeout was expected) 
      //      if (nextTimeoutInMillis < currentTimeInMillis && this.intervalDuration <= 0)
      //      {
      //         throw new NoMoreTimeoutsException("No more timeouts for timer " + this);
      //      }
      return nextTimeoutInMillis - currentTimeInMillis;
   }

   @Override
   public boolean isAutoTimer()
   {
      return false;
   }
   
   /**
    * Cancels any scheduled timer task and appropriately updates the state of this timer
    * to {@link TimerState#CANCELED}
    * <p>
    *   This method then persists the timer changes
    * </p>
    */
   protected void cancelTimer()
   {
      if (timerState != TimerState.EXPIRED)
      {
         setTimerState(TimerState.CANCELED);
      }
      // cancel any scheduled timer task
      future.cancel(false);

      // persist changes
      timerService.persistTimer(this);
   }

   /**
    * Returns the initial (first) timeout date of this timer
    * @return
    */
   public Date getInitialExpiration()
   {
      return this.initialExpiration;
   }

   /**
    * Returns the interval (in milli seconds), between timeouts, of this timer.
    * @return
    */
   public long getInterval()
   {
      return this.intervalDuration;
   }

   /**
    * Returns the timed object id to which this timer belongs
    * @return
    */
   public String getTimedObjectId()
   {
      return this.timerService.getInvoker().getTimedObjectId();
   }

   /**
    * Returns the timer service through which this timer was created
    * @return
    */
   public TimerServiceImpl getTimerService()
   {
      return this.timerService;
   }

   /**
    * Returns true if this timer is active. Else returns false.
    * <p>
    *   A timer is considered to be "active", if its {@link TimerState}
    *   is neither of the following:
    *   <ul>
    *   <li>{@link TimerState#CANCELED}</li>
    *   <li>{@link TimerState#CANCELED_IN_TX}</li>
    *   <li>{@link TimerState#EXPIRED}</li>
    *   </ul>
    * </p>
    * @return
    */
   public boolean isActive()
   {
      return !isCanceled() && !isExpired();
   }

   /**
    * Returns true if this timer is either in {@link TimerState#CANCELED_IN_TX} or in
    * {@link TimerState#CANCELED} state. Else returns false.
    * 
    * @return
    */
   public boolean isCanceled()
   {
      return timerState == TimerState.CANCELED_IN_TX || timerState == TimerState.CANCELED;
   }

   /**
    * Returns true if this timer is in {@link TimerState#EXPIRED} state. Else returns false
    * @return
    */
   public boolean isExpired()
   {
      return timerState == TimerState.EXPIRED;
   }

   /**
    * Returns true if this timer is in {@link TimerState#RETRY_TIMEOUT}. Else returns false.
    * 
    * @return
    */
   public boolean isInRetry()
   {
      return timerState == TimerState.RETRY_TIMEOUT;
   }

   /**
    * Returns the {@link Date} of the previous timeout of this timer 
    * @return
    */
   public Date getPreviousRun()
   {
      return this.previousRun;
   }

   /**
    * Sets the {@link Date} of the previous timeout of this timer
    * @param previousRun
    */
   public void setPreviousRun(Date previousRun)
   {
      this.previousRun = previousRun;
   }

   /**
    * Returns the current state of this timer
    * @return
    */
   public TimerState getState()
   {
      return this.timerState;
   }

   /**
    * Asserts that the timer is <i>not</i> in any of the following states:
    * <ul>
    *   <li>{@link TimerState#CANCELED}</li>
    *   <li>{@link TimerState#CANCELED_IN_TX}</li>
    *   <li>{@link TimerState#EXPIRED}</li>
    * </ul>
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
    * Expire, and remove it from the timer service. 
    * 
    * 
    */
   public void expireTimer()
   {
      logger.debug("expireTimer: " + this);
      setTimerState(TimerState.EXPIRED);
      // remove from timerservice
      timerService.removeTimer(this);
      // Cancel any scheduled timer task
      future.cancel(false);

      // persist changes
      timerService.persistTimer(this);
   }

   /**
    * Sets the state of this timer
    * @param state The state of this timer
    */
   public void setTimerState(TimerState state)
   {
      this.timerState = state;
   }

   /**
    * Returns the current persistent state of this timer 
    */
   public TimerEntity getPersistentState()
   {
      if (this.persistent == false)
      {
         throw new IllegalStateException("Timer " + this + " is not persistent");
      }
      if (this.persistentState == null)
      {
         // create a new new persistent state
         this.persistentState = this.createPersistentState();
      }
      else
      {
         // just refresh the fields which change in the persistent timer
         this.persistentState.setNextDate(this.nextExpiration);
         this.persistentState.setPreviousRun(this.previousRun);
         this.persistentState.setTimerState(this.timerState);
      }
      return this.persistentState;
   }

   /**
    * Suspends any currently scheduled task for this timer
    * <p>
    *   Note that, suspend does <b>not</b> cancel the {@link Timer}. Instead,
    *   it just cancels the <b>next scheduled timeout</b>. So once the {@link Timer}
    *   is restored (whenever that happens), the {@link Timer} will continue to 
    *   timeout at appropriate times. 
    * </p>
    */
   public void suspend()
   {
      if (this.future != null)
      {
         this.future.cancel(false);
      }
   }
   
   /**
    * Creates and schedules a {@link TimerTask} for the next timeout of this timer
    */
   public void scheduleTimeout()
   {
      // create the timer task
      Runnable timerTask = this.getTimerTask();
      // find out how long is it away from now
      long delay = this.nextExpiration.getTime() - System.currentTimeMillis();
      // if in past, then trigger immediately
      if (delay < 0)
      {
         delay = 0;
      }
      if (this.intervalDuration > 0)
      {
         logger.debug("Scheduling timer " + this + " at fixed rate, starting at " + delay
               + " milli seconds from now with repeated interval=" + this.intervalDuration);
         // schedule the task
         this.future = this.timerService.getExecutor().scheduleAtFixedRate(timerTask, delay, this.intervalDuration,
               TimeUnit.MILLISECONDS);
      }
      else
      {
         logger.debug("Scheduling a single action timer " + this + " starting at " + delay + " milli seconds from now");
         // schedule the task
         this.future = this.getTimerService().getExecutor().schedule(timerTask, delay, TimeUnit.MILLISECONDS);
      }
   }

   /**
    * Creates and returns a new persistent state of this timer
    * @return
    */
   protected TimerEntity createPersistentState()
   {
      return new TimerEntity(this);
   }

   /**
    * Returns the task which handles the timeouts of this {@link TimerImpl}
    * @return
    * 
    * @see TimerTask
    */
   protected TimerTask<?> getTimerTask()
   {
      return new TimerTask<TimerImpl>(this);
   }
   
   /**
    * A nice formatted string output for this timer
    * {@inheritDoc}
    */
   @Override
   public String toString()
   {
      //TODO: Cache this 
      StringBuilder sb = new StringBuilder();
      sb.append("[id=");
      sb.append(this.id);
      sb.append(" ");
      sb.append("timedObjectId=");
      if (this.timedObjectInvoker == null)
      {
         sb.append("null");
      }
      else
      {
         sb.append(this.timedObjectInvoker.getTimedObjectId());
      }
      sb.append(" ");
      sb.append("auto-timer?:");
      sb.append(this.isAutoTimer());
      sb.append(" ");
      sb.append("persistent?:");
      sb.append(this.persistent);
      sb.append(" ");
      sb.append("timerService=");
      sb.append(this.timerService);
      sb.append(" ");
      sb.append("initialExpiration=");
      sb.append(this.initialExpiration);
      sb.append(" ");
      sb.append("intervalDuration(in milli sec)=");
      sb.append(this.intervalDuration);
      sb.append(" ");
      sb.append("nextExpiration=");
      sb.append(this.nextExpiration);
      sb.append(" ");
      sb.append("timerState=");
      sb.append(this.timerState);

      return sb.toString();
   }

}
