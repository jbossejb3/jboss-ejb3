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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * TimeoutMethod
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Entity
public class TimeoutMethod implements Serializable
{

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private Long id;

   private String methodName;

   private String[] methodParams;

   public TimeoutMethod()
   {

   }

   public TimeoutMethod(String methodName, String[] methodParams)
   {
      this.methodName = methodName;
      this.methodParams = methodParams;
   }

   public Long getId()
   {
      return id;
   }

   public String getMethodName()
   {
      return methodName;
   }

   public void setMethodName(String methodName)
   {
      this.methodName = methodName;
   }

   public String[] getMethodParams()
   {
      return methodParams;
   }

   public void setMethodParams(String[] methodParams)
   {
      this.methodParams = methodParams;
   }

}
