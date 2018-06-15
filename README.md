# Jankson
JSON / HJSON parser and preprocessor which preserves ordering and comments


## Compiling
Just clone the repo and run a gradle build. The only dependency is jsr305 for nullity.

## Using
Jankson is, for the most part, a drop-in replacement for Gson or HJson, but can also be
used as a preprocessor to fix quirks and strip comments, rebaking it into standard JSON
syntax for another parser to consume.

```java
	try {
		JsonObject configObject = Jankson
			.builder()
			.build()
			.load(new File(configPath, "config.hjson"));
			
		//This will strip comments and regularize the file, but emit newlines and indents for readability
		String processed = configObject.toJson(false, true);
		
		//This will inject a default setting after the last listed key in the object, if it doesn't already exist.
		//Otherwise it does nothing to the comment, ordering, or value.
		configObject.putDefault("someConfigKey", new JsonPrimitive(Boolean.TRUE), "Turns on the someConfigKey thing (default=TRUE)");
		
	} catch (IOException ex) {
		log.error("Couldn't read the config file", ex);
		return; //or System.exit(-1) or rethrow an exception
	} catch (SyntaxError error) {
		log.error(error.getMessage());
		log.error(error.getLineMessage());
		return; //or System.exit(-1) or rethrow an exception
	}
```



This processor produces reliable behavior when encountering many quirks which are normal
for configuration files:

* Comments, normally disallowed in json, are completely legal, inspectable, and preserved
  across re-saves of the file.
  
* Missing or extra commas. These are completely ignored, allowing smaller config file
  diffs when object or array elements are added or removed. This also protects end-users
  from some hard-to-notice syntax errors, and eliminates the need for lengthy restarts
  because the user intent was clear.
  
* Unquoted object keys. This is a very common quirk, and as usual the user intent is very
  clear in these cases.

This processor will reliably produce descriptive errors for certain other quirks:

* Unmatched quotes are completely ambiguous. The amount of text captured may have greatly
  exceeded the size of the intended quotation, possibly even running into the end of the
  stream. This constitutes a macro-structural ambiguity and must be addressed by the user
  
* Unmatched braces are direct structural ambiguities. One might be able to recover the
  user's intent from indentation, but for unknown input where the indentation may have
  been clobbered or minified, we can't assume good faith and must ask the user to clarify.

## Displaying errors  

In nearly any case where the processor can't accept the input, the SyntaxError subclass is
capable of producing a String which describes both the line and character that the element
started parsing at, and the line and character where the error was discovered. When
presenting a SyntaxError to the user, it's strongly reccommended that the stack trace is
ommitted, and instead two lines are printed: the exception's ```getMessage()```, followed by its
```getLineMessage()```. This will give the user the most relevant information available about
how to fix the problem. If multiple json files are being parsed, it may also be necessary
to indicate the name and/or path to the file so that the problem can be located.
