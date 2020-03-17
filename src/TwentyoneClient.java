import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingWorker;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Font;
/**
 *This is the class of Client(Player) side
 *, which interact with Server and receive cards from server
 * **/
public class TwentyoneClient extends JFrame implements ActionListener {

	/**
	 * inner class used for listening from the server and react with 
	 * corresponding Packages, which is the core interaction part of client in this program
	 * It also extends SwingWorker so it can print the progress of this game in real time
	 **/
	private class ReadWorker extends SwingWorker<Void, Void> {
		private Socket socket = null;
		private ObjectInputStream inputStream = null;
		private TwentyoneClient parent;

		// constructor
		public ReadWorker(Socket socket, TwentyoneClient parent) {
			this.socket = socket;
			this.parent = parent;
			try {
				inputStream = new ObjectInputStream(this.socket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * receive all kinds of packages from server and doing corresponding behaviours
		 **/
		public Void doInBackground() {
			System.out.println("started read worker");
			Package p = null; 
			try {
				//receive a package and decide what to do based on the messageType 
				while ((p = (Package) inputStream.readObject()) != null) {
					//change the words of the stateLabel
					if (p.getMessageType().equals("GAME_STATE")) {
						parent.stateLabel.setText((String) p.getObject());
					}
					//change the words of the resultLabel
					if(p.getMessageType().equals("MESSAGE")) {
						parent.resultLabel.setText((String) p.getObject());
					}
					//notify this is the stage to decide a dealer based on drawing card
					if (p.getMessageType().equals("DEALER")) {
						parent.stateLabel.setText("This is the stage of choosing dealer by card");
						//show the drawn card
						parent.resultLabel.setText("You have draw card: " + ((Card) p.getObject()).getName());
					}
					//this package means you are the dealer,the dealer label of your will be activated
					if(p.getMessageType().equals("DEALER_LABEL")){
						parent.dealerLabel.setText("Dealer");
					}
					//disable dealerLabel when receiving this package type
					if(p.getMessageType().equals("RESET_DEALER")) {
						parent.dealerLabel.setText("");
					}
					/**
					 * this is the dealing card stage, players get two cards when receiving this
					 * package, two Card objects will be sent within the package, player stores
					 * these two cards in its object. 
					 * **/
					if(p.getMessageType().equals("CARDS")) {
						parent.stateLabel.setText("-----Game Start-----");
						Card card = (Card) p.getObject();			//generates the received card object
						JLabel label = new JLabel(card.getName());
						label.setHorizontalAlignment(SwingConstants.CENTER);
						parent.cardPanel.add(label);
						parent.resultLabel.setText("You draw card: " + card.getName());
						parent.cards.add(card);						// add the card that he received to his card list
						parent.cardsPoints = PointCounter.returnPoint(cards);	//calculates all the points in the current card list
						parent.cardLabel.setText("Your Point: "+cardsPoints);
						//here to determine if these two cards are nature vingt-un, if it is, win this round
						//notify the server that "I am the nature vingt-un"
						if(cardsPoints == 21) {
							parent.stateLabel.setText("NATURE VINGT-UN~~~ <`21`>");
							parent.send(new Package("VINGT-UN",""));
						}

					}
					//the nature vingt-un player will be told how many points he got in this round
					if(p.getMessageType().equals("21REWARD")) {
						parent.resultLabel.setText("You Win ~ Everyone give you 2 stacks");
						int reward = (Integer)p.getObject();
						parent.stacks +=reward;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
					}
					//the rest players will be notified that there is a nature vingt-un and all give this winner 2 stacks
					if(p.getMessageType().equals("21LOSE")) {
						parent.stacks -=2;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.stateLabel.setText("You lose,"+(String)p.getObject()+" is 21 Big Winner");
						parent.resultLabel.setText("You Pay 2 stacks to "+(String)p.getObject());
						parent.newCardButton.setEnabled(false);
						parent.standButton.setEnabled(false);
					}
					//if there are two nature vingt-un in this round, nobody wins and no one needs to pay
					if(p.getMessageType().equals("MUTI_21PLAYERS")){
						parent.stackLabel.setText("More than one nature 21 players");
						parent.resultLabel.setText("No one lose, waiting for another round");
						parent.newCardButton.setEnabled(false);
						parent.standButton.setEnabled(false);
					}
					
					
					
					//this happens after player request for another card
					if(p.getMessageType().equals("CARD")) {
						Card card = (Card) p.getObject();			//generates received card object
						JLabel label = new JLabel(card.getName());
						label.setHorizontalAlignment(SwingConstants.CENTER);
						parent.cardPanel.add(label);
						parent.resultLabel.setText("Ask for another card, draw: "+((Card) p.getObject()).getName());
						parent.cards.add(card);						// add the card that he received to his card list
						parent.cardsPoints = PointCounter.returnPoint(cards);	//compute scores of current cards he has
						parent.cardLabel.setText("Your Point: "+cardsPoints);
						//determine if the total score of player over 21(explode),if so, lose a stack and go to waiting list
						//Declarer will deduct one point if it explodes as a regular player, and the extra point will be added back later
						if(cardsPoints>21) {
							parent.stateLabel.setText("---Points over 21, You lose this round---");
							parent.resultLabel.setText("You are in waiting list now");
							parent.send(new Package("EXPLOSION",""));
							parent.stacks--;
							parent.stackLabel.setText("Your Stacks: "+parent.stacks);
							parent.newCardButton.setEnabled(false);
							parent.standButton.setEnabled(false);
						}
					
					}
					//Server send "QUERY" to each player,activate the Buttons and ask them if they want another card 
					if(p.getMessageType().equals("QUERY")) {
						parent.resultLabel.setText((String) p.getObject());
						parent.newCardButton.setEnabled(true);
						parent.standButton.setEnabled(true);
					}
					if(p.getMessageType().equals("DEALER_MESSAGE")) {
						parent.resultLabel.setText((String) p.getObject());
					}
					//after ordinary players stand, activate the dealer's buttons 
					if(p.getMessageType().equals("ACTIVATE_DEALER")) {
						parent.newCardButton.setEnabled(true);
						parent.standButton.setEnabled(true);
					}
					// (Dealer only）one player's score over 21(explode) dealer get one stack
					if(p.getMessageType().equals("PLAYER_EXPLODE")) {
						parent.stacks++;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.resultLabel.setText((String) p.getObject()+" is out, stack++");
					}
					//(Dealer only）after all players' score over 21 (all explode), game stop, waiting for another round
					if(p.getMessageType().equals("ALL_PLAYER_EXPLODE")) {
						parent.newCardButton.setEnabled(false);
						parent.standButton.setEnabled(false);
						parent.stateLabel.setText("You win~ All other players exploded");
						parent.resultLabel.setText("Waiting for another round start");
					}
					
					//(Dealer only）the corresponding number of chips is deducted, this round over
					if(p.getMessageType().equals("DEALER_EXPLODE")) {
						int winners = (Integer)p.getObject();
						parent.stacks -=winners;
						parent.stacks ++;  //after the dealer explodes over 21, reduce one stack just like the ordinary players, so need to add one stack back
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.stateLabel.setText("You lose !!!");
						parent.resultLabel.setText("You need pay stacks to: "+winners+" Players");
					
					}
					//(player only）player's score is higher than dealer's
					if(p.getMessageType().equals("WIN")) {
						parent.stacks++;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.stateLabel.setText("You Win ~~~");
						parent.resultLabel.setText("Earn one stack, waiting a new round");
						parent.newCardButton.setEnabled(false);
						parent.standButton.setEnabled(false);
					}
					//(player only）receive this after dealer's score over 21 (dealer lose)
					if(p.getMessageType().equals("WIN_DEALER_EXPLODED")) {
						parent.stacks++;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.stateLabel.setText("Dealer Exploded, You Win~");
						parent.resultLabel.setText("Earn one stack, waiting a new round");
						parent.newCardButton.setEnabled(false);
						parent.standButton.setEnabled(false);
					}
					
					//(player only）player's score is less than dealer's
					if(p.getMessageType().equals("LOSE")) {
						parent.stacks--;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.stateLabel.setText("You LOSE...");
						parent.resultLabel.setText("Lose one stack, waiting a new round");
					}
					//(player only）player's score equals to dealer
					if(p.getMessageType().equals("DRAW")) {
						parent.stateLabel.setText("Draw");
						parent.resultLabel.setText("No change, waiting a new round");
					}
					//(Dealer only）dealer shows his final score change after receive this
					if(p.getMessageType().equals("DEALER_RESULT")) {
						int stackChange = (Integer)p.getObject();
						parent.stacks += stackChange;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.disableButton();		//the dealer will be activated when all the players stand, and Buttons will be needed to disabled again
						parent.stateLabel.setText("Within rest players you earn: "+stackChange+" stack(s)");
						parent.resultLabel.setText("waiting a new round");
						parent.send(new Package("ROUND_OVER",""));
					}
					if(p.getMessageType().equals("INITIATE")) {
						parent.initiate();
					}
				}
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			} finally {
				return null;
			}
		}
	}


	/**
	 * create main body of swing
	 **/
	private Socket server = null;
	private String name = "PlayerX";
	private ObjectOutputStream outputStream;
	private JLabel stateLabel, nameLabel, stackLabel, resultLabel,dealerLabel,cardLabel;
	private JButton ContinueButton, quitButton, newCardButton, standButton;
	private JPanel cardPanel;
	private ReadWorker rw;
	private ArrayList<Card> cards = new ArrayList<Card>();	//All of cards of Players
	private int cardsPoints = 0;//the score of all cards of player
	private int stacks=10;		//stacks of player

	public TwentyoneClient() {
		getContentPane().setLayout(null);
		this.setSize(384, 497);
		name = JOptionPane.showInputDialog(this, "What's your name?");
		this.setTitle("Welcome to 21  "+ name+" !!!");

		quitButton = new JButton("Quit");
		quitButton.setBounds(260, 401, 102, 40);
		quitButton.addActionListener(this);
		getContentPane().add(quitButton);

		ContinueButton = new JButton("Continue");
		ContinueButton.setBounds(260, 350, 102, 40);
		ContinueButton.setEnabled(false);
		getContentPane().add(ContinueButton);

		newCardButton = new JButton("Request Card");
		newCardButton.setBounds(260, 249, 102, 40);
		newCardButton.setEnabled(false);
		newCardButton.addActionListener(this);
		getContentPane().add(newCardButton);

		standButton = new JButton("Stand");
		standButton.setBounds(260, 299, 102, 40);
		standButton.setEnabled(false);
		standButton.addActionListener(this);
		getContentPane().add(standButton);

		stateLabel = new JLabel("Game State");
		stateLabel.setFont(new Font("Tahoma", Font.BOLD | Font.ITALIC, 16));
		stateLabel.setBounds(10, 11, 352, 48);
		getContentPane().add(stateLabel);

		nameLabel = new JLabel("Username: " + name);
		nameLabel.setFont(new Font("Times New Roman", Font.BOLD, 14));
		nameLabel.setBounds(10, 112, 219, 35);
		getContentPane().add(nameLabel);

		stackLabel = new JLabel("Stack: "+stacks);
		stackLabel.setFont(new Font("Times New Roman", Font.BOLD, 14));
		stackLabel.setBounds(10, 145, 219, 35);
		getContentPane().add(stackLabel);

		resultLabel = new JLabel("Game result");
		resultLabel.setFont(new Font("Tw Cen MT", Font.PLAIN, 19));
		resultLabel.setBounds(10, 61, 352, 48);
		getContentPane().add(resultLabel);

		cardPanel = new JPanel();
		cardPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		cardPanel.setBounds(10, 206, 208, 235);
		getContentPane().add(cardPanel);
		cardPanel.setLayout(new GridLayout(0,1));

		cardLabel = new JLabel("Your Card");
		cardLabel.setBounds(10, 180, 102, 27);
		getContentPane().add(cardLabel);
		
		dealerLabel = new JLabel("");
		dealerLabel.setForeground(new Color(128, 0, 0));
		dealerLabel.setFont(new Font("Tw Cen MT", Font.ITALIC, 18));
		dealerLabel.setBounds(208, 123, 112, 40);
		getContentPane().add(dealerLabel);

		this.setVisible(true);

		// create connection with server
		connect();

		try {
			outputStream = new ObjectOutputStream(server.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// here we send the name to server to register
		try {
			outputStream.writeObject(new Package("PLAYER", name));
			System.out.println("new Player register");
		} catch (IOException e) {
			e.printStackTrace();
		}

		// here we start a inner class object to listen 
		rw = new ReadWorker(server, this);
		rw.execute();
		System.out.println("HERE");

	}
	/**
	 * method for sending a package to Server
	 * **/
	public void send(Package p) {
		try {
			outputStream.writeObject(p);
		} catch (IOException e) {
			e.printStackTrace();
			
		}
	}
	//helper method for disable two buttons.
	public void disableButton() {
		this.newCardButton.setEnabled(false);
		this.standButton.setEnabled(false);
	}

	/**
	 * acionPerformed
	 **/
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == ContinueButton) {
			send(new Package("CONTINUE", name));
		}
		if(e.getSource()==quitButton) {
			send(new Package("QUIT",name));
			this.dispose();
		}
		if(e.getSource()==newCardButton) {
			send(new Package("MORECARD",name));
		}
		if(e.getSource()==standButton) {
			standTurn();
		}

	}
	/**
	 * the player choose to stand, send "STAND" package, disable Buttons
	 * and waiting for result
	 * **/
	public void standTurn() {
		send(new Package("STAND",name));
		this.newCardButton.setEnabled(false);
		this.standButton.setEnabled(false);
		this.resultLabel.setText("You have standed, waiting result");
	}
	/**
	 * Initiate the data of all Players
	 * **/
	public void initiate() {
		cards.clear();
		cardsPoints = 0;
		this.cardPanel.removeAll();
		cardPanel.setVisible(false);
		cardPanel.setVisible(true);
	}

	/**
	 * connect to the server
	 **/
	private void connect() {
		try {
			server = new Socket("127.0.0.1", 8888);
			System.out.println("Connected");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new TwentyoneClient();
	}
}
