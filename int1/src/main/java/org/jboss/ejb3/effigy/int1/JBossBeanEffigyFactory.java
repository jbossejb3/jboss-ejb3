/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @authors tag. See the copyright.txt in the
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
package org.jboss.ejb3.effigy.int1;

import org.jboss.ejb3.effigy.EnterpriseBeanEffigy;
import org.jboss.ejb3.effigy.common.JBossBeanEffigyInfo;
import org.jboss.ejb3.effigy.common.JBossEnterpriseBeanEffigy;
import org.jboss.ejb3.effigy.common.JBossSessionBeanEffigy;
import org.jboss.ejb3.effigy.spi.BeanEffigyFactory;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMessageDrivenBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossSessionBeanMetaData;
import org.jboss.metadata.ejb.jboss.jndipolicy.plugins.JBossSessionPolicyDecorator;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class JBossBeanEffigyFactory implements BeanEffigyFactory<JBossBeanEffigyInfo>
{
   @Override
   public <T extends EnterpriseBeanEffigy> T create(JBossBeanEffigyInfo info, Class<T> expectedType)
           throws ClassNotFoundException
   {
      JBossEnterpriseBeanMetaData beanMetaData = info.getBeanMetaData();
      // hack one: JNDI policy might be woven into the class hierarchy
      if(beanMetaData instanceof JBossSessionPolicyDecorator)
         beanMetaData = ((JBossSessionPolicyDecorator<JBossSessionBeanMetaData>) beanMetaData).getDelegate();
      // TODO: a lot
      if(beanMetaData instanceof JBossMessageDrivenBeanMetaData)
         return (T) new JBossEnterpriseBeanEffigy(info.getClassLoader(), beanMetaData);
      return (T) new JBossSessionBeanEffigy(info.getClassLoader(), (JBossSessionBeanMetaData) beanMetaData);
   }
}
