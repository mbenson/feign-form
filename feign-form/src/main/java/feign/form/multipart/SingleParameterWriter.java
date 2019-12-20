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

import java.nio.charset.CharsetEncoder;

import feign.codec.EncodeException;

/**
 *
 * @author Artem Labazin
 */
public class SingleParameterWriter extends AbstractWriter {
  private static final String CONTENT_DISPOSITION_FORMAT =
      "Content-Disposition: form-data; name=\"%s\"";
  private static final String CONTENT_TYPE_SPECIFICATION = "Content-Type: text/plain; charset=";

  private static final int BASE_LENGTH =
      CONTENT_DISPOSITION_FORMAT.length() - 2 + CONTENT_TYPE_SPECIFICATION.length() + CRLF.length() * 3;

  @Override
  public boolean isApplicable (Object value) {
    return value instanceof Number ||
           value instanceof CharSequence ||
           value instanceof Boolean;
  }

  @Override
  protected void write (Output output, String key, Object value) throws EncodeException {
    output.write(String.format(CONTENT_DISPOSITION_FORMAT, key));
    output.write(CRLF);
    output.write(CONTENT_TYPE_SPECIFICATION);
    output.write(output.getCharset().name());
    output.write(CRLF);
    output.write(CRLF);
    output.write(String.valueOf(value));
  }

  @Override
  protected int length (CharsetEncoder encoder, String key, Object value) {
    return predictByteCount(encoder, BASE_LENGTH + key.length() + encoder.charset().name().length()
        + String.valueOf(value).length());
  }
}
