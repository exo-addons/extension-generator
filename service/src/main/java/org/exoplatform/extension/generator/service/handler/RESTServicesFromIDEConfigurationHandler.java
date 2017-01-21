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

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.script.groovy.GroovyScript2RestLoader;
import org.exoplatform.services.jcr.ext.script.groovy.GroovyScript2RestLoaderPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.core.Response;

/**
 * The Class RESTServicesFromIDEConfigurationHandler.
 */
public class RESTServicesFromIDEConfigurationHandler extends AbstractConfigurationHandler {
  
  /** The Constant CONFIGURATION_LOCATION. */
  protected static final String CONFIGURATION_LOCATION = "WEB-INF/conf/custom-extension/rest";
  
  /** The Constant SCRIPTS_CONFIGURATION_LOCATION. */
  protected static final String SCRIPTS_CONFIGURATION_LOCATION = "WEB-INF/conf/custom-extension/rest/scripts";
  
  /** The Constant CONFIGURATION_NAME. */
  private static final String CONFIGURATION_NAME = "rest-groovy-configuration.xml";
  
  /** The Constant configurationPaths. */
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + "/" + CONFIGURATION_NAME);
  }

  /** The log. */
  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean writeData(ZipOutputStream zos, String extensionName, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.IDE_REST_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    GroovyScript2RestLoader script2RestLoader = (GroovyScript2RestLoader) PortalContainer.getInstance().getComponentInstanceOfType(GroovyScript2RestLoader.class);
    RepositoryService repositoryService = (RepositoryService) PortalContainer.getInstance().getComponentInstanceOfType(RepositoryService.class);

    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
    try {
      String repository = repositoryService.getCurrentRepository().getConfiguration().getName();
      for (String selectedResoucePath : filteredSelectedResources) {
        selectedResoucePath = selectedResoucePath.replace(ExtensionGenerator.IDE_REST_PATH, "");
        String[] parts = selectedResoucePath.split("::");
        if (parts.length != 2) {
          getLogger().warn("IDE REST Services - Selected Path: '" + selectedResoucePath + "' can't be processed. Ignore it.");
          continue;
        }
        String workspace = parts[0];
        String scriptPath = parts[1].substring(1);

        String scriptName = scriptPath.substring(scriptPath.lastIndexOf("/") + 1);
        String scriptParentPath = scriptPath.substring(0, scriptPath.lastIndexOf("/") + 1);
        String scriptPathInArchive = SCRIPTS_CONFIGURATION_LOCATION + "/" + scriptName;

        Response response = script2RestLoader.getScript(repository, workspace, scriptPath);
        InputStream inputStream = (InputStream) response.getEntity();
        Utils.writeZipEnry(zos, scriptPathInArchive, extensionName, inputStream, false);

        scriptPathInArchive = scriptPathInArchive.replace("WEB-INF", "war:");

        InitParams scriptInitParams = new InitParams();
        scriptInitParams.addParameter(getValueParam("node", "/" + scriptParentPath));
        scriptInitParams.addParameter(getValueParam("workspace", workspace));
        PropertiesParam propertiesParam = new PropertiesParam();
        propertiesParam.setName(scriptName);
        propertiesParam.setProperty("autoload", "true");
        propertiesParam.setProperty("path", scriptPathInArchive);
        scriptInitParams.addParameter(propertiesParam);

        ComponentPlugin plugin = createComponentPlugin(scriptName, GroovyScript2RestLoaderPlugin.class.getName(), "addPlugin", scriptInitParams);
        addComponentPlugin(externalComponentPlugins, GroovyScript2RestLoader.class.getName(), plugin);
      }

      return Utils.writeConfiguration(zos, CONFIGURATION_LOCATION + "/" + CONFIGURATION_NAME, extensionName, externalComponentPlugins);
    } catch (Exception e) {
      getLogger().error(e);
      return false;
    }
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
