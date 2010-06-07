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
package org.jboss.ejb3.timerservice.mk2.test.common;

import java.sql.Connection;

import javax.naming.InitialContext;

import org.hsqldb.jdbc.jdbcDataSource;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class HSQLDBService
{
   private static final Logger log = Logger.getLogger(HSQLDBService.class);
   
   private String jndiName = "java:/DefaultDS";
   
   private jdbcDataSource ds;
   private InitialContext ctx;
   private Connection conn;
   
   public void create() throws Exception
   {
      log.info("Creating HSQLDB service");
      
      ds = new jdbcDataSource();
      ds.setDatabase("jdbc:hsqldb:file:target/hsqldb/db");
      ds.setUser("sa");
      ds.setPassword("");
      
      ctx = new InitialContext();
   }
   
   public void destroy() throws Exception
   {
      log.info("Destroying HSQLDB service");
      
      if(ctx != null)
      {
         ctx.close();
         ctx = null;
      }
      ds = null;
   }
   
   public void start() throws Exception
   {
      log.info("Starting HSQLDB service");
      
      conn = ds.getConnection();
      
      ctx.bind(jndiName, ds);
   }
   
   public void stop() throws Exception
   {
      log.info("Stopping HSQLDB service");
      
      ctx.unbind(jndiName);
      
      if(conn != null)
      {
         conn.close();
         conn = null;
      }
   }
}
