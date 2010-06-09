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
package org.jboss.ejb3.timerservice.integration.test.timerinfo;

import java.io.Serializable;
import java.util.Date;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;

import org.jboss.ejb3.annotation.RemoteBinding;
import org.jboss.logging.Logger;

/**
 * SimpleTimerBean
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Singleton
@Remote (SimpleTimer.class)
@RemoteBinding (jndiBinding = SerializableInfoTimerBean.JNDI_NAME)
public class SerializableInfoTimerBean implements SimpleTimer
{

   public static final String JNDI_NAME = "SerializableInfoTestBean";
   
   private Serializable infoFromTimer;
   
   private static Logger logger = Logger.getLogger(SerializableInfoTimerBean.class);
   
   @Resource
   private TimerService timerService;
   
   public void createTimer(Date timeoutDate, Serializable info)
   {
      this.timerService.createTimer(timeoutDate, info);

   }
   
   @Timeout
   public void timeout(Timer timer)
   {
      this.infoFromTimer = timer.getInfo();
      logger.info("Got info: " + this.infoFromTimer);
   }
   
   @Override
   public Serializable getInfoFromTimer()
   {
      return this.infoFromTimer;
   }
}
