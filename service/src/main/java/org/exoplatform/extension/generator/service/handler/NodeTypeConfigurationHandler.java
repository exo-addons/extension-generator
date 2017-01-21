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
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.management.ecmadmin.operations.nodetype.NodeTypeExportTask;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValuesList;
import org.exoplatform.services.jcr.impl.AddNodeTypePlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;

/**
 * The Class NodeTypeConfigurationHandler.
 */
public class NodeTypeConfigurationHandler extends AbstractConfigurationHandler {
  
  /** The Constant JCR_CONFIGURATION_NAME. */
  private static final String JCR_CONFIGURATION_NAME = "jcr-component-plugins-configuration.xml";
  
  /** The Constant JCR_CONFIGURATION_LOCATION. */
  private static final String JCR_CONFIGURATION_LOCATION = "WEB-INF/conf/custom-extension/jcr/";
  
  /** The Constant NODETYPE_CONFIGURATION_LOCATION. */
  private static final String NODETYPE_CONFIGURATION_LOCATION = JCR_CONFIGURATION_LOCATION + "nodetypes.xml";
  
  /** The Constant JCR_NAMESPACES_CONFIGURATION_XML. */
  private static final String JCR_NAMESPACES_CONFIGURATION_XML = "ecmadmin/nodetype/jcr-namespaces-configuration.xml";
  
