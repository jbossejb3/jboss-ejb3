/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.ejb3.effigy.common.test;

import org.jboss.ejb3.effigy.EnterpriseBeanEffigy;
import org.jboss.ejb3.effigy.SessionBeanEffigy;
import org.jboss.ejb3.effigy.common.JBossBeanEffigyInfo;
import org.jboss.ejb3.effigy.int2.JBossBeanEffigyFactory;
import org.jboss.metadata.ejb.jboss.JBossAssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;
import org.jboss.metadata.ejb.spec.BusinessLocalsMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionsMetaData;
import org.jboss.metadata.ejb.spec.MethodMetaData;
import org.jboss.metadata.ejb.spec.MethodsMetaData;
import org.junit.Assert;
import org.junit.Test;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagementType;
import java.lang.reflect.Method;

/**
 * Author : Jaikiran Pai
 */
public class TransactionAttributeTypeUnitTestCase
{
   @Test
   public void testWildCardMethodName() throws  Exception
   {
      ContainerTransactionMetaData mandatoryContainerManagedTx = new ContainerTransactionMetaData();
      mandatoryContainerManagedTx.setTransAttribute(TransactionAttributeType.MANDATORY);

      MethodMetaData wildCardMethod = new MethodMetaData();
      wildCardMethod.setMethodName("*");
      wildCardMethod.setEjbName(SimpleSessionBean.class.getSimpleName());
      MethodsMetaData methods = new MethodsMetaData();
      methods.add(wildCardMethod);

      mandatoryContainerManagedTx.setMethods(methods);

      ContainerTransactionsMetaData containerTransactionsMetaData = new ContainerTransactionsMetaData();
      containerTransactionsMetaData.add(mandatoryContainerManagedTx);

      JBossEnterpriseBeanMetaData bean = this.getBeanMetaData();
      bean.setTransactionType(TransactionManagementType.CONTAINER);
      bean.getJBossMetaData().getAssemblyDescriptor().setContainerTransactions(containerTransactionsMetaData);

      EnterpriseBeanEffigy effigy = this.getEffigy(bean);
      Method method = SimpleSessionLocal.class.getDeclaredMethod("someMethod", null);
      TransactionAttributeType txAttrType = effigy.getTransactionAttributeType(method);

      Assert.assertNotNull("No tx attribute type found for method", txAttrType);
      Assert.assertEquals("Unexpected tx attribute type found for method", TransactionAttributeType.MANDATORY, txAttrType);

   }

   private JBossEnterpriseBeanMetaData getBeanMetaData()
   {
      JBossMetaData jBossMetaData = new JBossMetaData();
      jBossMetaData.setAssemblyDescriptor(new JBossAssemblyDescriptorMetaData());

      JBossEnterpriseBeansMetaData enterpriseBeansMetaData = new JBossEnterpriseBeansMetaData();
      enterpriseBeansMetaData.setEjbJarMetaData(jBossMetaData);

      JBossSessionBeanMetaData beanMetaData = new JBossSessionBeanMetaData();
      beanMetaData.setEnterpriseBeansMetaData(enterpriseBeansMetaData);
      beanMetaData.setName(SimpleSessionBean.class.getSimpleName());
      beanMetaData.setEjbClass(SimpleSessionBean.class.getName());
      
      BusinessLocalsMetaData businessLocals = new BusinessLocalsMetaData();
      businessLocals.add(SimpleSessionLocal.class.getName());
      beanMetaData.setBusinessLocals(businessLocals);

      return beanMetaData;
   }

   private EnterpriseBeanEffigy getEffigy(JBossEnterpriseBeanMetaData bean) throws ClassNotFoundException
   {
      JBossBeanEffigyFactory factory = new JBossBeanEffigyFactory();
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      JBossBeanEffigyInfo info = new JBossBeanEffigyInfo(classLoader, bean);
      SessionBeanEffigy effigy = factory.create(info, SessionBeanEffigy.class);

      return effigy;
   }

}
