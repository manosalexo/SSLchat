package ssl.chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A simple Swing-based client for the chat server.  Graphically
 * it is a frame with a text field for entering messages and a
 * textarea to see the whole dialog.
 *
 * The client follows the Chat Protocol which is as follows.
 * When the server sends "SUBMITNAME" the client replies with the
 * desired screen name.  The server will keep sending "SUBMITNAME"
 * requests as long as the client submits screen names that are
 * already in use.  When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all
 * chatters connected to the server.  When the server sends a
 * line beginning with "MESSAGE " then all characters following
 * this string should be displayed in its message area.
 */
public class ChatClient {
	private final int PORT= 9001;
	private SSLSocket sslSocket = null;
    private BufferedReader in;
    private PrintWriter out;
    
    JFrame frame= new JFrame("Chatter");
    JTextField textField = new JTextField(40);
    JTextArea messageArea= new JTextArea(8, 40);

    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Return in the
     * listener sends the textfield contents to the server.  Note
     * however that the textfield is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED
     * message from the server.
     */
    public ChatClient() {
        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        frame.pack();

        // Add Listeners
        textField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield by sending
             * the contents of the text field to the server.    Then clear
             * the text area in preparation for the next message.
             */
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });
    }

    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
            frame, "Enter IP Address of the Server:",
            "Welcome to the Chatter",
            JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() {
        return JOptionPane.showInputDialog(
            frame, "Choose a screen name:",
            "Screen name selection",
            JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void process() throws Exception {
        // Make connection and initialise streams
        String serverAddress = getServerAddress();
		prepareClientKeys(serverAddress, PORT);
		sslSocket.startHandshake();

        in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
        out= new PrintWriter(sslSocket.getOutputStream(), true);

        // Process all messages from server, according to the protocol.
        while(true) {
            String line= in.readLine();
            if(line.startsWith("SUBMITNAME")) {
                out.println(getName());
            }else if(line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
            }else if(line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");
            }
        }
    }
    
    
	private void prepareClientKeys(String host, int port) throws Exception {
		//Load client private key================================
		KeyStore clientKeys = KeyStore.getInstance("JKS");
		clientKeys.load(new FileInputStream("keystore/plainclient.jks"), "mypass".toCharArray());
		KeyManagerFactory clientKeyManager = KeyManagerFactory.getInstance("SunX509");
		clientKeyManager.init(clientKeys, "mypass".toCharArray());
		
		//Load server public key=================================
		KeyStore serverPub = KeyStore.getInstance("JKS");
		serverPub.load(new FileInputStream("keystore/serverpub.jks"), "mypass".toCharArray());
		TrustManagerFactory trustManager = TrustManagerFactory.getInstance("SunX509");
		trustManager.init(serverPub);
		
		//Use keys to create SSLSoket============================
		SSLContext ssl = SSLContext.getInstance("TLS");
		ssl.init(clientKeyManager.getKeyManagers(), trustManager.getTrustManagers(), SecureRandom.getInstance("SHA1PRNG"));
		sslSocket= (SSLSocket) ssl.getSocketFactory().createSocket(host, port);
	}

    /**
     ***************************************************************
     * Runs the client as an application with a closeable frame.
     ***************************************************************/
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.process();
    }
}