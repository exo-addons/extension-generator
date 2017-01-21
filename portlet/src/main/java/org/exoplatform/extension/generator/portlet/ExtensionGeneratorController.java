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
package org.exoplatform.extension.generator.portlet;

import juzu.Path;
import juzu.Resource;
import juzu.Response;
import juzu.SessionScoped;
import juzu.View;
import juzu.template.Template;

import org.exoplatform.commons.juzu.ajax.Ajax;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Node;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

/**
 * The Class ExtensionGeneratorController.
 */
@SessionScoped
public class ExtensionGeneratorController {
  
  /** The log. */
  private static Log log = ExoLogger.getLogger(ExtensionGeneratorController.class);

  /** The extension generator service. */
  @Inject
  ExtensionGenerator extensionGeneratorService;

  /** The form. */
  @Inject
  @Path("form.gtmpl")
  Template form;

  /** The index. */
  @Inject
  @Path("index.gtmpl")
  Template index;

  /** The selected resources. */
  Set<String> selectedResources = Collections.synchronizedSet(new HashSet<String>());
  
  /** The resources. */
  Map<String, List<Node>> resources = new HashMap<String, List<Node>>();

  /** The parameters. */
  static Map<String, Object> parameters = new HashMap<String, Object>();
  static {
    // PATHS
    parameters.put("portalSitePath", ExtensionGenerator.SITES_PORTAL_PATH);
    parameters.put("groupSitePath", ExtensionGenerator.SITES_GROUP_PATH);
    parameters.put("userSitePath", ExtensionGenerator.SITES_USER_PATH);
    parameters.put("siteContentPath", ExtensionGenerator.CONTENT_SITES_PATH);
    parameters.put("applicationCLVTemplatesPath", ExtensionGenerator.ECM_TEMPLATES_APPLICATION_CLV_PATH);
    parameters.put("documentTypeTemplatesPath", ExtensionGenerator.ECM_TEMPLATES_DOCUMENT_TYPE_PATH);
    parameters.put("metadataTemplatesPath", ExtensionGenerator.ECM_TEMPLATES_METADATA_PATH);
    parameters.put("taxonomyPath", ExtensionGenerator.ECM_TAXONOMY_PATH);
    parameters.put("queryPath", ExtensionGenerator.ECM_QUERY_PATH);
    parameters.put("drivePath", ExtensionGenerator.ECM_DRIVE_PATH);
    parameters.put("scriptPath", ExtensionGenerator.ECM_SCRIPT_PATH);
    // TODO /ecmadmin/action not used for PLF 4.3+
    //parameters.put("actionNodeTypePath", ExtensionGenerator.ECM_ACTION_PATH);
    parameters.put("nodeTypePath", ExtensionGenerator.ECM_NODETYPE_PATH);
    parameters.put("registryPath", ExtensionGenerator.REGISTRY_PATH);
    parameters.put("viewTemplatePath", ExtensionGenerator.ECM_VIEW_TEMPLATES_PATH);
    parameters.put("viewConfigurationPath", ExtensionGenerator.ECM_VIEW_CONFIGURATION_PATH);
    parameters.put("ideGroovyRestServicesPath", ExtensionGenerator.IDE_REST_PATH);
    parameters.put("gadgetPath", ExtensionGenerator.GADGET_PATH);
  }

