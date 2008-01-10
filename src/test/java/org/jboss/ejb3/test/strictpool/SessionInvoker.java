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

import org.jboss.logging.Logger;
import EDU.oswego.cs.dl.util.concurrent.CountDown;

/** Invoker thread for StatelessSession tests.
 * Adapted from the EJB 2.1 tests (org.jboss.test.cts.test.SessionInvoker)
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @version $Revision$
 */
public class SessionInvoker extends Thread
{
   int id;
   CountDown done;
   public Exception runEx;
   StrictlyPooledSession strictlyPooledSession;

   public SessionInvoker(int id, CountDown done, StrictlyPooledSession strictlyPooledSession)
   {
      super("SessionInvoker#"+id);
      this.id = id;
      this.done = done;
      this.strictlyPooledSession = strictlyPooledSession;
   }
   public void run()
   {
      System.out.println("Begin run, this="+this);
      try
      {
         strictlyPooledSession.methodA();
      }
      catch(Exception e)
      {
         runEx = e;
      }
      done.release();
      System.out.println("End run, this="+this);
   }

}
