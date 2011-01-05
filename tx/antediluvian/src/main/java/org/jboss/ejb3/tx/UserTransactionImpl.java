/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.tx;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.jboss.logging.Logger;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision$
 */
public final class UserTransactionImpl implements UserTransaction, java.io.Externalizable
{
   private static final long serialVersionUID = 2403204397187452430L;

   protected static Logger log = Logger.getLogger(UserTransactionImpl.class);

   /**
    * Timeout value in seconds for new transactions started
    * by this bean instance.
    */
   private TransactionManager tm;

   public UserTransactionImpl()
   {
      if (log.isDebugEnabled())
         log.debug("new UserTx: " + this);
      this.tm = TxUtil.getTransactionManager();
   }

   public void begin()
           throws NotSupportedException, SystemException
   {
      // Start the transaction
      tm.begin();

      Transaction tx = tm.getTransaction();
      if (log.isDebugEnabled())
         log.debug("UserTx begin: " + tx);

      EJB3UserTransactionProvider.getSingleton().userTransactionStarted();
   }

   public void commit()
           throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
                  SecurityException, IllegalStateException, SystemException
   {
      Transaction tx = tm.getTransaction();
      if (log.isDebugEnabled())
         log.debug("UserTx commit: " + tx);

      tm.commit();
   }

   public void rollback()
           throws IllegalStateException, SecurityException, SystemException
   {
      Transaction tx = tm.getTransaction();
      if (log.isDebugEnabled())
         log.debug("UserTx rollback: " + tx);
      tm.rollback();
   }

   public void setRollbackOnly()
           throws IllegalStateException, SystemException
   {
      Transaction tx = tm.getTransaction();
      if (log.isDebugEnabled())
         log.debug("UserTx setRollbackOnly: " + tx);

      tm.setRollbackOnly();
   }

   public int getStatus()
           throws SystemException
   {
      return tm.getStatus();
   }

   /**
    * Set the transaction timeout value for new transactions
    * started by this instance.
    */
   public void setTransactionTimeout(int seconds)
           throws SystemException
   {
      tm.setTransactionTimeout(seconds);
   }

   public void writeExternal(ObjectOutput out) throws IOException
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      this.tm = TxUtil.getTransactionManager();
   }

}
