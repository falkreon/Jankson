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

package blue.endless.jankson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Runs in a fresh JVM so every scenario exercises cold class initialization. */
public final class OptionsInitializationProbe {
	public static void main(String[] args) throws Exception {
		String prefix = "blue.endless.jankson.api.io.json.";
		String[] names = {"JsonReaderOptions", "JsonReaderOptions$Builder", "JsonWriterOptions", "JsonWriterOptions$Builder", "JsonFormat"};
		int order = Integer.parseInt(args[0]);
		CountDownLatch ready = new CountDownLatch(names.length), start = new CountDownLatch(1);
		AtomicReference<Throwable> failure = new AtomicReference<>();
		List<Thread> threads = new ArrayList<>();
		for (int i = 0; i < names.length; i++) {
			String name = prefix + names[(i + order) % names.length];
			Thread thread = new Thread(() -> {
				ready.countDown();
				try {
					start.await();
					Class<?> type = Class.forName(name);
					if (name.endsWith("$Builder")) type.getMethod("build").invoke(type.getConstructor().newInstance());
					else if (type.isEnum()) {
						for (Object format : type.getEnumConstants()) {
							type.getMethod("readerOptions").invoke(format);
							type.getMethod("writerOptions").invoke(format);
						}
					} else type.getField(name.endsWith("JsonReaderOptions") ? "UNSPECIFIED" : "DEFAULTS").get(null);
				} catch (Throwable error) { failure.compareAndSet(null, error); }
			});
			threads.add(thread); thread.start();
		}
		ready.await(); start.countDown();
		for (Thread thread : threads) thread.join();
		if (failure.get() != null) throw new AssertionError(failure.get());
	}
}
