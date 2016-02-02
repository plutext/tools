/**
 * HTMLOutputOperator.java
 *
 * Created on 12. 1. 2016, 11:42:43 by burgetr
 */
package org.fit.layout.tools.io;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.fit.layout.impl.BaseOperator;
import org.fit.layout.model.Area;
import org.fit.layout.model.AreaTree;
import org.fit.layout.model.Border;
import org.fit.layout.model.Box;
import org.fit.layout.model.Rectangular;

/**
 * This operator serializes the area tree to an HTML file.
 * 
 * @author burgetr
 */
public class HTMLOutputOperator extends BaseOperator
{
    /** Default length unit */
    protected static final String UNIT = "px";
    
    /** Should we produce the HTML header and footer? */
    protected boolean produceHeader;
    
    /** Path to the output file/ */
    protected String filename;
    
    protected final String[] paramNames = { "filename", "produceHeader" };
    protected final ValueType[] paramTypes = { ValueType.STRING, ValueType.BOOLEAN };


    
    public HTMLOutputOperator()
    {
        produceHeader = false;
        filename = "out.html";
    }

    public HTMLOutputOperator(String filename, boolean produceHeader)
    {
        this.filename = filename;
        this.produceHeader = produceHeader;
    }

    @Override
    public String getId()
    {
        return "FitLayout.Tools.HTMLOutput";
    }

    @Override
    public String getName()
    {
        return "HTML serialization of the area tree";
    }

    @Override
    public String getDescription()
    {
        return "Serializes the area tree to an HTML file";
    }

    @Override
    public String[] getParamNames()
    {
        return paramNames;
    }

    @Override
    public ValueType[] getParamTypes()
    {
        return paramTypes;
    }

    public boolean getProduceHeader()
    {
        return produceHeader;
    }

    public void setProduceHeader(boolean produceHeader)
    {
        this.produceHeader = produceHeader;
    }

