package org.exoplatform.extension.generator.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.inject.Singleton;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.extension.generator.service.api.ConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Node;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.extension.generator.service.handler.ActionNodeTypeConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.ApplicationRegistryConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.CLVTemplatesConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.DrivesConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.JCRQueryConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.MOPSiteConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.MetadataTemplatesConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.NodeTypeConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.NodeTypeTemplatesConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.ScriptsConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.SearchTemplatesConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.SiteContentsConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.SiteExplorerTemplatesConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.SiteExplorerViewConfigurationHandler;
import org.exoplatform.extension.generator.service.handler.TaxonomyConfigurationHandler;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.ContentType;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.controller.ManagedRequest;
import org.gatein.management.api.controller.ManagedResponse;
import org.gatein.management.api.controller.ManagementController;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ReadResourceModel;

@Singleton
public class ExtensionGeneratorImpl implements ExtensionGenerator {
  private static final String WEB_XML_LOCATION = "WEB-INF/web.xml";
  private static final String WEB_XML_TEMPLATE_LOCATION = "generator/template/web.xml";
  private static final String CONFIGURATION_XML_LOCATION = "WEB-INF/conf/configuration.xml";

  private Log log = ExoLogger.getLogger(this.getClass());

  private ManagementController managementController = null;

  private List<ConfigurationHandler> handlers = new ArrayList<ConfigurationHandler>();

  public ExtensionGeneratorImpl() {
    handlers.add(new ActionNodeTypeConfigurationHandler());
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
    handlers.add(new SearchTemplatesConfigurationHandler());
    handlers.add(new CLVTemplatesConfigurationHandler());
    handlers.add(new TaxonomyConfigurationHandler());
    handlers.add(new SiteExplorerTemplatesConfigurationHandler());
    handlers.add(new SiteExplorerViewConfigurationHandler());
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
  public List<Node> getApplicationSearchTemplatesNodes() {
    return getNodes(ECM_TEMPLATES_APPLICATION_SEARCH_PATH);
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
  public List<Node> getActionNodeTypeNodes() {
    return getNodes(ECM_ACTION_PATH);
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
  public InputStream generateExtensionEAR(String extensionName, Set<String> selectedResources) throws Exception {
    File file = generateExtensionEARFile(extensionName, selectedResources);
    return new FileInputStream(file);
  }

  private File generateExtensionEARFile(String extensionName, Set<String> selectedResources) throws Exception {
    File file = File.createTempFile("CustomExtension", ".ear");
    file.deleteOnExit();
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
    // Put WAR file
    Utils.writeZipEnry(zos, extensionName + ".war", extensionName, generateWARExtension(extensionName, selectedResources), false);
    // Put JAR file
    Utils.writeZipEnry(zos, "lib/" + extensionName + "-config.jar", extensionName, generateActiovationJar(extensionName), false);
    // Put application.xml
    InputStream applicationXMLInputStream = getClass().getClassLoader().getResourceAsStream("generator/template/application.xml");
    Utils.writeZipEnry(zos, "META-INF/application.xml", extensionName, applicationXMLInputStream, true);
    zos.close();
    return file;
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
    return new FileInputStream(zipFile);
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public InputStream generateWARExtension(String extensionName, Set<String> selectedResources) throws Exception {
    File file = File.createTempFile(extensionName, ".war");
    file.deleteOnExit();
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
    Vector<String> tempSelectedResources = new Vector<String>(selectedResources);

    Configuration configuration = new Configuration();
    for (ConfigurationHandler configurationHandler : handlers) {
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

    return new FileInputStream(file);
  }

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

  private InputStream generateActiovationJar(String extensionName) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(out);

    InputStream xmlInputStream = getClass().getClassLoader().getResourceAsStream("generator/template/configuration.xml");
    Utils.writeZipEnry(zos, "conf/configuration.xml", extensionName, xmlInputStream, true);
    zos.close();
    return new ByteArrayInputStream(out.toByteArray());
  }

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

  private ManagementController getManagementController() {
    if (managementController == null) {
      managementController = (ManagementController) PortalContainer.getInstance().getComponentInstanceOfType(ManagementController.class);
    }
    return managementController;
  }

}