import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class Result implements Serializable {

    private int resultCode;
    private String result;

    private HashMap<String, byte[]> serializedObjectStructure;

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
