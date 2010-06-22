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
package org.jboss.ejb3.test.tx.common;

import java.rmi.RemoteException;

import org.jboss.aop.joinpoint.Invocation;
import org.jboss.ejb3.cache.Cache;
import org.jboss.ejb3.tx.AbstractInterceptor;
import org.jboss.logging.Logger;

/**
 * Comment
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 72219 $
 */
public class StatefulInstanceInterceptor extends AbstractInterceptor
{
   private static final Logger log = Logger.getLogger(StatefulInstanceInterceptor.class);
   
   public StatefulInstanceInterceptor()
   {
   }

   private <T> Object doInvoke(Invocation invocation) throws Throwable
   {
      StatefulContainerMethodInvocation ejb = (StatefulContainerMethodInvocation) invocation;
      Object id = ejb.getId();
      assert id != null : "id is null";
      StatefulContainer<T> container = getContainer(invocation);
      Cache<StatefulBeanContext<T>> cache = container.getCache();
      StatefulBeanContext<T> target = container.getCache().get(id);

      ejb.setBeanContext(target);
//      StatefulBeanContext.currentBean.push(target);
//      container.pushContext(target);
      try
      {
//         if (target.isDiscarded()) throw new EJBException("SFSB was discarded by another thread");
         return ejb.invokeNext();
      }
      catch (Exception ex)
      {
//         if (StatefulRemoveInterceptor.isApplicationException(ex, (MethodInvocation)invocation)) throw ex;
         if (ex instanceof RuntimeException
                 || ex instanceof RemoteException)
         {
            if(log.isTraceEnabled())
               log.trace("Removing bean " + id + " because of exception", ex);
            // TODO: should be discard
            container.getCache().remove(id);
         }
         throw ex;
      }
      finally
      {
//         container.popContext();
//         StatefulBeanContext.currentBean.pop();
         // the bean context is disassociated, but then the StatefulRemoveInterceptor will fail
         ejb.setBeanContext(null);
         synchronized (target)
         {
//            target.setInInvocation(false);
//            if (!target.isTxSynchronized() && !target.isDiscarded()) container.getCache().release(target);
//            if (block) target.getLock().unlock();
            cache.release(target);
         }
      }      
   }
   
   public Object invoke(Invocation invocation) throws Throwable
   {
      return doInvoke(invocation);
   }
}
