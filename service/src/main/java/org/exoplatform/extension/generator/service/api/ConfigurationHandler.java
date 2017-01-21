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

import java.util.Collection;
import java.util.List;
import java.util.zip.ZipOutputStream;

/**
 * The Interface ConfigurationHandler.
 */
public interface ConfigurationHandler {

  /**
   * Writes XML files corresponding to the set of selected managed resources in
   * Archive.
   *
   * @param zos Generated WAR output stream
   * @param extensionName the extension name
   * @param tempSelectedResources Set of selected managed resources path
   * @return true if some files was written in archive
   */
  public abstract boolean writeData(ZipOutputStream zos, String extensionName, Collection<String> tempSelectedResources);

  /**
   * Gets the configuration paths.
   *
   * @return list of configuration paths written in archive
   */
  public abstract List<String> getConfigurationPaths();

}
