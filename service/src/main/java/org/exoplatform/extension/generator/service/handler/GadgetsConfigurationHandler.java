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

import org.exoplatform.application.gadget.Gadget;
import org.exoplatform.application.gadget.GadgetRegistryService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

/**
 * The Class GadgetsConfigurationHandler.
 */
public class GadgetsConfigurationHandler extends AbstractConfigurationHandler {
  
  /** The Constant GADGETS_LOCATION. */
  protected static final String GADGETS_LOCATION = "gadgets";
  
  /** The Constant GADGETS_CONFIGURATION_PATH. */
  private static final String GADGETS_CONFIGURATION_PATH = "WEB-INF/gadget.xml";

  /** The repository service. */
  private RepositoryService repositoryService;
  
  /** The gadget registry service. */
  private GadgetRegistryService gadgetRegistryService;

  /** The log. */
  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean writeData(ZipOutputStream zos, String extensionName, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.GADGET_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    gadgetRegistryService = (GadgetRegistryService) PortalContainer.getInstance().getComponentInstanceOfType(GadgetRegistryService.class);
    repositoryService = (RepositoryService) PortalContainer.getInstance().getComponentInstanceOfType(RepositoryService.class);

    try {
      String repository = repositoryService.getCurrentRepository().getConfiguration().getName();

      StringBuilder gadgetsConfiguration = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
      gadgetsConfiguration.append("<gadgets\r\n\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n");
      gadgetsConfiguration.append("\txsi:schemaLocation=\"http://www.gatein.org/xml/ns/gatein_objects_1_0 http://www.gatein.org/xml/ns/gadgets_1_0\"\r\n");
      gadgetsConfiguration.append("\txmlns=\"http://www.gatein.org/xml/ns/gadgets_1_0\">\r\n");
      for (String selectedResoucePath : filteredSelectedResources) {
        selectedResoucePath = selectedResoucePath.replace(ExtensionGenerator.GADGET_PATH, "");
        Gadget gadget = gadgetRegistryService.getGadget(selectedResoucePath);
        String url = gadget.getUrl();
        String path = url.substring(url.indexOf(repository) + repository.length() + 1);
        String workspace = path.substring(0, path.indexOf("/"));
        path = path.replace(workspace, "");

        Session session = getSession(workspace);
        if (!session.itemExists(path)) {
          getLogger().warn("Cannot find Gadget '" + gadget.getName() + "' in ths location: " + gadget.getUrl());
          continue;
        }
        Node gadgetXMLNode = (Node) session.getItem(path);
        if (!gadgetXMLNode.isNodeType("nt:file")) {
          getLogger().warn("Cannot handle Gadget '" + gadget.getName() + "'. It's not a file.");
          continue;
        }
        Node gadgetParentNode = gadgetXMLNode.getParent();
        if (gadgetParentNode.getPath().equals("/")) {
          gadgetsConfiguration.append("\r\n\r\n\t<!-- Gadget '" + gadget.getName() + "' is not exported. Please add it in a separate folder, not under '/' (root folder of the workspace). -->\r\n\r\n");
          getLogger().warn("Cannot export Gadget '" + gadget.getName() + "'. Each gadget have to be in a separate folder and not under root folder of the workspace.");
          continue;
        }
        if (!gadgetParentNode.isNodeType("nt:folder")) {
          getLogger().warn("Cannot export Gadget '" + gadget.getName() + "'. Its parent node is not a folder.");
          continue;
        }

        String parentPath = gadgetParentNode.getParent().getPath();

        writeFileNode(gadgetParentNode, parentPath, zos, extensionName, gadget.getName());

        String xmlPath = GADGETS_LOCATION + "/" + gadgetXMLNode.getPath().replaceFirst(parentPath, gadget.getName());
        xmlPath = xmlPath.replaceAll("//", "/");

        gadgetsConfiguration.append(" <gadget name=\"").append(gadget.getName()).append("\">");
        gadgetsConfiguration.append("\r\n   <path>/").append(xmlPath).append("</path>\r\n");
        gadgetsConfiguration.append(" </gadget>\r\n");
      }
      gadgetsConfiguration.append("</gadgets>");
      Utils.writeZipEnry(zos, GADGETS_CONFIGURATION_PATH, extensionName, gadgetsConfiguration.toString(), false);
      return true;
    } catch (Exception e) {
      getLogger().error(e);
      return false;
    }
  }

  /**
   * Write file node.
   *
   * @param gadgetFileNode the gadget file node
   * @param parentPath the parent path
   * @param zos the zos
   * @param extensionName the extension name
   * @param gadgetName the gadget name
   * @throws Exception the exception
   */
  private void writeFileNode(Node gadgetFileNode, String parentPath, ZipOutputStream zos, String extensionName, String gadgetName) throws Exception {
    NodeIterator nodeIterator = gadgetFileNode.getNodes();
    while (nodeIterator.hasNext()) {
      Node node = nodeIterator.nextNode();
      if (node.isNodeType("nt:file")) {
        String filePath = GADGETS_LOCATION + "/" + node.getPath().replaceFirst(parentPath, gadgetName);
        filePath = filePath.replaceAll("//", "/");
        Utils.writeZipEnry(zos, filePath, extensionName, getContent(node), false);
      } else if (node.isNodeType("nt:folder")) {
        writeFileNode(node, parentPath, zos, extensionName, gadgetName);
      }
    }
  }

  /**
   * Gets the content.
   *
   * @param node the node
   * @return the content
   * @throws Exception the exception
   */
  private InputStream getContent(Node node) throws Exception {
    return node.getNode("jcr:content").getProperty("jcr:data").getStream();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getConfigurationPaths() {
    return Collections.emptyList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Log getLogger() {
    return log;
  }

  /**
   * Gets the session.
   *
   * @param workspace the workspace
   * @return the session
   * @throws Exception the exception
   */
  private Session getSession(String workspace) throws Exception {
    SessionProvider provider = SessionProvider.createSystemProvider();
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Session session = provider.getSession(workspace, repository);
    return session;
  }

}
