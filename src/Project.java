import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Project implements Serializable {


    private static final long serialVersionUID = -998624166256962694L;
    //Multicast IP chat
    private String MIPAddress = null;
    private ArrayList<String> messageHistory;


    private File projectDir;


    //An hashmap foreach card list
    private ConcurrentHashMap <String, Card> toDo;
    private ConcurrentHashMap <String, Card> inProgress;
    private ConcurrentHashMap <String, Card> toBeRevised;
    private ConcurrentHashMap <String, Card> done;

    private ArrayList<Card> cards; //cards list
    private ArrayList<String> cardsName; //card's name list
    private ArrayList<String> members; //members list
    private String projectName;


    public Project(){
        super();
    }



    //Constructor
    public Project(String projectName) {
        this.projectName = projectName;
        cards = new ArrayList<>();
        toDo = new ConcurrentHashMap();
        inProgress = new ConcurrentHashMap();
        toBeRevised = new ConcurrentHashMap();
        done = new ConcurrentHashMap();
        cardsName = new ArrayList<>();
        members = new ArrayList<>();
        messageHistory = new ArrayList<>();
        projectDir = new File("./BackupDir/" + projectName);

        //create the directory for this project
        if (!projectDir.exists()){
            projectDir.mkdir();
        }
    }



    //move the card identified by cardName from listaPartenza to listaDestinazione
    public synchronized boolean moveCard(String cardName, String listaPartenza, String listaDestinazione){
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
                } else return false; //return false if the card is not in the "listaPartenza"

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

                    } else if (listaDestinazione.equalsIgnoreCase("DONE")) {
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

    public synchronized void addCard(String cardName, String description){
        //check if the card cardName already exist in this project
        if (cardsName.contains(cardName)) throw new IllegalArgumentException("Card already exist");

        //add the card
        Card cTmp = new Card(cardName, description);
        cTmp.putMovement("toDo");
        cards.add(cTmp);
        cardsName.add(cardName);
        toDo.put(cardName, cTmp);
    }

    //add a member if does not already exist
    public synchronized void addMember(String nickUtente){
        //check if the member nickUtente already exist in this project
        if (members.contains(nickUtente)) throw new IllegalArgumentException("the user already exist");

        //add the member
        members.add(nickUtente);
    }

    public synchronized Card getCardToSend(String cardName){
        //check if the card is in the project
        if (!cardsName.contains(cardName)) throw new IllegalArgumentException("the card does not exist");

        //search and return that card
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

    //search and return the card cardName
    public synchronized Card searchCard(String cardName){
        if (cardName.contains(cardName)){
            for (Card card : cards){
                if (card.getName().equals(cardName)){
                    return card;
                }
            }
        }
        return null;
    }

    public void backup(){
        File cardFile;
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        if (cards.size()!=0){
            for (Card card : cards){
                cardFile = new File(projectDir + "/" + card.getName() + ".json");
                //System.out.println(cardFile.getPath());
                try {
                    //check if the file exist, if not, create the file
                    cardFile.createNewFile();

                    try (Writer writer = new FileWriter(projectDir + "/" + card.getName() + ".json")){
                        gson.toJson(card, writer);
                    }

                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public File getProjectDir() {
        return projectDir;
    }

    public void setProjectDir(File projectDir) {
        this.projectDir = projectDir;
    }

    //check if all the card are in the done list
    public boolean checkCard(){
        if (toDo.isEmpty() && inProgress.isEmpty() && toBeRevised.isEmpty()) return true;
        else return false;
    }

    //restituisce true se l'user e' membro del progetto, altrimenti false
    public synchronized boolean searchMember(String memberName){
        if (members.contains(memberName)){
            return true;
        } else {
            return false;
        }
    }



    //add a message in the message history
    public void addMessage(String message){
        messageHistory.add(message);
    }

    //"showMembers" function, return the member's list of this project
    public ArrayList<String> getMembers(){
        return members;
    }

    //return the cards of this project
    public ArrayList<String> getCards() {
        return cardsName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setToDo(ConcurrentHashMap<String, Card> toDo) {
        this.toDo = toDo;
    }

    public void setInProgress(ConcurrentHashMap<String, Card> inProgress) {
        this.inProgress = inProgress;
    }

    public void setToBeRevised(ConcurrentHashMap<String, Card> toBeRevised) {
        this.toBeRevised = toBeRevised;
    }

    public void setDone(ConcurrentHashMap<String, Card> done) {
        this.done = done;
    }

    public void setCards(ArrayList<Card> cards) {
        this.cards = cards;
    }

    public void setCardsName(ArrayList<String> cardsName) {
        this.cardsName = cardsName;
    }

    public void setMembers(ArrayList<String> members) {
        this.members = members;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public ConcurrentHashMap<String, Card> getToDo() {
        return toDo;
    }

    public ConcurrentHashMap<String, Card> getInProgress() {
        return inProgress;
    }

    public ConcurrentHashMap<String, Card> getToBeRevised() {
        return toBeRevised;
    }

    public ConcurrentHashMap<String, Card> getDone() {
        return done;
    }

    public ArrayList<String> getCardsName() {
        return cardsName;
    }

    public String getMIPAddress() {
        return MIPAddress;
    }

    public void setMIPAddress(String MIPAddress) {
            this.MIPAddress = MIPAddress;
    }

    public ArrayList<String> getMessageHistory() {
        return messageHistory;
    }

    public void setMessageHistory(ArrayList<String> messageHistory) {
        this.messageHistory = messageHistory;
    }

    public ArrayList<Card> getCard(){ return cards; }



}
