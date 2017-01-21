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

import com.thoughtworks.xstream.XStream;

import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.management.ecmadmin.operations.templates.NodeTemplate;
import org.exoplatform.management.ecmadmin.operations.templates.metadata.MetadataTemplatesMetaData;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.cms.templates.impl.TemplateConfig;
import org.exoplatform.services.cms.templates.impl.TemplateConfig.NodeType;
import org.exoplatform.services.cms.templates.impl.TemplatePlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * The Class MetadataTemplatesConfigurationHandler.
 */
public class MetadataTemplatesConfigurationHandler extends AbstractConfigurationHandler {
  
  /** The Constant METADATA_CONFIGURATION_LOCATION. */
  private static final String METADATA_CONFIGURATION_LOCATION = DMS_CONFIGURATION_LOCATION + "templates/metadata";
  
  /** The Constant METADATA_CONFIGURATION_NAME. */
  private static final String METADATA_CONFIGURATION_NAME = "metadata-templates-configuration.xml";
  
  /** The Constant configurationPaths. */
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(DMS_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + METADATA_CONFIGURATION_NAME);
  }

  /** The log. */
  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  public boolean writeData(ZipOutputStream zos, String extensionName, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.ECM_TEMPLATES_METADATA_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    List<String> filterMetadatas = new ArrayList<String>();
    for (String resourcePath : filteredSelectedResources) {
      String metadataName = resourcePath.replace(ExtensionGenerator.ECM_TEMPLATES_METADATA_PATH + "/", "");
      filterMetadatas.add(metadataName);
    }
    List<MetadataTemplatesMetaData> metaDatas = new ArrayList<MetadataTemplatesMetaData>();
    ZipFile zipFile = null;
    try {
      zipFile = getExportedFileFromOperation(ExtensionGenerator.ECM_TEMPLATES_METADATA_PATH, filterMetadatas.toArray(new String[0]));
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = (ZipEntry) entries.nextElement();
        try {
          InputStream inputStream = zipFile.getInputStream(zipEntry);
          if (zipEntry.getName().endsWith("metadata.xml")) {
            XStream xStream = new XStream();
            xStream.alias("metadata", MetadataTemplatesMetaData.class);
            xStream.alias("template", NodeTemplate.class);
            MetadataTemplatesMetaData metadata = (MetadataTemplatesMetaData) xStream.fromXML(new InputStreamReader(inputStream));
            metaDatas.add(metadata);
          } else {
            String location = DMS_CONFIGURATION_LOCATION + zipEntry.getName().replace(":", "_");
            Utils.writeZipEnry(zos, location, extensionName, inputStream, false);
          }
        } catch (Exception e) {
          log.error("Error while serializing Metadata templates data", e);
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

    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
    List<NodeType> nodeTypes = new ArrayList<NodeType>();
    {
      InitParams params = new InitParams();
      params.addParam(getValueParam("autoCreateInNewRepository", "true"));
      params.addParam(getValueParam("storedLocation", METADATA_CONFIGURATION_LOCATION.replace("WEB-INF", "war:").replace("custom-extension", extensionName)));
      ObjectParameter objectParameter = new ObjectParameter();
      objectParameter.setName("metadata.template.configuration");
      TemplateConfig templateConfig = new TemplateConfig();
      templateConfig.setNodeTypes(nodeTypes);
      objectParameter.setObject(templateConfig);
      params.addParam(objectParameter);

      ComponentPlugin plugin = createComponentPlugin("addPlugins", TemplatePlugin.class.getName(), "addPlugins", params);
      addComponentPlugin(externalComponentPlugins, MetadataService.class.getName(), plugin);
    }

    for (MetadataTemplatesMetaData metadataTemplatesMetaData : metaDatas) {
      NodeType nodeType = new NodeType();
      nodeType.setDocumentTemplate(metadataTemplatesMetaData.isDocumentTemplate());
      nodeType.setLabel(metadataTemplatesMetaData.getLabel());
      nodeType.setNodetypeName(metadataTemplatesMetaData.getNodeTypeName());
      nodeType.setReferencedDialog(Utils.convertTemplateList(metadataTemplatesMetaData.getTemplates().get("dialogs")));
      nodeType.setReferencedView(Utils.convertTemplateList(metadataTemplatesMetaData.getTemplates().get("views")));
      nodeType.setReferencedSkin(Utils.convertTemplateList(metadataTemplatesMetaData.getTemplates().get("skins")));
      nodeTypes.add(nodeType);
    }
    return Utils.writeConfiguration(zos, DMS_CONFIGURATION_LOCATION + METADATA_CONFIGURATION_NAME, extensionName, externalComponentPlugins);
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
