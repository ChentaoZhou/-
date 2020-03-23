import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JButton;
import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
/**
 * The Swing window of Server which has a button can start a round of game 
 * and can track the progress of this game in real time.
 * All Players' behaviours will be printed on the JTextArea.
 * **/
public class ServerView extends JFrame implements ActionListener {
	/**
	 * A inner class which extends SwingWorker so the Server view window can update
	 * the progress of the game in real time
	 **/
	private class ServerWorker extends SwingWorker<Void, Void> {

		/**
		 * When a new round of game start(the start button is clicked) it will call
		 * corresponding methods of the TwentyoneServer object(the real server)
		 **/
		protected Void doInBackground() throws Exception {
			startButton.setEnabled(false);
			server.initiate(); // initiate game, create deck
			if (server.getDealer() == null)
				server.findDealer(); // find a dealer
			server.dealCard(); // release two card to each players
			return null;
		}

	}

	private Server server; // the real server object
	private JTextArea textArea;
	private JButton startButton;
	private JLabel inGamePlayerLabel, waitPlayerLabel,startLabel;
	private JScrollPane sp;
	private ServerWorker sw;

	/**
	 * The Constructor of Server Swing Window
	 **/
	public ServerView(Server server) {
		this.server = server;

		getContentPane().setLayout(null);
		this.setSize(693, 305);

		inGamePlayerLabel = new JLabel("0 players in Game");
		inGamePlayerLabel.setFont(new Font("Tw Cen MT", Font.PLAIN, 17));
		inGamePlayerLabel.setBounds(10, 10, 175, 44);
		getContentPane().add(inGamePlayerLabel);

		waitPlayerLabel = new JLabel("0 players in waiting list");
		waitPlayerLabel.setFont(new Font("Tw Cen MT", Font.PLAIN, 17));
		waitPlayerLabel.setBounds(10, 65, 175, 44);
		getContentPane().add(waitPlayerLabel);

		startButton = new JButton("start");
		startButton.setFont(new Font("Tw Cen MT", Font.BOLD, 19));
		startButton.setBounds(31, 182, 97, 39);
		startButton.addActionListener(this);
		startButton.setEnabled(false);
		getContentPane().add(startButton);

		textArea = new JTextArea();
		textArea.setBounds(195, 11, 260, 232);

		sp = new JScrollPane(textArea);
		sp.setBounds(195, 11, 454, 234);
		getContentPane().add(sp);
		
		startLabel = new JLabel("at least 2 players to start");
		startLabel.setFont(new Font("Tw Cen MT", Font.PLAIN, 17));
		startLabel.setBounds(10, 138, 186, 44);
		getContentPane().add(startLabel);
		this.setVisible(true);

	}

	// methods for changing the number of players in both Player Label and Waiting
	// player Label.
	public void refreshInandWait(int in, int wait) {
		refreshIn(in);
		refreshWait(wait);
	}

	// method for changing the number of Player Label
	public void refreshIn(int in) {
		inGamePlayerLabel.setText(in + " players in Game.");
	}

	// method for changing the number of WaitingPlayer Label
	public void refreshWait(int wait) {
		waitPlayerLabel.setText(wait + " players are waiting");
	}

	// add String to textArea track the process in real time
	public void addText(String line) {
		this.textArea.append(line);
	}

	public JTextArea getTextArea() {
		return textArea;
	}

	public JLabel getStartLabel() {
		return startLabel;
	}
	public void clearTextArea() {
		this.textArea.setText("");
	}

	public JLabel getInGamePlayerLabel() {
		return inGamePlayerLabel;
	}

	public JLabel getWaitPlayerLabel() {
		return waitPlayerLabel;
	}

	public JButton getStartButton() {
		return startButton;
	}

	/**
	 * This swing only contains one Button: Start To start a new round of game
	 * 
	 **/
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == startButton) {
			sw = new ServerWorker();
			sw.execute();

		}

	}
}
