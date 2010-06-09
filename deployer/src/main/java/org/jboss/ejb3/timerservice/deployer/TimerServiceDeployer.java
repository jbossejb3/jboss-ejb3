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


import org.jboss.beans.metadata.plugins.AbstractInjectionValueMetaData;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.ejb3.common.deployers.spi.AttachmentNames;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeansMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;

/**
 * TimerServiceDeployer
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class TimerServiceDeployer extends AbstractDeployer
{

   /**
    * Logger
    */
   private Logger logger = Logger.getLogger(TimerServiceDeployer.class);
   
   private static final String MC_BEAN_PREFIX = "auto-timer-initializer:";
   
   public TimerServiceDeployer()
   {
      setStage(DeploymentStages.REAL);
      setInput(JBossMetaData.class);
      addInput(AttachmentNames.PROCESSED_METADATA);
      // we deploy MC beans
      addOutput(BeanMetaData.class);
   }

   @Override
   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      if (logger.isTraceEnabled())
      {
         logger.trace("Deploying unit " + unit.getName());
      }
      // get processed metadata
      JBossMetaData metaData = unit.getAttachment(AttachmentNames.PROCESSED_METADATA, JBossMetaData.class);
      if (metaData == null)
      {
         if (logger.isTraceEnabled())
            logger.trace("No JBossMetadata for unit : " + unit.getName());
         return;
      }
      if (metaData.isEJB3x() == false)
      {
         return;
      }
      // work on the ejbs
      JBossEnterpriseBeansMetaData beans = metaData.getEnterpriseBeans();
      for (JBossEnterpriseBeanMetaData bean : beans)
      {
         if (bean.isSession())
         {
            JBossSessionBeanMetaData sessionBean = (JBossSessionBeanMetaData) bean;
            if (sessionBean.isStateful())
            {
               continue;
            }
         }
         else if (bean.isEntity() || bean.isService())
         {
            continue;
         }
         // process
         String mcBeanName = MC_BEAN_PREFIX + unit.getName() + "$" + bean.getEjbName();
         BeanMetaData bmd = this.createAutoTimerInitializer(mcBeanName, bean);
         unit.addAttachment(BeanMetaData.class + ":" + mcBeanName, bmd);
      }
   }

   private BeanMetaData createAutoTimerInitializer(String mcBeanName, JBossEnterpriseBeanMetaData bean)
   {
      AutoTimerInitializer autoTimerInitializer = new AutoTimerInitializer();
      
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(mcBeanName, autoTimerInitializer.getClass().getName());
      builder.setConstructorValue(autoTimerInitializer);

      // add dependency
      AbstractInjectionValueMetaData injectMetaData = new AbstractInjectionValueMetaData(bean.getContainerName());
      injectMetaData.setDependentState(ControllerState.INSTALLED);

      // Too bad we have to know the field name. Need to do more research on MC to see if we can
      // add property metadata based on type instead of field name.
      builder.addPropertyMetaData("container", injectMetaData);

      return builder.getBeanMetaData();
   }
}
