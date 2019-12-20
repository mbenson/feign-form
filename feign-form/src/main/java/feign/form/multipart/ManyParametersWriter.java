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

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import feign.codec.EncodeException;
import lombok.experimental.FieldDefaults;
import lombok.val;

/**
 *
 * @author Artem Labazin
 */
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class ManyParametersWriter extends AbstractWriter {
  private static class PrimitiveArrayIterator implements Iterator<Object> {
    Object array;
    int len;
    int pos;

    PrimitiveArrayIterator(Object array) {
      this.array = array;
      len = Array.getLength(array);
    }

    @Override
    public boolean hasNext () {
      return pos < len;
    }

    @Override
    public Object next () {
      if (pos >= len) {
        throw new NoSuchElementException();
      }
      return Array.get(array, pos++);
    }

    @Override
    public void remove () {
      throw new UnsupportedOperationException();
    }
  }

  private static Iterable<?> toIterable (final Object value) {
    if (value instanceof Iterable) {
      return (Iterable<?>) value;
    }
    if (value != null && value.getClass().isArray()) {
      final int sz = Array.getLength(value);
      if (sz == 0) {
        return null;
      }
      if (value instanceof Object[]) {
        return Arrays.asList((Object[]) value);
      }
      return new Iterable<Object>() {
        @Override
        public Iterator<Object> iterator () {
          return new PrimitiveArrayIterator(value);
        }
      };
    }
    return null;
  }

  SingleParameterWriter parameterWriter = new SingleParameterWriter();

  @Override
  public boolean isApplicable (Object value) {
    val iterable = toIterable(value);
    if (iterable == null) {
      return false;
    }
    val iterator = iterable.iterator();
    return iterator.hasNext() && parameterWriter.isApplicable(iterator.next());
  }

  @Override
  public void write (Output output, String boundary, String key, Object value) throws EncodeException {
    val iterable = toIterable(value);
    if (iterable == null) {
      throw new IllegalArgumentException(String.valueOf(value));
    }
    for (val object : iterable) {
      parameterWriter.write(output, boundary, key, object);
    }
  }

  @Override
  public int length (Charset charset, String boundary, String key, Object value) {
    val iterable = toIterable(value);
    if (iterable == null) {
      throw new IllegalArgumentException(String.valueOf(value));
    }
    int result = 0;
    for (val object : iterable) {
      result += parameterWriter.length(charset, boundary, key, object);
    }
    return result;
  }
}
