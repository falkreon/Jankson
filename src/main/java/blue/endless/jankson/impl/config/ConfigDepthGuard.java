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

package blue.endless.jankson.impl.config;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

import blue.endless.jankson.api.SyntaxError;
import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.KeyValuePairElement;
import blue.endless.jankson.api.document.ObjectElement;
import blue.endless.jankson.api.document.ValueElement;
import blue.endless.jankson.api.io.StructuredData;
import blue.endless.jankson.api.io.StructuredDataReader;
import blue.endless.jankson.api.io.StructuredDataWriter;

/** Configuration pipeline limits. The root (including a primitive root) has depth zero. */
public final class ConfigDepthGuard {
	public static final int MAX_DEPTH = 256;

	private ConfigDepthGuard() {}

	private static void checkDepth(int depth) throws IOException {
		if (depth > MAX_DEPTH) throw new IOException("Configuration nesting exceeds " + MAX_DEPTH);
	}

	/** Checks each value before the tree writer can allocate or recursively dispatch it. */
	public static void transfer(StructuredDataReader reader, StructuredDataWriter writer)
			throws IOException, SyntaxError {
		ArrayDeque<Set<String>> keys = new ArrayDeque<>();
		while (reader.hasNext()) {
			StructuredData data = reader.next();
			switch (data.type()) {
				case OBJECT_START, ARRAY_START -> {
					checkDepth(keys.size());
					keys.push(new HashSet<>());
				}
				case PRIMITIVE -> checkDepth(keys.size());
				case OBJECT_END, ARRAY_END -> keys.pop();
				case OBJECT_KEY -> {
					if (!keys.peek().add((String) data.value())) {
						throw new IOException("Duplicate configuration key: " + data.value());
					}
				}
				default -> {}
			}
			writer.write(data);
		}
	}

	/** Checks depth and duplicate keys while enforcing a positive non-EOF event limit. */
	public static void transfer(StructuredDataReader reader, StructuredDataWriter writer, long maxEvents)
			throws IOException, SyntaxError {
		if (maxEvents <= 0) throw new IllegalArgumentException("maxEvents must be positive");
		transfer(reader, new StructuredDataWriter() {
			private long remaining = maxEvents;

			@Override public void write(StructuredData data) throws IOException, SyntaxError {
				if (data.type() != StructuredData.Type.EOF) {
					if (remaining == 0) throw new IOException("Configuration events exceed " + maxEvents);
					remaining--; // Check before decrementing, even for Long.MAX_VALUE budgets.
				}
				writer.write(data);
			}
		});
	}

	/** Iterative, path-local identity tracking accepts DAGs but rejects back edges. */
	public static void checkDocument(ValueElement root) throws IOException {
		ArrayDeque<Frame> stack = new ArrayDeque<>();
		IdentityHashMap<ValueElement, Boolean> active = new IdentityHashMap<>();
		IdentityHashMap<ValueElement, Integer> heights = new IdentityHashMap<>();
		ValueElement value = root;
		while (true) {
			checkDepth(stack.size());
			if (value == null) throw new IOException("Null configuration document value");
			if (value instanceof ObjectElement || value instanceof ArrayElement) {
				Integer height = heights.get(value);
				if (height != null) {
					checkDepth(stack.size() + height);
					if (!stack.isEmpty()) stack.peek().include(height);
				} else {
					if (active.put(value, Boolean.TRUE) != null) throw new IOException("Cyclic configuration document");
					stack.push(new Frame(value));
				}
			} else if (!stack.isEmpty()) {
				stack.peek().include(0);
			}
			while (!stack.isEmpty() && !stack.peek().children.hasNext()) {
				Frame complete = stack.pop();
				active.remove(complete.value);
				heights.put(complete.value, complete.height);
				if (!stack.isEmpty()) stack.peek().include(complete.height);
			}
			if (stack.isEmpty()) return;
			value = stack.peek().next();
		}
	}

	private static final class Frame {
		final ValueElement value;
		final Iterator<?> children;
		final Set<String> keys = new HashSet<>();
		int height;

		void include(int childHeight) { height = Math.max(height, childHeight + 1); }

		Frame(ValueElement value) {
			this.value = value;
			children = value instanceof ObjectElement object ? object.iterator() : ((ArrayElement) value).iterator();
		}

		ValueElement next() throws IOException {
			Object child = children.next();
			if (child instanceof KeyValuePairElement entry) {
				if (!keys.add(entry.getKey())) throw new IOException("Duplicate configuration key: " + entry.getKey());
				return entry.getValue();
			}
			return (ValueElement) child;
		}
	}
}
