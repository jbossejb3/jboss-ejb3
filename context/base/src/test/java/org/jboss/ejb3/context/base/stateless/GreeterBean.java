/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.ejb3.context.base.stateless;

import javax.annotation.Resource;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;
import javax.xml.rpc.handler.MessageContext;
import java.security.Principal;
import java.util.concurrent.Callable;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateless
public class GreeterBean
{
   private SessionContext ctx;

   private static <T> T expectIllegalStateException(Callable<T> callable)
   {
      try
      {
         T result = callable.call();
         throw new AssertionError("expected IllegalStateException");
      }
      catch(IllegalStateException e)
      {
         // good
         return null;
      }
      catch(RuntimeException e)
      {
         throw e;
      }
      catch(Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   @Resource
   public void setSessionContext(final SessionContext ctx)
   {
      this.ctx = ctx;

      expectIllegalStateException(new Callable<Object>() {
         public Object call() throws Exception
         {
            return ctx.getBusinessObject(GreeterBean.class);
         }
      });
      //ctx.getCallerIdentity();
      expectIllegalStateException(new Callable<Principal>() {
         public Principal call() throws Exception
         {
            return ctx.getCallerPrincipal();
         }
      });
      // it's an EJB 3
      expectIllegalStateException(new Callable<EJBHome>() {
         public EJBHome call() throws Exception
         {
            return ctx.getEJBHome();
         }
      });
      // it's an EJB 3
      expectIllegalStateException(new Callable<EJBLocalHome>() {
         public EJBLocalHome call() throws Exception
         {
            return ctx.getEJBLocalHome();
         }
      });
      expectIllegalStateException(new Callable<EJBLocalObject>() {
         public EJBLocalObject call() throws Exception
         {
            return ctx.getEJBLocalObject();
         }
      });
      expectIllegalStateException(new Callable<EJBObject>() {
         public EJBObject call() throws Exception
         {
            return ctx.getEJBObject();
         }
      });
      expectIllegalStateException(new Callable<Class<?>>() {
         public Class<?> call() throws Exception
         {
            return ctx.getInvokedBusinessInterface();
         }
      });
      expectIllegalStateException(new Callable<MessageContext>() {
         public MessageContext call() throws Exception
         {
            return ctx.getMessageContext();
         }
      });
      expectIllegalStateException(new Callable<Boolean>() {
         public Boolean call() throws Exception
         {
            return ctx.getRollbackOnly();
         }
      });
      expectIllegalStateException(new Callable<TimerService>() {
         public TimerService call() throws Exception
         {
            return ctx.getTimerService();
         }
      });
      expectIllegalStateException(new Callable<UserTransaction>() {
         public UserTransaction call() throws Exception
         {
            return ctx.getUserTransaction();
         }
      });
      expectIllegalStateException(new Callable<Boolean>() {
         public Boolean call() throws Exception
         {
            return ctx.isCallerInRole("test");
         }
      });
      ctx.lookup("value");
      expectIllegalStateException(new Callable<Void>() {
         public Void call() throws Exception
         {
            ctx.setRollbackOnly();
            return null;
         }
      });
   }
}
