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
package org.jboss.ejb3.test.pool.common;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class MockBean
{
   static int finalized = 0;
   static int preDestroys = 0;
   static int postConstructs = 0;
   
   @Override
   protected void finalize() throws Throwable
   {
      super.finalize();
      
      finalized++;
   }
   
   public static int getFinalized()
   {
      return finalized;
   }
   
   public static int getPreDestroys()
   {
      return preDestroys;
   }
   
   public static int getPostConstructs()
   {
      return postConstructs;
   }
   
   public void postConstruct()
   {
      postConstructs++;
   }
   
   public void preDestroy()
   {
      preDestroys++;
   }
   
   public static void reset()
   {
      finalized = 0;
      preDestroys = 0;
      postConstructs = 0;
   }
}
