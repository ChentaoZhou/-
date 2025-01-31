import java.io.Serializable;
/**
 * Class to store the information of card
 * **/
public class Card implements Serializable{
	private String name;
	private int number;
	
	public Card(String s, int n) {
		name = s;
		number = n;
	}

	public String getName() {
		return name;
	}

	public int getNumber() {
		return number;
	}
	public String toString() {
		return name+" "+number;
	}
}
