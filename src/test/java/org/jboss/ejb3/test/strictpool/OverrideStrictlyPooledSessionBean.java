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
package org.jboss.ejb3.test.strictpool;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.Pool;


/**
 * @version <tt>$Revision$</tt>
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateless
@Pool(value="BogusPoo", maxSize=0, timeout=0)
@Remote(StrictlyPooledSession.class)
public class OverrideStrictlyPooledSessionBean implements StrictlyPooledSession
{
   /** The class wide max count of instances allows */
   public static final int maxActiveCount = 5;
   /** The class wide count of instances active in business code */
   private static int activeCount;

   private SessionContext ctx;

   @Resource public void setSessionContext(SessionContext ctx)
   {
      System.out.println("setSessionContext()");
      this.ctx = ctx;
   }

   public void methodA()
   {
      int count = incActiveCount();
      System.out.println("Begin methodA, activeCount="+count+", ctx="+ctx);
      try
      {
         if( count > maxActiveCount )
         {
            String msg = "IllegalState, activeCount > maxActiveCount, "
                  + count + " > " + maxActiveCount;
            throw new EJBException(msg);
         }
         // Sleep to let the client thread pile up
         Thread.currentThread().sleep(1000);
      }
      catch(InterruptedException e)
      {
      }
      finally
      {
         count = decActiveCount();
         System.out.println("End methodA, activeCount="+count+", ctx="+ctx);
      }
   }

   private static synchronized int incActiveCount()
   {
      return activeCount ++;
   }
   private static synchronized int decActiveCount()
   {
      return activeCount --;
   }

}
