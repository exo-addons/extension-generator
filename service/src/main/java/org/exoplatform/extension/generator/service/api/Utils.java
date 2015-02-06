package org.exoplatform.extension.generator.service.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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

public class Utils {
  private static Log log = ExoLogger.getLogger(Utils.class);
  private static final String CONFIGURATION_FILE_XSD = "<configuration " + "\r\n   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
      + "\r\n   xsi:schemaLocation=\"http://www.exoplatform.org/xml/ns/kernel_1_2.xsd http://www.exoplatform.org/xml/ns/kernel_1_2.xsd\""
      + "\r\n   xmlns=\"http://www.exoplatform.org/xml/ns/kernel_1_2.xsd\">";

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

  public static void writeZipEnry(ZipOutputStream zos, String entryName, String extensionName, InputStream inputStream, boolean changeContent) throws Exception {
    writeZipEnry(zos, entryName, extensionName, inputStream, changeContent, true);
  }

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

  public static void writeZipEnry(ZipOutputStream zos, String entryName, String extensionName, String content, boolean changeContent) throws Exception {
    entryName = entryName.replace("custom-extension", extensionName);
    if (changeContent) {
      content = content.replaceAll("custom-extension", extensionName);
    }
    writeZipEnry(zos, entryName, content.getBytes("UTF-8"));
  }

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

  public static <T> T fromXML(byte[] bytes, Class<T> clazz) throws Exception {
    ByteArrayInputStream baos = new ByteArrayInputStream(bytes);
    IBindingFactory bfact = BindingDirectory.getFactory(clazz);
    UnmarshallingContext uctx = (UnmarshallingContext) bfact.createUnmarshallingContext();
    Object obj = uctx.unmarshalDocument(baos, "UTF-8");
    return clazz.cast(obj);
  }

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
