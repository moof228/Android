package com.example.myapplication5;

public class User {
    private int m_id;
    private String m_login;
    private String m_pass;

    public User() {}

    public User(int id, String login, String pass)
    {
        m_id = id;
        m_login = login;
        m_pass = pass;
    }

    public User(String login, String pass)
    {
        m_login = login;
        m_pass = pass;
    }

    public int getID()
    { return m_id; }

    public void setID(int id)
    { m_id = id; }

    public String getLogin()
    { return m_login; }

    public void setLogin(String login)
    { m_login = login; }

    public String getPass()
    { return m_pass; }

    public void setPass(String pass)
    { m_pass = pass; }


}
