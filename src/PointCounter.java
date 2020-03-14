import java.util.ArrayList;

public class PointCounter {
	public static int returnPoint(ArrayList<Card> cards) {
		int result = 0;
		for(Card card: cards) {
			result+=card.getNumber();
		}
		return result;
	}
}
