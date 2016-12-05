import Common.Server;
import Common.Settings;
import Common.Voter;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.*;

public class CTF implements Runnable {
    // Constants
    private static final String CTFTRUSTSTORE = Settings.KEYLOCATION + "CTFTruststore.ks";
    private static final String CTFKEYSTORE   = Settings.KEYLOCATION + "CTFKeystore.ks";
    private static final String CTFPASSWORD   = "password";

    // String versions of CLA's validation numbers
    private Vector<String> authorizedVoters = new Vector<>();
    private Vector<Voter> voters = new Vector<>();
    private Map<Integer, Integer> votes = new HashMap<Integer, Integer>();

    // Server/client socket and IO vars
    private SSLSocket incoming;
    private BufferedReader serverInput;
    private PrintWriter serverOutput;

    /**
     * Constructor
     * @param incoming
     */
    public CTF(SSLSocket incoming) {
        this.incoming = incoming;
    }

    /**
     *
     * @param authorizedVoters
     */
    public void setAuthorizedVoters(Vector<String> authorizedVoters) {
        this.authorizedVoters = authorizedVoters;
    }

    /**
     * Setter for voters
     * @param voters
     */
    public void setVoters(Vector<Voter> voters) {
        this.voters = voters;
    }

    /**
     * Setter for votes
     * @param votes
     */
    public void setVotes(Map<Integer, Integer> votes) {
        this.votes = votes;
    }

    /**
     * Registers the validation number from client
     * @throws Exception
     */
    private void registerValidationNumber() throws Exception {
        String str = serverInput.readLine();
        System.out.println("s: " + str);
        if (!authorizedVoters.contains(str)) {
            authorizedVoters.add(str);
        }
    }

    /**
     * Register the actual vote
     * @throws Exception
     */
    private void registerVote() throws Exception {
        String str = serverInput.readLine();
        System.out.println("s: " + str);
        String[] s = str.split("-");
        if (authorizedVoters.contains(s[1])) {
            int id = Integer.parseInt(s[0]),
                choice = Integer.parseInt(s[2]);
            BigInteger validationNumber = new BigInteger(s[1]);
            Voter v = new Voter(validationNumber, choice, id);
            if (!voters.contains(v)) {
                System.out.println(v);
                voters.add(v);
                // Save the vote
                votes.put(choice, votes.getOrDefault(choice, 0) + 1);
            }
        }
    }

    /**
     * Send result to server
     * @throws Exception
     */
    private void sendResult() throws Exception {
        int total = voters.size();
        serverOutput.println("Total votes: " + total);
        // get all votes and calculate their percentage
        for (Map.Entry<Integer, Integer> v : votes.entrySet()) {
            float res = 100 * v.getValue() / total;
            serverOutput.println("Alternative " + v.getKey() + ": "
                    + v.getValue() + " (" +  res + "%)");
        }
        serverOutput.println("The voters:");
        for (Voter v : voters) {
            serverOutput.println(v.idAndVote());
        }
        serverOutput.println(Settings.Commands.END);
    }

    public void run() {
        try {
            // Prepare incoming connections
            serverInput = new BufferedReader(
                    new InputStreamReader(incoming.getInputStream()));
            serverOutput = new PrintWriter(incoming.getOutputStream(), true);
            String str = serverInput.readLine();
            while (str != null) {
                switch (str) {
                    case Settings.Commands.REGISTER_VALID:
                        registerValidationNumber();
                        break;
                    case Settings.Commands.REGISTER_VOTE:
                        registerVote();
                        break;
                    case Settings.Commands.REQUEST_RESULT:
                        sendResult();
                        break;
                    default:
                        System.out.println("Unknown command: " + str);
                        break;
                }

                str = serverInput.readLine();
            }
            incoming.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Main function for the CTF, starts the server
     * @param args
     */
    public static void main(String[] args) {
        try {
            Server s = new Server(CTFKEYSTORE, CTFTRUSTSTORE, CTFPASSWORD, Settings.CTF_PORT);
            // Shared resources for all threads
            Vector<String> authorizedVoters = new Vector<>();
            Vector<Voter> voters = new Vector<>();
            Map<Integer, Integer> votes = new HashMap<Integer, Integer>();

            while (true) {
                SSLSocket socket = (SSLSocket) s.getServerSocket().accept();
                System.out.println("New client connected");

                CTF c = new CTF(socket);
                c.setAuthorizedVoters(authorizedVoters);
                c.setVoters(voters);
                c.setVotes(votes);
                Thread t = new Thread(c);
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
