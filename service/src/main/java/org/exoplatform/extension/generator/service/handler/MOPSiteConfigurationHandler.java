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

import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.extension.generator.service.api.AbstractConfigurationHandler;
import org.exoplatform.extension.generator.service.api.Utils;
import org.exoplatform.portal.config.NewPortalConfig;
import org.exoplatform.portal.config.NewPortalConfigListener;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * The Class MOPSiteConfigurationHandler.
 */
public class MOPSiteConfigurationHandler extends AbstractConfigurationHandler {
  
  /** The Constant SITES_CONFIGURATION_LOCATION. */
  private static final String SITES_CONFIGURATION_LOCATION = "WEB-INF/conf/custom-extension/portal/";
  
  /** The Constant SITES_CONFIGURATION_NAME. */
  private static final String SITES_CONFIGURATION_NAME = "-sites-configuration.xml";
  
  /** The configuration paths. */
  private final List<String> configurationPaths = new ArrayList<String>();

  /** The log. */
  private Log log = ExoLogger.getLogger(this.getClass());
  
  /** The site type. */
  String siteType;
  
  /** The site resource path. */
  String siteResourcePath;

  /**
   * Instantiates a new MOP site configuration handler.
   *
   * @param portal the portal
   */
  public MOPSiteConfigurationHandler(SiteType portal) {
    this.siteType = portal.getName();
    siteResourcePath = "/site/" + this.siteType + "sites/";
    configurationPaths.add(SITES_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + this.siteType + SITES_CONFIGURATION_NAME);
  }

  /**
   * {@inheritDoc}
   */
  public boolean writeData(ZipOutputStream zos, String extensionName, Collection<String> selectedResources) {
    Set<String> filteredSelectedResources = filterSelectedResources(selectedResources, siteResourcePath);
    if (filteredSelectedResources.isEmpty()) {
      return false;
    }
    HashSet<String> siteNames = new HashSet<String>();
    ZipFile zipFile = null;
    try {
      for (String resourcePath : filteredSelectedResources) {
        siteNames.add(resourcePath.replace(siteResourcePath, ""));
        zipFile = getExportedFileFromOperation(resourcePath);
        try {
          Enumeration<? extends ZipEntry> entries = zipFile.entries();
          while (entries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) entries.nextElement();
            InputStream inputStream = null;
            try {
              inputStream = zipFile.getInputStream(zipEntry);
              Utils.writeZipEnry(zos, SITES_CONFIGURATION_LOCATION + zipEntry.getName(), extensionName, inputStream, false);
            } finally {
              if (inputStream != null) {
                try {
                  inputStream.close();
                } catch (IOException e) {
                  log.error(e);
                }
              }
            }
          }
        } catch (Exception e) {
          log.error("Error while serializing MOP data", e);
          return false;
        } finally {
          if (zipFile != null) {
            try {
              zipFile.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }
    } finally {
      clearTempFiles();
    }

    ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
    InitParams params = new InitParams();
    ObjectParameter objectParameter = new ObjectParameter();
    objectParameter.setName(siteType + ".configuration");
    NewPortalConfig portalConfig = new NewPortalConfig();
    portalConfig.setOwnerType(siteType);
    portalConfig.setTemplateLocation(SITES_CONFIGURATION_LOCATION.replace("WEB-INF", "war:").replace("custom-extension", extensionName));
    portalConfig.setPredefinedOwner(siteNames);
    objectParameter.setObject(portalConfig);
    params.addParam(objectParameter);
    ComponentPlugin plugin = createComponentPlugin(siteType + ".config.user.listener", NewPortalConfigListener.class.getName(), "initListener", params);
    addComponentPlugin(externalComponentPlugins, UserPortalConfigService.class.getName(), plugin);
    return Utils.writeConfiguration(zos, SITES_CONFIGURATION_LOCATION + siteType + SITES_CONFIGURATION_NAME, extensionName, externalComponentPlugins);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Log getLogger() {
    return log;
  }
}
