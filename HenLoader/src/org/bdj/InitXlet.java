package org.bdj;

import java.io.*;
import java.util.*;
import javax.tv.xlet.*;
import java.awt.BorderLayout;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.dvb.event.UserEvent;
import org.dvb.event.EventManager;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.bluray.ui.event.HRcEvent;
import org.bdj.sandbox.DisableSecurityManagerAction;
import org.bdj.external.*;

public class InitXlet implements Xlet, UserEventListener
{
    private static InitXlet instance;
    public static class EventQueue
    {
        private LinkedList l;
        int cnt = 0;
        EventQueue()
        {
            l = new LinkedList();
        }
        public synchronized void put(Object obj)
        {
            l.addLast(obj);
            cnt++;
        }
        public synchronized Object get() {
            if (cnt == 0) return null;
            Object o = l.getFirst();
            l.removeFirst();
            cnt--;
            return o;
        }
    }
    private EventQueue eq;
    private HScene scene;
    private Screen gui;
    private XletContext context;
    private static PrintStream console;
    private static final ArrayList messages = new ArrayList();
    public void initXlet(XletContext context)
    {
        // Privilege escalation
        try {
            DisableSecurityManagerAction.execute();
        } catch (Exception e) {}

        instance = this;
        this.context = context;
        this.eq = new EventQueue();
        scene = HSceneFactory.getInstance().getDefaultHScene();
        try
        {
            gui = new Screen(messages);
            gui.setSize(Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT); // BD screen size
            scene.add(gui, BorderLayout.CENTER);
            UserEventRepository repo = new UserEventRepository("input");
            repo.addKey(Constants.BUTTON_X);
            repo.addKey(Constants.BUTTON_O);
            repo.addKey(Constants.BUTTON_U);
            repo.addKey(Constants.BUTTON_D);
            EventManager.getInstance().addUserEventListener(this, repo);
            (new Thread()
            {
                public void run()
                {
                    try
                    {
                        scene.repaint();
                        console = new PrintStream(new MessagesOutputStream(messages, scene));
                        //InputStream is = getClass().getResourceAsStream("/program.data.bin");
                        //CRunTime.init(is);

                        Logger.log(console, "Hen Loader LP v1.0, based on:");
                        Logger.log(console, "- GoldHEN 2.4b18.7 by SiSTR0");
                        Logger.log(console, "- poops code by theflow0");
                        Logger.log(console, "- lapse code by Gezine");
                        Logger.log(console, "- BDJ build environment by kimariin");
                        Logger.log(console, "- java console by sleirsgoevy");
                        Logger.log(console, "");
                        System.gc(); // this workaround somehow makes Call API working
                        if (System.getSecurityManager() != null) {
                            Logger.log(console,"Priviledge escalation failure, unsupported firmware?");
                        } else {
                            Kernel.initializeKernelOffsets();
                            String fw = Helper.getCurrentFirmwareVersion();
                            Logger.log(console, "Firmware: " + fw);
                            if (!KernelOffset.hasPS4Offsets())
                            {
                                Logger.log(console, "Unsupported Firmware");
                            } else {
                                boolean lapseSupported = (!fw.equals("12.50") && !fw.equals("12.52"));
                                int lapseFailCount = 0;

                                while (true)
                                {
                                    int c = 0;
                                    if (!lapseSupported) {
                                        if (runExploit(false,Constants.BUTTON_O,console,lapseFailCount)) break;
                                    } else {
                                        Logger.log(console, "\nSelect the mode to run:");
                                        Logger.log(console, "* X = Lapse");
                                        Logger.log(console, "* O = Poops");
                                        while ((c != Constants.BUTTON_O || !lapseSupported) && c != Constants.BUTTON_X )
                                        {
                                            c = pollInput();
                                        }

                                        if (c == Constants.BUTTON_X && lapseSupported)
                                        {
                                            if (runExploit(true,Constants.BUTTON_X,console,lapseFailCount++)) break;
                                        } else {
                                            if (runExploit(false,Constants.BUTTON_O,console,lapseFailCount)) break;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable e) 
                    {
                        scene.repaint();
                    }
                }
            }).start();
        }
        catch(Throwable e)
        {
            printStackTrace(e);
        }
        scene.validate();
    }
    public void startXlet()
    {
        gui.setVisible(true);
        scene.setVisible(true);
        gui.requestFocus();
    }
    public void pauseXlet()
    {
        gui.setVisible(false);
    }
    public void destroyXlet(boolean unconditional)
    {
        scene.remove(gui);
        scene = null;
    }
    private void printStackTrace(Throwable e)
    {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        Logger.log(console, sw.toString());
    }
    public void userEventReceived(UserEvent evt)
    {
        if (evt.getType() == HRcEvent.KEY_PRESSED)
        {
            switch (evt.getCode())
            {
                case Constants.BUTTON_U:
                    gui.top += 270;
                    scene.repaint();
                    return;
                case Constants.BUTTON_D:
                    gui.top -= 270;
                    scene.repaint();
                    return;
                default:
                    eq.put(new Integer(evt.getCode()));
            }
        }
    }
    public static void repaint()
    {
        instance.scene.repaint();
    }
    public static int pollInput()
    {
        Object ans = instance.eq.get();
        return (ans == null) ? 0 : ((Integer) ans).intValue();
    }
    private boolean runExploit(boolean lapseSupported, int button, PrintStream console, int lapseFailCount)
    {
        int result;
        try {
            result = (button == Constants.BUTTON_X && lapseSupported)
              ? org.bdj.external.Lapse.main(console)
              : org.bdj.external.Poops.main(console);
              
            if (result == 0)
            {
                Logger.log(console, "Success");
                return true;
            } else if (button == Constants.BUTTON_X && lapseSupported && (result <= -6 || lapseFailCount >= 3))
            {
                Logger.log(console, "Fatal fail(" + result + "), please REBOOT PS4");
                return true;
            } else 
            {
                Logger.log(console, "Failed (" + result + "), but you can try again");
                return false;
            }
        } catch (Exception e)
        {
            Logger.log(console, "Exception: " + e.getMessage());
            printStackTrace(e);
            return true;
        }
    }
}
