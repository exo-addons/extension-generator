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
package org.exoplatform.extension.generator.service;

import org.apache.commons.io.FileUtils;
import org.exoplatform.application.gadget.Gadget;
import org.exoplatform.application.gadget.GadgetRegistryService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.Component;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.extension.generator.service.api.ConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Node;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.extension.generator.service.handler.ApplicationRegistryConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.CLVTemplatesConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.DrivesConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.GadgetsConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.JCRQueryConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.MOPSiteConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.MetadataTemplatesConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.NodeTypeConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.NodeTypeTemplatesConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.RESTServicesFromIDEConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.ScriptsConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.SiteContentsConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.SiteExplorerTemplatesConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.SiteExplorerViewConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.TaxonomyConfigurationHandler;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.script.groovy.GroovyScript2RestLoader;
import org.exoplatform.services.jcr.ext.script.groovy.GroovyScript2RestLoader.ScriptList;
import org.exoplatform.services.jcr.ext.script.groovy.GroovyScript2RestLoaderPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.ContentType;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.controller.ManagedRequest;
import org.gatein.management.api.controller.ManagedResponse;
import org.gatein.management.api.controller.ManagementController;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ReadResourceModel;
import org.picocontainer.ComponentAdapter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;

/**
 * The Class ExtensionGeneratorImpl.
 */
@Singleton
public class ExtensionGeneratorImpl implements ExtensionGenerator {
  
  /** The Constant WEB_XML_LOCATION. */
  private static final String WEB_XML_LOCATION = "WEB-INF/web.xml";
  
  /** The Constant WEB_XML_TEMPLATE_LOCATION. */
  private static final String WEB_XML_TEMPLATE_LOCATION = "generator/template/web.xml";
  
  /** The Constant CONFIGURATION_XML_LOCATION. */
  private static final String CONFIGURATION_XML_LOCATION = "WEB-INF/conf/configuration.xml";

  /** The Constant log. */
  private static final Log log = ExoLogger.getLogger(ExtensionGeneratorImpl.class);

  /** The management controller. */
  private ManagementController managementController = null;

  /** The handlers. */
  private List<ConfigurationHandler> handlers = new ArrayList<ConfigurationHandler>();

