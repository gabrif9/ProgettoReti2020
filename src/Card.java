import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;

public class Card implements Serializable {

    private static final long serialVersionUID = -6914309705953066728L;
    private String name;
    private String description;
    private String cardPosition;
    private ArrayList<String> cardHistory;



    public Card(){
        super();
    }

    public Card(String name, String description){
        this.name = name;
        this.description = description;
        cardPosition = "todo";
        cardHistory = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCardPosition() {
        return cardPosition;
    }

    public void setCardPosition(String cardPosition) {
        this.cardPosition = cardPosition;
    }

    public ArrayList<String> getCardHistory() {
        return cardHistory;
    }

    public void setCardHistory(ArrayList<String> cardHistory) {
        this.cardHistory = cardHistory;
    }

    //insert the movement on the card history movement, foreach time that the card is moved from one list to another
    public void putMovement(String movement){
        cardHistory.add(movement);
    }
}
