package blue.endless.jankson.api;

/**
 * Thrown when a Marshaller is unable to reconcile source data with a target Type
 */
public class MarshallerException extends Exception {
	private static final long serialVersionUID = 1L;

	public MarshallerException() {
		super();
	}
	
	public MarshallerException(String message) {
		super(message);
	}
	
	public MarshallerException(Throwable cause) {
		super(cause);
	}
	
	public MarshallerException(String message, Throwable cause) {
		super(message, cause);
	}
}
