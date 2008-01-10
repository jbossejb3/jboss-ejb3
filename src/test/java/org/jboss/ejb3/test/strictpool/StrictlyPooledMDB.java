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
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.jboss.ejb3.annotation.Pool;
import org.jboss.logging.Logger;

/**
 * Adapted from the EJB 2.1 tests (org.jboss.test.cts.ejb.StrictlyPooledMDB)
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @version $Revision$
 */
@MessageDriven(activationConfig =
        {
        @ActivationConfigProperty(propertyName="destinationType", propertyValue="javax.jms.Queue"),
        @ActivationConfigProperty(propertyName="destination", propertyValue="queue/queueA"),
        @ActivationConfigProperty(propertyName="maxMessages", propertyValue="10"),
        @ActivationConfigProperty(propertyName="minSession", propertyValue="10"),
        @ActivationConfigProperty(propertyName="maxSession", propertyValue="10")
        })
@Pool (value="StrictMaxPool", maxSize=StrictlyPooledMDB.maxActiveCount, timeout=10000)
public class StrictlyPooledMDB implements MessageListener
{
   private static final Logger log = Logger.getLogger(StrictlyPooledMDB.class);
   
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
      log.info("setMessageDrivenContext()");
      this.ctx = ctx;
      try
      {
         InitialContext iniCtx = new InitialContext();
         QueueConnectionFactory factory = (QueueConnectionFactory) iniCtx.lookup("java:/ConnectionFactory");
         queConn = factory.createQueueConnection();
         session = queConn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
         sender = session.createSender(null);
         sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      }
      catch(Exception e)
      {
         log.info("Setup failure");
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
         log.info("Failed to close JMS resources");
         e.printStackTrace();
      }
   }

   public void onMessage(Message message)
   {
      int count = incActiveCount();
      log.info("Begin onMessage, activeCount="+count+", ctx="+ctx);
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
         Thread.sleep(1000);
         sender.send(message.getJMSReplyTo(), reply);
      }
      catch(JMSException e)
      {
         log.info("Failed to send error message");
         e.printStackTrace();
      }
      catch(InterruptedException e)
      {
      }
      finally
      {
         count = decActiveCount();
         log.info("End onMessage, activeCount="+count+", ctx="+ctx);
      }
   }
}
