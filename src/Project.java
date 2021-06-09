import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class Project {

    //An hashmap foreach card list
    private ConcurrentHashMap <String, Card> toDo;
    private ConcurrentHashMap <String, Card> inProgress;
    private ConcurrentHashMap <String, Card> toBeRevised;
    private ConcurrentHashMap <String, Card> done;

    private ArrayList<Card> cards; //cards list
    private ArrayList<String> cardsName; //card's name list
    private ArrayList<String> members; //members list
    private String projectName;


    //Constructor
    public Project(String projectName) {
        this.projectName = projectName;
        toDo = new ConcurrentHashMap();
        inProgress = new ConcurrentHashMap();
        toBeRevised = new ConcurrentHashMap();
        done = new ConcurrentHashMap();
        cardsName = new ArrayList<>();
        members = new ArrayList<>();
    }

    //move the card identified by cardName from listaPartenza to listaDestinazione
    public boolean moveCard(String cardName, String listaPartenza, String listaDestinazione){
        switch (listaPartenza.toUpperCase()){
            case "TODO":
                if (!listaDestinazione.equalsIgnoreCase("inprogress")){
                    throw new IllegalArgumentException("Wrong destination");
                }
                //check if the card cardName is inside the to/Do list
                if (toDo.containsKey(cardName)){
                    //get and remove card from the source list and put this card in the destination list
                    Card cTmp = toDo.get(cardName);
                    toDo.remove(cardName);
                    cTmp.setCardPosition(listaDestinazione.toLowerCase());
                    cTmp.putMovement(listaDestinazione.toLowerCase());
                    searchAndRemoveCard(cardName, cTmp);
                    inProgress.putIfAbsent(cTmp.getName(), cTmp);
                    return true;
                } else return false;

            case "INPROGRESS":
                //check if the card cardName is inside the inProgress list
                if (inProgress.containsKey(cardName)) {
                    //get and remove card from the source list and put this card in the destination list
                    Card cTmp = inProgress.get(cardName);
                    inProgress.remove(cardName);
                    cTmp.setCardPosition(listaDestinazione.toLowerCase());
                    cTmp.putMovement(listaDestinazione.toLowerCase());
                    searchAndRemoveCard(cardName, cTmp);
                    if (listaDestinazione.equalsIgnoreCase("toberevised")) {
                        toBeRevised.putIfAbsent(cTmp.getName(), cTmp);
                        return true;

                    } else if (listaDestinazione.toUpperCase().equals("DONE")) {
                            done.putIfAbsent(cTmp.getName(), cTmp);
                            return true;
                        }
                } else throw new IllegalArgumentException("Wrong destination");

                return false;

            case "TOBEREVISED":
                //check if the card cardName is inside the toBeRevised list
                if (toBeRevised.containsKey(cardName)){
                    //get and remove card from the source list and put this card in the destination list
                    Card cTmp = toBeRevised.get(cardName);
                    toBeRevised.remove(cardName);
                    cTmp.setCardPosition(listaDestinazione.toLowerCase());
                    cTmp.putMovement(listaDestinazione.toLowerCase());
                    searchAndRemoveCard(cardName, cTmp);
                if (listaDestinazione.equalsIgnoreCase("INPROGRESS")) {
                    inProgress.putIfAbsent(cTmp.getName(), cTmp);
                    return true;

                } else if (listaDestinazione.equalsIgnoreCase("DONE")) {
                    done.putIfAbsent(cTmp.getName(), cTmp);
                    return true;
                    }

                } else throw new IllegalArgumentException("Wrong destination");

            default:
                return false;
        }
    }


    public void addCard(String cardName, String description){
        //check if the card cardName already exist in this project
        if (cardsName.contains(cardName)) throw new IllegalArgumentException("Card already exist");

        //add the card
        Card cTmp = new Card(cardName, description);
        cardsName.add(cardName);
        toDo.put(cardName, cTmp);
    }

    //"showMembers" function, return the member's list of this project
    public ArrayList<String> getMembers(){
        return members;
    }

    //return the cards of this project
    public ArrayList<String> getCards() {
        return cardsName;
    }

    //add a member if does not already exist
    public void addMember(String nickUtente){
        //check if the member nickUtente already exist in this project
        if (members.contains(nickUtente)) throw new IllegalArgumentException("the user already exist");

        //add the member
        members.add(nickUtente);
    }


    public Card getCard(String cardName){
        //check if the card is in the project
        if (cardsName.contains(cardName)) throw new IllegalArgumentException("the card does not already exist");

        //
        for (int i = 0; i < cards.size(); i++) {
            Card cTmp = cards.get(i);
            if (cTmp.getName().equals(cardName)) return cTmp;
        }
        return null;
    }

    //function that search and replace the modified card on cards list
    private void searchAndRemoveCard(String cardName, Card cTmp){
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getName().equals(cardName)){
                cards.remove(i);
                cards.add(cTmp);
            }
        }
    }


    //check if all the card are in the done list
    public boolean checkCard(){
        if (toDo.isEmpty() && inProgress.isEmpty() && toBeRevised.isEmpty()) return true;
        else return false;
    }


}
