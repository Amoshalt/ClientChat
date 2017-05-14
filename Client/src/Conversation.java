
public class Conversation {
	
	private Message[] m_messages;
	private byte m_idConv;
	private int m_idMessCourant;
	
	public Conversation(Message[] messages, byte idConv) 
	{
        this.m_messages = messages;
        this.m_idConv = idConv;
    }
	public Conversation(byte idConv)
	{
		this.m_messages = new Message[10000];
		this.m_idConv = idConv;
	}

	public Conversation() 
	{
        this.m_messages = new Message[10000];
        this.m_idConv = 0;
    }
	
    public Message[] getM_messages() 
    {
        return m_messages;
    }

    public void setM_messages(Message[] messages) 
    {
        this.m_messages = messages;
    }

    public byte getIdConv() 
    {
        return m_idConv;
    }

    public void setIdConv(byte idConv) 
    {
        this.m_idConv = idConv;
    }
    
    public void addMess(byte[] data,int idProp)
    {
    	m_messages[m_idMessCourant].setM_texte(new String(data));
    	m_messages[m_idMessCourant].setM_idProp(idProp);
    }

}
