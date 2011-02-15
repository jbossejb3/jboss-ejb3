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

import org.jboss.ejb3.effigy.AccessTimeoutEffigy;
import org.jboss.ejb3.effigy.SessionBeanEffigy;
import org.jboss.ejb3.effigy.StatefulTimeoutEffigy;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;
import org.jboss.metadata.ejb.spec.BusinessLocalsMetaData;
import org.jboss.metadata.ejb.spec.BusinessRemotesMetaData;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class JBossSessionBeanEffigy extends JBossEnterpriseBeanEffigy
        implements SessionBeanEffigy
{
   public JBossSessionBeanEffigy(ClassLoader classLoader, JBossSessionBeanMetaData beanMetaData)
           throws ClassNotFoundException
   {
      super(classLoader, beanMetaData);
   }

   @Override
   public AccessTimeoutEffigy getAccessTimeout(Method method)
   {
      return null;
   }

   @Override
   public Method getAfterBeginMethod()
   {
      return null;
   }

   @Override
   public Method getAfterCompletionMethod()
   {
      return null;
   }

   @Override
   public Method getBeforeCompletionMethod()
   {
      return null;
   }

   @Override
   public StatefulTimeoutEffigy getStatefulTimeout()
   {
      return null;
   }

   @Override
   protected Set<Class<?>> getAllViews(ClassLoader cl)
   {
      JBossSessionBeanMetaData sessionBeanMetaData = (JBossSessionBeanMetaData) this.getBeanMetaData();
      Set<Class<?>> views = new HashSet<Class<?>>();
      try
      {
         // home
         String home = sessionBeanMetaData.getHome();
         if (home != null)
         {
            views.add(cl.loadClass(home));
         }
         // remote
         String remote = sessionBeanMetaData.getRemote();
         if (remote != null)
         {
            views.add(cl.loadClass(remote));
         }

         // local home
         String localHome = sessionBeanMetaData.getLocalHome();
         if (localHome != null)
         {
            views.add(cl.loadClass(localHome));
         }

         // local
         String local = sessionBeanMetaData.getLocal();
         if (local != null)
         {
            views.add(cl.loadClass(local));
         }

         // business locals
         BusinessLocalsMetaData businessLocals = sessionBeanMetaData.getBusinessLocals();
         if (businessLocals != null)
         {
            for (String businessLocal : businessLocals)
            {
               views.add(cl.loadClass(businessLocal));
            }
         }

         // business remotes
         BusinessRemotesMetaData businessRemotes = sessionBeanMetaData.getBusinessRemotes();
         if (businessRemotes != null)
         {
            for (String businessRemote : businessRemotes)
            {
               views.add(cl.loadClass(businessRemote));
            }
         }

         // service endpoint
         String serviceEndpoint = sessionBeanMetaData.getServiceEndpoint();
         if (serviceEndpoint != null)
         {
            views.add(cl.loadClass(serviceEndpoint));
         }
      }
      catch (ClassNotFoundException cnfe)
      {
         throw new RuntimeException(cnfe);
      }

      return views;
   }

   @Override
   protected Class<?> getMethodInterface(ClassLoader cl, MethodInterfaceType methodIntf) throws ClassNotFoundException
   {
      JBossSessionBeanMetaData sessionBean = (JBossSessionBeanMetaData) this.getBeanMetaData();
      String className = null;
      if (methodIntf == null)
      {
         return this.getEjbClass();
      }
      switch (methodIntf)
      {
         case Home:
            String home = sessionBean.getHome();
            if (home == null || home.trim().isEmpty())
            {
               return null;
            }
            className = home;
            break;

         case Local:
            String local = sessionBean.getLocal();
            if (local == null || local.trim().isEmpty())
            {
               return null;
            }
            className = local;
            break;

         case LocalHome:
            String localHome = sessionBean.getLocalHome();
            if (localHome == null || localHome.isEmpty())
            {
               return null;
            }
            className = localHome;
            break;

         case Remote:
            String remote = sessionBean.getRemote();
            if (remote == null || remote.isEmpty())
            {
               return null;
            }
            className = remote;
            break;

         case ServiceEndpoint:
            String serviceEndPoint = sessionBean.getServiceEndpoint();
            if (serviceEndPoint == null || serviceEndPoint.isEmpty())
            {
               return null;
            }
            className = serviceEndPoint;
            break;

         default:
            throw new IllegalArgumentException("Unknown method interface type: " + methodIntf);

      }
      return cl.loadClass(className);
   }

}
