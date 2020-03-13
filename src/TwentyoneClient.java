import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.SwingWorker;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Font;

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
					if(p.getMessageType().equals("CARD")) {
						parent.stateLabel.setText("-----Game Start-----");
						parent.resultLabel.setText("You have draw card: " + ((Card) p.getObject()).getName());
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
	private JLabel stateLabel, nameLabel, stackLabel, resultLabel;
	JButton ContinueButton, quitButton, newCardButton, standButton;
	private JPanel cardPanel;

	public TwentyoneClient() {
		getContentPane().setLayout(null);
		this.setSize(384, 497);
		name = JOptionPane.showInputDialog(this, "What's your name?");

		quitButton = new JButton("Quit");
		quitButton.setBounds(260, 401, 102, 40);
		quitButton.addActionListener(this);
		getContentPane().add(quitButton);

		ContinueButton = new JButton("Continue");
		ContinueButton.setBounds(260, 350, 102, 40);
		getContentPane().add(ContinueButton);

		newCardButton = new JButton("Another Card");
		newCardButton.setBounds(260, 249, 102, 40);
		getContentPane().add(newCardButton);

		standButton = new JButton("Stand");
		standButton.setBounds(260, 299, 102, 40);
		getContentPane().add(standButton);

		stateLabel = new JLabel("Game State");
		stateLabel.setFont(new Font("Tahoma", Font.BOLD | Font.ITALIC, 16));
		stateLabel.setBounds(10, 11, 352, 48);
		getContentPane().add(stateLabel);

		nameLabel = new JLabel("Username: " + name);
		nameLabel.setFont(new Font("Times New Roman", Font.BOLD, 14));
		nameLabel.setBounds(10, 112, 219, 35);
		getContentPane().add(nameLabel);

		stackLabel = new JLabel("Stack: 10");
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

		JLabel cardLabel = new JLabel("Your Card");
		cardLabel.setBounds(10, 180, 102, 27);
		getContentPane().add(cardLabel);

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
		ReadWorker rw = new ReadWorker(server, this);
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
