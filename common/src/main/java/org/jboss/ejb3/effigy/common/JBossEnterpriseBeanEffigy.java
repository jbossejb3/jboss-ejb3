/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @authors tag. See the copyright.txt in the
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
package org.jboss.ejb3.effigy.common;

import org.jboss.ejb3.effigy.ApplicationExceptionEffigy;
import org.jboss.ejb3.effigy.EnterpriseBeanEffigy;
import org.jboss.metadata.ejb.jboss.JBossAssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.ApplicationExceptionMetaData;
import org.jboss.metadata.ejb.spec.ApplicationExceptionsMetaData;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class JBossEnterpriseBeanEffigy implements EnterpriseBeanEffigy
{
   private static final ApplicationExceptionEffigy NULL = new JBossApplicationExceptionEffigy();
   
   private JBossEnterpriseBeanMetaData beanMetaData;
   private Class<?> ejbClass;
   private Collection<ApplicationExceptionEffigy> applicationExceptionEffigies;
   private Map<Class<?>, ApplicationExceptionEffigy> applicationExceptionEffigyMap = new ConcurrentHashMap<Class<?>, ApplicationExceptionEffigy>();

   protected JBossEnterpriseBeanEffigy(ClassLoader classLoader, JBossEnterpriseBeanMetaData beanMetaData)
           throws ClassNotFoundException
   {
      this.beanMetaData = beanMetaData;
      this.ejbClass = classLoader.loadClass(beanMetaData.getEjbClass());
      this.applicationExceptionEffigies = createApplicationExceptionEffigies(classLoader, beanMetaData.getEjbJarMetaData().getAssemblyDescriptor());
   }

   private Collection<ApplicationExceptionEffigy> createApplicationExceptionEffigies(ClassLoader classLoader, JBossAssemblyDescriptorMetaData assemblyDescriptorMetaData)
           throws ClassNotFoundException
   {
      if(assemblyDescriptorMetaData == null)
         return null;

      ApplicationExceptionsMetaData applicationExceptionsMetaData = assemblyDescriptorMetaData.getApplicationExceptions();
      if(applicationExceptionsMetaData == null)
         return null;

      Collection<ApplicationExceptionEffigy> applicationExceptionEffigies = new LinkedList<ApplicationExceptionEffigy>();
      for(ApplicationExceptionMetaData applicationExceptionMetaData : applicationExceptionsMetaData)
      {
         applicationExceptionEffigies.add(createApplicationExceptionEffigy(classLoader, applicationExceptionMetaData));
      }

      return applicationExceptionEffigies;
   }

   protected ApplicationExceptionEffigy createApplicationExceptionEffigy(ClassLoader classLoader, ApplicationExceptionMetaData metaData)
           throws ClassNotFoundException
   {
      return new JBossApplicationExceptionEffigy(classLoader, metaData);
   }
   
   @Override
   public ApplicationExceptionEffigy getApplicationException(Class<?> exceptionClass)
   {
      if(applicationExceptionEffigies == null)
         return null;

      ApplicationExceptionEffigy applicationExceptionEffigy = applicationExceptionEffigyMap.get(exceptionClass);
      if(applicationExceptionEffigy == NULL)
         return null;
      if(applicationExceptionEffigy != null)
         return applicationExceptionEffigy;
      applicationExceptionEffigy = getApplicationException(exceptionClass, false);
      if(applicationExceptionEffigy == null)
         applicationExceptionEffigyMap.put(exceptionClass, NULL);
      else
         applicationExceptionEffigyMap.put(exceptionClass, applicationExceptionEffigy);
      return applicationExceptionEffigy;
   }

   /**
    * slow
    */
   private ApplicationExceptionEffigy getApplicationException(Class<?> exceptionClass, boolean onlyInherited)
   {
      for(ApplicationExceptionEffigy applicationExceptionEffigy : applicationExceptionEffigies)
      {
         boolean isInherited = applicationExceptionEffigy.isInherited();
         if((isInherited && onlyInherited) || !onlyInherited)
            if(applicationExceptionEffigy.getExceptionClass().equals(exceptionClass))
               return applicationExceptionEffigy;
      }

      return getApplicationException(exceptionClass.getSuperclass(), true);
   }

   @Override
   public Class<?> getEjbClass()
   {
      return ejbClass;
   }

   @Override
   public String getName()
   {
      return beanMetaData.getEjbName();
   }
}
