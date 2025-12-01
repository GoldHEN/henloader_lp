package org.bdj;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;

public class Screen extends Container
{
    private static final long serialVersionUID = 4761178503523947426L;
    private ArrayList messages;
    private Font font;
    public int top = 40;
    public Screen(ArrayList messages)
    {
        this.messages = messages;
        font = new Font(null, Font.PLAIN, 20);
    }
    public void paint(Graphics g)
    {
        g.setColor(new Color(9, 35, 64));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setFont(font);
        g.setColor(Color.WHITE);
        for(int i = 0; i < messages.size(); i++)
        {
            String message = (String)messages.get(i);
            g.drawString(message, 0, top + (i*40));
        }
    }
}
