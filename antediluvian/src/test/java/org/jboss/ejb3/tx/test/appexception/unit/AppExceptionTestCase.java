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
package org.jboss.ejb3.tx.test.appexception.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.ejb.EJBTransactionRequiredException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.jboss.ejb3.test.tx.common.AbstractTxTestCase;
import org.jboss.ejb3.test.tx.common.StatefulContainer;
import org.jboss.ejb3.tx.test.appexception.Barfing;
import org.jboss.ejb3.tx.test.appexception.BarfingStatefulBean;
import org.jboss.ejb3.tx.test.appexception.DoNotRollbackAppException;
import org.jboss.ejb3.tx.test.appexception.NotAnAppException;
import org.jboss.ejb3.tx.test.appexception.SubAppException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * What happens on a sub class of an application exception that specified rollback false?
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class AppExceptionTestCase extends AbstractTxTestCase
{
   private static TransactionManager tm;
   private static StatefulContainer<BarfingStatefulBean> container;

   @BeforeClass
   public static void setUpBeforeClass() throws Throwable
   {
      AbstractTxTestCase.beforeClass();
      
      InitialContext ctx = new InitialContext();
      tm = (TransactionManager) ctx.lookup("java:/TransactionManager");
      
      container = new StatefulContainer<BarfingStatefulBean>("BarfingStatefulBean", "Stateful Container", BarfingStatefulBean.class);
   }
   
   @Test
   public void testDoNotRollbackAppException() throws Throwable
   {
      Barfing bean = container.constructProxy(Barfing.class);
      
      tm.begin();
      try
      {
         bean.barf(new DoNotRollbackAppException());
         fail("Should have thrown DoNotRollbackAppException (EJB 3.0 14.3.1 table 14)");
      }
      catch(DoNotRollbackAppException e)
      {
         // good
         
         int status = tm.getStatus();
         assertEquals(Status.STATUS_ACTIVE, status);
      }
      finally
      {
         tm.rollback();
      }
   }
   
   /**
    * Sanity check.
    */
   @Test
   public void testMandatoryWithoutTx() throws Throwable
   {
      Barfing bean = container.constructProxy(Barfing.class);
      
      try
      {
         bean.barf(null);
         fail("Should have thrown EJBTransactionRequiredException (EJB 3.0 13.6.2.5)");
      }
      catch(EJBTransactionRequiredException e)
      {
         // good
      }
   }

   @Test
   public void testNotAnAppException() throws Throwable
   {
      Barfing bean = container.constructProxy(Barfing.class);
      
      tm.begin();
      try
      {
         bean.barf(new NotAnAppException());
         fail("Should have thrown EJBTransactionRolledbackException (EJB 3.0 14.3.1 table 14)");
      }
      catch(EJBTransactionRolledbackException e)
      {
         // good
      }
      finally
      {
         tm.rollback();
      }
   }

   @Test
   public void testSubAppException() throws Throwable
   {
      Barfing bean = container.constructProxy(Barfing.class);
      
      tm.begin();
      try
      {
         bean.barf(new SubAppException());
         fail("Should have thrown EJBTransactionRolledbackException (EJB 3.0 14.3.1 table 14)");
      }
      catch(EJBTransactionRolledbackException e)
      {
         // good
      }
      finally
      {
         tm.rollback();
      }
   }
}
