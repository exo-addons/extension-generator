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

import com.thoughtworks.xstream.XStream;

import org.apache.commons.io.FileUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.Parameter;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.management.ecmadmin.operations.templates.applications.ApplicationTemplatesMetadata;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.ContentType;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.controller.ManagedRequest;
import org.gatein.management.api.controller.ManagedResponse;
import org.gatein.management.api.controller.ManagementController;
import org.gatein.management.api.operation.OperationNames;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The Class AbstractConfigurationHandler.
 */
public abstract class AbstractConfigurationHandler implements ConfigurationHandler {
  
  /** The Constant DMS_CONFIGURATION_LOCATION. */
  protected static final String DMS_CONFIGURATION_LOCATION = "WEB-INF/conf/custom-extension/dms/";

  /** The management controller. */
  // GateIN Management Controller
  private ManagementController managementController = null;

  /** The temp files. */
  protected List<File> tempFiles = new ArrayList<File>();

  /**
   * Gets the logger.
   *
   * @return the logger
   */
  protected abstract Log getLogger();

  /**
   * Call GateIN Management Controller to export selected resource using options
   * passed in filters.
   *
   * @param path managed path
   * @param filters passed to GateIN Management SPI
   * @return archive file exported from GateIN Management Controller call
   */
  protected ZipFile getExportedFileFromOperation(String path, String... filters) {
    ManagedRequest request = null;
    if (filters != null && filters.length > 0) {
      Map<String, List<String>> attributes = new HashMap<String, List<String>>();
      attributes.put("filter", Arrays.asList(filters));
      request = ManagedRequest.Factory.create(OperationNames.EXPORT_RESOURCE, PathAddress.pathAddress(path), attributes, ContentType.ZIP);
    } else {
      request = ManagedRequest.Factory.create(OperationNames.EXPORT_RESOURCE, PathAddress.pathAddress(path), ContentType.ZIP);
    }
    FileOutputStream outputStream = null;
    File tmpFile = null;
    try {
      // Call GateIN Management SPI
      ManagedResponse response = getManagementController().execute(request);
      // Create temp file
      tmpFile = File.createTempFile("exo", "-extension-generator.zip");
      outputStream = new FileOutputStream(tmpFile);
      // Create temp file
      response.writeResult(outputStream, false);
      return new ZipFile(tmpFile);
    } catch (Exception e) {
      throw new RuntimeException("Error while handling Response from GateIN Management, export operation", e);
    } finally {
      if (outputStream != null) {
        try {
          outputStream.flush();
          outputStream.close();
        } catch (IOException ioExp) {
          // Nothing to do
        }
      }
      if (tmpFile != null && tmpFile.exists()) {
        tempFiles.add(tmpFile);
      }
    }
  }

  /**
   * Delete temp files created by GateIN management operations.
   */
  protected void clearTempFiles() {
    for (File tempFile : tempFiles) {
      if (tempFile != null && tempFile.exists()) {
        try {
          FileUtils.forceDelete(tempFile);
        } catch (Exception e) {
          getLogger().warn("Unable to delete temp file: " + tempFile.getAbsolutePath() + ". Not blocker.");
          tempFile.deleteOnExit();
        }
      }
    }
    tempFiles.clear();
  }

  /**
   * Filters subresources of parentPath. This operation retains only paths that
   * contains parentPath.
   * 
   * @param selectedResources
   *          Set of managed resources paths
   * @param parentPath
   *          parent resource path
   * @return Set of sub resources path of type String
   */
  public static Set<String> filterSelectedResources(Collection<String> selectedResources, String parentPath) {
    Set<String> filteredSelectedResources = new HashSet<String>();
    for (String resourcePath : selectedResources) {
      if (resourcePath.contains(parentPath)) {
        filteredSelectedResources.add(resourcePath);
      }
    }
    return filteredSelectedResources;
  }

  /**
   * Adds the component plugin.
   *
   * @param externalComponentPlugins the external component plugins
   * @param componentKey the component key
   * @param componentPlugin the component plugin
   */
  protected void addComponentPlugin(ExternalComponentPlugins externalComponentPlugins, String componentKey, ComponentPlugin componentPlugin) {
    externalComponentPlugins.setTargetComponent(componentKey);
    if (externalComponentPlugins.getComponentPlugins() == null) {
      externalComponentPlugins.setComponentPlugins(new ArrayList<ComponentPlugin>());
    }
    externalComponentPlugins.getComponentPlugins().add(componentPlugin);
  }

  /**
   * Creates the component plugin.
   *
   * @param name the name
   * @param type the type
   * @param methodName the method name
   * @param params the params
   * @param parameters the parameters
   * @return the component plugin
   */
  protected ComponentPlugin createComponentPlugin(String name, String type, String methodName, InitParams params, Parameter... parameters) {
    ComponentPlugin plugin = new ComponentPlugin();
    plugin.setName(name);
    plugin.setSetMethod(methodName);
    plugin.setType(type);
    plugin.setInitParams(params);
    addParameter(plugin, parameters);
    return plugin;
  }

  /**
   * Adds the parameter.
   *
   * @param plugin the plugin
   * @param parameters the parameters
   */
  protected void addParameter(ComponentPlugin plugin, Parameter... parameters) {
    InitParams params = plugin.getInitParams();
    if (params == null) {
      params = new InitParams();
      plugin.setInitParams(params);
    }
    if (parameters != null && parameters.length > 0) {
      for (Parameter parameter : parameters) {
        params.addParameter(parameter);
      }
    }
  }

  /**
   * Gets the value param.
   *
   * @param name the name
   * @param value the value
   * @return the value param
   */
  protected ValueParam getValueParam(String name, String value) {
    ValueParam valueParam = new ValueParam();
    valueParam.setName(name);
    valueParam.setValue(value);
    return valueParam;
  }

  /**
   * Gets the application templates metadata.
   *
   * @param zipFile the zip file
   * @return the application templates metadata
   */
  protected ApplicationTemplatesMetadata getApplicationTemplatesMetadata(ZipFile zipFile) {
    ZipEntry applicationTemplateMetadataEntry = zipFile.getEntry("ecmadmin/templates/applications/metadata.xml");
    if (applicationTemplateMetadataEntry != null) {
      try {
        InputStream inputStream = zipFile.getInputStream(applicationTemplateMetadataEntry);
        XStream xStream = new XStream();
        xStream.alias("metadata", ApplicationTemplatesMetadata.class);
        return (ApplicationTemplatesMetadata) xStream.fromXML(new InputStreamReader(inputStream));
      } catch (IOException e) {
        getLogger().error("Error while gettin Application Template Metadata", e);
      }
    }
    return null;
  }

  /**
   * Gets the management controller.
   *
   * @return the management controller
   */
  protected ManagementController getManagementController() {
    if (managementController == null) {
      managementController = (ManagementController) PortalContainer.getInstance().getComponentInstanceOfType(ManagementController.class);
    }
    return managementController;
  }
}
