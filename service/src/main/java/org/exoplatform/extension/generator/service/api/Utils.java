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

import org.apache.commons.io.IOUtils;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.management.ecmadmin.operations.templates.NodeTemplate;
import org.exoplatform.services.cms.templates.impl.TemplateConfig;
import org.exoplatform.services.cms.templates.impl.TemplateConfig.Template;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.impl.UnmarshallingContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * The Class Utils.
 */
public class Utils {
  
  /** The log. */
  private static Log log = ExoLogger.getLogger(Utils.class);
  
  /** The Constant CONFIGURATION_FILE_XSD. */
  private static final String CONFIGURATION_FILE_XSD = "<configuration " + "\r\n   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
      + "\r\n   xsi:schemaLocation=\"http://www.exoplatform.org/xml/ns/kernel_1_2.xsd http://www.exoplatform.org/xml/ns/kernel_1_2.xsd\""
      + "\r\n   xmlns=\"http://www.exoplatform.org/xml/ns/kernel_1_2.xsd\">";

  /**
   * Write configuration.
   *
   * @param zos the zos
   * @param entryName the entry name
   * @param extensionName the extension name
   * @param configuration the configuration
   * @return true, if successful
   */
  public static boolean writeConfiguration(ZipOutputStream zos, String entryName, String extensionName, Configuration configuration) {
    entryName = entryName.replace("custom-extension", extensionName);
    try {
      if (entryName.startsWith("/")) {
        entryName = entryName.substring(1);
      }
      zos.putNextEntry(new ZipEntry(entryName));
      zos.write(toXML(configuration, extensionName));
      zos.closeEntry();
    } catch (Exception e) {
      log.error("Error while writing file " + entryName, e);
      return false;
    }
    return true;
  }

  /**
   * Write configuration.
   *
   * @param zos the zos
   * @param entryName the entry name
   * @param extensionName the extension name
   * @param externalComponentPlugins the external component plugins
   * @return true, if successful
   */
  public static boolean writeConfiguration(ZipOutputStream zos, String entryName, String extensionName, ExternalComponentPlugins... externalComponentPlugins) {
    entryName = entryName.replace("custom-extension", extensionName);
    Configuration configuration = new Configuration();
    for (ExternalComponentPlugins externalComponentPlugin : externalComponentPlugins) {
      configuration.addExternalComponentPlugins(externalComponentPlugin);
    }
    try {
      if (entryName.startsWith("/")) {
        entryName = entryName.substring(1);
      }
      zos.putNextEntry(new ZipEntry(entryName));
      zos.write(toXML(configuration, extensionName));
      zos.closeEntry();
    } catch (Exception e) {
      log.error("Error while writing file " + entryName, e);
      return false;
    }
    return true;
  }

  /**
   * Copy zip enries.
   *
   * @param zin the zin
   * @param zos the zos
   * @param extensionName the extension name
   * @param rootPathInTarget the root path in target
   * @throws Exception the exception
   */
  public static void copyZipEnries(ZipInputStream zin, ZipOutputStream zos, String extensionName, String rootPathInTarget) throws Exception {
    if (rootPathInTarget == null) {
      rootPathInTarget = "";
    }
    ZipEntry entry;
    while ((entry = zin.getNextEntry()) != null) {
      if (entry.isDirectory() || !entry.getName().contains(".")) {
        continue;
      }
      String targetEntryName = rootPathInTarget + ("/") + entry.getName();
      while (targetEntryName.contains("//")) {
        targetEntryName = targetEntryName.replace("//", "/");
      }
      if (targetEntryName.startsWith("/")) {
        targetEntryName = targetEntryName.substring(1);
      }
      writeZipEnry(zos, targetEntryName, extensionName, zin, true, false);
    }
    zos.flush();
    zin.close();
  }

  /**
   * Write zip enry.
   *
   * @param zos the zos
   * @param entryName the entry name
   * @param extensionName the extension name
   * @param inputStream the input stream
   * @param changeContent the change content
   * @throws Exception the exception
   */
  public static void writeZipEnry(ZipOutputStream zos, String entryName, String extensionName, InputStream inputStream, boolean changeContent) throws Exception {
    writeZipEnry(zos, entryName, extensionName, inputStream, changeContent, true);
  }

