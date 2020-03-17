import java.util.ArrayList;

/**
 * class contains a static method used for computing the current cards score of a player
 * **/
public class PointCounter {
	public static int returnPoint(ArrayList<Card> cards) {
		int result = 0;
		for(Card card: cards) {
			result+=card.getNumber();
		}
		return result;
	}
}
