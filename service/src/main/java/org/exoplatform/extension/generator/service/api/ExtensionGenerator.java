package org.exoplatform.extension.generator.service.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ExtensionGenerator {

  public static final String SITES_PORTAL_PATH = "/site/portalsites";
  public static final String SITES_GROUP_PATH = "/site/groupsites";
  public static final String SITES_USER_PATH = "/site/usersites";
  public static final String CONTENT_SITES_PATH = "/content/sites";
  public static final String ECM_TEMPLATES_APPLICATION_CLV_PATH = "/ecmadmin/templates/applications/content-list-viewer";
  public static final String ECM_TEMPLATES_APPLICATION_SEARCH_PATH = "/ecmadmin/templates/applications/search";
  public static final String ECM_TEMPLATES_DOCUMENT_TYPE_PATH = "/ecmadmin/templates/nodetypes";
  public static final String ECM_TEMPLATES_METADATA_PATH = "/ecmadmin/templates/metadata";
  public static final String ECM_TAXONOMY_PATH = "/ecmadmin/taxonomy";
  public static final String ECM_QUERY_PATH = "/ecmadmin/queries";
  public static final String ECM_DRIVE_PATH = "/ecmadmin/drive";
  public static final String ECM_SCRIPT_PATH = "/ecmadmin/script";
  public static final String ECM_ACTION_PATH = "/ecmadmin/action";
  public static final String ECM_NODETYPE_PATH = "/ecmadmin/nodetype";
  public static final String ECM_VIEW_CONFIGURATION_PATH = "/ecmadmin/view/configuration";
  public static final String ECM_VIEW_TEMPLATES_PATH = "/ecmadmin/view/templates";
  public static final String REGISTRY_PATH = "/registry";

  /**
   * Returns the list of sub resources of MOP of type portalsites computed from
   * GateIN Management SPI
   * 
   * @return list of portal sites managed paths.
   */
  List<Node> getPortalSiteNodes();

  /**
   * Returns the list of sub resources of MOP of type groupsites computed from
   * GateIN Management SPI
   * 
   * @return list of portal sites managed paths.
   */
  List<Node> getGroupSiteNodes();

  /**
   * Returns the list of sub resources of MOP of type usersites computed from
   * GateIN Management SPI
   * 
   * @return list of portal sites managed paths.
   */
  List<Node> getUserSiteNodes();

  /**
   * Returns the list of sub resources of /content/sites managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of site contents managed paths.
   */
  List<Node> getSiteContentNodes();

  /**
   * Returns the list of sub resources of
   * /ecmadmin/templates/applications/content-list-viewer managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of CLV templates managed paths.
   */
  List<Node> getApplicationCLVTemplatesNodes();

  /**
   * Returns the list of sub resources of
   * /ecmadmin/templates/applications/search managed resources computed from
   * GateIN Management SPI
   * 
   * @return list of Search Portlet Templates managed paths.
   */
  List<Node> getApplicationSearchTemplatesNodes();

  /**
   * Returns the list of sub resources of
   * /ecmadmin/templates/applications/nodetype managed resources computed from
   * GateIN Management SPI
   * 
   * @return list of DocumentType templates managed paths.
   */
  List<Node> getDocumentTypeTemplatesNodes();

  /**
   * Returns the list of sub resources of
   * /ecmadmin/templates/applications/metadata managed resources computed from
   * GateIN Management SPI
   * 
   * @return list of metadata templates managed paths.
   */
  List<Node> getMetadataTemplatesNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/taxonomy managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of taxonomy managed paths.
   */
  List<Node> getTaxonomyNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/queries managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of JCR Query managed paths.
   */
  List<Node> getQueryNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/drive managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of ECMS Drives managed paths.
   */
  List<Node> getDriveNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/script managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of ECMS Script managed paths.
   */
  List<Node> getScriptNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/action managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of action nodetype managed paths.
   */
  List<Node> getActionNodeTypeNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/nodetype managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of JCR Nodetype managed paths.
   */
  List<Node> getNodeTypeNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/taxonomy managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of application registry categories managed paths.
   */
  List<Node> getRegistryNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/view/templates managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of ECMS View Templates managed paths.
   */
  List<Node> getViewTemplatesNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/view managed resources
   * computed from GateIN Management SPI
   * 
   * @return list of ECMS View Configuration managed paths.
   */
  List<Node> getViewConfigurationNodes();

  /**
   * Generates the WAR Extension by including seleted managed paths to export
   * @param extensionName 
   * 
   * @param selectedResources
   * @return InputStream pointing to a ZipFile
   * @throws IOException
   */
  InputStream generateWARExtension(String extensionName, Set<String> selectedResources) throws Exception;

  /**
   * Generates EAR containing WAR Extension and Activation JAR by including
   * seleted managed paths to export
   * @param extensionName 
   * 
   * @param selectedResources
   * @return InputStream pointing to a ZipFile
   * @throws IOException
   */
  InputStream generateExtensionEAR(String extensionName, Set<String> selectedResources) throws Exception;

  /**
   * Generates Maven Project containing modules for WAR Extension and Activation
   * JAR. This operation is done by including seleted managed paths to export.
   * @param extensionName 
   * 
   * @param selectedResources
   *          Selected Managed Resources Paths.
   * @return InputStream pointing to a ZipFile
   * @throws IOException
   */
  InputStream generateExtensionMavenProject(String extensionName, Set<String> selectedResources) throws Exception;

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
  Set<String> filterSelectedResources(Collection<String> selectedResources, String parentPath);

}
