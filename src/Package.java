import java.io.Serializable;

public class Package implements Serializable{
	private String messageType;
	private Serializable object;
	
	//Only contain message
	public Package(String messageType, Serializable object) {
		this.messageType = messageType;
		this.object = object;
	}
	
	
	public Serializable getObject() {
		return object;
	}


	public String getMessageType() {
		return messageType;
	}
	
}
