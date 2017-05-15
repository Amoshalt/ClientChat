import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class Client {
	
	public final static int Exit = 0, //Code envoyé par le client à la fermeture
			ACK = 1,
			Refuse = 2, //Erreur par défaut
			TestConnexion = 3, //Teste si le client est toujours actif
			Online = 4,	//Valeur retournée par le client pour signaler qu'il est toujours actif
			UsrListe = 7, //Liste des correspondances ID <-> username envoyé par le serveur au client à la connexion
			NewUse = 15, //Code envoyé par le serveur pour notifier un client de la connexion d'un nouvel utilisateur
			NewChat = 11, //Demande de création de chat, suivi du nom des dest.
			DelUse = 16, //Code envoyé par le serveur pour notifier un client de la déconnexion d'un utilisateur
			TestAuth = 32, //Code envoyé par le client pour s'authentifier, suivi du pseudo sans espace
			NewMsg = 42, //Envoi d'un msg, suivi de l'ID du chat et du message
			MsgOK = 43, //Code envoyé au client par le serveur pour indiquer que le message a été transmi.
			MsgErr = 44, //Code envoyé au client par le serveur pour indiquer que le message n'a pas été transmi
			NeedAuth = 100, //Demande d'authentification du serveur vers le client
			PseudoInvalide = 101, //Erreur d'authentification retournée par le serveur
			AuthOK = 102, //Validation de l'authentification
			IDChatInvalide = 110, //Impossible d'écrire dans ce chat
			RetourIDChat = 111, //Retourne l'ID du chat voulu, suivi
			ContactInvalide = 112 //Erreur : le destinataire voulu n'existe pas, suivi du pseudo invalide
			 
			;
	
	private Scanner m_sc;
	private Conversation[] m_conv;
	private int m_nbConv;
	private InetAddress m_ia;
	private int m_port;
	private DatagramSocket m_socket;
	
	//Constructor
	
	public Client (String serveur)
	{
		m_sc = new Scanner(System.in);
		m_conv = new Conversation[127];
		try {
			m_ia = InetAddress.getByName(serveur);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_port = 1235;
		m_nbConv = 0;
		try
		{
			m_socket = new DatagramSocket(m_port);
		}
		catch (SocketException e)
		{
			e.printStackTrace();
		}
		
	}
	
	//Destructor
	public void DestroyClient()
	{
		m_sc.close();
		
	}
	
	//Envoi d'un DTG jusqu'à 3 fois, test de réception du ACK
    public boolean send(byte[] tab)
    {
    	DatagramPacket dp = new DatagramPacket(tab, tab.length, m_ia, m_port);
    	System.out.println("taille: " + dp.getLength());
    	System.out.println("port : "+ dp.getPort());
    	System.out.println("adresse ; " + dp.getAddress());
    	try
    	{
    		int i;
    		//On essaye d'envoyer le DTG jusqu'à 3 fois si on ne reçoit aucun ACK
    		for(i = 0; i < 3; i = ACK() ? 5 : (i + 1))
    			m_socket.send(dp);
    		if(i == 3)
    			return false;
		}
    	catch (IOException e1)
    	{
			e1.printStackTrace();
		}
    	return true;
    }

    //Retourne "true" à la la réception du ACK correspondant au numéro du DTG,
    //		   "false" si on passe par un Timeout
    public boolean ACK()
    {
    	DatagramPacket packet;
    	try
    	{
    		//On reçoit jusqu'à recevoir un ACK pour le DTG voulu ou avoir un Timeout
    		while(true)
    		{
    			//On n'attend que 3 secondes
        		m_socket.setSoTimeout(3000);
	    		packet = new DatagramPacket(new byte[512], 512);
	    		m_socket.receive(packet);
	    		//Si on a un ACK pour le DTG, on retourne "true"
	    		if(packet.getData()[0] == ACK)
	    			return true;
    		}
    	}
    	catch(IOException e)
    	{
    		//Timeout : on retourne "false"
    		return false;
    	}
    }

    public void SendACK()
    {
    	DatagramPacket packet;
    	try
    	{
    		byte[] data = new byte[] {ACK};
    		packet = new DatagramPacket(data, data.length, m_ia, m_port);
    		m_socket.send(packet);
    	}
    	catch(IOException e)
    	{
    		e.printStackTrace();
    	}
    }

    //Receptionne un DTG et envoie un ACK
    public DatagramPacket Reception()
    {
    	DatagramPacket packet;
    	try
    	{
    		//On attend jusqu'à recevoir une réponse
    		m_socket.setSoTimeout(0);
    		packet = new DatagramPacket(new byte[512], 512);
    		m_socket.receive(packet);
    		m_ia = packet.getAddress();
    		m_port= packet.getPort();
    		SendACK();
    		return packet;
    	}
    	catch(IOException e)
    	{
    		e.printStackTrace();
    	}
    	return null;
    }

    //Traitement d'un datagramPacket
    public void Traitement(DatagramPacket dp)
    {
    	/*byte flag = 100;
    	DatagramPacket dp = new DatagramPacket(new byte[512], 512);
    	dp.setData(ByteBuffer.allocate(512).put((byte)AuthOK).put(" on est mal la".getBytes()).array());*/
    	InetAddress ia = dp.getAddress();
    	byte[] data = dp.getData();
    	int port = dp.getPort();
    	System.out.println("Adresse : " + ia + "\n" +
    					   "Port : " + port + "\n" +
    					   "Données : " + new String(data));
    	byte dpFlag= data[0];
    	switch(dpFlag)
    	{
    		case RetourIDChat:
    			NouvelleConversation(data);
    			break;
    			
    		case PseudoInvalide:
    			System.out.println("Pseudo Invalide");
    			DemandeAuth();
    			break;
    			
    		case Refuse:
    			break;
    			
    		case TestConnexion:
    			ConfirmerActivite();
    			break;
    		
    		case NeedAuth:
    			DemandeAuth();
    			break;
    			
    		case AuthOK:
    			System.out.println("Authentification reussie");
    			DemandeConv();
    			break;
    			
    		case IDChatInvalide:
    			System.out.println("serveur complet");
    			break;
    			
    		case ContactInvalide:
    			System.out.println("Contact Invalide");
    			DemandeConv();
    			break;
    			
    		case Exit:
    			break;
    			
    		case ACK:
    			break;
    			
    		case NewMsg:
    			EnregistrerMessage(data);
    			break;
    		
    	}
    	affichage(dp);
    }
    
    //Reception du message et enregistrement dans la conversation correspondante
    public void EnregistrerMessage(byte[] data)
    {
    	byte idConvAct = data[1];
    	byte idEmetteur = data[2];
    	int i = getConv(idConvAct);
    	if( i == -1)
    	{
    		NouvelleConversation(data);
    		i = getConv(idConvAct);
    	}
    	
    	Conversation conv = m_conv[i];
    	conv.addMess(data,idEmetteur);
    	
    	
    }
    
    //Demande du pseudo à l'utilisateur
    public String RentrerPseudo()
    {
    	String pseudo= "";
    	boolean contientEspace;
		
    	do
		{
    		contientEspace = false;
    		System.out.println("Rentrez votre pseudo");
    		pseudo = m_sc.nextLine();
    		int i = 0;
    		while (i < pseudo.length())
    		{
    			if(pseudo.charAt(i) == ' ')
    			{
    				contientEspace = true;
    			}
    			
    			i++;
    		}
    		
		}while (contientEspace);
    	
    	
    	System.out.println("pseudo rentré: " + pseudo);
    	return pseudo;
    }
    
    //Demande authentification au serveur
    public void DemandeAuth()
    {
    	String pseudo = RentrerPseudo();
    	
    	byte flag = 32;
    	byte end = 0;
    	ByteBuffer bbuff = ByteBuffer.allocate(512);
    	bbuff.put(flag).put(pseudo.getBytes()).put(end);
    	
    	send(bbuff.array());

       	DatagramPacket dp = Reception();
       	Traitement(dp);
    	
    }
    
    //Demande au serveur l'acces a une conversation
    public void DemandeConv()
    {
    	byte section = 0;
    	byte[] flag = new byte[2];
    	flag[0] = 11;
    	flag[1]= 1;
    	
    	
    	ByteBuffer bbuff = ByteBuffer.allocate(512);
		bbuff.put(flag);
    	String temp ="0";
    	int i = 1;
    	while (!temp.isEmpty())
		{
    		temp="";
    		System.out.println("Saisissez le "+ i + " ème destinataire \n Appuyer sur entrer si vous avez fini. \n");
    		temp = m_sc.nextLine();
    		bbuff.put(temp.getBytes());
    		bbuff.put(section);
    		i++;
		}
    	
    	send(bbuff.array());

    	DatagramPacket dp = Reception();
    	Traitement(dp);
    }
    
    //rentrer un message
    public void EntrerMessage()
    {
    	
    }
    
    //Envoyer un message
    public void EnvoyerMessage()
    {
    	byte section = 0;
    	byte[] flag = new byte[2];
    	flag[0] = 11;
    	flag[1]= 1;
    	
    	
    	ByteBuffer bbuff = ByteBuffer.allocate(512);
		bbuff.put(flag);
    	String tempMessage ="0";
    	System.out.println("Saisissez votre message \n Appuyer deux fois sur entrer si vous avez fini. \n");
    	tempMessage = m_sc.nextLine();
		bbuff.put(tempMessage.getBytes());
		bbuff.put(section);
		
    	
    	send(bbuff.array());

    	DatagramPacket dp = Reception();
    	Traitement(dp);
    }
    
    //Affichage des données
    public void affichage(DatagramPacket dp)
    {
    	System.out.println(new String(dp.getData()));
    }

    //Creation nouvelle conversation en cours
    public void NouvelleConversation(byte[] data)
    {
    	
    	m_conv[m_nbConv]= new Conversation(data[1]);
    	m_nbConv++;
    }
    
  //Creation nouvelle conversation en cours
    public void SupprimerConversation(byte id)
    {
    	
    }
    
  //Repondre à la demande d'activité du serveur
    public void ConfirmerActivite()
    {
    	
    }

    //Getter Setter
    
    //recupère l'index de la conversation avec l'id byte
    public int getConv(byte id)
    {
    	int i = 0;
    	boolean trouve = false;
    	while(i < m_conv.length && !trouve)
    	{
    		if (m_conv[i].getIdConv() == id)
    		{
    			trouve = true;
    			return i;
    		}
    		else
    		{
    			i++;
    		}
    		
    	}
    	
    	return -1;
    }
}
