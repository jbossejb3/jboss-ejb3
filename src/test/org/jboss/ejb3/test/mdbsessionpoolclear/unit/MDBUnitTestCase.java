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
package org.jboss.ejb3.test.mdbsessionpoolclear.unit;

import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import junit.framework.Test;

import org.jboss.ejb3.InitialContextFactory;
import org.jboss.ejb3.test.mdbsessionpoolclear.TestStatus;
import org.jboss.logging.Logger;
import org.jboss.test.JBossTestCase;

/**
 * Sample client for the jboss container.
 * 
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Id: MDBUnitTestCase.java 63518 2007-06-13 22:32:28Z wolfc $
 */
public class MDBUnitTestCase extends JBossTestCase
{
   private static final Logger log = Logger.getLogger(MDBUnitTestCase.class);

   public MDBUnitTestCase(String name)
   {
      super(name);
   }

  
   public void testEjb21Mdb() throws Exception
   {
      TestStatus status = (TestStatus) getInitialContext().lookup(
            "TestStatusBean/remote");
      status.clear();
      
      QueueConnection cnn = null;
      QueueSender sender = null;
      QueueSession session = null;

      Queue queue = (Queue) getInitialContext().lookup("queue/mdbsessionpoolclearQueue");
      QueueConnectionFactory factory = getQueueConnectionFactory();
      cnn = factory.createQueueConnection();
      session = cnn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);

      TextMessage msg = session.createTextMessage("Hello World " + new Date());

      sender = session.createSender(queue);
      System.out.println(new Date() + "*** Sending 1" );
      sender.send(msg);
      session.close();
      cnn.close();
      
      Thread.sleep(5 * 1000);
      
      assertEquals(1, status.queueFired());
      
      MBeanServerConnection server = getServer();
      
      ObjectName provider = null;
      String jmsProvider = System.getProperty("jboss.jms.provider");
      System.out.println("JMS Provider is " + jmsProvider);
      if (jmsProvider.equals("mq"))
         provider = new ObjectName("jboss.mq:service=DestinationManager");
      else
      {
         ObjectName providerQuery = new ObjectName("jboss.j2ee:service=EJB,*");
         Iterator mbeans = server.queryMBeans(providerQuery, null).iterator();
         while (provider == null)
         {
            ObjectInstance providerInstance = (ObjectInstance)mbeans.next();
            String name = providerInstance.getObjectName().toString();
            if (name.contains("Ejb21Mdb") && !name.contains("plugin"))
               provider = providerInstance.getObjectName();
         }
      }
      
      Object[] params = {};
      String[] sig = {};
      System.out.println(new Date() + "*** Stopping JMS Provider");
      Object success = server.invoke(provider, "stop", params, sig);
      
      ObjectName jmsContainerInvokerQuery = new ObjectName("jboss.j2ee:binding=my-message-driven-bean,*");
      Set mbeans = server.queryMBeans(jmsContainerInvokerQuery, null);
      assertEquals(1, mbeans.size());
      ObjectInstance jmsContainerInvokerInstance = (ObjectInstance)mbeans.iterator().next();
      ObjectName jmsContainerInvoker = jmsContainerInvokerInstance.getObjectName();
      System.out.println("jmsContainerInvoker " + jmsContainerInvoker);
 //     int numActiveSessions = (Integer)server.getAttribute(jmsContainerInvoker, "NumActiveSessions");
 //     assertEquals(1, numActiveSessions);
      boolean forceClear = (Boolean)server.getAttribute(jmsContainerInvoker, "ForceClearOnShutdown");
      assertTrue(forceClear);
      int forceClearAttempts = (Integer)server.getAttribute(jmsContainerInvoker, "ForceClearAttempts");
      assertEquals(5, forceClearAttempts);
      long forceClearOnShutdownInterval = (Long)server.getAttribute(jmsContainerInvoker, "ForceClearOnShutdownInterval");
      assertEquals(500, forceClearOnShutdownInterval);
      
      Thread.sleep(2000);
      
      System.out.println(new Date() + "*** Starting JMS Provider");
      success = server.invoke(provider, "start", params, sig);

      Thread.sleep(60 * 1000);
      
 //     numActiveSessions = (Integer)server.getAttribute(jmsContainerInvoker, "NumActiveSessions");
 //     assertEquals(1, numActiveSessions);
      
      cnn = factory.createQueueConnection();
      session = cnn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);

      msg = session.createTextMessage("Hello World " + new Date());

      sender = session.createSender(queue);
      
      sender.send(msg);
      System.out.println(new Date() + "*** Sending 2");
      session.close();
      cnn.close();
      
      Thread.sleep(10 * 1000);
      
      assertEquals(2, status.queueFired());
   }
   
   public void testEjb3Mdb() throws Exception
   {
      TestStatus status = (TestStatus) getInitialContext().lookup(
            "TestStatusBean/remote");
      status.clear();
      
      QueueConnection cnn = null;
      QueueSender sender = null;
      QueueSession session = null;

      Queue queue = (Queue) getInitialContext().lookup("queue/ejb3mdbsessionpoolclearQueue");
      QueueConnectionFactory factory = getQueueConnectionFactory();
      cnn = factory.createQueueConnection();
      session = cnn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);

      TextMessage msg = session.createTextMessage("Hello World " + new Date());

      sender = session.createSender(queue);
      System.out.println(new Date() + "*** Sending 1" );
      sender.send(msg);
      session.close();
      cnn.close();
      
      Thread.sleep(2000);
      
      MBeanServerConnection server = getServer();
      ObjectName provider = null;
      String jmsProvider = System.getProperty("jboss.jms.provider");
      System.out.println("JMS Provider is " + jmsProvider);
      if (jmsProvider.equals("mq"))
         provider = new ObjectName("jboss.mq:service=DestinationManager");
      else
      {
         provider = new ObjectName("jboss.j2ee:jar=mdbsessionpoolclear-test.ejb3,name=Ejb3Mdb,service=EJB3");
      }
      Object[] params = {};
      String[] sig = {};
      System.out.println(new Date() + "*** Stopping JMS Provider");
      Object success = server.invoke(provider, "stop", params, sig);
      
      Thread.sleep(2000);
          
      System.out.println(new Date() + "*** Starting JMS Provider");
      success = server.invoke(provider, "start", params, sig);

      Thread.sleep(5 * 1000);
      
      assertEquals(1, status.queueFired());
      
      cnn = factory.createQueueConnection();
      session = cnn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);

      msg = session.createTextMessage("Hello World " + new Date());

      sender = session.createSender(queue);
      
      sender.send(msg);
      System.out.println(new Date() + "*** Sending 2");
      session.close();
      cnn.close();
      
      Thread.sleep(10 * 1000);
      
      assertEquals(2, status.queueFired());
   }


   protected QueueConnectionFactory getQueueConnectionFactory()
         throws Exception
   {
      try
      {
         return (QueueConnectionFactory) getInitialContext().lookup(
               "ConnectionFactory");
      } catch (NamingException e)
      {
         return (QueueConnectionFactory) getInitialContext().lookup(
               "java:/ConnectionFactory");
      }
   }

   protected InitialContext getInitialContext() throws Exception
   {
      return InitialContextFactory.getInitialContext();
   }

   public static Test suite() throws Exception
   {
      return getDeploySetup(MDBUnitTestCase.class, 
            "mdbsessionpoolclear-test-jms-ra.rar, mdbsessionpoolclear-jbm-service.xml, mdbsessionpoolclear-test.ejb3, mdbsessionpoolclear-test.jar");
   }

}