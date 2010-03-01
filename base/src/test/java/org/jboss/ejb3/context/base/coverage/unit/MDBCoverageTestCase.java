/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.ejb3.context.base.coverage.unit;

import org.jboss.ejb3.context.base.BaseMessageDrivenContext;
import org.jboss.ejb3.context.base.coverage.MDBeanManager;
import org.jboss.ejb3.context.spi.MessageDrivenBeanManager;
import org.jboss.ejb3.context.spi.MessageDrivenContext;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MDBCoverageTestCase
{
   @Test
   public void testMDB()
   {
      MessageDrivenBeanManager manager = new MDBeanManager();
      Object instance = new Object();
      MessageDrivenContext ctx = new BaseMessageDrivenContext(manager, instance);
      
      MessageDrivenBeanManager result = ctx.getManager();
      assertSame(manager, result);
   }
}
