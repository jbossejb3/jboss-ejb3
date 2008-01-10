/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.test.strictpool.unit;

import java.util.concurrent.Callable;

import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;

import org.jboss.logging.Logger;

/**
 * Send a JMS text message and expect a reply.
 * 
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class MDBCallable implements Callable<String>
{
   private static final Logger log = Logger.getLogger(MDBCallable.class);
   
   private QueueConnection connection;
   private Queue queue;
   private String text;
   
   public MDBCallable(QueueConnection connection, Queue queue, String text)
   {
      assert connection != null : "connection is null";
      assert queue != null : "queue is null";
      assert text != null : "text is null";
      
      this.connection = connection;
      this.queue = queue;
      this.text = text;
   }
   
   public String call() throws Exception
   {
      log.info("start mdb call " + this);
      // JMS 1.1 4.4.6: since we're concurrent, let's create our own session
      QueueSession session = connection.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
      try
      {
         TemporaryQueue replyQueue = session.createTemporaryQueue();
         QueueReceiver receiver = session.createReceiver(replyQueue);
         
         QueueSender sender = session.createSender(queue);
         sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
         TextMessage message = session.createTextMessage();
         message.setJMSReplyTo(replyQueue);
         message.setText(text);
         sender.send(message);
         sender.close();
         
         Message reply = receiver.receive(20000);
         receiver.close();
         replyQueue.delete();
         
         if(reply instanceof TextMessage)
            return ((TextMessage) reply).getText();
         
         if(reply instanceof ObjectMessage)
         {
            Object obj = ((ObjectMessage) reply).getObject();
            if(obj instanceof Exception)
               throw (Exception) obj;
            
            if(obj instanceof String)
               return (String) obj;
            
            throw new IllegalStateException("Can't handle " + obj);
         }
         
         throw new IllegalStateException("Can't handle " + reply);
      }
      finally
      {
         session.close();
         log.info("end mdb call " + this);
      }
   }
}
