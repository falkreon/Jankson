/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Falkreon (Isaac Ellingson)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package blue.endless.jankson.api.config;

import java.io.IOException;
import java.nio.file.Path;
import blue.endless.jankson.api.io.json.JsonFormat;

/** Contextual failure. Parser locations remain available in the cause chain. */
public class ConfigFileException extends IOException {
	private final Path path;
	private final JsonFormat format;
	private final ConfigStage stage;

	public ConfigFileException(Path path, JsonFormat format, ConfigStage stage, Throwable cause) {
		super(stage + " failed for " + path + " (" + format + ")", cause);
		this.path = path;
		this.format = format;
		this.stage = stage;
	}

	public Path path() { return path; }
	public JsonFormat format() { return format; }
	public ConfigStage stage() { return stage; }
}
