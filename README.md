# [Jankson](https://falkreon.github.io/Jankson)

JSON5 / HJSON parser and preprocessor that preserves ordering and comments

Official Discord: https://discord.gg/tV6FYXE8QH

## [Json Quirks](https://falkreon.github.io/Jankson/quirks)

The full list of JSON5 quirks, and several HJSON quirks are supported.

A full list of supported quirks is available [on the wiki](https://falkreon.github.io/Jankson/quirks)!

## [Compiling](https://falkreon.github.io/Jankson/getting_started)

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

Jankson is, for the most part, a drop-in replacement for [Gson] or [Hjson], but can also be
used as a preprocessor to fix quirks and strip comments, re-baking it into standard JSON
syntax for another parser to consume.

[Gson]:https://github.com/Google/Gson
[Hjson]:https://github.com/hjson/hjson-java

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

## Displaying errors  

In nearly any case where the processor can't accept the input, the SyntaxError subclass is
capable of producing a String which describes both the line and character that the element
started parsing at, and the line and character where the error was discovered.<br>
When presenting a SyntaxError to the user, it's strongly recommended that the stack trace is
omitted, and instead two lines are printed: the exception's `getMessage()`, followed by its
`getLineMessage()`.<br>
This will give the user the most relevant information available about how to fix the problem.
If multiple JSON files are being parsed, it may also be necessary to indicate the name and/or path to the file
so that the problem can be located.
