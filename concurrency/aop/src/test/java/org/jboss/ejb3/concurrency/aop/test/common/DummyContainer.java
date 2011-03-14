/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.ejb3.concurrency.aop.test.common;

import org.jboss.aop.MethodInfo;
import org.jboss.aop.util.MethodHashing;
import org.jboss.ejb3.effigy.SessionBeanEffigy;
import org.jboss.ejb3.effigy.common.JBossBeanEffigyInfo;
import org.jboss.ejb3.effigy.int2.JBossBeanEffigyFactory;
import org.jboss.ejb3.interceptors.container.BeanContext;
import org.jboss.ejb3.interceptors.direct.DirectContainer;
import org.jboss.metadata.annotation.creator.ejb.jboss.JBoss50Creator;
import org.jboss.metadata.annotation.finder.DefaultAnnotationFinder;
import org.jboss.metadata.ejb.jboss.JBoss50MetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class DummyContainer<T> extends DirectContainer<T>
{
   private SessionBeanEffigy effigy;
   
   public DummyContainer(String name, String domainName, Class<? extends T> beanClass)
      throws ClassNotFoundException
   {
      super(name, domainName, beanClass);

      JBoss50Creator creator = new JBoss50Creator(new DefaultAnnotationFinder());
      JBoss50MetaData metaData = creator.create(new HashSet(Arrays.asList(beanClass)));
      JBossSessionBean31MetaData beanMetaData = (JBossSessionBean31MetaData) metaData.getEnterpriseBean(name);
      
      this.effigy = effigy(name, beanClass);
   }

   private static SessionBeanEffigy effigy(String name, Class<?> beanClass)
   {
      JBoss50Creator creator = new JBoss50Creator(new DefaultAnnotationFinder());
      JBoss50MetaData metaData = creator.create(new HashSet(Arrays.asList(beanClass)));
      JBossSessionBean31MetaData beanMetaData = (JBossSessionBean31MetaData) metaData.getEnterpriseBean(name);

      JBossBeanEffigyInfo info = new JBossBeanEffigyInfo(beanClass.getClassLoader(), beanMetaData);
      try
      {
         return new JBossBeanEffigyFactory().create(info, SessionBeanEffigy.class);
      }
      catch (ClassNotFoundException e)
      {
         throw new RuntimeException(e);
      }
   }

   @Override
   public Object invoke(BeanContext<T> target, Method method, Object[] arguments) throws Throwable
   {
      long methodHash = MethodHashing.calculateHash(method);
      MethodInfo info = getAdvisor().getMethodInfo(methodHash);
      if(info == null)
         throw new IllegalArgumentException("method " + method + " is not under advisement by " + this);
      DummyContainerMethodInvocation invocation = new DummyContainerMethodInvocation(info, target, arguments, effigy);
      return invocation.invokeNext();
   }
}
