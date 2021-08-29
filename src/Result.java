import java.io.Serializable;
import java.util.HashMap;

public class Result implements Serializable {

    private int resultCode; //String with the int result code
    private String result; //String with the outcome of the operation or the reason for the failure

    private HashMap<String, byte[]> serializedObjectStructure; //Serialized object result of the operation

    public void addSerializedObject(String objectSerialized, byte[] byteSerializedObject){
        serializedObjectStructure.put(objectSerialized, byteSerializedObject);
    }

    public Result(){
        serializedObjectStructure = new HashMap<>();
    }

    public HashMap<String, byte[]> getSerializedObjectStructure() {
        return serializedObjectStructure;
    }

    public void setSerializedObjectStructure(HashMap<String, byte[]> serializedObjectStructure) {
        this.serializedObjectStructure = serializedObjectStructure;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int code) {
        this.resultCode = code;
    }
}
