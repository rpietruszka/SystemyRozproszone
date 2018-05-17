package sr;

import org.jgroups.*;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.protocols.pbcast.STATE_TRANSFER;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Util;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.*;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class DistributedMap implements SimpleStringMap {

    Map<String, String> map;
    JChannel jChannel;

    private String groupAddress = "230.0.0.111";
    private String chanelName = "defaultPrivateChannel";

    public DistributedMap() {
        map = new HashMap<>();
    }

    public void joinChannel() throws Exception {
        jChannel = new JChannel(false);
        ProtocolStack stack = new ProtocolStack();
        jChannel.setProtocolStack(stack);
        stack.addProtocol(new UDP().setValue("mcast_group_addr", InetAddress.getByName(groupAddress)))
                .addProtocol(new PING())
                .addProtocol(new MERGE3())
                .addProtocol(new FD_SOCK())
                .addProtocol(new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000))
                .addProtocol(new VERIFY_SUSPECT())
                .addProtocol(new BARRIER())
                .addProtocol(new NAKACK2())
                .addProtocol(new UNICAST3())
                .addProtocol(new STABLE())
                .addProtocol(new GMS())
                .addProtocol(new UFC())
                .addProtocol(new MFC())
                .addProtocol(new FRAG2())
                .addProtocol(new STATE_TRANSFER());//potrzebne do getState

        initReceiver();
        stack.init();
        jChannel.connect(chanelName);
        jChannel.getState(null, 0);
    }

    public void initReceiver() {
        jChannel.setReceiver(new ReceiverAdapter() {
            @Override
            public void viewAccepted(View view) {
                handleView(jChannel, view);
                //System.out.println(view.toString());
            }

            @Override
            public void receive(Message msg) {
                //System.out.println("Received message " + msg.getSrc());
                Operation op = (Operation) msg.getObject();
                switch (op.getType()) {
                    case PUT:
                        map.put(op.getKey(), op.getValue());
                        break;

                    case REMOVE:
                        map.remove(op.getKey());
                        break;

                }
            }

            @Override
            public void getState(OutputStream out) {
                synchronized (map) {
                    try {
                        Util.objectToStream(map, new DataOutputStream(out));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void setState(InputStream in) {
                try {
                    Map<String, String> m = (Map<String, String>) Util.objectFromStream(new DataInputStream(in));
                    synchronized (map) {
                        map.clear();
                        map.putAll(m);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("setState()");
            }
        });
    }

    private static void handleView(JChannel ch, View view) {
        if (view instanceof MergeView) {
            ViewHandler handler = new ViewHandler(ch, (MergeView) view);
            // requires separate thread as we don't want to block JGroups
            handler.start();
        }
    }

    public void shutdownChannel() {
        jChannel.close();
        System.out.println("[JChannel] Shutdown.");
        try {
            Thread.sleep(1000 * 10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    @Override
    public String get(String key) {
        return map.get(key);
    }

    @Override
    public String put(String key, String value) {
        sendMessage(new Message(null, new Operation(OperationType.PUT, key, value)));
        return map.put(key, value);
    }

    @Override
    public String remove(String key) {
        if (map.containsKey(key)) {
            sendMessage(new Message(null, new Operation(OperationType.REMOVE, key, "")));
        }
        return map.remove(key);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        map.entrySet()
                .forEach(entry -> builder.append("<" + entry.getKey() + ", " + entry.getValue() + "> "));
        return builder.toString();
    }

    public void sendMessage(Message msg) {
        try {
            jChannel.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mergeWith(String chName){
        System.out.println("Try to merge with " + chName);
        chanelName = chName;
        try {
            jChannel.disconnect();
            jChannel.connect(chName);
            jChannel.getState(null, 0);
            //jChannel.send(new Message(null, new Operation(OperationType.MERGE, chName, "")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ViewHandler extends Thread {
        JChannel ch;
        MergeView view;

        private ViewHandler(JChannel ch, MergeView view) {
            this.ch = ch;
            this.view = view;
        }

        public void run() {
            List<View> subgroups = view.getSubgroups();
            View tmp_view = subgroups.get(0); // picks the first
            Address local_addr = ch.getAddress();
            if (!tmp_view.getMembers().contains(local_addr)) {
                System.out.println("Not member of the new primary partition ("
                        + tmp_view + "), will re-acquire the state");
                try {
                    ch.getState(tmp_view.getMembers().get(0), 30000);
                    System.out.println("I have new state");
                } catch (Exception ex) {
                    System.out.println("\n\n---------------------------------------\n");
                    ex.printStackTrace();
                    System.out.println("\n\n---------------------------------------\n");
                }
            } else {
                System.out.println("Not member of the new primary partition ("
                        + tmp_view + "), will do nothing");
            }
        }

    }
}
