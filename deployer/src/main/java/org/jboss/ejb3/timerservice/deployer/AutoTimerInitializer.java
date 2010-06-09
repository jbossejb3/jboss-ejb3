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
package org.jboss.ejb3.timerservice.deployer;

import java.util.List;

import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.jboss.beans.metadata.api.annotations.Start;
import org.jboss.ejb3.EJBContainer;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMessageDrivenBean31MetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBean31MetaData;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.metadata.ejb.spec.TimerMetaData;

/**
 * TimerServiceBootstrap
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class AutoTimerInitializer
{
   private EJBContainer container;

   public AutoTimerInitializer()
   {

   }

   public AutoTimerInitializer(EJBContainer container)
   {
      this.container = container;
   }

   public void setContainer(EJBContainer container)
   {
      this.container = container;
   }

   public EJBContainer getContainer()
   {
      return this.container;
   }

   @Start
   public void initializeAutoTimers()
   {
      if (this.container == null)
      {
         throw new IllegalStateException("Cannot initialize auto-timers since container is not present");
      }

      JBossEnterpriseBeanMetaData enterpriseBeanMetaData = this.container.getXml();
      if (enterpriseBeanMetaData.getJBossMetaData().isEJB31() == false)
      {
         return;
      }
      List<TimerMetaData> autoTimersMetaData = null;
      if (enterpriseBeanMetaData.isSession())
      {
         JBossSessionBean31MetaData sessionBean = (JBossSessionBean31MetaData) enterpriseBeanMetaData;
         if (sessionBean.isStateful())
         {
            return;
         }
         autoTimersMetaData = sessionBean.getTimers();
      }
      else if (enterpriseBeanMetaData.isMessageDriven())
      {
         JBossMessageDrivenBean31MetaData mdb = (JBossMessageDrivenBean31MetaData) enterpriseBeanMetaData;
         autoTimersMetaData = mdb.getTimers();
      }

      if (autoTimersMetaData == null)
      {
         return;
      }
      TimerService timerService = this.container.getTimerService();

      if (timerService instanceof org.jboss.ejb3.timerservice.extension.TimerService == false)
      {
         // can't do anything about this
         return;
      }
      org.jboss.ejb3.timerservice.extension.TimerService ejb31TimerService = (org.jboss.ejb3.timerservice.extension.TimerService) timerService;
      for (TimerMetaData autoTimerMetaData : autoTimersMetaData)
      {
         TimerConfig timerConfig = new TimerConfig();
         timerConfig.setPersistent(autoTimerMetaData.isPersistent());
         timerConfig.setInfo(autoTimerMetaData.getInfo());

         String timeoutMethodName = autoTimerMetaData.getTimeoutMethod().getMethodName();
         MethodParametersMetaData methodParams = autoTimerMetaData.getTimeoutMethod().getMethodParams();
         String[] timeoutMethodParams = null;
         if (methodParams != null)
         {
            timeoutMethodParams = methodParams.toArray(new String[methodParams.size()]);
         }
         ejb31TimerService.getAutoTimer(autoTimerMetaData.getScheduleExpression(), timerConfig, timeoutMethodName,
               timeoutMethodParams);
      }

   }

}