  /**
   * Write zip enry.
   *
   * @param zos the zos
   * @param entryName the entry name
   * @param extensionName the extension name
   * @param inputStream the input stream
   * @param changeContent the change content
   * @param closeInputStream the close input stream
   * @throws Exception the exception
   */
  public static void writeZipEnry(ZipOutputStream zos, String entryName, String extensionName, InputStream inputStream, boolean changeContent, boolean closeInputStream) throws Exception {
    entryName = entryName.replaceAll("/ecmadmin", "");
    if (changeContent) {
      String content = IOUtils.toString(inputStream);
      writeZipEnry(zos, entryName, extensionName, content, true);
    } else {
      entryName = entryName.replace("custom-extension", extensionName);
      writeZipEnry(zos, entryName, IOUtils.toByteArray(inputStream));
    }
    if (closeInputStream) {
      inputStream.close();
    }
  }

  /**
   * Write zip enry.
   *
   * @param zos the zos
   * @param entryName the entry name
   * @param extensionName the extension name
   * @param content the content
   * @param changeContent the change content
   * @throws Exception the exception
   */
  public static void writeZipEnry(ZipOutputStream zos, String entryName, String extensionName, String content, boolean changeContent) throws Exception {
    entryName = entryName.replace("custom-extension", extensionName);
    if (changeContent) {
      content = content.replaceAll("custom-extension", extensionName);
    }
    writeZipEnry(zos, entryName, content.getBytes("UTF-8"));
  }

  /**
   * To XML.
   *
   * @param obj the obj
   * @param extensionName the extension name
   * @return the byte[]
   * @throws Exception the exception
   */
  public static byte[] toXML(Object obj, String extensionName) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IBindingFactory bfact = BindingDirectory.getFactory(obj.getClass());
    IMarshallingContext mctx = bfact.createMarshallingContext();
    mctx.setIndent(2);
    mctx.marshalDocument(obj, "UTF-8", null, out);
    String content = new String(out.toByteArray());
    content = content.replace("<configuration>", CONFIGURATION_FILE_XSD);
    content = content.replaceAll("<field name=\"([A-z])*\"/>", "");
    content = content.replaceAll("custom-extension", extensionName);
    return content.getBytes();
  }

  /**
   * From XML.
   *
   * @param <T> the generic type
   * @param bytes the bytes
   * @param clazz the clazz
   * @return the t
   * @throws Exception the exception
   */
  public static <T> T fromXML(byte[] bytes, Class<T> clazz) throws Exception {
    ByteArrayInputStream baos = new ByteArrayInputStream(bytes);
    IBindingFactory bfact = BindingDirectory.getFactory(clazz);
    UnmarshallingContext uctx = (UnmarshallingContext) bfact.createUnmarshallingContext();
    Object obj = uctx.unmarshalDocument(baos, "UTF-8");
    return clazz.cast(obj);
  }

  /**
   * Convert template list.
   *
   * @param list the list
   * @return the list
   */
  public static List<Template> convertTemplateList(List<NodeTemplate> list) {
    List<Template> templates = new ArrayList<TemplateConfig.Template>();
    if (list == null || list.isEmpty()) {
      return templates;
    }
    for (NodeTemplate nodeTemplate : list) {
      if (nodeTemplate.getTemplateFile() != null) {
        Template template = new Template();
        template.setTemplateFile(nodeTemplate.getTemplateFile().replace(":", "_"));
        template.setRoles(nodeTemplate.getRoles());
        templates.add(template);
      }
    }
    return templates;
  }

  /**
   * Write zip enry.
   *
   * @param zos the zos
   * @param entryName the entry name
   * @param bytes the bytes
   */
  private static void writeZipEnry(ZipOutputStream zos, String entryName, byte[] bytes) {
    try {
      if (entryName.startsWith("/")) {
        entryName = entryName.substring(1);
      }
      zos.putNextEntry(new ZipEntry(entryName));
      zos.write(bytes);
      zos.closeEntry();
    } catch (Exception e) {
      log.error("Error while writing file " + entryName, e);
    }
  }

}
