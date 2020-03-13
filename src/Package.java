import java.io.Serializable;

public class Package implements Serializable{
	private String messageType;
	private Serializable object;
	
	/**
	 * messageTypes: PLAYER; CARD; DEALER; DEALER_MORECARD;STAND; QUIT;GAME_STATE; MESSAGE;
	 * DEALER_LABEL:	used for notify client he is dealer and show mark on its swing
	 * CARD:			notify the client that package contains another new Card it request.
	 * CARDS:			notify clients at the beginning, there are two cards are sending to them
	 * QUIT:			notify the server that this client leave the game
	 * DEALER_MESSAGE:	specific message for dealer
	 * ACTIVATE_DEALER:	activate dealer's buttons(activate dealer's turn for choose another card or standing)
	 * **/
	
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
