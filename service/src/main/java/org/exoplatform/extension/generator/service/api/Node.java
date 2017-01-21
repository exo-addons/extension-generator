/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
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
package org.exoplatform.extension.generator.service.api;

import java.io.Serializable;

/**
 * The Class Node.
 */
public class Node implements Serializable, Comparable<Node> {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -8330709372395885654L;

  /** The path. */
  private String path;
  
  /** The text. */
  private String text;
  
  /** The description. */
  private String description;

  /**
   * Instantiates a new node.
   *
   * @param text the text
   * @param description the description
   * @param path the path
   */
  public Node(String text, String description, String path) {
    this.text = text;
    this.path = path;
    this.description = description;
  }

  /**
   * Gets the text.
   *
   * @return the text
   */
  public String getText() {
    return text;
  }

  /**
   * Sets the text.
   *
   * @param text the new text
   */
  public void setText(String text) {
    this.text = text;
  }

  /**
   * Sets the path.
   *
   * @return the string
   */
  public String setPath() {
    return this.path;
  }

  /**
   * Gets the path.
   *
   * @return the path
   */
  public String getPath() {
    return this.path;
  }

  /**
   * Gets the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description the new description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    } else if (obj instanceof Node) {
      return text.equals(((Node) obj).getPath());
    } else if (obj instanceof String) {
      return text.equals(obj);
    } else {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return path.hashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(Node o) {
    return text.compareTo(o.getText());
  }
}
