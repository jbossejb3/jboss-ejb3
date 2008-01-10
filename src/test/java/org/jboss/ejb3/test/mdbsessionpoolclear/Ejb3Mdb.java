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
package org.jboss.ejb3.test.mdbsessionpoolclear;

import org.jboss.logging.Logger;
import org.jboss.ejb3.annotation.ResourceAdapter;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.ActivationConfigProperty;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 *
 * @version <tt>$Revision: 60233 $</tt>
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@MessageDriven(activationConfig =
{
@ActivationConfigProperty(propertyName="destinationType", propertyValue="javax.jms.Queue"),
@ActivationConfigProperty(propertyName="destination", propertyValue="queue/ejb3mdbsessionpoolclearQueue"),
@ActivationConfigProperty(propertyName="forceClearOnShutdown", propertyValue="true"),
@ActivationConfigProperty(propertyName="forceClearOnShutdownInterval", propertyValue="600"),
@ActivationConfigProperty(propertyName="forceClearAttempts", propertyValue="6"),
@ActivationConfigProperty(propertyName="maxSession", propertyValue="1")
})
@ResourceAdapter("mdbsessionpoolclear-test-jms-ra.rar")
public class Ejb3Mdb implements MessageListener
{
   private static final Logger log = Logger.getLogger(Ejb3Mdb.class);

   @EJB TestStatus status;
   
   public void onMessage(Message message)
   {
      try
      {
         int count = status.increment();
         log.info("**** Ejb3Mdb got message " + count + " " + ((TextMessage)message).getText());
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}
