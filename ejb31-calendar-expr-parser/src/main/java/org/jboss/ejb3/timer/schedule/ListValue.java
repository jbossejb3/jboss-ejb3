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
package org.jboss.ejb3.timer.schedule;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Represents a value for a {@link ScheduleExpression} which is expressed as an list type. An
 * {@link ListValue} comprises of values separated by a ",".
 * 
 * <p>
 *  Each value in the {@link ListValue} must be an individual attribute value or a range. 
 *  List items <b>cannot</b> themselves be lists, wild-cards, or increments. 
 *  Duplicate values are allowed, but are ignored.
 * </p>
 *
 * @see ScheduleExpressionType#LIST
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class ListValue
{

   /**
    * Separator used for parsing a {@link String} which represents
    * a {@link ListValue}
    */
   public static final String LIST_SEPARATOR = ",";
   
   /**
    * The individual values in a {@link ListValue}
    * <p>
    *   Each value in this set may be a {@link String} representing a {@link SingleValue}
    *   or a {@link RangeValue}
    * </p>
    */
   private Set<String> values = new HashSet<String>();

   /**
    * Creates a {@link ListValue} by parsing the passed <code>value</code>.
    * 
    * @param list The value to be parsed
    * @throws IllegalArgumentException If the passed <code>value</code> cannot be 
    *           represented as an {@link ListValue}
    * 
    */
   public ListValue(String list)
   {
      StringTokenizer tokenizer = new StringTokenizer(list, LIST_SEPARATOR);
      while (tokenizer.hasMoreTokens())
      {
         String value = tokenizer.nextToken();
         this.values.add(value);
      }
   }

   /**
    * Returns the values that make up the {@link ListValue}. 
    * <p>
    *   Each value in this set may be a {@link String} representing a {@link SingleValue}
    *   or a {@link RangeValue}
    * </p>
    * @return
    */
   public Set<String> getValues()
   {
      return this.values;
   }
   
}
