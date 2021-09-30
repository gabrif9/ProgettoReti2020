import java.io.Serializable;
import java.util.ArrayList;



public class Card implements Serializable{

    private String name;
    private String description;
    private String cardPosition;
    private ArrayList<String> cardHistory;

    public Card(String name, String description, String cardPosition, ArrayList<String> cardHistory) {
        this.name = name;
        this.description = description;
        this.cardPosition = cardPosition;
        this.cardHistory = cardHistory;
    }


    public Card(String name, String description){
        this.name = name;
        this.description = description;
        cardPosition = "todo";
        cardHistory = new ArrayList<>();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCardPosition(String cardPosition) {
        this.cardPosition = cardPosition;
    }

    public void setCardHistory(ArrayList<String> cardHistory) {
        this.cardHistory = cardHistory;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCardPosition() {
        return cardPosition;
    }

    public ArrayList<String> getCardHistory() {
        return cardHistory;
    }

    //insert the movement on the card history movement, foreach time that the card is moved from one list to another
    public void putMovement(String movement){
        cardHistory.add(movement);
    }
}
