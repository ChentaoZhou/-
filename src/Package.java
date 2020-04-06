import java.io.Serializable;
/**
 * This is the class which implements Serializable 
 * used for communication between Server and Client.
 * **/
public class Package implements Serializable{
	
	private Serializable object;	//Contain the data receiver will use.It can be any type of object
	
	private String messageType;		//this attribute notify receiver that which kind of package it is
	/**
	 * messageTypes: when receiver get the Package object, it will know what to do based on the messageType attribute 
	 * 
	 * 21REWARD:			(only for nature 21 winner)let him know how many stacks he get
	 * 21LOSE				Notify all players that there is a nature 21 winner, this round over, waiting for another round
	 * ACTIVATE_DEALER:		Activate dealer's buttons(activate dealer's turn for choose another card or standing)
	 * ALL_PLAYER_EXPLODE	(only for dealer)after all player exploded, game stop, dealer win
	 * CARDS:				Notify clients at the beginning, there are two cards are sending to them
	 * CARD:				Notify the client that package contains another new Card it request.
	 * DEALER				Server send to client to notify now is the stage to choose a Dealer
	 * DEALER_LABEL:		Used for notify client he is dealer and show mark on its swing
	 * DEALER_MESSAGE:		Specific message for dealer
	 * DEALER_EXPLODE:		(only for dealer)after dealer exploded, game stop, dealer give all surviver one stack
	 * DRAW					(only for ordinary player)Player's points equals to dealer's points
	 * DEALER_RESULT		(only for dealer)send the final stack change of dealer, dealer change its scores based on it.
	 * EXPLOSION			Client send to sever say its cards point over 21, bust.
	 * GAME_STATE			Server send to client to change the stateLabel information
	 * INITIATE				Server send to client: notify client to reset the attribute and wite a new round
	 * LOSE:				(only for ordinary player)Player's points is less than dealer, lose this round
	 * MESSAGE				Server send to client to change the resultLabel information
	 * MORECARD				Call Server's dealOneCard method when client ask for another card
	 * MUTI_21PLAYERS		Server send to Client: there are more than 1 nature vingt-un, no one wins
	 * PLAYER				New client registers,create a new player to list by the received name
	 * PLAYER_EXPLODE:		If ordinary player exploded, send message to dealer, dealer get one stack
	 * QUERY				Ask ordinary players if they want another card
	 * QUIT:				Notify the server that this client leave the game
	 * ROUND_OVER			Receive a "start a new round" package from Client
	 * STAND				Received the player's choice of no more CARDS
	 * VINGT-UN             When someone get nature 21, game stop, everyone give him 2 stacks if there is no another nature 21
	 * WIN_DEALER_EXPLODED	(only for ordinary player)player get one stack after dealer exploded
	 * WIN:					(only for ordinary player)Player win /or after dealer exploded or lost,all players still in game get one stack
	 * 
	 * 
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
