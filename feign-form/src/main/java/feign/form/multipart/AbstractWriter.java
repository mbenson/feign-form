/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package feign.form.multipart;

import static feign.form.ContentProcessor.CRLF;

import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import feign.codec.EncodeException;
import lombok.SneakyThrows;
import lombok.val;

/**
 *
 * @author Artem Labazin
 */
public abstract class AbstractWriter implements Writer {

  private static final String BINARY_ENCODING_SPECIFICATION =
      "Content-Transfer-Encoding: binary" + CRLF + CRLF;

  protected static int predictByteCount (CharsetEncoder encoder, int strLen) {
    return (int) encoder.maxBytesPerChar() * strLen;
  }

  @Override
  public void write (Output output, String boundary, String key, Object value) throws EncodeException {
    output.write("--").write(boundary).write(CRLF);
    write(output, key, value);
    output.write(CRLF);
  }

  /**
   * Writes data for its children.
   *
   * @param output  output writer.
   * @param key     name for piece of data.
   * @param value   piece of data.
   *
   * @throws EncodeException in case of write errors
   */
  @SuppressWarnings({
      "PMD.UncommentedEmptyMethodBody",
      "PMD.EmptyMethodInAbstractClassShouldBeAbstract"
  })
  protected void write (Output output, String key, Object value) throws EncodeException {
  }

  @Override
  public int length (Charset charset, String boundary, String key, Object value) {
    val encoder = charset.newEncoder();

    return predictByteCount(encoder, boundary.length() + (CRLF.length() * 2))
        + length(encoder, key, value);
  }

  /**
   * Predict required capacity for key/value portion of writer's output.
   * @param encoder to use.
   * @param key     name for piece of data.
   * @param value   piece of data.
   * @return {@code int}
   */
  protected int length (CharsetEncoder encoder, String key, Object value) {
    return predictByteCount(encoder, 1024);
  }

  /**
   * Writes file's metadata.
   *
   * @param output      output writer.
   * @param name        name for piece of data.
   * @param fileName    file name.
   * @param contentType type of file content. May be the {@code null}, in that case it will be determined by file name.
   */
  @SneakyThrows
  protected void writeFileMetadata (Output output, String name, String fileName, String contentType) {
    output.write(createFileMetadata(name, fileName, contentType));
  }

  /**
   * Estimate length of file metadata.
   * @param encoder to estimate bytes per character
   * @param name        name for piece of data.
   * @param fileName    file name.
   * @param contentType type of file content. May be {@code null}, in which case it will be determined by file name.
   * @return {@code int} maximum file metadata length
   * @see CharsetEncoder#maxBytesPerChar()
   */
  protected int fileMetadataLength (CharsetEncoder encoder, String name, String fileName, String contentType) {
    int length = contentDisposition(name, fileName).length();
    length += contentTypeSpecification(contentType, fileName).length();
    length += CRLF.length() * 2;

    return predictByteCount(encoder, length);
  }

  private String createFileMetadata (String name, String fileName, String contentType) {
    val contentDisposition = contentDisposition(name, fileName);
    val contentTypeSpecification = contentTypeSpecification(contentType, fileName);

    return new StringBuilder()
        .append(contentDisposition).append(CRLF)
        .append(contentTypeSpecification).append(CRLF)
        .append(BINARY_ENCODING_SPECIFICATION)
        .toString();
  }

  private String contentDisposition (String name, String fileName) {
    val contentDispositionBuilder =
        new StringBuilder("Content-Disposition: form-data; name=\"").append(name).append('"');

    if (fileName != null) {
      contentDispositionBuilder.append("; filename=\"").append(fileName).append('"');
    }
    return contentDispositionBuilder.toString();
  }

  private String contentTypeSpecification (String contentType, String fileName) {
    String fileContentType = contentType;
    if (fileContentType == null) {
      if (fileName != null) {
        fileContentType = URLConnection.guessContentTypeFromName(fileName);
      }
      if (fileContentType == null) {
        fileContentType = "application/octet-stream";
      }
    }
    return "Content-Type: " + fileContentType;
  }
}
