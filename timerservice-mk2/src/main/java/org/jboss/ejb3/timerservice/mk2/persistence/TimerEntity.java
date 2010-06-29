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
package org.jboss.ejb3.timerservice.mk2.persistence;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.jboss.ejb3.timerservice.mk2.TimerImpl;
import org.jboss.ejb3.timerservice.mk2.TimerState;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
@Entity
@Table(name = "timer")
@Inheritance(strategy = InheritanceType.JOINED)
public class TimerEntity implements Serializable
{
   @Id
   protected UUID id;

   @Column(nullable = false)
   @NotNull
   protected String timedObjectId;

   @Column(nullable = false)
   @NotNull
   protected Date initialDate;

   protected long interval;

   protected Date nextDate;

   protected Date previousRun;

   @Lob
   protected byte[] info;


   protected TimerState timerState;

   public TimerEntity()
   {

   }

   public TimerEntity(TimerImpl timer)
   {
      this.id = timer.getId();
      this.initialDate = timer.getInitialExpiration();
      this.interval = timer.getInterval();
      this.nextDate = timer.getNextExpiration();
      this.previousRun = timer.getPreviousRun();
      this.timerState = timer.getState();
      this.timedObjectId = timer.getTimedObjectId();
      if (timer.getTimerInfo() != null)
      {
         this. info = this.getBytes(timer.getTimerInfo());
      }

   }

   public UUID getId()
   {
      return id;
   }

   public String getTimedObjectId()
   {
      return timedObjectId;
   }

   public Date getInitialDate()
   {
      return initialDate;
   }

   public long getInterval()
   {
      return interval;
   }

   public byte[] getInfo()
   {
      return this.info;
   }

   public Date getNextDate()
   {
      return nextDate;
   }

   public void setNextDate(Date nextDate)
   {
      this.nextDate = nextDate;
   }

   public Date getPreviousRun()
   {
      return previousRun;
   }

   public void setPreviousRun(Date previousRun)
   {
      this.previousRun = previousRun;
   }

   public TimerState getTimerState()
   {
      return timerState;
   }

   public void setTimerState(TimerState timerState)
   {
      this.timerState = timerState;
   }

   public boolean isCalendarTimer()
   {
      return false;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
      {
         return false;
      }
      if (obj instanceof TimerEntity == false)
      {
         return false;
      }
      TimerEntity other = (TimerEntity) obj;
      if (this.id == null)
      {
         return false;
      }
      return this.id.equals(other.id);
   }

   @Override
   public int hashCode()
   {
      if (this.id == null)
      {
         return super.hashCode();
      }
      return this.id.hashCode();
   }

   private byte[] getBytes(Serializable ser)
   {
      try
      {
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
         objectOutputStream.writeObject(ser);
         return outputStream.toByteArray();
      }
      catch (IOException ioe)
      {
         throw new RuntimeException("Could not get bytes out of serializable object: " + ser, ioe);
      }

   }
   
}
