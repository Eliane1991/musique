/*
 * Cuelib library for manipulating cue sheets.
 * Copyright (C) 2007-2008 Jan-Willem van den Broek
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package jwbroek.cuelib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple representation of an INDEX datum in a cue sheet.
 * @author jwbroek
 */
public class Index
{
  /**
   * The logger for this class.
   */
  private final static Logger logger = LoggerFactory.getLogger(Index.class.getCanonicalName());
  /**
   * The index number. -1 signifies that the number was not specified.
   */
  private int number = -1;
  /**
   * The position of this index. Null signifies that it was not specified.
   */
  private Position position = null;
  
  /**
   * Create a new Index.
   */
  public Index()
  {
    Index.logger.info(Index.class.getCanonicalName(), "Index()");
    Index.logger.info(Index.class.getCanonicalName(), "Index()");
  }

  /**
   * Create a new Index.
   * @param number The number of this index. -1 signifies that it was not specified.
   * @param position The position of this index. Null signifies that it was not specified.
   */
  public Index(final int number, final Position position)
  {
    Index.logger.info
      (Index.class.getCanonicalName(), "Index(int,Position)", new Object[] {number, position});
    this.number = number;
    this.position = position;
    Index.logger.info(Index.class.getCanonicalName(), "Index(int,Position)");
  }

  /**
   * Get the number of this index. -1 signifies that it was not specified.
   * @return The number of this index. -1 signifies that it was not specified.
   */
  public int getNumber()
  {
    Index.logger.info(Index.class.getCanonicalName(), "getNumber()");
    Index.logger.info(Index.class.getCanonicalName(), "getNumber()", this.number);
    return this.number;
  }

  /**
   * Set the number of this index. -1 signifies that it was not specified.
   * @param number The number of this index. -1 signifies that it was not specified.
   */
  public void setNumber(final int number)
  {
    Index.logger.info(Index.class.getCanonicalName(), "setNumber()", number);
    this.number = number;
    Index.logger.info(Index.class.getCanonicalName(), "setNumber()");
  }

  /**
   * Get the position of this index. Null signifies that it was not specified.
   * @return The position of this index. Null signifies that it was not specified.
   */
  public Position getPosition()
  {
    Index.logger.info(Index.class.getCanonicalName(), "getPosition()");
    Index.logger.info(Index.class.getCanonicalName(), "getPosition()", this.position);
    return this.position;
  }

  /**
   * Set the position of this index. Null signifies that it was not specified.
   * @param position The position of this index. Null signifies that it was not specified.
   */
  public void setPosition(final Position position)
  {
    Index.logger.info(Index.class.getCanonicalName(), "setPosition(Position)", position);
    this.position = position;
    Index.logger.info(Index.class.getCanonicalName(), "setPosition(Position)");
  }
}
