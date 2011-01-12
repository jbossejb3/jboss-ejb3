/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.ejb3.servitor.stateless;

import org.jboss.ejb3.context.spi.EJBContext;

import javax.ejb.EJBException;
import javax.interceptor.InvocationContext;
import java.rmi.RemoteException;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatelessInstanceInterceptor
{
   public Object aroundInvoke(InvocationContext context) throws Exception
   {
      StatelessServitor servitor = (StatelessServitor) context.getTarget();

      // TODO: get rid of this cast
      Pool<EJBContext> pool = (Pool<EJBContext>) servitor.getPool();
      EJBContext instance = pool.get();

      Object params[] = context.getParameters();
      params[0] = instance;
      context.setParameters(params);

      boolean discard = false;

      try
      {
         return context.proceed();
      }
      catch (Exception ex)
      {
         /*
          * EJB 3.1 FR 4.7.3: Dealing with Exceptions
          * A RuntimeException that is not an application exception thrown from any method of the enterprise
          * bean class (including the business methods and the lifecycle callback interceptor methods invoked by
          * the container) results in the transition to the “does not exist” state.
          */
         discard = (ex instanceof EJBException) ||
                 ((ex instanceof RuntimeException || ex instanceof RemoteException) && !servitor.isApplicationException(ex, context.getMethod()));
         throw ex;
      }
      catch (final Error e)
      {
         discard = true;
         throw e;
      }
      catch(Throwable t)
      {
         discard = true;
         throw new Error(t);
      }
      finally
      {
         params[0] = null;
         context.setParameters(params);
         if (discard) pool.discard(instance);
         else pool.release(instance);
      }
   }
}
