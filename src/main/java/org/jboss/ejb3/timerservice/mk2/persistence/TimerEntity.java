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

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
@Entity
@Table(name="timer")
public class TimerEntity
{
   @Id
   private UUID id;
   
   @Column(nullable=false)
   private String targetId;
   
   @Column(nullable=false)
   private Date initialDate;
   
   private long interval;
   
   @Lob
   private Object instanceKey;
   
   @Lob
   private Serializable info;

   public UUID getId()
   {
      return id;
   }

   public void setId(UUID id)
   {
      this.id = id;
   }

   public String getTargetId()
   {
      return targetId;
   }

   public void setTargetId(String targetId)
   {
      this.targetId = targetId;
   }

   public Date getInitialDate()
   {
      return initialDate;
   }

   public void setInitialDate(Date initialDate)
   {
      this.initialDate = initialDate;
   }

   public long getInterval()
   {
      return interval;
   }

   public void setInterval(long interval)
   {
      this.interval = interval;
   }

   public Object getInstanceKey()
   {
      return instanceKey;
   }

   public void setInstanceKey(Object instanceKey)
   {
      this.instanceKey = instanceKey;
   }

   public Serializable getInfo()
   {
      return info;
   }

   public void setInfo(Serializable info)
   {
      this.info = info;
   }
}
