package org.exoplatform.extension.generator.service.handler;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

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

public class GadgetsConfigurationHandler extends AbstractConfigurationHandler {
  protected static final String GADGETS_LOCATION = "gadgets";
  private static final String GADGETS_CONFIGURATION_PATH = "WEB-INF/gadget.xml";

  private RepositoryService repositoryService;
  private GadgetRegistryService gadgetRegistryService;

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

      StringBuilder gadgetsConfiguration = new StringBuilder("<gadgets>\r\n");
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
        if (!gadgetParentNode.isNodeType("nt:folder") || gadgetParentNode.getPath().equals("/")) {
          getLogger().warn("Cannot export Gadget '" + gadget.getName() + "'. Each gadget have to be in a separate folder.");
          continue;
        }

        String parentPath = gadgetParentNode.getParent().getPath();

        writeFileNode(gadgetParentNode, parentPath, zos, extensionName);

        String xmlPath = GADGETS_LOCATION + "/" + gadgetXMLNode.getPath().replaceFirst(parentPath, "");

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

  private void writeFileNode(Node gadgetFileNode, String parentPath, ZipOutputStream zos, String extensionName) throws Exception {
    NodeIterator nodeIterator = gadgetFileNode.getNodes();
    while (nodeIterator.hasNext()) {
      Node node = nodeIterator.nextNode();
      if (node.isNodeType("nt:file")) {
        String filePath = GADGETS_LOCATION + "/" + node.getPath().replaceFirst(parentPath, "");
        filePath.replaceAll("//", "/");
        Utils.writeZipEnry(zos, filePath, extensionName, getContent(node), false);
      } else if (node.isNodeType("nt:folder")) {
        writeFileNode(node, parentPath, zos, extensionName);
      }
    }
  }

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

  @Override
  protected Log getLogger() {
    return log;
  }

  private Session getSession(String workspace) throws Exception {
    SessionProvider provider = SessionProvider.createSystemProvider();
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Session session = provider.getSession(workspace, repository);
    return session;
  }

}
