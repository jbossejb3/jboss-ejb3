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
package org.jboss.ejb3.timerservice.integration.test.jndi;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.TimerService;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.ejb3.annotation.RemoteBinding;

/**
 * SimpleSingleton
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
@Remote (TimerServiceJNDIAccessTester.class)
@RemoteBinding (jndiBinding = SimpleSingleton.JNDI_NAME)
public class SimpleSingleton implements TimerServiceJNDIAccessTester
{

   public static final String JNDI_NAME = "TimerServiceJNDITestBean";
   
   @Resource
   private TimerService injectedTimerService;
   
   @Resource (name = "tService")
   private TimerService anotherInjectedTimerService;


   private TimerService lookedupTimerService;
   
   @PostConstruct
   public void onConstruct() throws Exception
   {
      Context ctx = new InitialContext();
      this.lookedupTimerService = (TimerService) ctx.lookup("java:comp/TimerService");
   }
   
   @Override
   public boolean wasTimerServiceAvailableInPostConstruct()
   {
      return this.lookedupTimerService != null;
   }
   
   
   public boolean isTimerServiceInjected()
   {
      return this.injectedTimerService != null;
   }
   
   public boolean isTimerServiceAvailableInENCAtCustomName()
   {
      try
      {
         Context ctx = new InitialContext();
         EJBContext ejbContext = (EJBContext) ctx.lookup("java:comp/EJBContext");
         TimerService tService = (TimerService) ejbContext.lookup("tService");
         return tService != null;
      }
      catch (NamingException ne)
      {
         throw new RuntimeException(ne);
      }
   }
   
   public boolean isTimerServiceAvailableThroughEJBContext()
   {
      try
      {
         Context ctx = new InitialContext();
         EJBContext ejbContext = (EJBContext) ctx.lookup("java:comp/EJBContext");
         TimerService tService = ejbContext.getTimerService();
         return tService != null;
      }
      catch (NamingException ne)
      {
         throw new RuntimeException(ne);
      }
      
   }
}
