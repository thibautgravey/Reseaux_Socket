/***
 * ClientGUI
 * A simple GUI for client side chat system
 * Date: 13/10/20
 * Authors: BRANCHEREAU Corentin, GRAVEY Thibaut
 */

package client;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientGUI implements ActionListener, WindowListener {

    private boolean connected;

    private JButton connect;
    private JButton send;
    private JTextField tfEnter;
    private JTextField tfHost;
    private JTextField tfPort;
    private JTextField tfPseudo;
    private JTextArea textArea;

    private static Socket echoSocket;
    private static DataOutputStream socOut;
    private static BufferedReader socIn;

    /**
     *  constructor
     *  create the entire GUI with Swing
     **/
    public ClientGUI(){
        connected = false;

        //Create the frame
        JFrame frame = new JFrame("B01 Chat System");

        //Properties
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500,500);
        GridLayout topLayout = new GridLayout(3,3);

        //Create Components
        //Top side
        JPanel panelTop = new JPanel();
        JPanel subTop = new JPanel();
        JPanel subBot = new JPanel();
        JLabel labelHost = new JLabel("Host");
        JLabel labelPort = new JLabel("Port");
        JLabel labelPseudo = new JLabel("Pseudo");
        tfHost = new JTextField(15);
        tfPort = new JTextField(15);
        tfPseudo = new JTextField(15);
        connect = new JButton("Connect");
        tfHost.setText("127.0.0.1");
        tfPort.setText("1024");
        labelHost.setHorizontalAlignment(JLabel.CENTER);
        labelPort.setHorizontalAlignment(JLabel.CENTER);
        labelPseudo.setHorizontalAlignment(JLabel.CENTER);
        subTop.setLayout(topLayout);
        subTop.add(labelHost);
        subTop.add(tfHost);
        subTop.add(labelPort);
        subTop.add(tfPort);
        subTop.add(labelPseudo);
        subTop.add(tfPseudo);
        subBot.add(connect);
        panelTop.add(BorderLayout.CENTER,subTop);
        panelTop.add(BorderLayout.SOUTH,subBot);

        //Center side
        textArea = new JTextArea();
        JScrollPane scroll = new JScrollPane(textArea);


        //Bottom side
        JPanel panelBottom = new JPanel();
        JLabel labelEnter = new JLabel("Text Input");
        tfEnter = new JTextField(30);
        send = new JButton("Send");
        panelBottom.add(labelEnter);
        panelBottom.add(tfEnter);
        panelBottom.add(send);
        send.setEnabled(false);
        tfEnter.setEnabled(false);

        //Listener
        connect.addActionListener(this);
        send.addActionListener(this);
        tfEnter.addActionListener(this);

        //Add Components
        frame.getContentPane().add(BorderLayout.SOUTH, panelBottom);
        frame.getContentPane().add(BorderLayout.CENTER,scroll);
        frame.getContentPane().add(BorderLayout.NORTH,panelTop);

        //Display GUI
        frame.setVisible(true);
    }

    /**
     *  main method
     *  launch the GUI
     **/
    public static void main(String[] args){
        ClientGUI clientGUI = new ClientGUI();
    }

    /**
     *  action performed from listeners
     *  listening to button and text input to interact with socket and GUI
     **/
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if(source instanceof JButton){
            JButton toCompare = (JButton) source;
            if(toCompare==connect){
                if(!tfPseudo.getText().isEmpty()){
                    System.out.println("Connect");
                    if(!connected) {
                        startSocket(tfHost.getText(), tfPort.getText(), tfPseudo.getText());
                    } else {
                        sendDisconnection();
                    }
                } else {
                    infosBox("You can't enter the chat with an empty pseudo.","Pseudo");
                }

            } else if (toCompare==send){
                if(tfEnter.getText().isEmpty()){
                    infosBox("You can't send an empty message","Empty message");
                } else {
                    System.out.println("Send Message : " + tfEnter.getText());
                    sendMessage(tfEnter.getText());
                    tfEnter.selectAll();
                    tfEnter.replaceSelection("");
                }
            }
        } else if (source instanceof JTextField){
            JTextField toCompare = (JTextField) source;
            if(toCompare==tfEnter){
                System.out.println("Enter in Text Input !");
                sendMessage(tfEnter.getText());
                tfEnter.selectAll();
                tfEnter.replaceSelection("");
            }
        }
    }

    /**
     *  start the client socket for server communication
     **/
    public void startSocket(String host, String port, String name){
        boolean error = false;
        try {
            // creation socket ==> connexion
            echoSocket = new Socket(host,new Integer(port).intValue());
            socIn = new BufferedReader(
                    new InputStreamReader(echoSocket.getInputStream()));
            socOut = new DataOutputStream(echoSocket.getOutputStream());
            socOut.writeUTF(name);
            socOut.flush();
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host:" + host);
            infosBox("Don't know about host : "+host,"Host");
            error = true;
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for "
                    + "the connection to:"+ host);
            infosBox("Couldn't get I/O for the connection to : "+host,"I/O");
            error = true;
        }

        if(!error) {
            ServerListenerThread serverListenerThread = new ServerListenerThread(echoSocket, socIn, textArea);
            serverListenerThread.start();

            connected = true;
            tfHost.setEnabled(false);
            tfPort.setEnabled(false);
            tfPseudo.setEnabled(false);
            connect.setText("Disconnect");
            send.setEnabled(true);
            tfEnter.setEnabled(true);
        }
    }

    /**
     *  send a message using the output stream of the socket
     *  @param msg the message before protocol generation
     **/
    private void sendMessage(String msg){
        //Generer le protocole pour envoyer un message
        int protocolType = 0;

        try {
            socOut.writeInt(protocolType);
            socOut.writeUTF(msg);
            socOut.flush();
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for "
                    + "the connection to:"+ echoSocket.getInetAddress());
            System.exit(1);
        }
    }

    /**
     *  send a disconnection message to prevent the server with protocol
     **/
    public void sendDisconnection(){
        int protocolType = 1;

        try {
            socOut.writeInt(protocolType);
            socOut.flush();

            socOut.close();
            socIn.close();
            echoSocket.close();
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for "
                    + "the connection to:"+ echoSocket.getInetAddress());
            System.exit(1);
        }

        tfHost.setEnabled(true);
        tfPort.setEnabled(true);
        tfPseudo.setEnabled(true);
        connect.setText("Connect");
        send.setEnabled(false);
        tfEnter.setEnabled(false);

        connected = false;
    }

    /**
     *  alert the client from bad GUI input
     **/
    private void infosBox(String infoMessage, String titleBar){
        JOptionPane.showMessageDialog(null,infoMessage,"Error : "+titleBar,JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    /**
     *  close application when window is closing
     **/
    @Override
    public void windowClosing(WindowEvent e) {
        System.out.println("Fermeture de l'application, socket connecté : "+connected);
        if(connected){
            sendDisconnection();
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
