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
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.jboss.ejb3.annotation.Pool;

/**
 * @version <tt>$Revision: 67628 $</tt>
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@MessageDriven(activationConfig =
        {
        @ActivationConfigProperty(propertyName="destinationType", propertyValue="javax.jms.Queue"),
        @ActivationConfigProperty(propertyName="destination", propertyValue="queue/overrideQueueA"),
        @ActivationConfigProperty(propertyName="maxMessages", propertyValue="10"),
        @ActivationConfigProperty(propertyName="minSession", propertyValue="10"),
        @ActivationConfigProperty(propertyName="maxSession", propertyValue="10")
        })
@Pool (value="BogusPool", maxSize=0, timeout=0)
public class OverrideStrictlyPooledMDB implements MessageListener
{
   /** The class wide max count of instances allows */
   public static final int maxActiveCount = 2;
   /** The class wide count of instances active in business code */
   private static int activeCount;

   private MessageDrivenContext ctx = null;
   private QueueConnection queConn;
   private QueueSession session;
   private QueueSender sender;

   private static synchronized int incActiveCount()
   {
      return activeCount ++;
   }
   private static synchronized int decActiveCount()
   {
      return activeCount --;
   }

   @Resource public void setMessageDrivenContext(MessageDrivenContext ctx)
      throws EJBException
   {
      System.out.println("setMessageDrivenContext()");
      this.ctx = ctx;
      try
      {
         InitialContext iniCtx = new InitialContext();
         QueueConnectionFactory factory = (QueueConnectionFactory) iniCtx.lookup("java:/ConnectionFactory");
         queConn = factory.createQueueConnection();
         session = queConn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
         Queue queue = (Queue) iniCtx.lookup("queue/overrideQueueB");
         sender = session.createSender(queue);
      }
      catch(Exception e)
      {
         System.out.println("Setup failure");
         e.printStackTrace();
         throw new EJBException("Setup failure", e);
      }
   }

   public void ejbCreate()
   {
   }

   public void ejbRemove()
   {
      try
      {
         if( sender != null )
            sender.close();
         if( session != null )
            session.close();
         if( queConn != null )
            queConn.close();
      }
      catch(Exception e)
      {
         System.out.println("Failed to close JMS resources");
         e.printStackTrace();
      }
   }

   public void onMessage(Message message)
   {
      int count = incActiveCount();
      System.out.println("Begin onMessage, activeCount="+count+", ctx="+ctx);
      try
      {
         Message reply = null;
         if( count > maxActiveCount )
         {
            String msg = "IllegalState, activeCount > maxActiveCount, "
                  + count + " > " + maxActiveCount;
            // Send an exception
            Exception e = new IllegalStateException(msg);
            reply = session.createObjectMessage(e);
         }
         else
         {
            TextMessage tm = (TextMessage) message;
            // Send an ack
            reply = session.createTextMessage("Recevied msg="+tm.getText());
         }
         Thread.currentThread().sleep(1000);
         sender.send(reply);
      }
      catch(JMSException e)
      {
         System.out.println("Failed to send error message");
         e.printStackTrace();
      }
      catch(InterruptedException e)
      {
      }
      finally
      {
         count = decActiveCount();
         System.out.println("End onMessage, activeCount="+count+", ctx="+ctx);
      }
   }
}
