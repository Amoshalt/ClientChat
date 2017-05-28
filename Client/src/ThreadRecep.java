import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ThreadRecep extends Thread
{
	public ArrayList<DatagramPacket> m_msg;
	protected boolean m_c, m_pause;
	protected DatagramSocket m_socket;
	protected InetAddress m_ia;
	protected int m_port;

	public ThreadRecep(DatagramSocket ds, InetAddress ia, int port)
	{
		m_port = port;
		m_ia = ia;
		m_socket = ds;
		m_msg = new ArrayList<DatagramPacket>();
		m_c = true;
		// TODO Auto-generated constructor stub
	}

	public void Stop()
	{
		m_c  = false;
	}

	public void run()
	{
		while(m_c)
		{
			while(m_pause)
			{
				try
				{
					Thread.sleep(100);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			DatagramPacket dp = Reception();
			if(dp != null)
			{
				System.out.println("Recep : " + new String(dp.getData()));
				m_msg.add(dp);
			}
		}
	}
	public DatagramPacket Reception()
    {
    	//m_timeout = false;
    	DatagramPacket packet;
    	try
    	{
    		//On attend jusqu'à 1 seconde pour recevoir une réponse, si on ne reçoit qu'un ACK, on recommence
    		do
    		{
	    		m_socket.setSoTimeout(1000);
	    		packet = new DatagramPacket(new byte[512], 512);
	    		m_socket.receive(packet);
    		}while(packet.getData()[0] == 1);
    		m_port = packet.getPort();
    		//Si le packet n'est pas un ACK, on en retourne un au client
    		SendACK();
    		return packet;
    	}
    	catch(IOException e)
    	{
    		//Timeout
    		if(!(e instanceof SocketTimeoutException))
    			e.printStackTrace();
    		//else System.out.println("Timeout :\n> Adresse : " + m_ia.getHostAddress() + "\n> Port : " + m_port);
    	}
    	return null;
    }

    //Retourne "true" à la la réception du ACK correspondant au numéro du DTG,
    //		   "false" si on passe par un Timeout
    public boolean ACK()
    {
    	DatagramPacket packet;
    	try
    	{
    		m_socket.setSoTimeout(500);
    		//On reçoit jusqu'à recevoir un ACK pour le DTG voulu ou avoir un Timeout
    		while(true)
    		{
	    		packet = new DatagramPacket(new byte[512], 512);
	    		m_socket.receive(packet);
	    		//Si on a un ACK pour le DTG, on retourne "true"
	    		if(packet.getData()[0] == 1)
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
    		byte[] data = new byte[] {1};
    		packet = new DatagramPacket(data, data.length, m_ia, m_port);
    		m_socket.send(packet);
    	}
    	catch(IOException e)
    	{
    		e.printStackTrace();
    	}
    }

}
