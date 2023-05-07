# Jankson
JSON / HJSON parser and preprocessor which preserves ordering and comments


Official Discord: https://discord.gg/tV6FYXE8QH

## Json Quirks

The full set of JSON5 quirks are supported:
```json5
{
  // comments
  unquoted: 'and you can quote me on that',
  singleQuotes: 'I can use "double quotes" here',
  lineBreaks: "Look, Mom! \
No \\n's!",
  hexadecimal: 0xdecaf,
  leadingDecimalPoint: .8675309, andTrailing: 8675309.,
  positiveSign: +1,
  trailingComma: 'in objects', andIn: ['arrays',],
  "backwardsCompatible": "with JSON",
}
```

The following hjson quirks are supported:

```hjson
{
  # use #, // or /**/ comments,
  // omit quotes for keys
  key: 1,
  // omit commas at the end of a line
  cool: {
    foo: 1
    bar: 2
  }
  // allow trailing commas
  list: [
    1,
    2,
  ]
}

```

The following hjson quirks are **NOT** supported:
```hjson
{
	// omit quotes for strings - these will never be supported by jankson
	// this is because other quirks require parsing out unquoted line text
	contains: everything on this line

	// use multiline strings - support is planned but isn't complete yet.
	realist:
	    '''
	    My half empty glass,
	    I will fill your empty half.
	    Now you are half full.
	    '''
}
```

The following supported quirks are unique to jankson:
```json5
{
	//Missing commas are fine anywhere
	key: 1 key2: 2 key3: 3
	items: [4 3 2 1 6 2 {foo: 'cool'} false]


}
```

## Compiling

NOTE: This artifact isn't on MavenCentral yet! Check the 1.8 branch or [the docs](https://falkreon.github.io/Jankson/)
for code you can get from MavenCentral right now.

```groovy
repositories {
	mavenCentral()
}

dependencies {
	"blue.endless:jankson:2.0.0"
}
```

## Using
Jankson is, for the most part, a drop-in replacement for Gson or HJson, but can also be
used as a preprocessor to fix quirks and strip comments, rebaking it into standard JSON
syntax for another parser to consume.

```java
	try {
		// configObject will represent the document root of the config file, and contains comments and formatting
		// that can be used to recreate the file with some minor formatting and indentation cleanup.
		ObjectElement configObject = Jankson.loadJsonObject(new File(configPath, "config.json"));
		
		
		String json5 = configObject.toString(); // toString for any JsonElement is its serialized form
		
		
		// Asking the writer to use STRICT json allows you to use Jankson as a preprocessor for other libraries
		StringWriter stringWriter = new StringWriter();
		JsonWriter jsonWriter = new JsonWriter(stringWriter, JsonWriterOptions.STRICT);
		configObject.write(jsonWriter);
		stringWriter.flush();
		String strictJson = stringWriter.toString(); //strictJson is your preprocessed data
		
	} catch (IOException ex) {
		log.error("Couldn't read the config file", ex);
		return;
	} catch (SyntaxError error) {
		log.error(error); // Stack traces printed or logged will be enhanced with line numbers
		return;
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
