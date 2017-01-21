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

import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.impl.AddNodeTypePlugin;
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
 * The Class ActionNodeTypeConfigurationHandler.
 * TODO /ecmadmin/action not used for PLF 4.3+
 */
@Deprecated
public class ActionNodeTypeConfigurationHandler extends AbstractConfigurationHandler {
  
  /** The Constant ACTION_CONFIGURATION_NAME. */
  private static final String ACTION_CONFIGURATION_NAME = "jcr-actions-component-plugins-configuration.xml";
  
  /** The Constant JCR_CONFIGURATION_LOCATION. */
  private static final String JCR_CONFIGURATION_LOCATION = "WEB-INF/conf/custom-extension/jcr/";
  
  /** The Constant configurationPaths. */
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(JCR_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + ACTION_CONFIGURATION_NAME);
  }

  /** The log. */
  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  public boolean writeData(ZipOutputStream zos, String extensionName, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.ECM_ACTION_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }

    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();

    List<String> filterActionTypes = new ArrayList<String>();
    for (String resourcePath : filteredSelectedResources) {
      String actionTypeName = resourcePath.replace(ExtensionGenerator.ECM_ACTION_PATH + "/", "");
      filterActionTypes.add(actionTypeName);
    }
    ZipFile zipFile = null;
    try {
      zipFile = getExportedFileFromOperation(ExtensionGenerator.ECM_ACTION_PATH, filterActionTypes.toArray(new String[0]));
      ValuesParam valuesParam = new ValuesParam();
      valuesParam.setName("autoCreatedInNewRepository");
      valuesParam.setValues(new ArrayList<String>());
      ComponentPlugin plugin = createComponentPlugin("add.nodetype", AddNodeTypePlugin.class.getName(), "addPlugin", null, valuesParam);
      addComponentPlugin(externalComponentPlugins, RepositoryService.class.getName(), plugin);

      //
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = (ZipEntry) entries.nextElement();
        String actionTypeConfigurationLocation = JCR_CONFIGURATION_LOCATION + zipEntry.getName();

        String path = actionTypeConfigurationLocation.replace("WEB-INF", "war:").replace("custom-extension", extensionName).replaceAll("/ecmadmin", "");

        valuesParam.getValues().add(path);
        try {
          InputStream inputStream = zipFile.getInputStream(zipEntry);
          Utils.writeZipEnry(zos, actionTypeConfigurationLocation, extensionName, inputStream, false);
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
    return Utils.writeConfiguration(zos, JCR_CONFIGURATION_LOCATION + ACTION_CONFIGURATION_NAME, extensionName, externalComponentPlugins);
  }

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
  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }
}
