package org.exoplatform.extension.generator.service.handler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.tika.io.IOUtils;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.impl.ManageViewPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class SiteExplorerViewConfigurationHandler extends AbstractConfigurationHandler {
  private static final String VIEW_CONFIGURATION_LOCATION = DMS_CONFIGURATION_LOCATION + "view";
  private static final String VIEW_CONFIGURATION_NAME = "view-configuration.xml";
  private static final String VIEW_CONFIGURATION_FULL_PATH = VIEW_CONFIGURATION_LOCATION + "/" + VIEW_CONFIGURATION_NAME;
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(VIEW_CONFIGURATION_FULL_PATH.replace("WEB-INF", "war:"));
  }

  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  public boolean writeData(ZipOutputStream zos, String extensionName, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.ECM_VIEW_CONFIGURATION_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }

    List<String> filterViews = new ArrayList<String>();
    for (String resourcePath : filteredSelectedResources) {
      String viewName = resourcePath.replace(ExtensionGenerator.ECM_VIEW_CONFIGURATION_PATH + "/", "");
      filterViews.add(viewName);
    }

    InitParams allParams = null;
    // Copy gtmpl in WAR and get all initParams in a single one
    ZipFile zipFile = null;
    try {
      zipFile = getExportedFileFromOperation(ExtensionGenerator.ECM_VIEW_CONFIGURATION_PATH, filterViews.toArray(new String[0]));
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = (ZipEntry) entries.nextElement();
        String filePath = entry.getName();
        if (!filePath.startsWith("ecmadmin/view/")) {
          continue;
        }
        // Skip directories
        // & Skip empty entries
        // & Skip entries not in sites/zip
        if (entry.isDirectory() || filePath.trim().isEmpty() || !(filePath.endsWith(".xml"))) {
          continue;
        }
        InputStream inputStream = zipFile.getInputStream(entry);
        if (log.isDebugEnabled()) {
          log.debug("Parsing : " + filePath);
        }

        allParams = Utils.fromXML(IOUtils.toByteArray(inputStream), InitParams.class);
        break;
      }

      ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
      // Add constant init params
      allParams.addParam(getValueParam("autoCreateInNewRepository", "true"));
      allParams.addParam(getValueParam("predefinedViewsLocation", VIEW_CONFIGURATION_LOCATION.replace("WEB-INF", "war:").replace("custom-extension", extensionName)));

      ComponentPlugin plugin = createComponentPlugin("manage.view.plugin", ManageViewPlugin.class.getName(), "setManageViewPlugin", allParams);
      addComponentPlugin(externalComponentPlugins, ManageViewService.class.getName(), plugin);

      return Utils.writeConfiguration(zos, VIEW_CONFIGURATION_FULL_PATH, extensionName, externalComponentPlugins);
    } catch (Exception e) {
      log.error("Error iccured while handling view templates", e);
      throw new RuntimeException(e);
    } finally {
      if (zipFile != null) {
        try {
          zipFile.close();
        } catch (Exception exp) {
          // Nothing to do
        }
      }
      clearTempFiles();
    }
  }

  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }

  @Override
  protected Log getLogger() {
    return log;
  }
}
