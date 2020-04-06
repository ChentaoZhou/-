import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
/** the Game Server **/
public class Server implements Runnable {

	/**
	 * inner class ClientHandler which used for receiving message from each client
	 * thread and response to these revived messages.
	 **/
	private class ClientHandler implements Runnable {
		private Socket client;
		private Server parent = null;
		private ObjectInputStream inputStream = null;
		private ObjectOutputStream outputStream = null;
		private String name;
		private ArrayList<Card> handCards = new ArrayList<Card>();
		private boolean isDealer;
		private boolean isNature21Winner;

		// constructor of inner class
		public ClientHandler(Socket client, Server parent) {
			this.client = client;
			this.parent = parent;
			try {
				outputStream = new ObjectOutputStream(this.client.getOutputStream());
				inputStream = new ObjectInputStream(this.client.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * This part is used to receive package from each client thread
		 * and give corresponding reactions.
		 * **/
		public void run() {
			try {
				Package p = null;
				while ((p = (Package) inputStream.readObject()) != null) {
					//if a player send a "QUIT" package, Server will delete him in this game.
					if (p.getMessageType().equals("QUIT")) {
						ArrayList<ClientHandler> deleteArray = new ArrayList<ClientHandler>();
						deleteArray.add(this);
						if(this.isDealer == true) {
							parent.dealer = null;
							this.isDealer = false;
						}
						parent.quit(deleteArray);
						int number = clients.size()+ waitingClients.size();
						if(number<2) {
							view.addText("There is not enough players to start a game\n");
							view.getStartButton().setEnabled(false);
							view.getStartLabel().setText("at least 2 players to start");
						}
					}
					//call Server's dealOneCard method when client ask for another card.
					if (p.getMessageType().equals("MORECARD")) {
						parent.dealOneCard(this);
					}
					/**
					 * received the player's choice of no more CARDS, and every time 
					 * judges whether all the players except the dealer have chosen stand
					 * **/
					if (p.getMessageType().equals("STAND")) {
						if (this.isDealer == true) {
							// Dealer chooses to stand, enter the settlement stage.
							int dealerPoint = PointCounter.returnPoint(this.handCards);
							
							//compares the score of other players' cards to three situations
							for(ClientHandler client:clients) {
								if(!client.isDealer) {
									int playerPoint = PointCounter.returnPoint(client.handCards);
									if(playerPoint < dealerPoint) {
										parent.losers++;		//How many stacks should the Dealer add
										client.send(new Package("LOSE",""));
									}else if(playerPoint > dealerPoint) {
										parent.winners++;		//How many stacks should the Dealer reduce
										client.send(new Package("WIN",""));
									}else {
										client.send(new Package("DRAW",""));
									}
									
								}
							}
							int stackChange = losers - winners;
							view.addText("This round over, within rest players, dealer earn: "+stackChange+"\n");
							this.send(new Package("DEALER_RESULT",stackChange));	//sends the dealer's score back to the client side
						}else {
							view.addText(this.name + " choose to stand" + "\n");
							parent.standCounter++;
							if (standCounter >= clients.size() - 1) { // after all ordinary client stand, start dealer's turn
								parent.activateDealer();
							}
						}
					}
					//receive the bust (over 21) package, put this bust clientThread into waiting list, delete the standCounter
					if (p.getMessageType().equals("EXPLOSION")) {
						
						if (this.isDealer == true) {
							//if dealer bust(over21), all live players get one stack, this round over.(dealer does not into waiting list)
							parent.dealerExplode();
							parent.startNewRound();
							
						} else {		//if ordinary players bust, execute this bust code
							ArrayList<ClientHandler> loser = new ArrayList<ClientHandler>();
							loser.add(this);
							clients.removeAll(loser);
							waitingClients.add(this);
							view.refreshInandWait(clients.size(), waitingClients.size());
							view.addText(this.name + " loses this round,in waiting list-------" + "\n");
							parent.standCounter++;
							view.addText("standCounter: "+standCounter+"\n");
							view.addText("clients.size() "+clients.size()+"\n");
							if (standCounter >= clients.size()) { // if all players bust, into dealer's turn and this round over soon
								view.addText("Dealer's Buttons are activated."+"\n");
								parent.activateDealer();
							}
							//add one stack to dealer if one player bust.
							for(ClientHandler client:clients) {
								if(client.isDealer == true) {
									client.send(new Package("PLAYER_EXPLODE",this.name));
									view.addText(this.name +" pay one stack to dealer"+"\n");
								}
							}
							
							//if all ordinary bust, dealer wins and waiting for a new round
							if(clients.size()<=1) {
								for(ClientHandler client:clients) {
									if(client.isDealer == true) {
										client.send(new Package("ALL_PLAYER_EXPLODE",""));
										view.addText("All palyer lose, dealer win.");
									}else {
										client.send(new Package("GAME_STATE","You lose"));
										client.send(new Package("MESSAGE","Waiting for another round."));
									}
								}
								//start a new round
								parent.startNewRound();
							}
						}
					}
					//nature vingt-un appear, Server returns twice the total number of current players to the player (not including the player himself)
					if(p.getMessageType().equals("VINGT-UN")) {
						parent.nature21Winner = this;
						this.isNature21Winner = true;
						nature21Players++;
						view.addText("first 21: "+nature21Players);
						if(nature21Players <= 1) {
							int reward = 2*(clients.size()-1);
							this.send(new Package("21REWARD",reward));
							view.addText(this.name+" get nature 21, big winner. get: "+(reward-2)+"\n");
							//send all rest players package, notify them nature vingt-un, reduce 2 stacks, get into waiting stage
							for(ClientHandler client:clients) {
								if(!client.isNature21Winner) {
									client.send(new Package("21LOSE",this.name));
									client.send(new Package("RESET_DEALER",""));	//the rest players all disable dealerLabel
									client.isDealer = false;
								}
							}
							
							//set this nature vingt-un the new dealer
							this.isDealer = true;
							parent.dealer = this;
							this.send(new Package("DEALER_LABEL",""));	//activate the dealerLabel of new dealer
						}else {
							//there are more than 1 nature vingt-un, no one wins and no one need to pay,dealer does not change
							for(ClientHandler client:clients) {
								client.send(new Package("MUTI_21PLAYERS",""));
							}
							view.addText("More than one nature 21 winner appear, this round over");
						}
					}
					//receive a "start a new round" package
					if(p.getMessageType().equals("ROUND_OVER")) {
						parent.startNewRound();
					}
					
					
					
				}
				inputStream.close();
			} catch (SocketException e) {

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		/**
		 * this is a method used for sending package to client
		 **/
		public void send(Package p) {
			try {
				outputStream.writeObject(p);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static ServerSocket server;
	private static ArrayList<Card> deck;
	private ArrayList<ClientHandler> clients = new ArrayList<ClientHandler>();
	private ArrayList<ClientHandler> waitingClients = new ArrayList<ClientHandler>();
	private ServerView view;
	private ClientHandler dealer;
	private int standCounter = 0; // count how many players stand
	private int losers = 0;			//only used to calculate players with lower scores than dealers in normal games (players not included in the bust).
	private int winners = 0;
	private int nature21Players = 0;
	private ClientHandler nature21Winner;
	private final static int PORT = 8888; 

	// constructor
	public Server() {
		view = new ServerView(this);
		try {
			server = new ServerSocket(PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		while (true) {
			Socket clientSocket = null;
			try {
				clientSocket = server.accept();
				System.out.println("New client connected");
				view.addText("New client connected" + "\n");
				ClientHandler client = new ClientHandler(clientSocket, this);
				// this is a step to compulsory register client first
				Package p;
				while ((p = (Package) client.inputStream.readObject()) != null) {
					// new client registers,create a new player to list by the received name.
					if (p.getMessageType().equals("PLAYER")) {
						client.name = (String) p.getObject();
						this.waitingClients.add(client);
						view.addText(client.name + " has joined in" + "\n");
						view.getWaitPlayerLabel().setText(waitingClients.size() + " players in waiting list");
						Package waitp = new Package("GAME_STATE", "Please waiting for a new round");
						client.send(waitp);
						
						
						//if players less then 2, game cannot start
						int number = clients.size()+waitingClients.size();
						if(number>1) {
							view.getStartButton().setEnabled(true);
							view.getStartLabel().setText("Ready to start");
						}
					}
					break;
				}
				new Thread(client).start();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * method for removing target clientHandler must use this to remove client from
	 * ArrayList
	 **/
	public void quit(ArrayList<ClientHandler> a) {
		clients.removeAll(a);
		waitingClients.removeAll(a);
		view.refreshInandWait(clients.size(), waitingClients.size());
		view.addText(a.get(0).name + " leave the game-------" + "\n");
	}


	
	/**
	 * start a new round, initiate all players' state
	 * **/
	public void startNewRound() {
		standCounter = 0;
		losers = 0;
		winners = 0;
		nature21Players = 0;
		nature21Winner = null;
		for(ClientHandler client:clients) {
			client.isNature21Winner = false;
			client.handCards.clear();
			client.send(new Package("INITIATE",""));
		}
		for(ClientHandler client:waitingClients) {
			client.isNature21Winner = false;
			client.handCards.clear();
			client.send(new Package("INITIATE",""));
		}
		
		view.getStartButton().setEnabled(true);
		
	}

	/**
	 * initiate another round of game: add clients in waiting list to in game list.
	 **/
	public void initiate() {
		for (ClientHandler client : waitingClients) {
			clients.add(client);
		}
		waitingClients.clear();
		view.refreshWait(waitingClients.size());
		view.refreshIn(clients.size());
		view.addText("----Game will start soon!----" + "\n");
		for (ClientHandler client : clients) {
			Package p = new Package("GAME_START", "Game Start!");
			client.send(p);
		}
	}

	/**
	 * in the start of an round, Server need to select a dealer if there is no
	 * dealer by the allocated card. 
	 **/
	public void findDealer() {
		createDeck();
		outer: while (true) {
			for (ClientHandler client : clients) {
				Card c = deck.remove(0); 
				Package p = new Package("DEALER", c);
				client.send(p);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				view.addText(client.name + " draw: " + c.getName() + "\n");
				if (c.getName().equals("A")) {
					dealer = client;
					client.isDealer = true;
					view.addText("Dealer has been confirmed: " + client.name + "\n" + "------------------" + "\n");

					// send message to each client and notify them there is a dealer
					for (ClientHandler cli : clients) {
						cli.send(new Package("GAME_STATE", "Dealer is: " + client.name));
					}
					client.send(new Package("GAME_STATE", "You are the dealer this round!"));
					client.send(new Package("DEALER_LABEL", "Dealer"));
					break outer;
				}
			}
		}
	}

	/**
	 * get all cards from card.txt and store them in a ArrayList deck 
	 **/
	public void createDeck() {
		FileReader fr;
		Scanner s = null;
		deck = new ArrayList<Card>();
		try {
			fr = new FileReader("card.txt");
			s = new Scanner(fr);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		while (s.hasNextLine()) {
			String line = s.nextLine();
			String[] info = line.trim().split("\\s+");
			Card card = new Card(info[0], Integer.parseInt(info[1]));
			deck.add(card);
		}
		Collections.shuffle(deck);
	}

	/**
	 * this is a method used for dealing two cards at first round
	 **/
	public void dealCard() {
		// create the deck first
		createDeck();

		// deal card to players, two round of card deal.
		for (ClientHandler client : clients) {
			Card c = deck.remove(0);
			client.handCards.add(c);
			view.addText(client.name + " has draw: " + c.getName() + "\n");
			try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}
			Package p = new Package("CARDS", c);
			client.send(p);

		}
		for (ClientHandler client : clients) {
			Card c = deck.remove(0);
			client.handCards.add(c);
			view.addText(client.name + " has draw: " + c.getName() + "\n");
			try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}
			Package p = new Package("CARDS", c);
			client.send(p);

		}
		//after card deal, if there is no nature vingt-un, ask players if they need another card or stand.
		try {Thread.sleep(500);} catch (InterruptedException e1) {e1.printStackTrace();}
		view.addText("nature21Player: "+nature21Players+"\n");
		if(nature21Players<1) {
		for (ClientHandler client : clients) {
			try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}
			Package p = new Package("GAME_STATE", "----Game is running----");
			client.send(p);
			if (dealer == client) {
				client.send(new Package("DEALER_MESSAGE", "Waiting other players to choose"));//let dealer wait other players to choose
			} else {
				Package p1 = new Package("QUERY", "Choose one more Card or Stand "); //ask ordinary players
				client.send(p1);
			}
		}
		}else {
			startNewRound();
		}
	}

	/**
	 * method for dealing one card if player request.
	 **/
	public void dealOneCard(ClientHandler client) {
		synchronized(this) {
		Card card = deck.remove(0);
		client.handCards.add(card);
		view.addText(client.name + " ask for another card, draw: " + card.getName() + "\n");
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		client.send(new Package("CARD", card));
		}
	}

	/**
	 * Activate dealers turn when other players all have chosen stand
	 **/
	public void activateDealer() {
		dealer.send(new Package("ACTIVATE_DEALER", ""));
	}

	/**
	 * dealer bust(cards over 21), this round over, give one stack to live players
	 * **/
	public void dealerExplode() {
		winners = clients.size()-1;				//dealer does not move to waiting list, need to count dealer here
		view.addText("Dealer exploded, survivers get a stack!!!"+"\n");
		view.addText("There are: "+winners+" winners");
		for(ClientHandler client: clients) {
			if(client.isDealer == true) {
				client.send(new Package("DEALER_EXPLODE",winners)); 
			}else {
				client.send(new Package("WIN_DEALER_EXPLODED",""));
			}
		}
	}
	
	
	
	
	
	
	
	

	public ClientHandler getDealer() {
		return dealer;
	}

	public static void main(String[] args) {
		Thread t = new Thread(new Server());
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
