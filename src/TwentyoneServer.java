
/**
 * 未解决的问题：1.同名的client
 * 接下来要做的事情：	庄家在所有玩家都爆了之后要结束游戏
 * 					
 * 					然后庄家点了不再要牌之后，清算比自己分高的有多少，分低的有多少，相应的减去分数。
 * **/
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

public class TwentyoneServer implements Runnable {

	/**
	 * inner class ClientHandler which used for receiving message from each client
	 * thread and response to these revived messages.
	 **/
	private class ClientHandler implements Runnable {
		private Socket client;
		private TwentyoneServer parent = null;
		private ObjectInputStream inputStream = null;
		private ObjectOutputStream outputStream = null;
		private String name;
		private ArrayList<Card> handCards = new ArrayList<Card>();
		private boolean isDealer;

		// constructor of inner class
		public ClientHandler(Socket client, TwentyoneServer parent) {
			this.client = client;
			this.parent = parent;
			try {
				outputStream = new ObjectOutputStream(this.client.getOutputStream());
				inputStream = new ObjectInputStream(this.client.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// z这里写：收消息，收到之后做什么
		public void run() {
			try {
				Package p = null;
				while ((p = (Package) inputStream.readObject()) != null) {
					if (p.getMessageType().equals("QUIT")) {
						ArrayList<ClientHandler> deleteArray = new ArrayList<ClientHandler>();
						deleteArray.add(this);
						parent.quit(deleteArray);
					}
					if (p.getMessageType().equals("MORECARD")) {
						parent.dealOneCard(this);
					}
					// z收到了玩家不再要牌的选择，每次都判断是不是所有除了dealer的玩家都已经选择了stand
					if (p.getMessageType().equals("STAND")) {
						if (this.isDealer == true) {

							// z庄家选择不再要牌，进入结算阶段。

						}
						view.addText(this.name + " choose to stand" + "\n");
						parent.standCounter++;
						if (standCounter >= clients.size() - 1) { // 其他玩家都不再要牌，开始庄家的回合
							parent.activateDealer();
						}
					}
					// 收到了玩家手牌爆炸的消息，把玩家删除这轮游戏放进waiting list,同时要减去standCounter（爆掉相当于stand）
					if (p.getMessageType().equals("EXPLOSION")) {
						
						if (this.isDealer == true) {
							// 如果是庄家爆炸，该轮结束，所有玩家获得一个筹码，庄家不用进入waiting list
							parent.dealerExplode();
							
						} else {		//如果是普通玩家则进入这个爆炸代码
							ArrayList<ClientHandler> loser = new ArrayList<ClientHandler>();
							loser.add(this);
							clients.removeAll(loser);
							waitingClients.add(this);
							view.refreshInandWait(clients.size(), waitingClients.size());
							view.addText(this.name + " loses this round,in waiting list-------" + "\n");
							parent.standCounter++;
							if (standCounter >= clients.size() - 1) { // 其他玩家都爆炸或者stand后，开始庄家的回合
								parent.activateDealer();
							}
							//此代码为给庄家加上一个筹码
							for(ClientHandler client:clients) {
								if(client.isDealer == true) {
									client.send(new Package("PLAYER_EXPLODE",this.name));
									view.addText(this.name +" pay one stack to dealer");
								}
							}
						}
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

	// z这里开始server主类
	private static ServerSocket server;
	private static ArrayList<Card> deck;
	private ArrayList<ClientHandler> clients = new ArrayList<ClientHandler>();
	private ArrayList<ClientHandler> waitingClients = new ArrayList<ClientHandler>();
	private ServerView view;
	private ClientHandler dealer;
	private int standCounter = 0; // count how many players stand,计算多少玩家选择stand
	private int losers = 0;			//只用于计算正常对局下比dealer分数低的玩家（不计入爆炸的玩家）
	private int winners = 0;

	// constructor
	public TwentyoneServer() {
		view = new ServerView(this);
		try {
			server = new ServerSocket(8888);
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
						Package waitp = new Package("GAME_STATE", "Previous round is still going, please wait..");
						client.send(waitp);
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
	 * method for finding the target clientHandler 此方法暂时还未被使用到
	 **/
	public ClientHandler findClient(String name) {
		for (ClientHandler client : clients) {
			if (client.name.equals("name"))
				return client;
		}
		for (ClientHandler client : waitingClients) {
			if (client.name.equals("name"))
				return client;
		}
		return null;

	}

	/**
	 * initiate another round of game: add clients in waiting list to in game list.
	 * 开始新一轮游戏，所有waiting列表中的玩家被加入游戏
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
			Package p = new Package("GAME_STATE", "Game Start!");
			client.send(p);
		}
	}

	/**
	 * in the start of an round, Server need to select a dealer if there is no
	 * dealer by the allocated card. 在游戏开始前，如果没有dealer，通过发牌选出一个庄家. 这里注意client手牌有所增加
	 **/
	public void findDealer() {
		createDeck();
		outer: while (true) {
			for (ClientHandler client : clients) {
				Card c = deck.remove(0); // 删除了原本需要先加进玩家手牌的操作，所以在对局开始前不需要再清空玩家手牌了
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
	 * get all cards and store them in a ArrayList deck 重新读取所有的卡牌并存入 deck
	 * 这个ArrayList中
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
	 * 这个方法用于在对局刚开始给玩家发两张手牌 注意：这里已经用过createDeck()方法洗过牌
	 **/
	public void dealCard() {
		// create the deck first
		createDeck();

		// 这里分两个循环给玩家发牌
		for (ClientHandler client : clients) {
			Card c = deck.remove(0);
			client.handCards.add(c);
			view.addText(client.name + " has draw: " + c.getName() + "\n");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Package p = new Package("CARDS", c);
			client.send(p);

		}
		for (ClientHandler client : clients) {
			Card c = deck.remove(0);
			client.handCards.add(c);
			view.addText(client.name + " has draw: " + c.getName() + "\n");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Package p = new Package("CARDS", c);
			client.send(p);

		}
		// z在这里发完了牌，询问是否要额外加牌，或是stand
		for (ClientHandler client : clients) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Package p = new Package("GAME_STATE", "----Game is processing----");
			client.send(p);
			if (dealer == client) {
				client.send(new Package("DEALER_MESSAGE", "Waiting other players to choose"));// 让dealer等待其他玩家选择是否要牌
			} else {
				Package p1 = new Package("QUERY", "One more Card or Stand ?"); // 询问非dealer的玩家是否要牌
				client.send(p1);
			}
		}
	}

	/**
	 * method for dealing one card if player request. 发一张牌给玩家，如果玩家点击了 Request Card
	 **/
	public void dealOneCard(ClientHandler client) {
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

	/**
	 * Activate dealers turn when other players all have chosen stand
	 * 激活庄家的选牌或是stand当其他所有玩家都选择不再要牌
	 **/
	public void activateDealer() {
		dealer.send(new Package("ACTIVATE_DEALER", ""));
	}

	/**
	 * 庄家手牌爆了，这一轮游戏结束，给所有仍在对局中的玩家发一个筹码
	 * **/
	public void dealerExplode() {
		winners = clients.size()-1;				//庄家在爆了之后没有被移入waiting list，所以clients list中仍旧包含庄家
		view.addText("Dealer exploded, survivers get a stack!!!"+"\n");
		view.addText("There are: "+winners+" winners");
		for(ClientHandler client: clients) {
			if(client.isDealer == true) {
				client.send(new Package("DEALER_EXPLODE",winners)); //这里winner是int但是可以编译，应该是自动装箱改为Integer
			}else {
				client.send(new Package("WIN",""));
			}
		}
	}
	
	
	
	
	
	
	

	public ClientHandler getDealer() {
		return dealer;
	}

	public static void main(String[] args) {
		Thread t = new Thread(new TwentyoneServer());
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
