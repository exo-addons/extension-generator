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
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.services.cms.impl.ResourceConfig;
import org.exoplatform.services.cms.impl.ResourceConfig.Resource;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.cms.scripts.impl.ScriptPlugin;
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
 * The Class ScriptsConfigurationHandler.
 */
public class ScriptsConfigurationHandler extends AbstractConfigurationHandler {
  
  /** The Constant SCRIPT_CONFIGURATION_NAME. */
  private static final String SCRIPT_CONFIGURATION_NAME = "scripts-configuration.xml";
  
  /** The Constant configurationPaths. */
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(DMS_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + SCRIPT_CONFIGURATION_NAME);
  }

  /** The log. */
  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  public boolean writeData(ZipOutputStream zos, String extensionName, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.ECM_SCRIPT_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }

    List<String> filterScripts = new ArrayList<String>();
    for (String resourcePath : filteredSelectedResources) {
      String scriptName = resourcePath.replace(ExtensionGenerator.ECM_SCRIPT_PATH + "/", "");
      filterScripts.add(scriptName);
    }

    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
    List<ResourceConfig.Resource> scripts = new ArrayList<ResourceConfig.Resource>();
    {
      InitParams params = new InitParams();
      params.addParam(getValueParam("autoCreateInNewRepository", "true"));
      String location = DMS_CONFIGURATION_LOCATION.replace("WEB-INF", "war:").replace("custom-extension", extensionName);
      // Delete last '/'
      location = location.substring(0, location.length() - 1);
      params.addParam(getValueParam("predefinedScriptsLocation", location));
      ObjectParameter objectParameter = new ObjectParameter();
      objectParameter.setName("predefined.scripts");
      ResourceConfig resourceConfig = new ResourceConfig();
      resourceConfig.setRessources(scripts);
      objectParameter.setObject(resourceConfig);
      params.addParam(objectParameter);

      ComponentPlugin plugin = createComponentPlugin("manage.script.plugin", ScriptPlugin.class.getName(), "addScriptPlugin", params);
      addComponentPlugin(externalComponentPlugins, ScriptService.class.getName(), plugin);
    }
    ZipFile zipFile = null;
    try {
      zipFile = getExportedFileFromOperation(ExtensionGenerator.ECM_SCRIPT_PATH, filterScripts.toArray(new String[0]));
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = (ZipEntry) entries.nextElement();
        String relatifLocation = zipEntry.getName().replaceFirst("ecmadmin/script", "");
        String scriptConfigurationLocation = DMS_CONFIGURATION_LOCATION + "scripts/" + relatifLocation;
        Resource scriptResource = new ResourceConfig.Resource();
        scriptResource.setName(relatifLocation);
        scriptResource.setDescription(relatifLocation);
        scripts.add(scriptResource);
        try {
          InputStream inputStream = zipFile.getInputStream(zipEntry);
          Utils.writeZipEnry(zos, scriptConfigurationLocation, extensionName, inputStream, false);
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
    return Utils.writeConfiguration(zos, DMS_CONFIGURATION_LOCATION + SCRIPT_CONFIGURATION_NAME, extensionName, externalComponentPlugins);
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
