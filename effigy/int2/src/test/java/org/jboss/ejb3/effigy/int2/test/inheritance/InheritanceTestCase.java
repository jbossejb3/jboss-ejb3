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
package org.jboss.ejb3.effigy.int2.test.inheritance;

import org.jboss.ejb3.effigy.ApplicationExceptionEffigy;
import org.jboss.ejb3.effigy.SessionBeanEffigy;
import org.jboss.ejb3.effigy.common.JBossBeanEffigyInfo;
import org.jboss.ejb3.effigy.int2.JBossBeanEffigyFactory;
import org.jboss.metadata.ejb.jboss.JBoss51MetaData;
import org.jboss.metadata.ejb.jboss.JBossAssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.metadata.ejb.spec.ApplicationExceptionMetaData;
import org.jboss.metadata.ejb.spec.ApplicationExceptionsMetaData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class InheritanceTestCase
{
   private static ApplicationExceptionMetaData applicationException(Class<?> exceptionClass, boolean rollback, Boolean inherited)
   {
      ApplicationExceptionMetaData applicationExceptionMetaData = new ApplicationExceptionMetaData();
      applicationExceptionMetaData.setExceptionClass(exceptionClass.getName());
      applicationExceptionMetaData.setRollback(rollback);
      applicationExceptionMetaData.setInherited(inherited);
      return applicationExceptionMetaData;
   }

   @Test
   public void testNonInheritedAppException() throws ClassNotFoundException
   {
      ApplicationExceptionsMetaData applicationExceptionsMetaData = new ApplicationExceptionsMetaData();
      applicationExceptionsMetaData.add(applicationException(NonInheritedAppException.class, true, false));

      JBossAssemblyDescriptorMetaData assemblyDescriptorMetaData = new JBossAssemblyDescriptorMetaData();
      assemblyDescriptorMetaData.setApplicationExceptions(applicationExceptionsMetaData);
      
      JBoss51MetaData jarMetaData = new JBoss51MetaData();
      jarMetaData.setAssemblyDescriptor(assemblyDescriptorMetaData);

      JBossEnterpriseBeansMetaData enterpriseBeansMetaData = new JBossEnterpriseBeansMetaData();
      enterpriseBeansMetaData.setEjbJarMetaData(jarMetaData);

      JBossSessionBean31MetaData beanMetaData = new JBossSessionBean31MetaData();
      beanMetaData.setEjbClass(InheritanceBean.class.getName());
      beanMetaData.setEjbName(InheritanceBean.class.getSimpleName());
      beanMetaData.setEnterpriseBeansMetaData(enterpriseBeansMetaData);

      JBossBeanEffigyFactory factory = new JBossBeanEffigyFactory();
      ClassLoader classLoader = ClassLoader.getSystemClassLoader();
      JBossBeanEffigyInfo info = new JBossBeanEffigyInfo(classLoader, beanMetaData);
      SessionBeanEffigy effigy = factory.create(info, SessionBeanEffigy.class);
      ApplicationExceptionEffigy applicationExceptionEffigy = effigy.getApplicationException(SubException.class);
      assertNotNull(applicationExceptionEffigy);
      assertEquals(NonInheritedAppException.class, applicationExceptionEffigy.getExceptionClass());
      assertFalse(applicationExceptionEffigy.isInherited());
   }
}
