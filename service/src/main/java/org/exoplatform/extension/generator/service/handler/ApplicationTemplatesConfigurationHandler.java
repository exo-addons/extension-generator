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
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.management.ecmadmin.operations.templates.applications.ApplicationTemplatesMetadata;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.cms.views.PortletTemplatePlugin;
import org.exoplatform.services.cms.views.PortletTemplatePlugin.PortletTemplateConfig;

import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * The Class ApplicationTemplatesConfigurationHandler.
 */
public abstract class ApplicationTemplatesConfigurationHandler extends AbstractConfigurationHandler {
  
  /** The application templates home path. */
  private String applicationTemplatesHomePath;
  
  /** The application configuration file name. */
  private String applicationConfigurationFileName;
  
  /** The staging extension path. */
  private String stagingExtensionPath;
  
  /** The portlet name. */
  private String portletName;

  /**
   * Instantiates a new application templates configuration handler.
   *
   * @param applicationTemplatesHomePath the application templates home path
   * @param applicationConfigurationFileName the application configuration file name
   * @param stagingExtensionPath the staging extension path
   * @param portletName the portlet name
   */
  public ApplicationTemplatesConfigurationHandler(String applicationTemplatesHomePath, String applicationConfigurationFileName, String stagingExtensionPath, String portletName) {
    this.applicationTemplatesHomePath = applicationTemplatesHomePath;
    this.applicationConfigurationFileName = applicationConfigurationFileName;
    this.stagingExtensionPath = stagingExtensionPath;
    this.portletName = portletName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean writeData(ZipOutputStream zos, String extensionName, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, stagingExtensionPath);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    ApplicationTemplatesMetadata metadata = new ApplicationTemplatesMetadata();
    for (String resourcePath : filteredSelectedResources) {
      ZipFile zipFile = null;
      try {
        zipFile = getExportedFileFromOperation(resourcePath);
        // Compute Metadata
        ApplicationTemplatesMetadata tmpMetadata = getApplicationTemplatesMetadata(zipFile);
        if (tmpMetadata != null) {
          metadata.getTitleMap().putAll(tmpMetadata.getTitleMap());
        }
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
          ZipEntry zipEntry = (ZipEntry) entries.nextElement();
          if (zipEntry.isDirectory() || zipEntry.getName().equals("") || !zipEntry.getName().endsWith(".gtmpl")) {
            continue;
          }
          try {
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            Utils.writeZipEnry(zos, DMS_CONFIGURATION_LOCATION + zipEntry.getName(), extensionName, inputStream, false);
          } catch (Exception e) {
            getLogger().error(e);
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
    }

    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
    InitParams params = new InitParams();
    params.addParam(getValueParam("portletName", portletName));
    params.addParam(getValueParam("portlet.template.path", applicationTemplatesHomePath.replace("WEB-INF", "war:").replace("custom-extension", extensionName)));

    for (String selectedResourcePath : filteredSelectedResources) {
      PortletTemplateConfig templateConfig = new PortletTemplateConfig();

      String tmpPath = selectedResourcePath.replace(stagingExtensionPath + "/", "");
      String[] paths = tmpPath.split("/");

      templateConfig.setCategory(paths[0]);
      templateConfig.setTemplateName(paths[1]);

      String relativePath = selectedResourcePath.replaceAll("/ecmadmin/", "");

      String templateTitle = templateConfig.getTemplateName();
      if (metadata != null && metadata.getTitle(relativePath) != null) {
        templateTitle = metadata.getTitle(relativePath);
      }
      templateConfig.setTitle(templateTitle);

      ObjectParameter objectParameter = new ObjectParameter();
      objectParameter.setName(paths[1].replace(".gtmpl", ""));
      objectParameter.setObject(templateConfig);
      params.addParam(objectParameter);
    }

    ComponentPlugin plugin = createComponentPlugin("templates.plugin", PortletTemplatePlugin.class.getName(), "addPlugin", params);
    addComponentPlugin(externalComponentPlugins, ApplicationTemplateManagerService.class.getName(), plugin);

    return Utils.writeConfiguration(zos, DMS_CONFIGURATION_LOCATION + applicationConfigurationFileName, extensionName, externalComponentPlugins);
  }

}