  /** The Constant configurationPaths. */
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(JCR_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + JCR_CONFIGURATION_NAME);
  }
  
  /** The ext manager. */
  private static ExtendedNodeTypeManager extManager = null;
  
  /** The log. */
  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * Instantiates a new node type configuration handler.
   */
  public NodeTypeConfigurationHandler() {
    RepositoryService repositoryService = (RepositoryService) PortalContainer.getInstance().getComponentInstanceOfType(RepositoryService.class);
    try {
      extManager = (ExtendedNodeTypeManager) repositoryService.getCurrentRepository().getNodeTypeManager();
    } catch (RepositoryException e) {
      log.error("Can't get reference to ExtendedNodeTypeManager Service.", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean writeData(ZipOutputStream zos, String extensionName, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.ECM_NODETYPE_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    // jcr-namespaces-configuration.xml
    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();

    List<String> filterNodeTypes = new ArrayList<String>();
    for (String resourcePath : filteredSelectedResources) {
      String nodeTypeName = resourcePath.replace(ExtensionGenerator.ECM_NODETYPE_PATH + "/", "");
      filterNodeTypes.add(nodeTypeName);
    }
    ZipFile zipFile = null;
    try {
      zipFile = getExportedFileFromOperation(ExtensionGenerator.ECM_NODETYPE_PATH, filterNodeTypes.toArray(new String[0]));
      ZipEntry namespaceConfigurationEntry = zipFile.getEntry(JCR_NAMESPACES_CONFIGURATION_XML);
      try {
        InputStream inputStream = zipFile.getInputStream(namespaceConfigurationEntry);
        Configuration configuration = Utils.fromXML(IOUtils.toByteArray(inputStream), Configuration.class);
        externalComponentPlugins = configuration.getExternalComponentPlugins(RepositoryService.class.getName());
      } catch (Exception e) {
        log.error("Error while getting NamespaceConfiguration Entry", e);
      }
      ValuesParam valuesParam = new ValuesParam();
      valuesParam.setName("autoCreatedInNewRepository");
      valuesParam.setValues(new ArrayList<String>());
      valuesParam.getValues().add(NODETYPE_CONFIGURATION_LOCATION.replace("WEB-INF", "war:").replace("custom-extension", extensionName));
      ComponentPlugin plugin = createComponentPlugin("add.nodetype", AddNodeTypePlugin.class.getName(), "addPlugin", null, valuesParam);
      plugin.setPriority(100);
      addComponentPlugin(externalComponentPlugins, RepositoryService.class.getName(), plugin);

      // Map of (NodeType, Path in WAR Extension)
      Vector<NodeType> nodeTypeValues = new Vector<NodeType>();

      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = (ZipEntry) entries.nextElement();
        if (zipEntry.getName().endsWith(JCR_NAMESPACES_CONFIGURATION_XML)) {
          continue;
        }
        String nodeTypeConfigurationLocation = JCR_CONFIGURATION_LOCATION + zipEntry.getName();
        try {
          addNodeType(zipFile, nodeTypeValues, zipEntry, nodeTypeConfigurationLocation.replace("WEB-INF", "war:").replace("custom-extension", extensionName));
        } catch (Exception e) {
          log.error("Error while marshalling " + zipEntry.getName(), e);
        }
      }

      // Sorts Nodetypes switch dependency
      int i = 0;
      while (i < nodeTypeValues.size()) {
        NodeType nodeType = nodeTypeValues.get(i);
        int nodeTypeIndex = nodeTypeValues.indexOf(nodeType);

        List<NodeType> dependencyNodeTypes = new ArrayList<NodeType>();
        getDependencies(nodeType, dependencyNodeTypes);
        boolean modified = false;
        for (NodeType nodeType2 : dependencyNodeTypes) {
          int superIndex = nodeTypeValues.indexOf(nodeType2);
          if (nodeTypeIndex < superIndex) {
            nodeTypeValues.remove(nodeTypeIndex);
            nodeTypeValues.add(superIndex, nodeType);
            nodeTypeIndex = superIndex;
            modified = true;
          }
        }
        if (!modified) {
          i++;
        }
      }

      String content = NodeTypeExportTask.getNodeTypeXML(nodeTypeValues);
      Utils.writeZipEnry(zos, NODETYPE_CONFIGURATION_LOCATION, extensionName, content, false);
    } catch (Exception e) {
      log.error("Error while serializing MOP data", e);
      return false;
    } finally {
      if (zipFile != null) {
        try {
          zipFile.close();
        } catch (Exception e) {
          // nothing to do
        }
      }
      clearTempFiles();
    }
    return Utils.writeConfiguration(zos, JCR_CONFIGURATION_LOCATION + JCR_CONFIGURATION_NAME, extensionName, externalComponentPlugins);
  }

  /**
   * Gets the dependencies.
   *
   * @param nodeType the node type
   * @param dependencyNodeTypes the dependency node types
   */
  private void getDependencies(NodeType nodeType, List<NodeType> dependencyNodeTypes) {
    NodeType[] superTypes = nodeType.getSupertypes();
    for (NodeType superType : superTypes) {
      if (!dependencyNodeTypes.contains(superType)) {
        dependencyNodeTypes.add(superType);
        getDependencies(superType, dependencyNodeTypes);
      }
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

  /**
   * Adds the node type.
   *
   * @param zipFile the zip file
   * @param nodeTypeValues the node type values
   * @param zipEntry the zip entry
   * @param nodeTypeConfigurationLocation the node type configuration location
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws JiBXException the ji BX exception
   * @throws NoSuchNodeTypeException the no such node type exception
   * @throws RepositoryException the repository exception
   */
  private void addNodeType(ZipFile zipFile, List<NodeType> nodeTypeValues, ZipEntry zipEntry, String nodeTypeConfigurationLocation) throws IOException, JiBXException, NoSuchNodeTypeException,
      RepositoryException {
    InputStream inputStream = zipFile.getInputStream(zipEntry);
    IBindingFactory factory = BindingDirectory.getFactory(NodeTypeValuesList.class);
    IUnmarshallingContext uctx = factory.createUnmarshallingContext();
    NodeTypeValuesList nodeTypeValuesList = (NodeTypeValuesList) uctx.unmarshalDocument(inputStream, null);
    ArrayList<?> ntvList = nodeTypeValuesList.getNodeTypeValuesList();
    if (ntvList.size() != 1) {
      log.warn("Incoherent nodetype declaration number in exported file.");
    } else {
      NodeTypeValue nodeTypeValue = (NodeTypeValue) ntvList.get(0);
      NodeType nodeType = extManager.getNodeType(nodeTypeValue.getName());
      nodeTypeValues.add(nodeType);
    }
  }

}
