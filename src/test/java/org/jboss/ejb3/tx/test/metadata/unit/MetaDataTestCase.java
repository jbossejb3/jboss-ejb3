/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.tx.test.metadata.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URL;

import javax.ejb.EJBTransactionRolledbackException;
import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.jboss.ejb3.test.tx.common.StatefulContainer;
import org.jboss.ejb3.test.tx.mc.UnitTestBootstrap;
import org.jboss.ejb3.tx.test.metadata.AppRuntimeException;
import org.jboss.ejb3.tx.test.metadata.MyStateful;
import org.jboss.ejb3.tx.test.metadata.MyStatefulBean;
import org.jboss.metadata.ejb.jboss.JBoss50MetaData;
import org.jboss.metadata.ejb.jboss.JBossAssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;
import org.jboss.metadata.ejb.spec.ApplicationExceptionMetaData;
import org.jboss.metadata.ejb.spec.ApplicationExceptionsMetaData;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class MetaDataTestCase
{
   private static UnitTestBootstrap bootstrap;
   
   private static InitialContext ctx;
   private static TransactionManager tm;
   
   private StatefulContainer<?> container;
   
   private static URL getResource(String name)
   {
      return Thread.currentThread().getContextClassLoader().getResource(name);
   }
   
   @BeforeClass
   public static void setUpBeforeClass() throws Throwable
   {
      bootstrap = new UnitTestBootstrap();
      bootstrap.deploy(getResource("instance/beans.xml"));
      
      ctx = new InitialContext();
      tm = (TransactionManager) ctx.lookup("java:/TransactionManager");
   }

   @AfterClass
   public static void tearDownAfterClass() throws Exception
   {
      if(ctx != null)
         ctx.close();
      
      if(bootstrap != null)
         bootstrap.shutdown();
   }

   @Before
   public void setUp() throws Exception
   {
   }

   @After
   public void tearDown() throws Exception
   {
   }

   @Test
   public void test1() throws Throwable
   {
      ApplicationExceptionMetaData applicationException = new ApplicationExceptionMetaData();
      applicationException.setExceptionClass(AppRuntimeException.class.getName());
      applicationException.setRollback(true);
      
      ApplicationExceptionsMetaData applicationExceptions = new ApplicationExceptionsMetaData();
      applicationExceptions.add(applicationException);
      
      JBossAssemblyDescriptorMetaData assemblyDescriptor = new JBossAssemblyDescriptorMetaData();
      assemblyDescriptor.setApplicationExceptions(applicationExceptions);
      
      JBossSessionBeanMetaData enterpriseBean = new JBossSessionBeanMetaData();
      enterpriseBean.setName("MyStatefulBean");
      
      JBossEnterpriseBeansMetaData enterpriseBeans = new JBossEnterpriseBeansMetaData();
      enterpriseBeans.add(enterpriseBean);
      
      JBoss50MetaData jarMetaData = new JBoss50MetaData();
      jarMetaData.setAssemblyDescriptor(assemblyDescriptor);
      jarMetaData.setEnterpriseBeans(enterpriseBeans);
      
      container = new StatefulContainer<MyStatefulBean>("Stateful Container", MyStatefulBean.class, enterpriseBean);
      
      MyStateful bean = container.constructProxy(MyStateful.class);
      
      tm.begin();
      try
      {
         bean.throwAppRuntimeException();
         fail("Should have thrown AppRuntimeException");
      }
      catch(AppRuntimeException e)
      {
         assertEquals("Transaction should have been marked for rollback", Status.STATUS_MARKED_ROLLBACK, tm.getStatus());
      }
      finally
      {
         tm.rollback();
      }
   }
   
   private void testNPE(JBossSessionBeanMetaData enterpriseBean) throws Throwable
   {
      container = new StatefulContainer<MyStatefulBean>("Stateful Container", MyStatefulBean.class, enterpriseBean);
      
      MyStateful bean = container.constructProxy(MyStateful.class);
      
      tm.begin();
      try
      {
         bean.throwAppRuntimeException();
         fail("Should have thrown EJBTransactionRolledbackException");
      }
      catch(EJBTransactionRolledbackException e)
      {
         // good, it's not an application exception
      }
      finally
      {
         tm.rollback();
      }   
   }
   
   @Test
   public void testNPE1() throws Throwable
   {
      JBossAssemblyDescriptorMetaData assemblyDescriptor = new JBossAssemblyDescriptorMetaData();
      //assemblyDescriptor.setApplicationExceptions(applicationExceptions);
      
      JBossSessionBeanMetaData enterpriseBean = new JBossSessionBeanMetaData();
      enterpriseBean.setName("MyStatefulBean");
      
      JBossEnterpriseBeansMetaData enterpriseBeans = new JBossEnterpriseBeansMetaData();
      enterpriseBeans.add(enterpriseBean);
      
      JBoss50MetaData jarMetaData = new JBoss50MetaData();
      jarMetaData.setAssemblyDescriptor(assemblyDescriptor);
      jarMetaData.setEnterpriseBeans(enterpriseBeans);
      
      testNPE(enterpriseBean);
   }
   
   @Test
   public void testNPE2() throws Throwable
   {
      JBossSessionBeanMetaData enterpriseBean = new JBossSessionBeanMetaData();
      enterpriseBean.setName("MyStatefulBean");
      
      JBossEnterpriseBeansMetaData enterpriseBeans = new JBossEnterpriseBeansMetaData();
      enterpriseBeans.add(enterpriseBean);
      
      JBoss50MetaData jarMetaData = new JBoss50MetaData();
      //jarMetaData.setAssemblyDescriptor(assemblyDescriptor);
      jarMetaData.setEnterpriseBeans(enterpriseBeans);
      
      testNPE(enterpriseBean);
   }
}
