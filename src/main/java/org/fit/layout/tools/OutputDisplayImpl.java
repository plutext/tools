/**
 * OutputDisplayImpl.java
 *
 * Created on 31. 10. 2014, 13:47:46 by burgetr
 */
package org.fit.layout.tools;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.Set;

import org.fit.layout.api.OutputDisplay;
import org.fit.layout.model.Area;
import org.fit.layout.model.Border;
import org.fit.layout.model.Box;
import org.fit.layout.model.Page;
import org.fit.layout.model.Rectangular;
import org.fit.layout.model.Tag;

/**
 * An output display implementation that shows the areas on a Graphics2D device.
 * 
 * @author burgetr
 */
public class OutputDisplayImpl implements OutputDisplay
{
    private Graphics2D g;
    private Color boxLogicalColor = Color.RED;
    private Color boxContentColor = Color.GREEN;
    private Color areaBoundsColor = Color.MAGENTA;
    
    public OutputDisplayImpl(Graphics2D g)
    {
        this.g = g;
    }

    @Override
    public Graphics2D getGraphics()
    {
        return g;
    }

    public void setGraphics(Graphics2D g)
    {
        this.g = g;
    }
    
    @Override
    public void drawPage(Page page)
    {
        recursivelyDrawBoxes(page.getRoot());
    }

    private void recursivelyDrawBoxes(Box root)
    {
        drawBox(root);
        for (int i = 0; i < root.getChildCount(); i++)
            recursivelyDrawBoxes(root.getChildBox(i));
    }
    
    @Override
    public void drawBox(Box box)
    {
        Box.Type type = box.getType();
        
        if (type == Box.Type.TEXT_CONTENT)
        {
            g.setColor(box.getColor());
            
            //setup the font
            Font font = new Font("Serif", Font.PLAIN, 12);
            String fmlspec = box.getFontFamily();
            float fontsize = box.getFontSize();
            int fs = Font.PLAIN;
            if (box.getFontWeight() > 0.5f)
                fs = Font.BOLD;
            if (box.getFontStyle() > 0.5f)
                fs = fs | Font.ITALIC;
            
            //TODO underline and overline
            font = new Font(fmlspec, fs, (int) fontsize);
            g.setFont(font);
            
            String text = box.getText();
            FontMetrics fm = g.getFontMetrics();
            Rectangle2D rect = fm.getStringBounds(text, g);
            g.drawString(text, box.getX1() + (int) rect.getX(), box.getY1() - (int) rect.getY());
        }
        else if (type == Box.Type.REPLACED_CONTENT)
        {
            g.setColor(box.getColor());
            Rectangular r = box.getBounds();
            g.drawRect(r.getX1(), r.getY1(), r.getWidth() - 1, r.getHeight() - 1);
        }
        else //element boxes
        {
            Rectangular r = box.getBounds();
            //background color
            Color bg = box.getBackgroundColor();
            if (bg != null)
            {
                g.setColor(bg);
                g.fillRect(r.getX1(), r.getY1(), r.getWidth() - 1, r.getHeight() - 1);
            }
            //borders
            Stroke oldStroke = g.getStroke();
            if (box.hasTopBorder())
            {
                final Border bst = box.getBorderStyle(Border.Side.TOP);
                drawBorder(g, r.getX1(), r.getY1(), r.getX2(), r.getY1(), bst.getWidth(), 0, 0, bst, false);
            }
            if (box.hasRightBorder())
            {
                final Border bst = box.getBorderStyle(Border.Side.RIGHT);
                drawBorder(g, r.getX2(), r.getY1(), r.getX2(), r.getY2(), bst.getWidth(), -bst.getWidth() + 1, 0, bst, true);
            }
            if (box.hasBottomBorder())
            {
                final Border bst = box.getBorderStyle(Border.Side.BOTTOM);
                drawBorder(g, r.getX1(), r.getY2(), r.getX2(), r.getY2(), bst.getWidth(), 0, -bst.getWidth() + 1, bst, true);
            }
            if (box.hasLeftBorder())
            {
                final Border bst = box.getBorderStyle(Border.Side.LEFT);
                drawBorder(g, r.getX1(), r.getY1(), r.getX1(), r.getY2(), bst.getWidth(), 0, 0, bst, false);
            }
            g.setStroke(oldStroke);
        }

    }
    
    private void drawBorder(Graphics2D g, int x1, int y1, int x2, int y2,
            int width, int right, int down, Border style, boolean reverse)
    {
        if (style.getWidth() >= 1)
        {
            g.setColor(style.getColor());
            g.setStroke(new BorderStroke(width, style.getStyle(), reverse));
            g.draw(new Line2D.Double(x1 + right, y1 + down, x2 + right, y2 + down));
        }
    }

    
    //=================================================================================

    @Override
    public void drawExtent(Box box)
    {
        //draw the visual content box
        g.setColor(boxLogicalColor);
        Rectangular r = box.getBounds();
        g.drawRect(r.getX1(), r.getY1(), r.getWidth() - 1, r.getHeight() - 1);
        
        //draw the visual content box
        g.setColor(boxContentColor);
        r = box.getVisualBounds();
        g.drawRect(r.getX1(), r.getY1(), r.getWidth() - 1, r.getHeight() - 1);
    }
    
    @Override
    public void drawExtent(Area area)
    {
        Rectangular bounds = area.getBounds();
        Color c = g.getColor();
        g.setColor(areaBoundsColor);
        g.drawRect(bounds.getX1(), bounds.getY1(), bounds.getWidth() - 1, bounds.getHeight() - 1);
        g.setColor(c);
    }

    @Override
    public void drawRectangle(Rectangular rect, Color color)
    {
        Color c = g.getColor();
        g.setColor(color);
        g.fillRect(rect.getX1(), rect.getY1(), rect.getWidth(), rect.getHeight());
        g.setColor(c);
    }
    
    @Override
    public void colorizeByTags(Area area, Set<Tag> s)
    {
        if (!s.isEmpty())
        {
            Rectangular bounds = area.getBounds();
            Color c = g.getColor();
            float step = (float) bounds.getHeight() / s.size();
            float y = bounds.getY1();
            for (Iterator<Tag> it = s.iterator(); it.hasNext();)
            {
                Tag tag = it.next();
                g.setColor(stringColor(tag.getValue()));
                g.fillRect(bounds.getX1(), (int) y, bounds.getWidth(), (int) (step+0.5));
                y += step;
            }
            g.setColor(c);
        }
    }

    @Override
    public void colorizeByClass(Area area, String cname)
    {
        if (cname != null && !cname.equals("") && !cname.equals("none"))
        {
            Rectangular bounds = area.getBounds();
            Color c = g.getColor();
            float step = (float) bounds.getHeight();
            float y = bounds.getY1();
            g.setColor(stringColor(cname));
            g.fillRect(bounds.getX1(), (int) y, bounds.getWidth(), (int) (step+0.5));
            g.setColor(c);
        }
    }

    protected Color stringColor(String cname)                                 
    {                                                                            
            if (cname == null || cname.equals(""))       
                    return Color.WHITE;                                                 
                                                                                 
            String s = new String(cname);                                        
            while (s.length() < 6) s = s + s;                                    
            int r = (int) s.charAt(0) *  (int) s.charAt(1);                      
            int g = (int) s.charAt(2) *  (int) s.charAt(3);                      
            int b = (int) s.charAt(4) *  (int) s.charAt(5);                      
            Color ret = new Color(100 + (r % 150), 100 + (g % 150), 100 + (b % 150), 128);              
            //System.out.println(cname + " => " + ret.toString());               
            return ret;                                                          
    }
    
}
