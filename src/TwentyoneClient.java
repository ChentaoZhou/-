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
					if (p.getMessageType().equals("GAME_STATE")) {
						parent.stateLabel.setText((String) p.getObject());
					}
					if (p.getMessageType().equals("DEALER")) {
						parent.stateLabel.setText("This is the stage of choosing dealer by card");
						parent.resultLabel.setText("You have draw card: " + ((Card) p.getObject()).getName());
					}
					if(p.getMessageType().equals("DEALER_LABEL")){
						parent.dealerLabel.setText("Dealer");
					}
					if(p.getMessageType().equals("CARDS")) {
						parent.stateLabel.setText("-----Game Start-----");
						Card card = (Card) p.getObject();			//把收到的card生成对象
						JLabel label = new JLabel(card.getName());
						label.setHorizontalAlignment(SwingConstants.CENTER);
						parent.cardPanel.add(label);
						parent.resultLabel.setText("You draw card: " + card.getName());
						parent.cards.add(card);						//将收到的card加到手牌中
						parent.cardsPoints = PointCounter.returnPoint(cards);	//计算当前手牌中所有的分数
						parent.cardLabel.setText("Your Point: "+cardsPoints);
						//这里判断玩家当前是不是21点nature venter，如果是直接赢，如果双AA，重新抽一张
					}
					if(p.getMessageType().equals("CARD")) {
						Card card = (Card) p.getObject();			//把收到的card生成对象
						JLabel label = new JLabel(card.getName());
						label.setHorizontalAlignment(SwingConstants.CENTER);
						parent.cardPanel.add(label);
						parent.resultLabel.setText("Ask for another card, draw: "+((Card) p.getObject()).getName());
						parent.cards.add(card);						//将收到的card加到手牌中
						parent.cardsPoints = PointCounter.returnPoint(cards);	//计算当前手牌中所有的分数
						parent.cardLabel.setText("Your Point: "+cardsPoints);
						//这里判断玩家当前手牌分数是否爆掉，如果爆掉，退出游戏进入waiting list
						//这里要判断dealer，因为dealer分数是之后统一扣除，所以dealer之后要自行把这一分加回来。
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
					//（Dealer专享）玩家爆牌，庄家获得一个筹码
					if(p.getMessageType().equals("PLAYER_EXPLODE")) {
						parent.stacks++;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.resultLabel.setText((String) p.getObject()+" is out, stack++");
					}
					//（Dealer专享）只有Dealer会收到DEALER_EXPLODE消息,扣掉对应的筹码数量，这一轮结束
					if(p.getMessageType().equals("DEALER_EXPLODE")) {
						int winners = (Integer)p.getObject();
						parent.stacks -=winners;
						parent.stacks ++;  //因为在dealer爆了之后，会和普通玩家一样先扣一个筹码，所以这里要把之前扣得筹码加回来。
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.stateLabel.setText("You lose !!!");
						parent.resultLabel.setText("You need pay stacks to: "+winners+" Players");
					
					}
					//（player专享）在Dealer爆了之后，其他仍在对局中的玩家会收到一条WIN消息
					if(p.getMessageType().equals("WIN")) {
						parent.stacks++;
						parent.stackLabel.setText("Your Stacks: "+parent.stacks);
						parent.stateLabel.setText("You Win ~~~");
						parent.resultLabel.setText("One stack has put into your account, waiting a new round");
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
	private ArrayList<Card> cards = new ArrayList<Card>();	//玩家的手牌
	private int cardsPoints;//玩家手牌的总分数
	private int stacks=10;		//玩家的筹码

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
