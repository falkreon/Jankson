/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Falkreon (Isaac Ellingson)
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

package blue.endless.jankson.api.document;

import java.util.Objects;

public class Configurations {

	public static boolean markDefaults(DocumentElement defaultConfig, DocumentElement config, String versionKey) {
		if (versionKey!=null) {
			if (defaultConfig instanceof ObjectElement obj) {
				Object defaultVersion = obj.get(versionKey);
				if (config instanceof ObjectElement configObj) {
					Object configVersion = configObj.get(versionKey);
					if (Objects.equals(defaultVersion, configVersion)) {
						//We have a version match. Apply the defaults
						markDefaultsInternal(defaultConfig, config);
						return true;
					} else {
						//Version mismatch, none of the defaults may be applied against this config
						return false;
					}
				} else {
					//Because config is not an object, it does not match a versioned schema.
					return false;
				}
				
			} else {
				//we've been supplied a versionKey, but the root element isn't an Object, so we have a basic schema failure
				throw new IllegalArgumentException("If a versionKey is supplied, the supplied defaultConfig MUST be an ObjectElement so that the default version can be identified (root defaultConfig element was "+defaultConfig.getClass().getSimpleName()+")");
			}
		}
		
		//Schema is not checked, apply defaults
		markDefaultsInternal(defaultConfig, config);
		return true;
	}
	
	private static void markDefaultsInternal(DocumentElement defaultConfig, DocumentElement config) {
		if (defaultConfig instanceof ObjectElement defaultObject) {
			if (config instanceof ObjectElement configObject) {
				
			}
		}
		
		
		if (defaultConfig.getClass().equals(config.getClass())) {
			
		} else {
			//Schema mismatch
			return;
		}
	}
	
	public static void pruneUnusedKeys(DocumentElement template, DocumentElement config) {
		
	}
	
	/**
	 * Replaces comments and values which were marked as defaults, with updated contents from the supplied template
	 * document. This can be helpful if default values are tweaked, or if config comments are tweaked to give a better
	 * understanding of a config key
	 * @param template
	 * @param config
	 */
	public static void updateDefaults(DocumentElement template, DocumentElement config) {
		
	}
}
