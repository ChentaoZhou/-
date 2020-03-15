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

public class ServerView extends JFrame implements ActionListener{
	
	private class ServerWorker extends SwingWorker<Void, Void> {

		protected Void doInBackground() throws Exception {
			startButton.setEnabled(false);
			server.initiate();					//initiate game, create deck
			if(server.getDealer()==null) server.findDealer(); //find a dealer
			server.dealCard();
			return null;
		}
		
		
	}

	private TwentyoneServer server;
	private JTextArea textArea;
	private JButton startButton;
	private JLabel inGamePlayerLabel, waitPlayerLabel;
	private JScrollPane sp;
	private ServerWorker sw;
	
	public ServerView(TwentyoneServer server) {
		this.server = server;
		
		getContentPane().setLayout(null);
		this.setSize(693,305);
		
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
		getContentPane().add(startButton);
		
		textArea = new JTextArea();
		textArea.setBounds(195, 11, 260, 232);
		
		
		sp = new JScrollPane(textArea);
		sp.setBounds(195, 11, 454, 234);
		getContentPane().add(sp);
		this.setVisible(true);
		
	}
	//methods for changing the number of players in the Labels
	public void refreshInandWait(int in,int wait) {
		refreshIn(in);
		refreshWait(wait);
	}
	public void refreshIn(int in) {
		inGamePlayerLabel.setText(in+" players in Game.");
	}
	public void refreshWait(int wait) {
		waitPlayerLabel.setText(wait+" players are waiting");
	}
	
	
	public JTextArea getTextArea() {
		return textArea;
	}
	public void clearTextArea() { 
		this.textArea.setText("");
	}
	//add String to textArea track the process
	public void addText(String line) {
		this.textArea.append(line);
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
	 * This calls all operation methods for conducting game
	 * 这个方法真正的在调用所有的游戏运行方法
	 * **/
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == startButton) {
			sw = new ServerWorker();
			sw.execute();
			
		}
		

	}

}
