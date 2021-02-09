import java.util.ArrayList;

public class Card {
    private String name;
    private String description;
    private String cardPosition;
    private ArrayList<String> cardHistory;

    public Card(String name, String description){
        this.name = name;
        this.description = description;
        cardPosition = "todo";
        cardHistory = new ArrayList<>();
    }

    //return the position of this card (the list where the card is)
    public String getCardPosition() {
        return cardPosition;
    }

    //set the card position
    public void setCardPosition(String cardPosition) {
        this.cardPosition = cardPosition;
    }

    //return the description of this card
    public String getDescription() {
        return description;
    }

    //return the name of this card
    public String getName() {
        return name;
    }

    //insert the movement on the card history movement, foreach time that the card is moved from one list to another
    public void putMovement(String movement){
        cardHistory.add(movement);
    }
}
