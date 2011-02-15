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
import org.jboss.ejb3.effigy.InterceptorEffigy;
import org.jboss.metadata.ejb.jboss.JBossAssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.ApplicationExceptionMetaData;
import org.jboss.metadata.ejb.spec.ApplicationExceptionsMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionsMetaData;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;
import org.jboss.metadata.ejb.spec.MethodMetaData;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.metadata.ejb.spec.MethodsMetaData;

import javax.ejb.TransactionAttributeType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class JBossEnterpriseBeanEffigy implements EnterpriseBeanEffigy
{
   private static final ApplicationExceptionEffigy NULL = new JBossApplicationExceptionEffigy();

   private JBossEnterpriseBeanMetaData beanMetaData;
   private Class<?> ejbClass;
   private Collection<ApplicationExceptionEffigy> applicationExceptionEffigies;
   private Map<Class<?>, ApplicationExceptionEffigy> applicationExceptionEffigyMap = new ConcurrentHashMap<Class<?>, ApplicationExceptionEffigy>();

   private Map<Method, TransactionAttributeType> methodApplicableTransactionAttributeTypes = new ConcurrentHashMap<Method, TransactionAttributeType>();

   public JBossEnterpriseBeanEffigy(ClassLoader classLoader, JBossEnterpriseBeanMetaData beanMetaData)
           throws ClassNotFoundException
   {
      this.beanMetaData = beanMetaData;
      this.ejbClass = classLoader.loadClass(beanMetaData.getEjbClass());
      this.applicationExceptionEffigies = createApplicationExceptionEffigies(classLoader, beanMetaData.getEjbJarMetaData().getAssemblyDescriptor());
      // process the transaction attributes of the methods on the bean
      this.initTransactionAttributeTypesForMethods(classLoader, beanMetaData);
   }

   private Collection<ApplicationExceptionEffigy> createApplicationExceptionEffigies(ClassLoader classLoader, JBossAssemblyDescriptorMetaData assemblyDescriptorMetaData)
           throws ClassNotFoundException
   {
      if (assemblyDescriptorMetaData == null)
      {
         return null;
      }

      ApplicationExceptionsMetaData applicationExceptionsMetaData = assemblyDescriptorMetaData.getApplicationExceptions();
      if (applicationExceptionsMetaData == null)
      {
         return null;
      }

      Collection<ApplicationExceptionEffigy> applicationExceptionEffigies = new LinkedList<ApplicationExceptionEffigy>();
      for (ApplicationExceptionMetaData applicationExceptionMetaData : applicationExceptionsMetaData)
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

   /**
    * slow
    */
   private ApplicationExceptionEffigy findApplicationException(Class<?> exceptionClass)
   {
      for (ApplicationExceptionEffigy applicationExceptionEffigy : applicationExceptionEffigies)
      {
         if (applicationExceptionEffigy.getExceptionClass().equals(exceptionClass))
         {
            return applicationExceptionEffigy;
         }
      }

      Class<?> superclass = exceptionClass.getSuperclass();
      if (superclass == null)
      {
         return null;
      }
      return findApplicationException(exceptionClass.getSuperclass());
   }

   @Override
   public Iterable<InterceptorEffigy> getAllInterceptors()
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.effigy.common.JBossEnterpriseBeanEffigy.getAllInterceptors");
   }

   @Override
   public ApplicationExceptionEffigy getApplicationException(Class<?> exceptionClass)
   {
      if (applicationExceptionEffigies == null)
      {
         return null;
      }

      ApplicationExceptionEffigy applicationExceptionEffigy = applicationExceptionEffigyMap.get(exceptionClass);
      if (applicationExceptionEffigy == NULL)
      {
         return null;
      }
      if (applicationExceptionEffigy != null)
      {
         return applicationExceptionEffigy;
      }
      applicationExceptionEffigy = findApplicationException(exceptionClass);
      if (applicationExceptionEffigy == null)
      {
         applicationExceptionEffigyMap.put(exceptionClass, NULL);
      }
      else
      {
         applicationExceptionEffigyMap.put(exceptionClass, applicationExceptionEffigy);
      }
      return applicationExceptionEffigy;
   }

   @Override
   public Iterable<Method> getAroundInvokes()
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.effigy.common.JBossEnterpriseBeanEffigy.getAroundInvokes");
   }

   protected JBossEnterpriseBeanMetaData getBeanMetaData()
   {
      return beanMetaData;
   }

   @Override
   public Class<?> getEjbClass()
   {
      return ejbClass;
   }

   @Override
   public Iterable<InterceptorEffigy> getInterceptors(Method method)
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.effigy.common.JBossEnterpriseBeanEffigy.getInterceptors");
   }

   @Override
   public String getName()
   {
      return beanMetaData.getEjbName();
   }

   @Override
   public Iterable<Method> getPostConstructs()
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.effigy.common.JBossEnterpriseBeanEffigy.getPostConstructs");
   }

   @Override
   public Iterable<Method> getPreDestroys()
   {
      throw new RuntimeException("NYI: org.jboss.ejb3.effigy.common.JBossEnterpriseBeanEffigy.getPreDestroys");
   }

   @Override
   public TransactionAttributeType getTransactionAttributeType(Method method)
   {
      return this.methodApplicableTransactionAttributeTypes.get(method);
   }

   private void initTransactionAttributeTypesForMethods(ClassLoader cl, JBossEnterpriseBeanMetaData enterpriseBeanMetaData)
   {
      String ejbName = enterpriseBeanMetaData.getEjbName();
      JBossAssemblyDescriptorMetaData assemblyDescriptorMetaData = enterpriseBeanMetaData.getJBossMetaData().getAssemblyDescriptor();
      if (assemblyDescriptorMetaData == null)
      {
         return;
      }
      ContainerTransactionsMetaData containerTransactions = assemblyDescriptorMetaData.getContainerTransactionsByEjbName(ejbName);
      if (containerTransactions == null)
      {
         return;
      }
      for (ContainerTransactionMetaData transactionMetaData : containerTransactions)
      {
         TransactionAttributeType txAttributeType = transactionMetaData.getTransAttribute();

         MethodsMetaData methods = transactionMetaData.getMethods();
         if (methods == null || methods.isEmpty())
         {
            continue;
         }
         for (MethodMetaData methodMetaData : methods)
         {
            String methodName = methodMetaData.getMethodName();
            MethodParametersMetaData methodParams = methodMetaData.getMethodParams();
            String[] params = null;
            if (methodParams != null)
            {
               params = methodParams.toArray(params);
            }
            MethodInterfaceType methodIntf = methodMetaData.getMethodIntf();
            try
            {
               if (methodIntf != null)
               {
                  Class<?> declaringClass = this.getMethodInterface(cl, methodIntf);
                  Collection<Method> applicableMethods = this.findMethods(cl, declaringClass, methodName, params);
                  for (Method applicableMethod : applicableMethods)
                  {
                     this.methodApplicableTransactionAttributeTypes.put(applicableMethod, txAttributeType);
                  }

               }
               else
               {
                  Set<Class<?>> views = this.getAllViews(cl);
                  if (views != null)
                  {
                     for (Class<?> view : views)
                     {
                        Collection<Method> applicableMethods = this.findMethods(cl, view, methodName, params);
                        for (Method applicableMethod : applicableMethods)
                        {
                           this.methodApplicableTransactionAttributeTypes.put(applicableMethod, txAttributeType);
                        }
                     }
                  }
               }
            }
            catch (ClassNotFoundException cnfe)
            {
               throw new RuntimeException("Could not process container managed transactions for bean: " + ejbName, cnfe);
            }
            catch (NoSuchMethodException nsme)
            {
               throw new RuntimeException("Could not find method while processing container managed transactions for bean: " + ejbName, nsme);
            }

         }


      }

   }

   private Collection<Method> findMethods(ClassLoader cl, Class<?> klass, String methodName, String... methodParams) throws NoSuchMethodException
   {
      if (methodName.equals("*"))
      {
         return Arrays.asList(klass.getMethods());
      }
      Class<?>[] methodParamTypes = null;
      if (methodParams != null)
      {
         methodParamTypes = new Class<?>[methodParams.length];
         int i = 0;
         for (String methodParam : methodParams)
         {
            try
            {
               methodParamTypes[i++] = cl.loadClass(methodParam);
            }
            catch (ClassNotFoundException cnfe)
            {
               throw new RuntimeException(cnfe);
            }
         }
      }
      Method method = klass.getMethod(methodName, methodParamTypes);
      return Collections.singleton(method);

   }

   protected Class<?> getMethodInterface(ClassLoader cl, MethodInterfaceType methodIntf) throws ClassNotFoundException
   {
      return null;
   }

   protected Set<Class<?>> getAllViews(ClassLoader cl)
   {
      return Collections.emptySet();
   }

}
