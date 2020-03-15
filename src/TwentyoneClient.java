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
 * 给Client端加上了ArrayList<Card>,存储接收到的手牌，并传递给计算总分的类来计算分数，打在swing上
 * **/

public class TwentyoneClient extends JFrame implements ActionListener {

	/**
	 * inner class used for listening from the server
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
		 * receive message from server
		 **/
		public Void doInBackground() {
			System.out.println("started read worker");
			Package p = null; // z从server收到一个package，要根据里面的messageType来决定做什么
			try {
				while ((p = (Package) inputStream.readObject()) != null) {
					// ....z这里打开package，根据type做事情
					//z只改变stateLabel标签的文字
					if (p.getMessageType().equals("GAME_STATE")) {
						parent.stateLabel.setText((String) p.getObject());
					}
					//z只改变resultLabel标签的文字
					if(p.getMessageType().equals("MESSAGE")) {
						parent.resultLabel.setText((String) p.getObject());
					}
					//z抽牌决定dealer的阶段，收到该消息的玩家调整stateLabel并展示抽到的牌
					if (p.getMessageType().equals("DEALER")) {
						parent.stateLabel.setText("This is the stage of choosing dealer by card");
						parent.resultLabel.setText("You have draw card: " + ((Card) p.getObject()).getName());
					}
					//z收到该信息的玩家将亮起 Dealer图标
					if(p.getMessageType().equals("DEALER_LABEL")){
						parent.dealerLabel.setText("Dealer");
					}
					//z收到该信息的玩家（发给所有玩家）都熄灭Dealer图标
					if(p.getMessageType().equals("RESET_DEALER")) {
						parent.dealerLabel.setText("");
					}
					
					
					
					
					if(p.getMessageType().equals("CARDS")) {
						parent.stateLabel.setText("-----Game Start-----");
						Card card = (Card) p.getObject();			//z把收到的card生成对象
						JLabel label = new JLabel(card.getName());
						label.setHorizontalAlignment(SwingConstants.CENTER);
						parent.cardPanel.add(label);
						parent.resultLabel.setText("You draw card: " + card.getName());
						parent.cards.add(card);						//z将收到的card加到手牌中
						parent.cardsPoints = PointCounter.returnPoint(cards);	//z计算当前手牌中所有的分数
						parent.cardLabel.setText("Your Point: "+cardsPoints);
						//z这里判断玩家当前是不是21点nature venter，如果是直接赢，如果双AA，重新抽一张
						
						//z这里只是先告知Server自己是21，如果只有一个21，自己赢，如果是两个21，没有赢家
						if(cardsPoints == 21) {
							parent.stateLabel.setText("NATURE VINGT-UN~~~ <`21`>");
							parent.send(new Package("VINGT-UN",""));
						}

					}
					//z这是大赢家被告知要加多少筹码给自己
					if(p.getMessageType().equals("21REWARD")) {
						parent.resultLabel.setText("You Win ~ Everyone give you 2 stacks");
						int reward = (Integer)p.getObject();
						parent.stacks +=reward;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
					}
					//z其余玩家都收到一个出现了 nature 21点的消息，都被扣掉两个筹码
					if(p.getMessageType().equals("21LOSE")) {
						parent.stacks -=2;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.stateLabel.setText("You lose,"+(String)p.getObject()+" is 21 Big Winner");
						parent.resultLabel.setText("You Pay 2 stacks to "+(String)p.getObject());
						parent.newCardButton.setEnabled(false);
						parent.standButton.setEnabled(false);
					}
					//z这里加上如果出现两个nature21，大家直接停止这轮游戏等待下轮，没有人需要付钱
					if(p.getMessageType().equals("MUTI_21PLAYERS")){
						parent.stackLabel.setText("More than one nature 21 players");
						parent.resultLabel.setText("No one lose, waiting for another round");
						parent.newCardButton.setEnabled(false);
						parent.standButton.setEnabled(false);
					}
					
					
					
					
					if(p.getMessageType().equals("CARD")) {
						Card card = (Card) p.getObject();			//z把收到的card生成对象
						JLabel label = new JLabel(card.getName());
						label.setHorizontalAlignment(SwingConstants.CENTER);
						parent.cardPanel.add(label);
						parent.resultLabel.setText("Ask for another card, draw: "+((Card) p.getObject()).getName());
						parent.cards.add(card);						//z将收到的card加到手牌中
						parent.cardsPoints = PointCounter.returnPoint(cards);	//z计算当前手牌中所有的分数
						parent.cardLabel.setText("Your Point: "+cardsPoints);
						//z这里判断玩家当前手牌分数是否爆掉，如果爆掉，退出游戏进入waiting list
						//z这里要判断dealer，因为dealer分数是之后统一扣除，所以dealer之后要自行把这一分加回来。
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
					//Server发送QUERY包给每个player，打开他们的Button询问是否要牌
					if(p.getMessageType().equals("QUERY")) {
						parent.resultLabel.setText((String) p.getObject());
						parent.newCardButton.setEnabled(true);
						parent.standButton.setEnabled(true);
					}
					if(p.getMessageType().equals("DEALER_MESSAGE")) {
						parent.resultLabel.setText((String) p.getObject());
					}
					if(p.getMessageType().equals("ACTIVATE_DEALER")) {
						parent.newCardButton.setEnabled(true);
						parent.standButton.setEnabled(true);
					}
					// (Dealer专享）一位玩家爆牌，庄家获得一个筹码
					if(p.getMessageType().equals("PLAYER_EXPLODE")) {
						parent.stacks++;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.resultLabel.setText((String) p.getObject()+" is out, stack++");
					}
					//(Dealer专享）所有玩家都已经爆牌之后，庄家停止所有行为，等待Server开始下一局
					if(p.getMessageType().equals("ALL_PLAYER_EXPLODE")) {
						parent.newCardButton.setEnabled(false);
						parent.standButton.setEnabled(false);
						parent.stateLabel.setText("You win~ All other players exploded");
						parent.resultLabel.setText("Waiting for another round start");
					}
					
					//(Dealer专享）只有Dealer会收到DEALER_EXPLODE消息,扣掉对应的筹码数量，这一轮结束
					if(p.getMessageType().equals("DEALER_EXPLODE")) {
						int winners = (Integer)p.getObject();
						parent.stacks -=winners;
						parent.stacks ++;  //z因为在dealer爆了之后，会和普通玩家一样先扣一个筹码，所以这里要把之前扣得筹码加回来。
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.stateLabel.setText("You lose !!!");
						parent.resultLabel.setText("You need pay stacks to: "+winners+" Players");
					
					}
					//（player专享）player分数比dealer高
					if(p.getMessageType().equals("WIN")) {
						parent.stacks++;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.stateLabel.setText("You Win ~~~");
						parent.resultLabel.setText("Earn one stack, waiting a new round");
						parent.newCardButton.setEnabled(false);
						parent.standButton.setEnabled(false);
					}
					//（player专享）在Dealer爆了之后，其他仍在对局中的玩家会收到一条WIN消息
					if(p.getMessageType().equals("WIN_DEALER_EXPLODED")) {
						parent.stacks++;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.stateLabel.setText("Dealer Exploded, You Win~");
						parent.resultLabel.setText("Earn one stack, waiting a new round");
						parent.newCardButton.setEnabled(false);
						parent.standButton.setEnabled(false);
					}
					
					//(player专享）player的分数低于dealer
					if(p.getMessageType().equals("LOSE")) {
						parent.stacks--;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.stateLabel.setText("You LOSE...");
						parent.resultLabel.setText("Lose one stack, waiting a new round");
					}
					//(player专享）player和dealer打成平局
					if(p.getMessageType().equals("DRAW")) {
						parent.stateLabel.setText("Draw");
						parent.resultLabel.setText("No change, waiting a new round");
					}
					//(Dealer专享）庄家清算最后自己分数的变化
					if(p.getMessageType().equals("DEALER_RESULT")) {
						int stackChange = (Integer)p.getObject();
						parent.stacks += stackChange;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.disableButton();		//z当所有玩家都stand后 dealer会被激活，这里要重新熄灭Buttons
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
	private ArrayList<Card> cards = new ArrayList<Card>();	//z玩家的手牌
	private int cardsPoints = 0;//z玩家手牌的总分数
	private int stacks=10;		//z玩家的筹码

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

		// here we start a inner class object to listen 这里创建一个内部类对象，监听端口
		rw = new ReadWorker(server, this);
		rw.execute();
		System.out.println("HERE");

	}

	public void send(Package p) {
		try {
			outputStream.writeObject(p);
		} catch (IOException e) {
			e.printStackTrace();
			
		}
	}
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
	//玩家选择stand，发送STAND数据包，熄灭Button，进入等待结果环节
	public void standTurn() {
		send(new Package("STAND",name));
		this.newCardButton.setEnabled(false);
		this.standButton.setEnabled(false);
		this.resultLabel.setText("You have standed, waiting result");
	}
	/**
	 * 初始化玩家数据
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
