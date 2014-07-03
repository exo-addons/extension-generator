package org.exoplatform.extension.generator.service.handler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.core.Response;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.script.groovy.GroovyScript2RestLoader;
import org.exoplatform.services.jcr.ext.script.groovy.GroovyScript2RestLoaderPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class RESTServicesFromIDEConfigurationHandler extends AbstractConfigurationHandler {
  protected static final String CONFIGURATION_LOCATION = "WEB-INF/conf/custom-extension/rest";
  protected static final String SCRIPTS_CONFIGURATION_LOCATION = "WEB-INF/conf/custom-extension/rest/scripts";
  private static final String CONFIGURATION_NAME = "rest-groovy-configuration.xml";
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + "/" + CONFIGURATION_NAME);
  }

  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean writeData(ZipOutputStream zos, String extensionName, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.IDE_REST_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    GroovyScript2RestLoader script2RestLoader = (GroovyScript2RestLoader) PortalContainer.getInstance().getComponentInstanceOfType(GroovyScript2RestLoader.class);
    RepositoryService repositoryService = (RepositoryService) PortalContainer.getInstance().getComponentInstanceOfType(RepositoryService.class);

    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
    try {
      String repository = repositoryService.getCurrentRepository().getConfiguration().getName();
      for (String selectedResoucePath : filteredSelectedResources) {
        selectedResoucePath = selectedResoucePath.replace(ExtensionGenerator.IDE_REST_PATH, "");
        String[] parts = selectedResoucePath.split("::");
        if (parts.length != 2) {
          getLogger().warn("IDE REST Services - Selected Path: '" + selectedResoucePath + "' can't be processed. Ignore it.");
          continue;
        }
        String workspace = parts[0];
        String scriptPath = parts[1].substring(1);

        String scriptName = scriptPath.substring(scriptPath.lastIndexOf("/") + 1);
        String scriptPathInArchive = SCRIPTS_CONFIGURATION_LOCATION + "/" + scriptName;

        Response response = script2RestLoader.getScript(repository, workspace, scriptPath);
        InputStream inputStream = (InputStream) response.getEntity();
        Utils.writeZipEnry(zos, scriptPathInArchive, extensionName, inputStream, false);

        scriptPathInArchive = scriptPathInArchive.replace("WEB-INF", "war:");

        InitParams scriptInitParams = new InitParams();
        scriptInitParams.addParameter(getValueParam("node", "/" + scriptPath));
        scriptInitParams.addParameter(getValueParam("workspace", workspace));
        PropertiesParam propertiesParam = new PropertiesParam();
        propertiesParam.setName(scriptName);
        propertiesParam.setProperty("autoload", "true");
        propertiesParam.setProperty("path", scriptPathInArchive);
        scriptInitParams.addParameter(propertiesParam);

        ComponentPlugin plugin = createComponentPlugin(scriptName, GroovyScript2RestLoaderPlugin.class.getName(), "addPlugin", scriptInitParams);
        addComponentPlugin(externalComponentPlugins, GroovyScript2RestLoader.class.getName(), plugin);
      }

      return Utils.writeConfiguration(zos, CONFIGURATION_LOCATION + "/" + CONFIGURATION_NAME, extensionName, externalComponentPlugins);
    } catch (Exception e) {
      getLogger().error(e);
      return false;
    }
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
}
