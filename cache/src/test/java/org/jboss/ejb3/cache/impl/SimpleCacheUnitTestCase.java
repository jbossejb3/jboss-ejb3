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
package org.jboss.ejb3.cache.impl;

import javax.ejb.NoSuchEJBException;

import org.jboss.ejb3.cache.Cache;
import org.jboss.ejb3.cache.StatefulObjectFactory;

import junit.framework.TestCase;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class SimpleCacheUnitTestCase extends TestCase
{

   public void test1()
   {
      StatefulObjectFactory<MockIdentifiable> factory = new MockStatefulObjectFactory();
      Cache<MockIdentifiable> cache = new SimpleCache<MockIdentifiable>(factory);
      
      try
      {
         cache.get(1);
         fail("Object 1 should not be in cache");
      }
      catch(NoSuchEJBException e)
      {
         // good
      }
   }
   
   public void test2()
   {
      StatefulObjectFactory<MockIdentifiable> factory = new MockStatefulObjectFactory();
      Cache<MockIdentifiable> cache = new SimpleCache<MockIdentifiable>(factory);
      
      MockIdentifiable object = cache.create(null, null);
      Object key = object.getId();
      
      MockIdentifiable obj2 = cache.get(key);
      assertEquals(object, obj2);
      
      cache.remove(key);
      
      try
      {
         cache.get(key);
         fail("Object should not be in cache");
      }
      catch(NoSuchEJBException e)
      {
         // good
      }
      
      // EJBTHREE-1218: throw NoSuchEJBException on remove
      try
      {
         cache.remove(key);
         fail("Expected NoSuchEJBException");
      }
      catch(NoSuchEJBException e)
      {
         // good
      }
   }
}