  /**
   * Index.
   *
   * @return the response. content
   */
  @View
  public Response.Content index() {
    selectedResources.clear();
    // NODES
    resources.put(ExtensionGenerator.SITES_PORTAL_PATH, extensionGeneratorService.getPortalSiteNodes());

    List<Node> groupSites = extensionGeneratorService.getGroupSiteNodes();
    // delete Spaces Group Sites Layout
    Iterator<Node> groupSitesIterator = groupSites.iterator();
    while (groupSitesIterator.hasNext()) {
      Node groupSite = groupSitesIterator.next();
      if(groupSite.getPath().contains("/spaces/")) {
        groupSitesIterator.remove();
      }
    }
    resources.put(ExtensionGenerator.SITES_GROUP_PATH, groupSites);

    resources.put(ExtensionGenerator.SITES_USER_PATH, extensionGeneratorService.getUserSiteNodes());
    resources.put(ExtensionGenerator.CONTENT_SITES_PATH, extensionGeneratorService.getSiteContentNodes());
    resources.put(ExtensionGenerator.ECM_TEMPLATES_APPLICATION_CLV_PATH, extensionGeneratorService.getApplicationCLVTemplatesNodes());
    resources.put(ExtensionGenerator.ECM_TEMPLATES_DOCUMENT_TYPE_PATH, extensionGeneratorService.getDocumentTypeTemplatesNodes());
    resources.put(ExtensionGenerator.ECM_TEMPLATES_METADATA_PATH, extensionGeneratorService.getMetadataTemplatesNodes());
    resources.put(ExtensionGenerator.ECM_TAXONOMY_PATH, extensionGeneratorService.getTaxonomyNodes());
    resources.put(ExtensionGenerator.ECM_QUERY_PATH, extensionGeneratorService.getQueryNodes());
    resources.put(ExtensionGenerator.ECM_DRIVE_PATH, extensionGeneratorService.getDriveNodes());
    resources.put(ExtensionGenerator.ECM_SCRIPT_PATH, extensionGeneratorService.getScriptNodes());
    // TODO /ecmadmin/action not used for PLF 4.3+
    //resources.put(ExtensionGenerator.ECM_ACTION_PATH, extensionGeneratorService.getActionNodeTypeNodes());
    resources.put(ExtensionGenerator.ECM_NODETYPE_PATH, extensionGeneratorService.getNodeTypeNodes());
    resources.put(ExtensionGenerator.REGISTRY_PATH, extensionGeneratorService.getRegistryNodes());
    resources.put(ExtensionGenerator.ECM_VIEW_TEMPLATES_PATH, extensionGeneratorService.getViewTemplatesNodes());
    resources.put(ExtensionGenerator.ECM_VIEW_CONFIGURATION_PATH, extensionGeneratorService.getViewConfigurationNodes());
    resources.put(ExtensionGenerator.IDE_REST_PATH, extensionGeneratorService.getIDEGroovyRestServices());
    resources.put(ExtensionGenerator.GADGET_PATH, extensionGeneratorService.getGadgets());

    // Set Nodes in parameters
    parameters.put("portalSiteNodes", resources.get(ExtensionGenerator.SITES_PORTAL_PATH));
    parameters.put("groupSiteNodes", resources.get(ExtensionGenerator.SITES_GROUP_PATH));
    parameters.put("userSiteNodes", resources.get(ExtensionGenerator.SITES_USER_PATH));
    parameters.put("siteContentNodes", resources.get(ExtensionGenerator.CONTENT_SITES_PATH));
    parameters.put("applicationCLVTemplatesNodes", resources.get(ExtensionGenerator.ECM_TEMPLATES_APPLICATION_CLV_PATH));
    parameters.put("documentTypeTemplatesNodes", resources.get(ExtensionGenerator.ECM_TEMPLATES_DOCUMENT_TYPE_PATH));
    parameters.put("metadataTemplatesNodes", resources.get(ExtensionGenerator.ECM_TEMPLATES_METADATA_PATH));
    parameters.put("taxonomyNodes", resources.get(ExtensionGenerator.ECM_TAXONOMY_PATH));
    parameters.put("queryNodes", resources.get(ExtensionGenerator.ECM_QUERY_PATH));
    parameters.put("driveNodes", resources.get(ExtensionGenerator.ECM_DRIVE_PATH));
    parameters.put("scriptNodes", resources.get(ExtensionGenerator.ECM_SCRIPT_PATH));
    // TODO /ecmadmin/action not used for PLF 4.3+
    //parameters.put("actionNodeTypeNodes", resources.get(ExtensionGenerator.ECM_ACTION_PATH));
    parameters.put("nodeTypeNodes", resources.get(ExtensionGenerator.ECM_NODETYPE_PATH));
    parameters.put("registryNodes", resources.get(ExtensionGenerator.REGISTRY_PATH));
    parameters.put("viewTemplateNodes", resources.get(ExtensionGenerator.ECM_VIEW_TEMPLATES_PATH));
    parameters.put("viewConfigurationNodes", resources.get(ExtensionGenerator.ECM_VIEW_CONFIGURATION_PATH));
    parameters.put("ideGroovyRestServicesNodes", resources.get(ExtensionGenerator.IDE_REST_PATH));
    parameters.put("gadgetNodes", resources.get(ExtensionGenerator.GADGET_PATH));

    parameters.put("selectedResources", selectedResources);

    parameters.put("portalSiteSelectedNodes", getSelectedResources(ExtensionGenerator.SITES_PORTAL_PATH));
    parameters.put("groupSiteSelectedNodes", getSelectedResources(ExtensionGenerator.SITES_GROUP_PATH));
    parameters.put("userSiteSelectedNodes", getSelectedResources(ExtensionGenerator.SITES_USER_PATH));
    parameters.put("siteContentSelectedNodes", getSelectedResources(ExtensionGenerator.CONTENT_SITES_PATH));
    parameters.put("applicationCLVTemplatesSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_TEMPLATES_APPLICATION_CLV_PATH));
    parameters.put("documentTypeTemplatesSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_TEMPLATES_DOCUMENT_TYPE_PATH));
    parameters.put("metadataTemplatesSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_TEMPLATES_METADATA_PATH));
    parameters.put("taxonomySelectedNodes", getSelectedResources(ExtensionGenerator.ECM_TAXONOMY_PATH));
    parameters.put("querySelectedNodes", getSelectedResources(ExtensionGenerator.ECM_QUERY_PATH));
    parameters.put("driveSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_DRIVE_PATH));
    parameters.put("scriptSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_SCRIPT_PATH));
    // TODO /ecmadmin/action not used for PLF 4.3+
    //parameters.put("actionNodeTypeSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_ACTION_PATH));
    parameters.put("nodeTypeSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_NODETYPE_PATH));
    parameters.put("registrySelectedNodes", getSelectedResources(ExtensionGenerator.REGISTRY_PATH));
    parameters.put("viewTemplateSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_VIEW_TEMPLATES_PATH));
    parameters.put("viewConfigurationSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_VIEW_CONFIGURATION_PATH));
    parameters.put("ideGroovyRestServicesSelectedNodes", getSelectedResources(ExtensionGenerator.IDE_REST_PATH));
    parameters.put("gadgetSelectedNodes", getSelectedResources(ExtensionGenerator.GADGET_PATH));

    return index.ok(parameters);
  }

  /**
   * Select resources.
   *
   * @param path the path
   * @param checked the checked
   * @return the response. content
   */
  @Ajax
  @Resource
  public synchronized Response.Content selectResources(String path, String checked) {
    if (checked != null && path != null && !checked.isEmpty() && !path.isEmpty()) {
      if (checked.equals("true")) {
        if (resources.containsKey(path)) {
          List<Node> children = resources.get(path);
          for (Node node : children) {
            selectedResources.add(node.getPath());
          }
        } else {
          selectedResources.add(path);
        }
      } else {
        if (resources.containsKey(path)) {
          List<Node> children = resources.get(path);
          for (Node node : children) {
            selectedResources.remove(node.getPath());
          }
        } else {
          selectedResources.remove(path);
        }
      }
    }

    parameters.put("portalSiteSelectedNodes", getSelectedResources(ExtensionGenerator.SITES_PORTAL_PATH));
    parameters.put("groupSiteSelectedNodes", getSelectedResources(ExtensionGenerator.SITES_GROUP_PATH));
    parameters.put("userSiteSelectedNodes", getSelectedResources(ExtensionGenerator.SITES_USER_PATH));
    parameters.put("siteContentSelectedNodes", getSelectedResources(ExtensionGenerator.CONTENT_SITES_PATH));
    parameters.put("applicationCLVTemplatesSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_TEMPLATES_APPLICATION_CLV_PATH));
    parameters.put("documentTypeTemplatesSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_TEMPLATES_DOCUMENT_TYPE_PATH));
    parameters.put("metadataTemplatesSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_TEMPLATES_METADATA_PATH));
    parameters.put("taxonomySelectedNodes", getSelectedResources(ExtensionGenerator.ECM_TAXONOMY_PATH));
    parameters.put("querySelectedNodes", getSelectedResources(ExtensionGenerator.ECM_QUERY_PATH));
    parameters.put("driveSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_DRIVE_PATH));
    parameters.put("scriptSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_SCRIPT_PATH));
    // TODO /ecmadmin/action not used for PLF 4.3+
    //parameters.put("actionNodeTypeSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_ACTION_PATH));
    parameters.put("nodeTypeSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_NODETYPE_PATH));
    parameters.put("registrySelectedNodes", getSelectedResources(ExtensionGenerator.REGISTRY_PATH));
    parameters.put("viewTemplateSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_VIEW_TEMPLATES_PATH));
    parameters.put("viewConfigurationSelectedNodes", getSelectedResources(ExtensionGenerator.ECM_VIEW_CONFIGURATION_PATH));
    parameters.put("ideGroovyRestServicesSelectedNodes", getSelectedResources(ExtensionGenerator.IDE_REST_PATH));
    parameters.put("gadgetSelectedNodes", getSelectedResources(ExtensionGenerator.GADGET_PATH));

    return form.ok(parameters);
  }

  /**
   * Export extension.
   *
   * @param archiveType the archive type
   * @param extensionName the extension name
   * @return the response. content
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Ajax
  @Resource
  public Response.Content exportExtension(String archiveType, String extensionName) throws IOException {
    try {
      InputStream inputStream = null;
      if (archiveType.equals("maven")) {
        inputStream = extensionGeneratorService.generateExtensionMavenProject(extensionName, selectedResources);
      } else if (archiveType.equals("package")) {
        inputStream = extensionGeneratorService.generateExtensionZip(extensionName, selectedResources);
      } else {
        throw new IllegalArgumentException("Wrong ArchiveType:" + archiveType + ", for extension '" + extensionName + "'");
      }
      return Response.ok(inputStream).withMimeType("application/zip").withHeader("Content-Disposition", "filename=\"" + extensionName + ".zip\"");
    } catch (Exception e) {
      log.error("Error while generating Archive file, ", e);
      return Response.content(500, "Error occured while importing resource. See full stack trace in log file");
    }
  }

  /**
   * Gets the selected resources.
   *
   * @param parentPath the parent path
   * @return the selected resources
   */
  private Set<String> getSelectedResources(String parentPath) {
    Set<String> resources = extensionGeneratorService.filterSelectedResources(selectedResources, parentPath);
    Set<String> selectedResources = new HashSet<String>();
    for (String resource : resources) {
      resource = resource.replace(parentPath, "");
      if (resource.startsWith("/")) {
        resource = resource.substring(1);
      }
      selectedResources.add(resource);
    }
    return selectedResources;
  }

}
