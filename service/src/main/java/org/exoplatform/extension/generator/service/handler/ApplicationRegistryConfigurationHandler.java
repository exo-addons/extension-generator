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

import org.apache.commons.io.IOUtils;
import org.exoplatform.application.registry.ApplicationCategoriesPlugins;
import org.exoplatform.application.registry.ApplicationRegistryService;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.ObjectParameter;
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
 * The Class ApplicationRegistryConfigurationHandler.
 */
public class ApplicationRegistryConfigurationHandler extends AbstractConfigurationHandler {
  
  /** The Constant APPLICATION_REGISTRY_CONFIGURATION_XML. */
  private static final String APPLICATION_REGISTRY_CONFIGURATION_XML = "WEB-INF/conf/custom-extension/portal/application-registry-configuration.xml";
  
  /** The Constant configurationPaths. */
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(APPLICATION_REGISTRY_CONFIGURATION_XML.replace("WEB-INF", "war:"));
  }

  /** The log. */
  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  @Override
  protected Log getLogger() {
    return log;
  }

  /**
   * {@inheritDoc}
   */
  public boolean writeData(ZipOutputStream zos, String extensionName, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.REGISTRY_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
    ComponentPlugin plugin = createComponentPlugin("new.registry.category", ApplicationCategoriesPlugins.class.getName(), "initListener", null);
    addComponentPlugin(externalComponentPlugins, ApplicationRegistryService.class.getName(), plugin);

    for (String resourcePath : filteredSelectedResources) {
      ZipFile zipFile = null;
      try {
        zipFile = getExportedFileFromOperation(resourcePath);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
          ZipEntry zipEntry = (ZipEntry) entries.nextElement();
          try {
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            ObjectParameter objectParameter = Utils.fromXML(IOUtils.toByteArray(inputStream), ObjectParameter.class);
            objectParameter.setName(zipEntry.getName().replace(".xml", ""));
            addParameter(plugin, objectParameter);
          } catch (Exception e) {
            log.error("Error while marshalling " + zipEntry.getName(), e);
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
    }
    return Utils.writeConfiguration(zos, APPLICATION_REGISTRY_CONFIGURATION_XML, extensionName, externalComponentPlugins);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }
}
