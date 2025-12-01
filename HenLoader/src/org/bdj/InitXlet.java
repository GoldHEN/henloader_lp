package org.bdj;

import java.io.*;
import java.util.*;
import javax.tv.xlet.*;
import java.awt.BorderLayout;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.bdj.sandbox.DisableSecurityManagerAction;
import org.bdj.external.*;

public class InitXlet implements Xlet {
    private static InitXlet instance;

    public static class EventQueue {
        private LinkedList l;
        int cnt = 0;

        EventQueue() {
            l = new LinkedList();
        }

        public synchronized void put(Object obj) {
            l.addLast(obj);
            cnt++;
        }

        public synchronized Object get() {
            if (cnt == 0)
                return null;
            Object o = l.getFirst();
            l.removeFirst();
            cnt--;
            return o;
        }
    }

    private EventQueue eq;
    private HScene scene;
    private Screen gui;
    // private XletContext context;
    private static PrintStream console;
    private static final ArrayList messages = new ArrayList();

    public void initXlet(XletContext context) {
        // Privilege escalation
        try {
            DisableSecurityManagerAction.execute();
        } catch (Exception e) {
        }

        instance = this;
        // this.context = context;
        this.eq = new EventQueue();
        scene = HSceneFactory.getInstance().getDefaultHScene();
        try {
            gui = new Screen(messages);
            gui.setSize(1920, 1080); // BD screen size
            scene.add(gui, BorderLayout.CENTER);
            (new Thread() {
                public void run() {
                    try {
                        scene.repaint();
                        console = new PrintStream(new MessagesOutputStream(messages, scene));
                        // InputStream is = getClass().getResourceAsStream("/program.data.bin");
                        // CRunTime.init(is);

                        Logger.log(console, "Hen Loader LP v1.0, based on:");
                        Logger.log(console, "- GoldHEN 2.4b18.7 by SiSTR0");
                        Logger.log(console, "- poops code by theflow0");
                        Logger.log(console, "- lapse code by Gezine");
                        Logger.log(console, "- BDJ build environment by kimariin");
                        Logger.log(console, "- java console by sleirsgoevy");
                        Logger.log(console, "");
                        System.gc(); // this workaround somehow makes Call API working
                        if (System.getSecurityManager() != null) {
                            Logger.log(console, "Priviledge escalation failure, unsupported firmware?");
                        } else {
                            Kernel.initializeKernelOffsets();
                            String fw = Helper.getCurrentFirmwareVersion();
                            Logger.log(console, "Firmware: " + fw);
                            if (!KernelOffset.hasPS4Offsets()) {
                                Logger.log(console, "Unsupported Firmware");
                            } else {
                                boolean lapseSupported = (!fw.equals("12.50") && !fw.equals("12.52"));
                                int lapseFailCount = 0;

                                Logger.log(console, lapseSupported ? "Loading Lapse" : "Loading Poops");

                                while (true) {
                                    // Break the loop if the exploit succeeds or encounters a fatal failure
                                    if (runExploit(lapseSupported, console, lapseFailCount)) {
                                        break;
                                    }

                                    // Increment lapseFailCount only if Lapse is supported
                                    if (lapseSupported) {
                                        lapseFailCount++;
                                    }
                                }
                            }
                        }
                    } catch (Throwable e) {
                        scene.repaint();
                    }
                }
            }).start();
        } catch (Throwable e) {
            printStackTrace(e);
        }
        scene.validate();
    }

    public void startXlet() {
        gui.setVisible(true);
        scene.setVisible(true);
        gui.requestFocus();
    }

    public void pauseXlet() {
        gui.setVisible(false);
    }

    public void destroyXlet(boolean unconditional) {
        scene.remove(gui);
        scene = null;
    }

    private void printStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        Logger.log(console, sw.toString());
    }

    public static void repaint() {
        instance.scene.repaint();
    }

    private boolean runExploit(boolean lapseSupported, PrintStream console, int lapseFailCount) {
        int result;
        try {
            result = lapseSupported
                    ? org.bdj.external.Lapse.main(console)
                    : org.bdj.external.Poops.main(console);

            if (result == 0) {
                Logger.log(console, "Success");
                return true;
            }

            if (lapseSupported && (result <= -6 || lapseFailCount >= 3)) {
                Logger.log(console, "Fatal fail(" + result + "), please REBOOT PS4");
                return true;
            }

            Logger.log(console, "Failed (" + result + "), but you can try again");
            return false;
        } catch (Exception e) {
            Logger.log(console, "Exception during exploit: " + e.getMessage());
            printStackTrace(e);
            return true;
        }
    }
}
