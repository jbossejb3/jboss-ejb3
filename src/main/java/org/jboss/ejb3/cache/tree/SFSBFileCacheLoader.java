/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.ejb3.cache.tree;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.loader.FileCacheLoader;

public class SFSBFileCacheLoader extends FileCacheLoader
{
   Log log = LogFactory.getLog(SFSBFileCacheLoader.class);
   
   @Override

   protected boolean isCharacterPortableTree(Fqn fqn)
   {
      /* For fqn, check '*' '<' '>' '|' '"' '?' and also '\' '/' and ':' */
      Pattern fqnPattern = Pattern.compile("[\\\\\\/:*<>|\"?]");

      List elements = fqn.peekElements();
      for (Object anElement : elements)
      {
         Matcher matcher = fqnPattern.matcher(anElement.toString());
         if (matcher.find())
         {
            log.warn("One of the Fqn ( " + fqn + " ) elements contains one of these characters: '*' '<' '>' '|' '\"' '?' '\\' '/' ':' ");
            log.warn("Directories containing these characters are illegal in some operative systems and could lead to portability issues");
            return false;
         }
      }

      return true;
   }
   
}
