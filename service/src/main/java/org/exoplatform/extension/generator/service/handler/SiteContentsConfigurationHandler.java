package org.exoplatform.extension.generator.service.handler;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.exoplatform.container.xml.Component;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.management.common.exportop.JCRNodeExportTask;
import org.exoplatform.management.content.operations.site.SiteConstants;
import org.exoplatform.management.content.operations.site.contents.SiteContentsVersionHistoryExportTask;
import org.exoplatform.management.content.operations.site.contents.SiteMetaData;
import org.exoplatform.services.deployment.DeploymentDescriptor;
import org.exoplatform.services.deployment.DeploymentDescriptor.Target;
import org.exoplatform.services.deployment.WCMContentInitializerService;
import org.exoplatform.services.deployment.plugins.XMLDeploymentPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.portal.artifacts.CreatePortalArtifactsService;
import org.exoplatform.services.wcm.portal.artifacts.IgnorePortalPlugin;

import com.thoughtworks.xstream.XStream;

public class SiteContentsConfigurationHandler extends AbstractConfigurationHandler {
  private static final String WCM_CONTENT_CONFIGURATION_LOCATION = "WEB-INF/conf/custom-extension/wcm/content";
  private static final String WCM_CONTENT_CONFIGURATION_NAME = "/content-artifacts-deployment-configuration.xml";
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(WCM_CONTENT_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + WCM_CONTENT_CONFIGURATION_NAME);
  }

  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  public boolean writeData(ZipOutputStream zos, String extensionName, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.CONTENT_SITES_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }

    Map<String, SiteMetaData> siteMetadatas = new HashMap<String, SiteMetaData>();
    Map<String, List<String>> siteContentsLocation = new HashMap<String, List<String>>();
    Set<String> contentsWithVersionHistory = new HashSet<String>();
    try {
      for (String filteredResource : filteredSelectedResources) {
        String[] filters = new String[3];
        filters[0] = "no-skeleton:true";
        filters[1] = "taxonomy:false";
        filters[2] = "no-hitory:true";
        ZipFile zipFile = null;
        try {
          zipFile = getExportedFileFromOperation(filteredResource, filters);

          Enumeration<? extends ZipEntry> entries = zipFile.entries();
          while (entries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) entries.nextElement();
            try {
              InputStream inputStream = zipFile.getInputStream(zipEntry);
              String siteName = extractSiteNameFromPath(zipEntry.getName());
              if (zipEntry.getName().endsWith("metadata.xml")) {
                // Unmarshall metadata xml file
                XStream xstream = new XStream();
                xstream.alias("metadata", SiteMetaData.class);
                InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8");
                siteMetadatas.put(siteName, (SiteMetaData) xstream.fromXML(isr));
                // Save unmarshalled metadata
              } else if (zipEntry.getName().endsWith("seo.xml")) {
                continue;
              } else {
                String[] fileParts = zipEntry.getName().split(JCRNodeExportTask.JCR_DATA_SEPARATOR);
                if (fileParts.length != 2) {
                  log.warn("Cannot parse file: " + zipEntry.getName());
                  continue;
                }
                List<String> siteContentLocation = siteContentsLocation.get(siteName);
                if (siteContentLocation == null) {
                  siteContentLocation = new ArrayList<String>();
                  siteContentsLocation.put(siteName, siteContentLocation);
                }
                String location = fileParts[1];
                if (location.endsWith(SiteContentsVersionHistoryExportTask.VERSION_HISTORY_FILE_SUFFIX)) {
                  contentsWithVersionHistory.add(location.replace(SiteContentsVersionHistoryExportTask.VERSION_HISTORY_FILE_SUFFIX, ".xml"));
                } else {
                  siteContentLocation.add(location);
                }
                Utils.writeZipEnry(zos, WCM_CONTENT_CONFIGURATION_LOCATION + location, extensionName, inputStream, false);
              }
            } catch (Exception e) {
              log.error("Exception while writing Data", e);
              return false;
            }
          }
        } catch (Exception e) {
          if (zipFile != null) {
            try {
              zipFile.close();
            } catch (Exception exp) {
              // Nothing to do
            }
          }
        }
      }
    } finally {
      clearTempFiles();
    }

    ExternalComponentPlugins ignoreContentComponentPlugin = null;
    ArrayList<String> ignoredSitesList = new ArrayList<String>(siteMetadatas.keySet());
    ignoredSitesList.remove("shared");
    if (!ignoredSitesList.isEmpty()) {
      ignoreContentComponentPlugin = new ExternalComponentPlugins();
      {
        InitParams params = new InitParams();
        ValuesParam valuesParam = new ValuesParam();
        valuesParam.setName("autoCreatedInNewRepository");
        valuesParam.setValues(ignoredSitesList);
        params.addParam(valuesParam);
        ComponentPlugin plugin = createComponentPlugin("Add as ignored portal", IgnorePortalPlugin.class.getName(), "addIgnorePortalPlugin", params);
        addComponentPlugin(ignoreContentComponentPlugin, CreatePortalArtifactsService.class.getName(), plugin);
      }
    }

    ExternalComponentPlugins contentExternalComponentPlugins = new ExternalComponentPlugins();
    Set<Entry<String, SiteMetaData>> sitesDataSet = siteMetadatas.entrySet();
    for (Entry<String, SiteMetaData> siteDataEntry : sitesDataSet) {
      InitParams params = new InitParams();
      ValueParam overrideParam = new ValueParam();
      overrideParam.setName("override");
      overrideParam.setValue("false");
      params.addParameter(overrideParam);

      ComponentPlugin plugin = createComponentPlugin(siteDataEntry.getKey() + " Content Initializer Service", XMLDeploymentPlugin.class.getName(), "addPlugin", params);
      addComponentPlugin(contentExternalComponentPlugins, WCMContentInitializerService.class.getName(), plugin);

      SiteMetaData siteData = siteDataEntry.getValue();
      String siteName = siteData.getOptions().get("site-name");

      List<String> exportedFiles = siteContentsLocation.get(siteName);
      for (String location : exportedFiles) {
        DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor();
        deploymentDescriptor.setCleanupPublication(false);
        String xmlLocation = WCM_CONTENT_CONFIGURATION_LOCATION.replace("WEB-INF", "war:").replace("custom-extension", extensionName) + location;

        deploymentDescriptor.setSourcePath(xmlLocation);

        Target target = new Target();
        target.setWorkspace(siteData.getOptions().get("site-workspace"));
        String targetNodePath = location.substring(0, location.lastIndexOf("/"));
        target.setNodePath(targetNodePath);
        deploymentDescriptor.setTarget(target);

        if(contentsWithVersionHistory.contains(location)) {
          String versionHistoryFile = xmlLocation.replace(".xml", SiteContentsVersionHistoryExportTask.VERSION_HISTORY_FILE_SUFFIX);
          deploymentDescriptor.setVersionHistoryPath(versionHistoryFile);
        }
        ObjectParameter objectParameter = new ObjectParameter();
        objectParameter.setName(location);
        objectParameter.setObject(deploymentDescriptor);
        params.addParam(objectParameter);
      }
    }

    Component component = new Component();
    component.setType(WCMContentInitializerService.class.getName());

    Configuration configuration = new Configuration();
    configuration.addComponent(component);
    configuration.addExternalComponentPlugins(contentExternalComponentPlugins);
    if (ignoreContentComponentPlugin != null) {
      configuration.addExternalComponentPlugins(ignoreContentComponentPlugin);
    }

    return Utils.writeConfiguration(zos, WCM_CONTENT_CONFIGURATION_LOCATION + WCM_CONTENT_CONFIGURATION_NAME, extensionName, configuration);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }

  @Override
  protected Log getLogger() {
    return log;
  }

  protected String extractSiteNameFromPath(String path) {
    String siteName = null;

    int beginIndex = SiteConstants.SITE_CONTENTS_ROOT_PATH.length() + 1;
    siteName = path.substring(beginIndex, path.indexOf("/", beginIndex));

    return siteName;
  }
}
