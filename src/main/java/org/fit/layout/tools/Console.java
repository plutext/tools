/**
 * Console.java
 *
 * Created on 27. 1. 2015, 13:40:51 by burgetr
 */
package org.fit.layout.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;

import javax.script.ScriptException;

import jline.console.ConsoleReader;

import org.fit.layout.process.ScriptableProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author burgetr
 */
public class Console
{
    private static Logger log = LoggerFactory.getLogger(Console.class);
    
    private ScriptableProcessor proc;
    
    public Console()
    {
        proc = new ScriptableProcessor();
        proc.put("console", this);
        init();
    }
    
    public void interactiveSession(InputStream in, PrintStream out, PrintStream err) throws IOException
    {
        BufferedReader rin = new BufferedReader(new InputStreamReader(in));
        Writer wout = new OutputStreamWriter(out);
        Writer werr = new OutputStreamWriter(err);
        proc.setIO(rin, wout, werr);
        
        ConsoleReader reader = new ConsoleReader(in, out);
        reader.setPrompt(prompt());
        
        try
        {
            initSession();
        } catch (ScriptException e) {
            log.error(e.getMessage());
        }
        
        while (true)
        {
            proc.flushIO();
            out.println();
            String cmd = reader.readLine();
            if (cmd == null)
                break;
            try
            {
                proc.execCommand(cmd);
            } catch (ScriptException e) {
                log.error(e.getMessage());
            }
        }
        out.println();
    }
    
    protected String prompt()
    {
        return "FitLayout> ";
    }
    
    protected ScriptableProcessor getProcessor()
    {
        return proc;
    }
    
    /**
     * This is called when the console is created. Usable for adding custom object to the script engine.
     */
    protected void init()
    {
        //nothing just now
    }
    
    /**
     * This function is called at the beginning of the interactive session. Usable for executing
     * various init scripts.  
     * @throws ScriptException
     */
    protected void initSession() throws ScriptException
    {
        proc.execInternal("init.js");
    }
    
    public void exit()
    {
        System.exit(0);
    }
    
    //=============================================================================================
    
    public static void main(String[] args)
    {
        System.out.println("FitLayout interactive console");
        Console con = new Console();
        try
        {
            con.interactiveSession(System.in, System.out, System.err);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
