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

import org.exoplatform.extension.generator.service.api.ExtensionGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class CLVTemplatesConfigurationHandler.
 */
public class CLVTemplatesConfigurationHandler extends ApplicationTemplatesConfigurationHandler {
  
  /** The Constant APPLICATION_CLV_CONFIGURATION_LOCATION. */
  private static final String APPLICATION_CLV_CONFIGURATION_LOCATION = DMS_CONFIGURATION_LOCATION + "templates/applications/content-list-viewer";
  
  /** The Constant APPLICATION_CLV_CONFIGURATION_NAME. */
  private static final String APPLICATION_CLV_CONFIGURATION_NAME = "application-clv-templates-configuration.xml";
  
  /** The Constant configurationPaths. */
  private static final List<String> configurationPaths = new ArrayList<String>();
  static {
    configurationPaths.add(DMS_CONFIGURATION_LOCATION.replace("WEB-INF", "war:") + APPLICATION_CLV_CONFIGURATION_NAME);
  }

  /**
   * Instantiates a new CLV templates configuration handler.
   */
  public CLVTemplatesConfigurationHandler() {
    super(APPLICATION_CLV_CONFIGURATION_LOCATION, APPLICATION_CLV_CONFIGURATION_NAME, ExtensionGenerator.ECM_TEMPLATES_APPLICATION_CLV_PATH, "content-list-viewer");
  }

  /** The log. */
  private Log log = ExoLogger.getLogger(this.getClass());

  /**
   * {@inheritDoc}
   */
  @Override
  protected Log getLogger() {
    return log;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getConfigurationPaths() {
    return configurationPaths;
  }
}
