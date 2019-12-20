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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;

/**
 * Output representation utility class.
 *
 * @author Artem Labazin
 */
@FieldDefaults(level = PRIVATE)
public class Output implements Closeable {
  private static final int DEFAULT_CAPACITY = 1024;

  ByteBuffer buffer;
  boolean open = true;

  @Getter
  final Charset charset;

  /**
   * Create a new {@link Output} with a default size.
   * @param charset to use
   */
  @Deprecated
  public Output(Charset charset) {
    this(charset, DEFAULT_CAPACITY);
  }

  /**
   * Create a new {@link Output}.
   * @param charset to use
   * @param capacity initially allocated
   */
  public Output(Charset charset, int capacity) {
    this.charset = charset;
    ensureCapacity(capacity);
  }

  /**
   * Writes the string to the output.
   *
   * @param string string to write to this output
   *
   * @return this output
   */
  public Output write (String string) {
    return write(string.getBytes(charset));
  }

  /**
   * Writes the byte array to the output.
   *
   * @param bytes byte arrays to write to this output
   *
   * @return this output
   */
  @SneakyThrows
  public Output write (byte[] bytes) {
    ensureOpen();
    if (bytes.length > 0) {
      ensureCapacity(buffer.position() + bytes.length);
      buffer.put(bytes);
    }
    return this;
  }

  /**
   * Writes the byte array to the output with specified offset and fixed length.
   *
   * @param bytes  byte arrays to write to this output
   * @param offset the offset within the array of the first byte to be read. Must be non-negative and no larger than <tt>bytes.length</tt>
   * @param length the number of bytes to be read from the given array
   *
   * @return this output
   */
  @SneakyThrows
  public Output write (byte[] bytes, int offset, int length) {
    ensureOpen();
    if (length > 0) {
      ensureCapacity(buffer.position() + length);
      buffer.put(bytes, offset, length);
    }
    return this;
  }

  /**
   * Returns byte array representation of this output class.
   *
   * @return byte array representation of output
   */
  public byte[] toByteArray () {
    return buffer.array();
  }

  @Override
  public void close () throws IOException {
    synchronized (this) {
      this.open = false;
    }
  }

  private void ensureOpen () {
    synchronized (this) {
      if (!open) {
        throw new IllegalStateException();
      }
    }
  }

  private void ensureCapacity (int i) {
    if (i < 0) {
      throw new OutOfMemoryError();
    }
    synchronized (this) {
      if (buffer == null || buffer.capacity() < i) {
        int capacity;
        if (buffer == null) {
          capacity = i;
        } else {
          capacity = buffer.capacity() << 1;
          if (capacity < 0) {
            capacity = Integer.MAX_VALUE - 8;
          }
          if (capacity < i) {
            capacity = i;
          }
        }
        byte[] content = new byte[capacity];

        int position;
        if (buffer == null) {
          position = 0;
        } else {
          position = buffer.position();
          System.arraycopy(buffer.array(), 0, content, 0, position);
        }
        buffer = ByteBuffer.wrap(content);
        buffer.position(position);
      }
    }
  }
}
