
public class Message {
	
	private String m_propritaire;
	private int m_idProp;
	private String m_texte;
	
	public Message()
	{
		this.m_propritaire = "";
        this.m_idProp = 0;
        this.m_texte = "";
	}
	
	public Message(String m_propritaire, int m_idProp, String m_texte) 
	{
        this.m_propritaire = m_propritaire;
        this.m_idProp = m_idProp;
        this.m_texte = m_texte;
    }

    public String getM_propritaire() 
    {
        return m_propritaire;
    }

    public void setM_propritaire(String m_propritaire) 
    {
        this.m_propritaire = m_propritaire;
    }

    public int getM_idProp() 
    {
        return m_idProp;
    }

    public void setM_idProp(int m_idProp) 
    {
        this.m_idProp = m_idProp;
    }

    public String getM_texte() 
    {
        return m_texte;
    }

    public void setM_texte(String m_texte) 
    {
        this.m_texte = m_texte;
    }
}
