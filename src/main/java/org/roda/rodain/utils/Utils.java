package org.roda.rodain.utils;

import org.apache.commons.io.IOUtils;
import org.roda.rodain.core.AppProperties;
import org.roda.rodain.utils.validation.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 24-09-2015.
 */
public class Utils {
  private static final Logger log = LoggerFactory.getLogger(Utils.class.getName());

  private Utils() {
  }

  /**
   * Formats a number to a readable size format (B, KB, MB, GB, etc)
   *
   * @param v
   *          The value to be formatted
   * @return The formatted String
   */
  public static String formatSize(long v) {
    if (v < 1024)
      return v + " B";
    int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
    return String.format("%.1f %sB", (double) v / (1L << (z * 10)), " KMGTPE".charAt(z));
  }

  /**
   * Reads a file and returns its content.
   *
   * @param path
   *          The path of the file to be read.
   * @param encoding
   *          The encoding to be used when reading the file.
   * @return A String with the content of the file
   * @throws IOException
   */
  public static String readFile(String path, Charset encoding) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }

  /**
   * Produces a String from the specified InputStream.
   * 
   * @param is
   *          The InputStream to be used.
   * @return The String produced using the InputStream.
   */
  public static String convertStreamToString(InputStream is) {
    Scanner s = new Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }

  /**
   * Validates a XML against a schema.
   * 
   * @param content
   *          The String content of the XML to be validated.
   * @param schemaString
   *          The String content of the schema used to validate.
   * @return True if the content can be validated using the schema, false
   *         otherwise.
   * @throws SAXException
   */
  public static boolean validateSchema(String content, String schemaString) throws SAXException {
    boolean isValid = false;
    try {
      isValid = validateSchemaWithoutCatch(content, IOUtils.toInputStream(schemaString, "UTF-8"));
    } catch (IOException e) {
      log.error("Can't access the schema file", e);
    }

    return isValid;
  }

  private static boolean validateSchemaWithoutCatch(String content, InputStream schemaStream)
    throws IOException, SAXException {
    // build the schema
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setResourceResolver(new ResourceResolver());
    StreamSource streamSource = new StreamSource(schemaStream, AppProperties.getRodainPath().toString());
    Schema schema = factory.newSchema(streamSource);
    Validator validator = schema.newValidator();

    // create a source from a string
    Source source = new StreamSource(new StringReader(content));

    // check input
    validator.validate(source);
    return true;
  }

}
