import java.io.Serializable;

public class RegistrationResult implements Serializable {
    private int result;

    public RegistrationResult(int result){
        this.result = result;
    }

    public int getResult() {
        return result;
    }
}
