package org.exoplatform.extension.generator.service.handler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class DrivesConfigurationHandler extends AbstractConfigurationHandler {
  private static final String DRIVE_CONFIGURATION_LOCATION_FROM_EXPORT = "ecmadmin/drive/drives-configuration.xml";
  private final List<String> configurationPaths = new ArrayList<String>();

  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  public boolean writeData(ZipOutputStream zos, String extensionName, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, ExtensionGenerator.ECM_DRIVE_PATH);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    configurationPaths.clear();
    List<String> filterDrives = new ArrayList<String>();
    for (String resourcePath : filteredSelectedResources) {
      String driveName = resourcePath.replace(ExtensionGenerator.ECM_DRIVE_PATH + "/", "");
      filterDrives.add(driveName);
    }
    ZipFile zipFile = null;
    try {
      zipFile = getExportedFileFromOperation(ExtensionGenerator.ECM_DRIVE_PATH, filterDrives.toArray(new String[0]));
      ZipEntry drivesConfigurationEntry = zipFile.getEntry(DRIVE_CONFIGURATION_LOCATION_FROM_EXPORT);
      InputStream inputStream = zipFile.getInputStream(drivesConfigurationEntry);
      Utils.writeZipEnry(zos, DMS_CONFIGURATION_LOCATION + drivesConfigurationEntry.getName(), extensionName, inputStream, false);
      configurationPaths.add(DMS_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + drivesConfigurationEntry.getName());
      return true;
    } catch (Exception e) {
      log.error("Error while serializing drives data", e);
      return false;
    } finally {
      if (zipFile != null) {
        try {
          zipFile.close();
        } catch (Exception e) {
          // Nothing to do
        }
      }
      clearTempFiles();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Log getLogger() {
    return log;
  }

  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }
}
