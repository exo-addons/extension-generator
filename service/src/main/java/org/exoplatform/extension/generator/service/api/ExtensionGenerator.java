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

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The Interface ExtensionGenerator.
 */
public interface ExtensionGenerator {

  /** The Constant SITES_PORTAL_PATH. */
  public static final String SITES_PORTAL_PATH = "/site/portalsites";
  
  /** The Constant SITES_GROUP_PATH. */
  public static final String SITES_GROUP_PATH = "/site/groupsites";
  
  /** The Constant SITES_USER_PATH. */
  public static final String SITES_USER_PATH = "/site/usersites";
  
  /** The Constant CONTENT_SITES_PATH. */
  public static final String CONTENT_SITES_PATH = "/content/sites";
  
  /** The Constant ECM_TEMPLATES_APPLICATION_CLV_PATH. */
  public static final String ECM_TEMPLATES_APPLICATION_CLV_PATH = "/ecmadmin/templates/applications/content-list-viewer";
  
  /** The Constant ECM_TEMPLATES_DOCUMENT_TYPE_PATH. */
  public static final String ECM_TEMPLATES_DOCUMENT_TYPE_PATH = "/ecmadmin/templates/nodetypes";
  
  /** The Constant ECM_TEMPLATES_METADATA_PATH. */
  public static final String ECM_TEMPLATES_METADATA_PATH = "/ecmadmin/templates/metadata";
  
  /** The Constant ECM_TAXONOMY_PATH. */
  public static final String ECM_TAXONOMY_PATH = "/ecmadmin/taxonomy";
  
  /** The Constant ECM_QUERY_PATH. */
  public static final String ECM_QUERY_PATH = "/ecmadmin/queries";
  
  /** The Constant ECM_DRIVE_PATH. */
  public static final String ECM_DRIVE_PATH = "/ecmadmin/drive";
  
  /** The Constant ECM_SCRIPT_PATH. */
  public static final String ECM_SCRIPT_PATH = "/ecmadmin/script";
  
  /** The Constant ECM_ACTION_PATH. */
  public static final String ECM_ACTION_PATH = "/ecmadmin/action";
  
  /** The Constant ECM_NODETYPE_PATH. */
  public static final String ECM_NODETYPE_PATH = "/ecmadmin/nodetype";
  
  /** The Constant ECM_VIEW_CONFIGURATION_PATH. */
  public static final String ECM_VIEW_CONFIGURATION_PATH = "/ecmadmin/view/configuration";
  
  /** The Constant ECM_VIEW_TEMPLATES_PATH. */
  public static final String ECM_VIEW_TEMPLATES_PATH = "/ecmadmin/view/templates";
  
  /** The Constant REGISTRY_PATH. */
  public static final String REGISTRY_PATH = "/registry";
  
  /** The Constant IDE_REST_PATH. */
  public static final String IDE_REST_PATH = "/ide/rest::";
  
  /** The Constant GADGET_PATH. */
  public static final String GADGET_PATH = "/gadget::";

  /**
   * Returns the list of sub resources of MOP of type portalsites computed from
   * GateIN Management SPI.
   *
   * @return list of portal sites managed paths.
   */
  List<Node> getPortalSiteNodes();

  /**
   * Returns the list of sub resources of MOP of type groupsites computed from
   * GateIN Management SPI.
   *
   * @return list of portal sites managed paths.
   */
  List<Node> getGroupSiteNodes();

  /**
   * Returns the list of sub resources of MOP of type usersites computed from
   * GateIN Management SPI.
   *
   * @return list of portal sites managed paths.
   */
  List<Node> getUserSiteNodes();

  /**
   * Returns the list of sub resources of /content/sites managed resources
   * computed from GateIN Management SPI.
   *
   * @return list of site contents managed paths.
   */
  List<Node> getSiteContentNodes();

  /**
   * Returns the list of sub resources of
   * /ecmadmin/templates/applications/content-list-viewer managed resources
   * computed from GateIN Management SPI.
   *
   * @return list of CLV templates managed paths.
   */
  List<Node> getApplicationCLVTemplatesNodes();

  /**
   * Returns the list of sub resources of
   * /ecmadmin/templates/applications/nodetype managed resources computed from
   * GateIN Management SPI.
   *
   * @return list of DocumentType templates managed paths.
   */
  List<Node> getDocumentTypeTemplatesNodes();

  /**
   * Returns the list of sub resources of
   * /ecmadmin/templates/applications/metadata managed resources computed from
   * GateIN Management SPI.
   *
   * @return list of metadata templates managed paths.
   */
  List<Node> getMetadataTemplatesNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/taxonomy managed resources
   * computed from GateIN Management SPI.
   *
   * @return list of taxonomy managed paths.
   */
  List<Node> getTaxonomyNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/queries managed resources
   * computed from GateIN Management SPI.
   *
   * @return list of JCR Query managed paths.
   */
  List<Node> getQueryNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/drive managed resources
   * computed from GateIN Management SPI.
   *
   * @return list of ECMS Drives managed paths.
   */
  List<Node> getDriveNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/script managed resources
   * computed from GateIN Management SPI.
   *
   * @return list of ECMS Script managed paths.
   */
  List<Node> getScriptNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/action managed resources
   * computed from GateIN Management SPI.
   *
   * @return list of action nodetype managed paths.
   */
  @Deprecated
  // TODO /ecmadmin/action not used for PLF 4.3+
  List<Node> getActionNodeTypeNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/nodetype managed resources
   * computed from GateIN Management SPI.
   *
   * @return list of JCR Nodetype managed paths.
   */
  List<Node> getNodeTypeNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/taxonomy managed resources
   * computed from GateIN Management SPI.
   *
   * @return list of application registry categories managed paths.
   */
  List<Node> getRegistryNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/view/templates managed resources
   * computed from GateIN Management SPI.
   *
   * @return list of ECMS View Templates managed paths.
   */
  List<Node> getViewTemplatesNodes();

  /**
   * Returns the list of sub resources of /ecmadmin/view managed resources
   * computed from GateIN Management SPI.
   *
   * @return list of ECMS View Configuration managed paths.
   */
  List<Node> getViewConfigurationNodes();

  /**
   * Returns the list of groovy REST Services defined by IDE.
   *
   * @return list of ECMS View Configuration managed paths.
   */
  List<Node> getIDEGroovyRestServices();

  /**
   * Returns the list of groovy REST Services defined by IDE.
   *
   * @return list of ECMS View Configuration managed paths.
   */
  List<Node> getGadgets();

  /**
   * Generates the WAR Extension by including seleted managed paths to export.
   *
   * @param extensionName the extension name
   * @param selectedResources the selected resources
   * @return InputStream pointing to a ZipFile
   * @throws Exception the exception
   */
  InputStream generateWARExtension(String extensionName, Set<String> selectedResources) throws Exception;

  /**
   * Generates ZIP file containing WAR Extension and Activation JAR by including
   * seleted managed resources to export.
   *
   * @param extensionName the extension name
   * @param selectedResources the selected resources
   * @return InputStream pointing to a ZipFile
   * @throws Exception the exception
   */
  InputStream generateExtensionZip(String extensionName, Set<String> selectedResources) throws Exception;

  /**
   * Generates Maven Project containing modules for WAR Extension and Activation
   * JAR. This operation is done by including seleted managed paths to export.
   *
   * @param extensionName the extension name
   * @param selectedResources Selected Managed Resources Paths.
   * @return InputStream pointing to a ZipFile
   * @throws Exception the exception
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
