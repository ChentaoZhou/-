import java.io.Serializable;

public class Package implements Serializable{
	private String messageType;
	private Serializable object;
	
	/**
	 * messageTypes: PLAYER; CARD; DEALER; DEALER_MORECARD;STAND; QUIT;GAME_STATE; MESSAGE;
	 * 
	 * DEALER_LABEL:		used for notify client he is dealer and show mark on its swing
	 * CARD:				notify the client that package contains another new Card it request.
	 * CARDS:				notify clients at the beginning, there are two cards are sending to them
	 * QUIT:				notify the server that this client leave the game
	 * DEALER_MESSAGE:		specific message for dealer
	 * ACTIVATE_DEALER:		activate dealer's buttons(activate dealer's turn for choose another card or standing)
	 * PLAYER_EXPLODE:		if ordinary player exploded, send message to dealer, dealer get one stack
	 * WIN_DEALER_EXPLODED	(only for ordinary player)player get one stack after dealer exploded
	 * ALL_PLAYER_EXPLODE：	(only for dealer)after all player exploded, game stop, dealer win
	 * DEALER_EXPLODE:		(only for dealer)after dealer exploded, game stop, dealer give all surviver one stack
	 * WIN:					(only for ordinary player)Player win /or after dealer exploded or lost,all players still in game get one stack
	 * VINGT-UN             when someone get nature 21, game stop, everyone give him 2 stacks if there is no another nature 21
	 * 21REWARD:			(only for nature 21 winner)let him know how many stacks he get
	 * 21LOSE				notify all players that there is a nature 21 winner, this round over, waiting for another round
	 * LOSE:				(only for ordinary player)Player's points is less than dealer, lose this round
	 * DRAW					(only for ordinary player)Player's points equals to dealer's points
	 * DEALER_RESULT		(only for dealer)send the final stack change of dealer, dealer change its scores based on it.
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
