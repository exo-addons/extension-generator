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
package org.exoplatform.extension.generator.service.handler;

import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * The Class JCRQueryConfigurationHandler.
 */
public class JCRQueryConfigurationHandler extends AbstractConfigurationHandler {
  
  /** The configuration paths. */
  private final List<String> configurationPaths = new ArrayList<String>();

  /** The log. */
  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  public boolean writeData(ZipOutputStream zos, String extensionName, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.ECM_QUERY_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    configurationPaths.clear();
    ZipFile zipFile = null;
    try {
      zipFile = getExportedFileFromOperation(ExtensionGenerator.ECM_QUERY_PATH);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = (ZipEntry) entries.nextElement();
        String location = zipEntry.getName().replaceAll("ecmadmin/", "");
        try {
          InputStream inputStream = zipFile.getInputStream(zipEntry);
          Utils.writeZipEnry(zos, DMS_CONFIGURATION_LOCATION + location, extensionName, inputStream, false);
          configurationPaths.add(DMS_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + location);
        } catch (Exception e) {
          log.error("Error while serializing JCR Query data", e);
          return false;
        }
      }
    } finally {
      if (zipFile != null) {
        try {
          zipFile.close();
        } catch (Exception e) {
          // Nothing to do
        }
      }
      clearTempFiles();
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Log getLogger() {
    return log;
  }
}
