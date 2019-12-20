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

import static lombok.AccessLevel.PRIVATE;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;

import feign.codec.EncodeException;
import lombok.experimental.FieldDefaults;
import lombok.val;

/**
 *
 * @author Artem Labazin
 */
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class ManyFilesWriter extends AbstractWriter {
  private static Iterable<?> toIterable (Object value) {
    if (value instanceof File[]) {
      return Arrays.asList((File[]) value);
    }
    if (value instanceof Iterable) {
      return (Iterable<?>) value;
    }
    return null;
  }

  SingleFileWriter fileWriter = new SingleFileWriter();

  @Override
  public boolean isApplicable (Object value) {
    val iterable = toIterable(value);
    if (iterable == null) {
      return false;
    }
    val iterator = iterable.iterator();
    return iterator.hasNext() && fileWriter.isApplicable(iterator.next());
  }

  @Override
  public void write (Output output, String boundary, String key, Object value) throws EncodeException {
    val iterable = toIterable(value);
    if (iterable == null) {
      throw new IllegalArgumentException(String.valueOf(value));
    }
    for (val file : iterable) {
      fileWriter.write(output, boundary, key, file);
    }
  }

  @Override
  public int length (Charset charset, String boundary, String key, Object value) {
    val iterable = toIterable(value);
    if (iterable == null) {
      throw new IllegalArgumentException(String.valueOf(value));
    }
    int result = 0;
    for (val file : iterable) {
      result += fileWriter.length(charset, boundary, key, file);
    }
    return result;
  }
}
