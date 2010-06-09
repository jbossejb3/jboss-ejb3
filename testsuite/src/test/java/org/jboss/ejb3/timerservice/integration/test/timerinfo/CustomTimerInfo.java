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
package org.jboss.ejb3.timerservice.integration.test.timerinfo;

import java.io.Serializable;

/**
 * CustomTimerInfo
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class CustomTimerInfo implements Serializable
{

   private String name;

   public CustomTimerInfo(String name)
   {
      if (name == null)
      {
         throw new IllegalArgumentException("Name cannot be null for " + this.getClass().getName());
      }
      this.name = name;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
      {
         return false;
      }
      if (this == obj)
      {
         return true;
      }
      if (obj instanceof CustomTimerInfo == false)
      {
         return false;
      }
      CustomTimerInfo other = (CustomTimerInfo) obj;
      return this.name.equals(other.name);
   }
   
   @Override
   public int hashCode()
   {
      return this.name.hashCode();
   }
   
   @Override
   public String toString()
   {
      return this.getClass().getName() + " name : " + this.name;
   }
}