    public String getFilename()
    {
        return filename;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    //=====================================================================================================
    
    @Override
    public void apply(AreaTree atree)
    {
        apply(atree, atree.getRoot());
    }

    @Override
    public void apply(AreaTree atree, Area root)
    {
        try
        {
            PrintWriter out = new PrintWriter(filename);
            dumpTo(atree, out);
            out.close();
        } catch (FileNotFoundException e) {
            System.err.println("Couldn't create output HTML file " + filename);
        }
    }

    //=====================================================================================================
    
    /**
     * Formats the complete tag tree to an output stream
     */
    public void dumpTo(AreaTree tree, PrintWriter out)
    {
        if (produceHeader)
        {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>" + tree.getRoot().getPage().getTitle() + "</title>");
            out.println("<meta charset=\"utf-8\">");
            out.println("</head>");
            out.println("<body>");
        }
        recursiveDump(tree.getRoot(), 1, out);
        if (produceHeader)
        {
            out.println("</body>");
            out.println("</html>");
        }
    }
    
    //=====================================================================
    
    private void recursiveDump(Area a, int level, java.io.PrintWriter p)
    {
        String tagName = "div";
        
        String stag = "<" + tagName
                        + " style=\"" + getAreaStyle(a) + "\""
                        + ">";

        String etag = "</" + tagName + ">";
        
        if (a.getChildCount() > 0)
        {
            indent(level, p);
            p.println(stag);
            
            for (int i = 0; i < a.getChildCount(); i++)
                recursiveDump(a.getChildArea(i), level+1, p);
            
            indent(level, p);
            p.println(etag);
        }
        else
        {
            indent(level, p);
            p.println(stag);
            dumpBoxes(a, p, level+1);
            indent(level, p);
            p.println(etag);
        }
        
    }
    
    private void dumpBoxes(Area a, java.io.PrintWriter p, int level)
    {
        Vector<Box> boxes = a.getBoxes();
        for (Box box : boxes)
        {
            indent(level, p);
            String stag = "<span"
                            + " style=\"" + getBoxStyle(a, box) + "\"" 
                            + ">";
            p.print(stag);
            p.print(HTMLEntities(box.getText()));
            p.println("</span>");
        }
    }
    
    protected String getAreaStyle(Area a)
    {
        Area parent = a.getParentArea();
        int px = 0;
        int py = 0;
        if (parent != null)
        {
            px = parent.getX1();
            py = parent.getY1();
            
            Border bleft = parent.getBorderStyle(Border.Side.LEFT);
            if (bleft != null)
                px += bleft.getWidth();
            Border btop = parent.getBorderStyle(Border.Side.TOP);
            if (btop != null)
                py += btop.getWidth();
        }
            
        Style style = new Style();
        style.put("position", "absolute");
        style.put("left", a.getX1() - px, UNIT);
        style.put("top", a.getY1() - py, UNIT);
        style.put("width", a.getWidth(), UNIT);
        style.put("height", a.getHeight(), UNIT);
        String bgcol = colorString(a.getBackgroundColor());
        if (!bgcol.isEmpty())
            style.put("background", bgcol);
        for (Border.Side side : Border.Side.values())
        {
            String brd = getBorderStyle(a.getBorderStyle(side));
            if (!brd.isEmpty())
                style.put("border-" + side.toString(), brd);
        }
        
        return style.toString();
    }
    
    protected String getBoxStyle(Area parent, Box box)
    {
        int px = 0;
        int py = 0;
        if (parent != null)
        {
            px = parent.getX1() + parent.getBorderStyle(Border.Side.LEFT).getWidth();
            py = parent.getY1() + parent.getBorderStyle(Border.Side.TOP).getWidth();
        }
            
        Rectangular pos = box.getVisualBounds();
        Style style = new Style();
        style.put("position", "absolute");
        style.put("top", (pos.getY1() - py), UNIT);
        style.put("left", (pos.getX1() - px), UNIT);
        style.put("color", (colorString(box.getColor())));
        style.put("font-family", box.getFontFamily());
        style.put("font-size", box.getFontSize(), UNIT);
        style.put("font-weight", ((box.getFontWeight() < 0.5f)?"normal":"bold"));
        style.put("font-style", ((box.getFontStyle() < 0.5f)?"normal":"italic"));
        String deco = "";
        if (box.getUnderline() >= 0.5f)
            deco += "underline";
        if (box.getLineThrough() >= 0.5f)
            deco += " line-through";
        if (deco.isEmpty())
            deco = "none";
        style.put("text-decoration", deco);
        for (Border.Side side : Border.Side.values())
        {
            String brd = getBorderStyle(box.getBorderStyle(side));
            if (!brd.isEmpty())
                style.put("border-" + side.toString(), brd);
        }
        
        return style.toString();
    }
    
    private String getBorderStyle(Border border)
    {
        if (border != null && border.getStyle() != Border.Style.NONE && border.getWidth() > 0)
        {
            StringBuilder ret = new StringBuilder();
            ret.append(border.getWidth()).append(UNIT);
            ret.append(' ').append(border.getStyle().toString().toLowerCase());
            ret.append(' ').append(colorString(border.getColor()));
            return ret.toString();
        }
        else
            return "";
    }
    
    private void indent(int level, java.io.PrintWriter p)
    {
        String ind = "";
        for (int i = 0; i < level*4; i++) ind = ind + ' ';
        p.print(ind);
    }
    
    private String colorString(java.awt.Color color)
    {
        if (color == null)
            return "";
        else
            return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    /**
     * Converts the CSS specification rgb(r,g,b) to #rrggbb
     * @param spec the CSS color specification
     * @return a #rrggbb string
     */
    public String colorString(String spec)
    {
        if (spec.startsWith("rgb("))
        {
            String s = spec.substring(4, spec.length() - 1);
            String[] lst = s.split(",");
            try {
                int r = Integer.parseInt(lst[0].trim());
                int g = Integer.parseInt(lst[1].trim());
                int b = Integer.parseInt(lst[2].trim());
                return String.format("#%02x%02x%02x", r, g, b);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        else
            return spec;
    }
    
    private String HTMLEntities(String s)
    {
        return s.replaceAll(">", "&gt;").replaceAll("<", "&lt;").replaceAll("&", "&amp;");
    }
    
    /**
     * Element style representation.
     * 
     * @author burgetr
     */
    protected class Style extends HashMap<String, String>
    {
        private static final long serialVersionUID = 1L;
        
        public void put(String key, int value, String unit)
        {
            put(key, value + unit);
        }
        
        public void put(String key, float value, String unit)
        {
            put(key, value + unit);
        }
        
        @Override
        public String toString()
        {
            StringBuilder ret = new StringBuilder();
            for (Map.Entry<String, String> entry : entrySet())
            {
                ret.append(entry.getKey()).append(':').append(entry.getValue()).append(';');
            }
            return ret.toString();
        }
        
    }

}