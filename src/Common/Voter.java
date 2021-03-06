package Common;

import java.math.BigInteger;
import java.util.Random;

public class Voter {
    private BigInteger validationNumber = BigInteger.ZERO;
    private int choice, id;

    public Voter(int choice, int id) {
        this.choice = choice;
        this.id = id;
    }

    public Voter(int choice) {
        Random r = new Random();
        this.id = r.nextInt(1000000000);
        this.choice = choice;
    }

    public Voter(BigInteger validationNumber, int choice, int id) {
        this.validationNumber = validationNumber;
        this.choice = choice;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public int getVote() {
        return id;
    }

    public void changeToTrump(){
        this.choice = 1;
    }

    public void setValidationNumber(BigInteger validationNumber) {
        this.validationNumber = validationNumber;
    }

    public String clientToCTF() {
        return "id=" + id;
    }

    public String fromCTF() {
        return validationNumber.toString();
    }

    public String toVote() {
        return id + "-" + validationNumber.toString() + "-" + choice;
    }

    public String idAndVote() {
        return "ID: " + id + ", Vote: " + choice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Voter voter = (Voter) o;

        return id == voter.id;
    }

    @Override
    public String toString() {
        return "Voter{" +
                "validationNumber=" + validationNumber +
                ", choice=" + choice +
                ", id=" + id +
                '}';
    }
}