  /**
   * Instantiates a new extension generator impl.
   */
  public ExtensionGeneratorImpl() {
    // TODO /ecmadmin/action not used for PLF 4.3+
    //handlers.add(new ActionNodeTypeConfigurationHandler());
    handlers.add(new NodeTypeConfigurationHandler());
    handlers.add(new ApplicationRegistryConfigurationHandler());
    handlers.add(new MOPSiteConfigurationHandler(SiteType.PORTAL));
    handlers.add(new MOPSiteConfigurationHandler(SiteType.GROUP));
    handlers.add(new MOPSiteConfigurationHandler(SiteType.USER));
    handlers.add(new ScriptsConfigurationHandler());
    handlers.add(new DrivesConfigurationHandler());
    handlers.add(new JCRQueryConfigurationHandler());
    handlers.add(new MetadataTemplatesConfigurationHandler());
    handlers.add(new NodeTypeTemplatesConfigurationHandler());
    handlers.add(new SiteContentsConfigurationHandler());
    handlers.add(new CLVTemplatesConfigurationHandler());
    handlers.add(new TaxonomyConfigurationHandler());
    handlers.add(new SiteExplorerTemplatesConfigurationHandler());
    handlers.add(new SiteExplorerViewConfigurationHandler());
    handlers.add(new RESTServicesFromIDEConfigurationHandler());
    handlers.add(new GadgetsConfigurationHandler());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getPortalSiteNodes() {
    return getNodes(SITES_PORTAL_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getGroupSiteNodes() {
    return getNodes(SITES_GROUP_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getUserSiteNodes() {
    return getNodes(SITES_USER_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getSiteContentNodes() {
    return getNodes(CONTENT_SITES_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getApplicationCLVTemplatesNodes() {
    return getNodes(ECM_TEMPLATES_APPLICATION_CLV_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getDocumentTypeTemplatesNodes() {
    return getNodes(ECM_TEMPLATES_DOCUMENT_TYPE_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getMetadataTemplatesNodes() {
    return getNodes(ECM_TEMPLATES_METADATA_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getTaxonomyNodes() {
    return getNodes(ECM_TAXONOMY_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getQueryNodes() {
    return getNodes(ECM_QUERY_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getDriveNodes() {
    return getNodes(ECM_DRIVE_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getScriptNodes() {
    return getNodes(ECM_SCRIPT_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
  public List<Node> getActionNodeTypeNodes() {
    // TODO /ecmadmin/action not used for PLF 4.3+
    //return getNodes(ECM_ACTION_PATH);
    return Collections.emptyList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getNodeTypeNodes() {
    return getNodes(ECM_NODETYPE_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getRegistryNodes() {
    return getNodes(REGISTRY_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getViewTemplatesNodes() {
    return getNodes(ECM_VIEW_TEMPLATES_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getViewConfigurationNodes() {
    return getNodes(ECM_VIEW_CONFIGURATION_PATH);
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public List<Node> getIDEGroovyRestServices() {
    GroovyScript2RestLoader script2RestLoader = (GroovyScript2RestLoader) PortalContainer.getInstance().getComponentInstanceOfType(GroovyScript2RestLoader.class);
    RepositoryService repositoryService = (RepositoryService) PortalContainer.getInstance().getComponentInstanceOfType(RepositoryService.class);
    List<Node> nodes = new ArrayList<Node>();
    if (script2RestLoader == null) {
      return nodes;
    }

    Set<String> predefinedScripts = getPredefinedScripts();
    try {
      String repository = repositoryService.getCurrentRepository().getConfiguration().getName();
      String[] workspaces = repositoryService.getCurrentRepository().getWorkspaceNames();
      for (String workspace : workspaces) {
        Response response = script2RestLoader.list(repository, workspace, null);
        ScriptList scriptList = (ScriptList) response.getEntity();
        List<String> list = scriptList.getList();
        for (String scriptPath : list) {
          String scriptCompletePath = workspace + "::" + scriptPath;
          if (predefinedScripts.contains(scriptCompletePath)) {
            continue;
          }
          String scriptName = scriptPath.substring(scriptPath.lastIndexOf("/") + 1);
          Node node = new Node(scriptName, scriptPath, ExtensionGenerator.IDE_REST_PATH + scriptCompletePath);
          nodes.add(node);
        }
      }
    } catch (Exception e) {
      log.error("Error while getting the list of groovy REST services.", e);
    }
    return nodes;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public List<Node> getGadgets() {
    GadgetRegistryService gadgetRegistryService = (GadgetRegistryService) PortalContainer.getInstance().getComponentInstanceOfType(GadgetRegistryService.class);
    List<Node> nodes = new ArrayList<Node>();
    if (gadgetRegistryService == null) {
      return nodes;
    }

    try {
      List<Gadget> gadgets = gadgetRegistryService.getAllGadgets();
      for (Gadget gadget : gadgets) {
        if (!gadget.isLocal() && !gadget.getUrl().contains("jcr/repository")) {
          continue;
        }
        Node node = new Node(gadget.getTitle(), gadget.getDescription(), GADGET_PATH + gadget.getName());
        nodes.add(node);
      }
    } catch (Exception e) {
      log.error("Error while getting the list of groovy REST services.", e);
    }
    return nodes;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public InputStream generateExtensionZip(String extensionName, Set<String> selectedResources) throws Exception {
    File file = File.createTempFile("CustomExtension", ".zip");
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));

    // Put WAR file
    Utils.writeZipEnry(zos, "webapps/" + extensionName + ".war", extensionName, generateWARExtension(extensionName, selectedResources), false);
    // Put JAR file
    Utils.writeZipEnry(zos, "lib/" + extensionName + "-config.jar", extensionName, generateActiovationJar(extensionName), false);
    zos.close();
    return new ClosableFileInputStream(file);
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public InputStream generateExtensionMavenProject(String extensionName, Set<String> selectedResources) throws Exception {
    File zipFile = File.createTempFile("Maven-CustomExtension", ".zip");
    zipFile.deleteOnExit();
    ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));

    // Copy Zip file containing Maven Project Structure in Temp File
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("generator/template/maven.zip");
    Utils.copyZipEnries(new ZipInputStream(inputStream), zipOutputStream, extensionName, null);

    // Add Activation JAR Configuration File in Maven Project
    InputStream jarInputStream = generateActiovationJar(extensionName);
    Utils.copyZipEnries(new ZipInputStream(jarInputStream), zipOutputStream, extensionName, "config/src/main/resources");

    // Add Extension WAR files in Maven Project
    InputStream warInputStream = generateWARExtension(extensionName, selectedResources);
    Utils.copyZipEnries(new ZipInputStream(warInputStream), zipOutputStream, extensionName, "war/src/main/webapp");

    zipOutputStream.close();
    return new ClosableFileInputStream(zipFile);
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public InputStream generateWARExtension(String extensionName, Set<String> selectedResources) throws Exception {
    File file = File.createTempFile(extensionName, ".war");
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
    Vector<String> tempSelectedResources = new Vector<String>(selectedResources);

    Configuration configuration = new Configuration();
    for (ConfigurationHandler configurationHandler : handlers) {
      try {
        boolean extracted = configurationHandler.writeData(zos, extensionName, tempSelectedResources);
        if (extracted) {
          List<String> configurationPaths = configurationHandler.getConfigurationPaths();
          if (configurationPaths != null) {
            for (String path : configurationPaths) {
              path = path.replace("custom-extension", extensionName);
              configuration.addImport(path);
            }
          }
        }
      } catch (Exception e) {
        log.error("Error while handling resources for " + configurationHandler.getClass().getName(), e);
      }
    }

    // Write main configuration.xml file
    Utils.writeConfiguration(zos, CONFIGURATION_XML_LOCATION, extensionName, configuration);

    // Write web.xml file
    InputStream applicationXMLInputStream = getClass().getClassLoader().getResourceAsStream(WEB_XML_TEMPLATE_LOCATION);
    Utils.writeZipEnry(zos, WEB_XML_LOCATION, extensionName, applicationXMLInputStream, true);

    try {
      zos.flush();
      zos.close();
    } catch (IOException e) {
      log.error("Error while closing ZipOutputStream.", e);
    }

    return new ClosableFileInputStream(file);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> filterSelectedResources(Collection<String> selectedResources, String parentPath) {
    Set<String> filteredSelectedResources = new HashSet<String>();
    for (String resourcePath : selectedResources) {
      if (resourcePath.contains(parentPath)) {
        filteredSelectedResources.add(resourcePath);
      }
    }
    return filteredSelectedResources;
  }

  /**
   * Generate actiovation jar.
   *
   * @param extensionName the extension name
   * @return the input stream
   * @throws Exception the exception
   */
  private InputStream generateActiovationJar(String extensionName) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(out);

    InputStream xmlInputStream = getClass().getClassLoader().getResourceAsStream("generator/template/configuration.xml");
    Utils.writeZipEnry(zos, "conf/configuration.xml", extensionName, xmlInputStream, true);
    zos.close();
    return new ByteArrayInputStream(out.toByteArray());
  }

  /**
   * Gets the nodes.
   *
   * @param path the path
   * @return the nodes
   */
  private List<Node> getNodes(String path) {
    ManagedRequest request = ManagedRequest.Factory.create(OperationNames.READ_RESOURCE, PathAddress.pathAddress(path), ContentType.JSON);
    ManagedResponse response = getManagementController().execute(request);
    if (!response.getOutcome().isSuccess()) {
      log.error(response.getOutcome().getFailureDescription());
      throw new RuntimeException(response.getOutcome().getFailureDescription());
    }
    ReadResourceModel result = (ReadResourceModel) response.getResult();
    List<Node> children = new ArrayList<Node>(result.getChildren().size());
    if (result.getChildren() != null && !result.getChildren().isEmpty()) {
      for (String childName : result.getChildren()) {
        String description = result.getChildDescription(childName).getDescription();
        String childPath = path + "/" + childName;
        Node child = new Node(childName, description, childPath);
        children.add(child);
      }
      Collections.sort(children);
    } else {
      Node parent = new Node(path, result.getDescription(), path);
      children.add(parent);
    }
    return children;
  }

  /**
   * Gets the predefined scripts.
   *
   * @return the predefined scripts
   */
  private Set<String> getPredefinedScripts() {
    Set<String> predefinedScripts = new HashSet<String>();
    addPredefinedScriptsForComponent(predefinedScripts, GroovyScript2RestLoader.class.getName());
    addPredefinedScriptsForComponent(predefinedScripts, "org.exoplatform.platform.gadget.services.GroovyScript2RestLoader.GroovyScript2RestLoaderExt");
    return predefinedScripts;
  }

  /**
   * Adds the predefined scripts for component.
   *
   * @param predefinedScripts the predefined scripts
   * @param groovyScript2RestLoaderClassName the groovy script 2 rest loader class name
   */
  private void addPredefinedScriptsForComponent(Set<String> predefinedScripts, String groovyScript2RestLoaderClassName) {
    ConfigurationManager configurationManager = (ConfigurationManager) PortalContainer.getInstance().getComponentInstanceOfType(ConfigurationManager.class);
    Class<?> groovyScript2RestLoaderClass = null;
    try {
      ComponentAdapter componentAdapter = PortalContainer.getInstance().getComponentAdapterOfType(Class.forName(groovyScript2RestLoaderClassName));
      if (componentAdapter != null) {
        groovyScript2RestLoaderClass = (Class<?>) componentAdapter.getComponentKey();
      }
    } catch (ClassNotFoundException e) {
      // nothing to display, Platform gadgets are disabled
    } catch (Exception e) {
      log.warn("Operation Error - Compute Predefined Groovy scripts : '" + groovyScript2RestLoaderClassName + "' Component was not found.");
    }
    if (groovyScript2RestLoaderClass == null) {
      return;
    }

    ExternalComponentPlugins plugins = configurationManager.getConfiguration().getExternalComponentPlugins(groovyScript2RestLoaderClass.getName());
    if (plugins != null) {
      List<ComponentPlugin> componentPlugins = plugins.getComponentPlugins();
      if (componentPlugins != null && !componentPlugins.isEmpty()) {
        addPredefinedScripts(predefinedScripts, componentPlugins);
      }
    }
    Component component = configurationManager.getConfiguration().getComponent(groovyScript2RestLoaderClass.getName());
    if (component.getComponentPlugins() != null && !component.getComponentPlugins().isEmpty()) {
      addPredefinedScripts(predefinedScripts, component.getComponentPlugins());
    }
  }

  /**
   * Adds the predefined scripts.
   *
   * @param predefinedScripts the predefined scripts
   * @param componentPlugins the component plugins
   */
  private void addPredefinedScripts(Set<String> predefinedScripts, List<ComponentPlugin> componentPlugins) {
    if (componentPlugins == null || componentPlugins.isEmpty()) {
      return;
    }
    for (ComponentPlugin componentPlugin : componentPlugins) {
      if (componentPlugin.getInitParams() != null && componentPlugin.getType().equals(GroovyScript2RestLoaderPlugin.class.getName()) && componentPlugin.getInitParams().containsKey("workspace")) {
        String workspace = componentPlugin.getInitParams().getValueParam("workspace").getValue();
        Iterator<?> scriptsDefinition = componentPlugin.getInitParams().getPropertiesParamIterator();
        while (scriptsDefinition.hasNext()) {
          PropertiesParam propertiesParam = (PropertiesParam) scriptsDefinition.next();
          String path = componentPlugin.getInitParams().getValueParam("node").getValue() + "/" + propertiesParam.getName();
          predefinedScripts.add(workspace + "::" + path);
        }
      }
    }
  }

  /**
   * Gets the management controller.
   *
   * @return the management controller
   */
  private ManagementController getManagementController() {
    if (managementController == null) {
      managementController = (ManagementController) PortalContainer.getInstance().getComponentInstanceOfType(ManagementController.class);
    }
    return managementController;
  }

  /**
   * The Class ClosableFileInputStream.
   */
  public static class ClosableFileInputStream extends FileInputStream {
    
    /** The file. */
    File file;

    /**
     * Instantiates a new closable file input stream.
     *
     * @param file the file
     * @throws FileNotFoundException the file not found exception
     */
    public ClosableFileInputStream(File file) throws FileNotFoundException {
      super(file);
      this.file = file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
      super.close();
      try {
        FileUtils.forceDelete(file);
      } catch (Exception e) {
        log.warn("Cannot delete file: " + file.getName());
        file.deleteOnExit();
      }
    }
  }
}