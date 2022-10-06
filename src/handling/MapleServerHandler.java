package handling;

import constants.ServerConstants;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import client.MapleClient;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.cashshop.handler.*;
import handling.channel.handler.*;
import handling.login.LoginServer;
import handling.login.handler.*;
import handling.world.World;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.io.FileWriter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import server.Randomizer;
import tools.MapleAESOFB;
import tools.packet.LoginPacket;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;
import tools.Pair;

import scripting.NPCScriptManager;
import server.MTSStorage;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.packet.CField;
import tools.packet.MTSCSPacket;

public class MapleServerHandler extends ChannelDuplexHandler {

    public static boolean Log_Packets = false;
    private int channel = -1;
    private static int numDC = 0;
    private static long lastDC = System.currentTimeMillis();
    private boolean cs;
    private final List<String> BlockedIP = new ArrayList<>();
    private final Map<String, Pair<Long, Byte>> tracker = new ConcurrentHashMap<>();
    //Screw locking. Doesn't matter.
//    private static final ReentrantReadWriteLock IPLoggingLock = new ReentrantReadWriteLock();
    private static final String nl = System.getProperty("line.separator");
    private static final File loggedIPs = new File("LogIPs.txt");
    private static final HashMap<String, FileWriter> logIPMap = new HashMap<String, FileWriter>();
    //Note to Zero: Use an enumset. Don't iterate through an array.
    private static final EnumSet<RecvPacketOpcode> blocked = EnumSet.noneOf(RecvPacketOpcode.class), sBlocked = EnumSet.noneOf(RecvPacketOpcode.class);

    public static void reloadLoggedIPs() {
//        IPLoggingLock.writeLock().lock();
//        try {
        for (FileWriter fw : logIPMap.values()) {
            if (fw != null) {
                try {
                    fw.write("=== Closing Log ===");
                    fw.write(nl);
                    fw.flush(); //Just in case.
                    fw.close();
                } catch (IOException ex) {
                    System.out.println("Error closing Packet Log.");
                    System.out.println(ex);
                }
            }
        }
        logIPMap.clear();
        try {
            Scanner sc = new Scanner(loggedIPs);
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.length() > 0) {
                    addIP(line);
                }
            }
        } catch (Exception e) {
            System.out.println("Could not reload packet logged IPs.");
            System.out.println(e);
        }
//        } finally {
//            IPLoggingLock.writeLock().unlock();
//        }
    }
    //Return the Filewriter if the IP is logged. Null otherwise.

    private static FileWriter isLoggedIP(Channel sess) {
        String a = sess.remoteAddress().toString();
        String realIP = a.substring(a.indexOf('/') + 1, a.indexOf(':'));
        return logIPMap.get(realIP);
    }

    public static void addIP(String theIP) {
        try {
            FileWriter fw = new FileWriter(new File("PacketLog_" + theIP + ".txt"), true);
            fw.write("=== Creating Log ===");
            fw.write(nl);
            fw.flush();
            logIPMap.put(theIP, fw);
        } catch (IOException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
        }

    }

    // <editor-fold defaultstate="collapsed" desc="Packet Log Implementation">
    private static final int Log_Size = 10000, Packet_Log_Size = 25;
    private static final ArrayList<LoggedPacket> Packet_Log = new ArrayList<LoggedPacket>(Log_Size);
    private static final ReentrantReadWriteLock Packet_Log_Lock = new ReentrantReadWriteLock();
    private static String Packet_Log_Output = "Packet/PacketLog";
    private static int Packet_Log_Index = 0;

    public static void log(String packet, String op, MapleClient c, Channel io) {
        try {
            Packet_Log_Lock.writeLock().lock();
            LoggedPacket logged = null;
            if (Packet_Log.size() == Log_Size) {
                logged = Packet_Log.remove(0);
            }
            //This way, we don't create new LoggedPacket objects, we reuse them =]
            if (logged == null) {
                logged = new LoggedPacket(packet, op, io.remoteAddress().toString(),
                        c == null ? -1 : c.getAccID(), FileoutputUtil.CurrentReadable_Time(),
                        c == null || c.getAccountName() == null ? "[Null]" : c.getAccountName(),
                        c == null || c.getPlayer() == null || c.getPlayer().getName() == null ? "[Null]" : c.getPlayer().getName(),
                        c == null || c.getPlayer() == null || c.getPlayer().getMap() == null ? "[Null]" : String.valueOf(c.getPlayer().getMapId()),
                        c == null || NPCScriptManager.getInstance().getCM(c) == null ? "[Null]" : String.valueOf(NPCScriptManager.getInstance().getCM(c).getNpc()));
            } else {
                logged.setInfo(packet, op, io.remoteAddress().toString(),
                        c == null ? -1 : c.getAccID(), FileoutputUtil.CurrentReadable_Time(),
                        c == null || c.getAccountName() == null ? "[Null]" : c.getAccountName(),
                        c == null || c.getPlayer() == null || c.getPlayer().getName() == null ? "[Null]" : c.getPlayer().getName(),
                        c == null || c.getPlayer() == null || c.getPlayer().getMap() == null ? "[Null]" : String.valueOf(c.getPlayer().getMapId()),
                        c == null || NPCScriptManager.getInstance().getCM(c) == null ? "[Null]" : String.valueOf(NPCScriptManager.getInstance().getCM(c).getNpc()));
            }
            Packet_Log.add(logged);
        } finally {
            Packet_Log_Lock.writeLock().unlock();
        }
    }

    private static class LoggedPacket {

        private static final String nl = System.getProperty("line.separator");
        private String ip, accName, accId, chrName, packet, mapId, npcId, op, time;
        private long timestamp;

        public LoggedPacket(String p, String op, String ip, int id, String time, String accName, String chrName, String mapId, String npcId) {
            setInfo(p, op, ip, id, time, accName, chrName, mapId, npcId);
        }

        public final void setInfo(String p, String op, String ip, int id, String time, String accName, String chrName, String mapId, String npcId) {
            this.ip = ip;
            this.op = op;
            this.time = time;
            this.packet = p;
            this.accName = accName;
            this.chrName = chrName;
            this.mapId = mapId;
            this.npcId = npcId;
            timestamp = System.currentTimeMillis();
            this.accId = String.valueOf(id);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[IP: ").append(ip).append("] [").append(accId).append('|').append(accName).append('|').append(chrName).append("] [").append(npcId).append('|').append(mapId).append("] [Time: ").append(timestamp).append("] [").append(time).append(']');
            sb.append(nl);
            sb.append("[Op: ").append(op).append("] [").append(packet).append(']');
            return sb.toString();
        }
    }

    public void writeLog() {
        writeLog(false);
    }

    public void writeLog(boolean crash) {
        Packet_Log_Lock.readLock().lock();
        try {
            FileWriter fw = new FileWriter(new File(Packet_Log_Output + Packet_Log_Index + (crash ? "_DC.txt" : ".txt")), true);
            String nl = System.getProperty("line.separator");
            for (LoggedPacket loggedPacket : Packet_Log) {
                fw.write(loggedPacket.toString());
                fw.write(nl);
            }
            final String logString = "Log has been written at " + lastDC + " [" + FileoutputUtil.CurrentReadable_Time() + "] - " + numDC + " have disconnected, within " + (System.currentTimeMillis() - lastDC) + " milliseconds. (" + System.currentTimeMillis() + ")";
            System.out.println(logString);
            fw.write(logString);
            fw.write(nl);
            fw.flush();
            fw.close();
            Packet_Log.clear();
            Packet_Log_Index++;
            if (Packet_Log_Index > Packet_Log_Size) {
                Packet_Log_Index = 0;
                Log_Packets = false;
            }
        } catch (IOException ex) {
            System.out.println("Error writing log to file.");
        } finally {
            Packet_Log_Lock.readLock().unlock();
        }

    }

    public static final void initiate() {
        reloadLoggedIPs();
        RecvPacketOpcode[] block = new RecvPacketOpcode[]{RecvPacketOpcode.NPC_ACTION, RecvPacketOpcode.MOVE_PLAYER, RecvPacketOpcode.PONG, RecvPacketOpcode.MOVE_PET, RecvPacketOpcode.MOVE_SUMMON, RecvPacketOpcode.MOVE_DRAGON, RecvPacketOpcode.MOVE_LIFE, RecvPacketOpcode.MOVE_ANDROID, RecvPacketOpcode.HEAL_OVER_TIME, RecvPacketOpcode.STRANGE_DATA, RecvPacketOpcode.AUTO_AGGRO, RecvPacketOpcode.CANCEL_DEBUFF, RecvPacketOpcode.MOVE_FAMILIAR};
        RecvPacketOpcode[] serverBlock = new RecvPacketOpcode[]{RecvPacketOpcode.CHANGE_KEYMAP, RecvPacketOpcode.ITEM_PICKUP, RecvPacketOpcode.PET_LOOT, RecvPacketOpcode.TAKE_DAMAGE, RecvPacketOpcode.FACE_EXPRESSION, RecvPacketOpcode.USE_ITEM, RecvPacketOpcode.CLOSE_RANGE_ATTACK, RecvPacketOpcode.MAGIC_ATTACK, RecvPacketOpcode.RANGED_ATTACK, RecvPacketOpcode.ARAN_COMBO, RecvPacketOpcode.SPECIAL_MOVE, RecvPacketOpcode.GENERAL_CHAT, RecvPacketOpcode.MONSTER_BOMB, RecvPacketOpcode.PASSIVE_ENERGY, RecvPacketOpcode.PET_AUTO_POT, RecvPacketOpcode.USE_CASH_ITEM, RecvPacketOpcode.PARTYCHAT, RecvPacketOpcode.CANCEL_BUFF, RecvPacketOpcode.SKILL_EFFECT, RecvPacketOpcode.CHAR_INFO_REQUEST, RecvPacketOpcode.ALLIANCE_OPERATION, RecvPacketOpcode.AUTO_ASSIGN_AP, RecvPacketOpcode.DISTRIBUTE_AP, RecvPacketOpcode.USE_MAGNIFY_GLASS, RecvPacketOpcode.SPAWN_PET, RecvPacketOpcode.SUMMON_ATTACK, RecvPacketOpcode.ITEM_MOVE, RecvPacketOpcode.PARTY_SEARCH_STOP};
        blocked.addAll(Arrays.asList(block));
        sBlocked.addAll(Arrays.asList(serverBlock));
        if (Log_Packets) {
            for (int i = 1; i <= Packet_Log_Size; i++) {
                if (!(new File(Packet_Log_Output + i + ".txt")).exists() && !(new File(Packet_Log_Output + i + "_DC.txt")).exists()) {
                    Packet_Log_Index = i;
                    break;
                }
            }
            if (Packet_Log_Index <= 0) { //25+ files, do not log
                Log_Packets = false;
            }
        }

        //   registerMBean();
    }

    private static ServerType type = null;

    public MapleServerHandler(int channel, ServerType type) {
        this.channel = channel;
        this.type = type;
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        /*	MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        log.error(MapleClient.getLogMessage(client, cause.getMessage()), cause);*/
        // cause.printStackTrace();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        // Start of IP checking
        final String address = ctx.channel().remoteAddress().toString().split(":")[0];

        if (BlockedIP.contains(address)) {
            ctx.channel().close();
            return;
        }
        final Pair<Long, Byte> track = tracker.get(address);

        byte count;
        if (track == null) {
            count = 1;
        } else {
            count = track.right;

            final long difference = System.currentTimeMillis() - track.left;
            if (difference < 2000) { // Less than 2 sec
                count++;
            } else if (difference > 20000) { // Over 20 sec
                count = 1;
            }
            if (count >= 10) {
                BlockedIP.add(address);
                tracker.remove(address); // Cleanup
                ctx.channel().close();
                return;
            }
        }
        tracker.put(address, new Pair<>(System.currentTimeMillis(), count));
        // End of IP checking.
        String IP = address.substring(address.indexOf('/') + 1, address.length());
        if (channel > -1) {
            if (ChannelServer.getInstance(channel).isShutdown()) {
                ctx.channel().close();
                return;
            }
            if (!LoginServer.containsIPAuth(IP)) {
                ctx.channel().close();
                return;
            }

        } else if (type == ServerType.商城伺服器) {
            if (CashShopServer.isShutdown()) {
                ctx.channel().close();
                return;
            }
        } else {
            if (LoginServer.isShutdown()) {
                ctx.channel().close();
                return;
            }
        }
        LoginServer.removeIPAuth(IP);
//        final byte ivRecv[] = new byte[]{(byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255)};
//        final byte ivSend[] = new byte[]{(byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255)};
        final byte serverRecv[] = new byte[]{(byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255)};
        final byte serverSend[] = new byte[]{(byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255)};
        final boolean fixed = ServerConstants.Use_Fixed_IV;
        final byte ivRecv[] = fixed ? new byte[]{8, 125, (byte) 155, (byte) 169} : serverRecv;
        final byte ivSend[] = fixed ? new byte[]{44, (byte) 231, 77, 33} : serverSend;

        final MapleClient client = new MapleClient(
                new MapleAESOFB(ivSend, (short) (0xFFFF - ServerConstants.MAPLE_VERSION)), // Sent Cypher
                new MapleAESOFB(ivRecv, ServerConstants.MAPLE_VERSION), // Recv Cypher
                ctx.channel());
        client.setChannel(channel);
        server.netty.MaplePacketDecoder.DecoderState decoderState = new server.netty.MaplePacketDecoder.DecoderState();
        ctx.channel().attr(server.netty.MaplePacketDecoder.DECODER_STATE_KEY).set(decoderState);

        ctx.channel().writeAndFlush(LoginPacket.getHello(ServerConstants.MAPLE_VERSION, ivSend, ivRecv));
        ctx.channel().attr(MapleClient.CLIENT_KEY).set(client);

        if (!ServerConstants.Use_Fixed_IV) {
            RecvPacketOpcode.reloadValues();
            SendPacketOpcode.reloadValues();
        }

        if (ServerConstants.isAdminOnly) {
            StringBuilder sb = new StringBuilder();
            if (channel > -1) {
                sb.append("[頻道伺服器] 頻道: ").append(channel).append(" : ");
            } else if (type == ServerType.商城伺服器) {
                sb.append("[Cash Server]");
            } else {
                sb.append("[登入伺服器]");
            }
            sb.append(" IP: ").append(address).append("已連線.");
            System.out.println(sb.toString());
        }

        FileWriter fw = isLoggedIP(ctx.channel());
        if (fw != null) {
            if (channel > -1) {
                client.setMonitored(true);
                fw.write("=== Logged Into Channel " + channel + " ===");
                fw.write(nl);
            } else if (type == ServerType.商城伺服器) {
                client.setMonitored(true);
                fw.write("=== Logged Into CashShop Server ===");
                fw.write(nl);
            } else {
                fw.write("=== Logged Into Login Server ===");
                fw.write(nl);
            }
            fw.flush();
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        final MapleClient client = (MapleClient) ctx.channel().attr(MapleClient.CLIENT_KEY).get();
        if (client != null && client.getAccID() > 0) {
            byte state = MapleClient.CHANGE_CHANNEL;
            if (Log_Packets && !LoginServer.isShutdown() && type != ServerType.商城伺服器 && channel > -1) {
                state = client.getLoginState();
            }
            if (state != MapleClient.CHANGE_CHANNEL) {
                log("Data: " + numDC, "CLOSED", client, ctx.channel());
                if (System.currentTimeMillis() - lastDC < 60000) { //within the minute
                    numDC++;
                    if (numDC > 100) { //100+ people have dc'd in minute in channelserver
                        System.out.println("Writing log...");
                        writeLog();
                        numDC = 0;
                        lastDC = System.currentTimeMillis(); //intentionally place here
                    }
                } else {
                    numDC = 0;
                    lastDC = System.currentTimeMillis(); //intentionally place here
                }
            }
            try {
                FileWriter fw = isLoggedIP(ctx.channel());
                if (fw != null) {
                    fw.write("=== Session Closed ===");
                    fw.write(nl);
                    fw.flush();
                }
                client.disconnect(true, type == ServerType.商城伺服器 || cs);

                if (client.getPlayer() != null) {
                    if (!(client.getLoginState() == MapleClient.CASH_SHOP_TRANSITION
                            || client.getLoginState() == MapleClient.CASH_SHOP_TRANSITION_LEAVE
                            || client.getLoginState() == MapleClient.MAPLE_TRADE_TRANSITION
                            || client.getLoginState() == MapleClient.MAPLE_TRADE_TRANSITION_LEAVE
                            || client.getLoginState() == MapleClient.CHANGE_CHANNEL
                            || client.getLoginState() == MapleClient.LOGIN_SERVER_TRANSITION)) {
                        int ch = World.Find.findChannel(client.getPlayer().getId());
                        if (ChannelServer.getInstance(ch) != null) {
                            ChannelServer.getInstance(ch).removePlayer(client.getPlayer());
                        } else if (ch == -10) {
                            CashShopServer.getPlayerStorage().deregisterPlayer(client.getPlayer());
                            CashShopServer.getPlayerStorageMTS().deregisterPlayer(client.getPlayer());
                        }
                    }
                }
                if (channel == 0) {
                    LoginServer.removeClient(client);
                }
            } finally {
                ctx.channel().close();
                ctx.channel().attr(MapleClient.CLIENT_KEY).remove();
            }
        }
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object status) throws Exception {
        final MapleClient client = (MapleClient) ctx.channel().attr(MapleClient.CLIENT_KEY).get();

        /*	if (client != null && client.getPlayer() != null) {
        System.out.println("Player "+ client.getPlayer().getName() +" went idle");
        }*/
        if (client != null) {
            client.sendPing();
        } else {
            ctx.channel().close();
            return;
        }
        super.userEventTriggered(ctx, status);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object message) {
        if (message == null || ctx.channel() == null) {
            return;
        }
        final LittleEndianAccessor slea = new LittleEndianAccessor(new ByteArrayByteStream((byte[]) message));
        if (slea.available() < 2) {
            return;
        }
        final MapleClient c = (MapleClient) ctx.channel().attr(MapleClient.CLIENT_KEY).get();
        if (c == null || !c.isReceiving()) {
            return;
        }
        final short header_num = slea.readShort();

        for (final RecvPacketOpcode recv : RecvPacketOpcode.values()) {
            if (recv.getValue() == header_num) {
//                StringBuilder sb = new StringBuilder("Received data :\n");
//                sb.append(HexTool.toString((byte[]) message)).append("\n").append(HexTool.toStringFromAscii((byte[]) message));
//                System.out.println(sb.toString());
                if (recv.NeedsChecking()) {
                    if (!c.isLoggedIn() && !c.isCSLoggedIn() && !c.isLoggedIn_beforeInGame()) {
                        return;
                    }
                }
                try {
                    if (c.getPlayer() != null && c.isMonitored() && !blocked.contains(recv)) {
                        FileoutputUtil.logToFile("logs/監聽訊息/" + c.getPlayer().getName() + ".txt", String.valueOf(recv) + " (" + Integer.toHexString(header_num) + ") 處理: \r\n" + slea.toString() + "\r\n");
                    }
                    //no login packets
                    if (Log_Packets && !blocked.contains(recv) && !sBlocked.contains(recv) && (type == ServerType.商城伺服器 || channel > -1)) {
                        log(slea.toString(), recv.toString(), c, ctx.channel());
                    }
                    if (c.getCloseSession()) {
                        c.getSession().close();
                        return;
                    }
                    handlePacket(recv, slea, c, type == ServerType.商城伺服器);
                    if (c.getPlayer() != null) {
                        // 假斷線處理
                        if (World.Find.findChannel(c.getPlayer().getId()) == -1) {
                            switch (recv) {
                                case PLAYER_LOGGEDIN:
                                    break;
                                default:
                                    c.updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN, c.getSessionIPAddress());
                                    c.getSession().close();
                                    return;
                            }
                        }

                    }
                    //Log after the packet is handle. You'll see why =]
                    FileWriter fw = isLoggedIP(ctx.channel());
                    if (fw != null && !blocked.contains(recv)) {
                        if (recv == RecvPacketOpcode.PLAYER_LOGGEDIN && c != null) { // << This is why. Win.
                            fw.write(">> [AccountName: "
                                    + (c.getAccountName() == null ? "null" : c.getAccountName()) + "] | [IGN: "
                                    + (c.getPlayer() == null || c.getPlayer().getName() == null ? "null" : c.getPlayer().getName()) + "] | [Time: "
                                    + FileoutputUtil.CurrentReadable_Time() + "]");
                            fw.write(nl);
                        }
                        fw.write("[" + recv.toString() + "]" + slea.toString(true));
                        fw.write(nl);
                        fw.flush();
                    }
                } catch (NegativeArraySizeException | ArrayIndexOutOfBoundsException e) {
                    //swallow, no one cares
                    if (!ServerConstants.Use_Fixed_IV) {
                        FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                        FileoutputUtil.log(FileoutputUtil.PacketEx_Log, "Packet: " + header_num + "\n" + slea.toString(true));
                    }
                } //swallow, no one cares
                catch (Exception e) {
                    FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                    FileoutputUtil.log(FileoutputUtil.PacketEx_Log, "Packet: " + header_num + "\n" + slea.toString(true));
                }
                return;
            }
        }
        if (ServerConstants.LOG_UNHandle_PACKETS) {
            final byte[] packet = slea.read((int) slea.available());
            final StringBuilder sb = new StringBuilder("發現未知用戶端數據包 - (包頭:0x" + Integer.toHexString(header_num) + ")");
            System.err.println(sb.toString());
            sb.append(":\r\n").append(HexTool.toString(packet)).append("\r\n").append(HexTool.toStringFromAscii(packet));
            FileoutputUtil.log(FileoutputUtil.UnknownPacket_Log, sb.toString());
        }
//        StringBuilder sb = new StringBuilder("Received data : (Unhandled)\n");
//        sb.append(HexTool.toString((byte[]) message)).append("\n").append(HexTool.toStringFromAscii((byte[]) message));
//        System.out.println(sb.toString());
    }

    public static final void handlePacket(final RecvPacketOpcode header, final LittleEndianAccessor slea, final MapleClient c, final boolean cs) throws Exception {
        switch (header) {
            case PONG:
                c.pongReceived();
                break;
            case INVALID_DECODING:
                FileoutputUtil.log("Invalid_Decoding.txt", slea.readMapleAsciiString());
                break;
            case LOGIN_AUTH:
                c.sendPacket(LoginPacket.getLoginAUTH());
                //CharLoginHandler.loginAuthRequest(slea, c);
                break;
            case SET_SECOND_PASSWORD:
                CharLoginHandler.set2ndPasswordRequest(slea, c);
                break;
            case STRANGE_DATA:
                // Does nothing for now, HackShield's heartbeat
                break;
            case LOGIN_PASSWORD:
                CharLoginHandler.login(slea, c);
                break;
            case SEND_ENCRYPTED:
                //if (c.isLocalhost()) {
                //    CharLoginHandler.login(slea, c);
                //} else {
                //    c.sendPacket(LoginPacket.getCustomEncryption());
                //}
                break;
            case TICK:
            case CLIENT_START:
            case CLIENT_FAILED:
                // c.sendPacket(LoginPacket.getCustomEncryption());
                break;
            case PART_TIME_JOB:
                CharLoginHandler.PartJob(slea, c);
                break;
            case CHARACTER_CARD:
                CharLoginHandler.updateCCards(slea, c);
                break;
            case VIEW_SERVERLIST:
                if (slea.readByte() == 0) {
                    CharLoginHandler.ServerListRequest(c, true);
                }
                break;
            case REDISPLAY_SERVERLIST:
            case SERVERLIST_REQUEST:
                CharLoginHandler.ServerListRequest(c, true);
                break;
            case CLIENT_HELLO:
                if (slea.readByte() != 6 || slea.readShort() != ServerConstants.MAPLE_VERSION || !String.valueOf(slea.readShort()).equals(ServerConstants.MAPLE_PATCH)) {
                    c.getSession().close();
                }
                break;
            case CHARLIST_REQUEST:
                CharLoginHandler.CharlistRequest(slea, c);
                break;
            case SERVERSTATUS_REQUEST:
                CharLoginHandler.ServerStatusRequest(c);
                break;
            case CHECK_CHAR_NAME:
                CharLoginHandler.CheckCharName(slea.readMapleAsciiString(), c);
                break;
            case CREATE_CHAR:
            case CREATE_SPECIAL_CHAR:
                CharLoginHandler.CreateChar(slea, c);
                break;
            case CREATE_ULTIMATE:
                CharLoginHandler.CreateUltimate(slea, c);
                break;
            case DELETE_CHAR:
                CharLoginHandler.DeleteChar(slea, c);
                break;
            case CHAR_SELECT_NO_PIC:
                CharLoginHandler.Character_WithoutSecondPassword(slea, c, false, false);
                break;
            case VIEW_REGISTER_PIC:
                CharLoginHandler.Character_WithoutSecondPassword(slea, c, true, true);
                break;
            case CHAR_SELECT:
                CharLoginHandler.Character_WithoutSecondPassword(slea, c, true, false);
                break;
            case VIEW_SELECT_PIC:
                CharLoginHandler.Character_WithSecondPassword(slea, c, true);
                break;
            case AUTH_SECOND_PASSWORD:
                CharLoginHandler.Character_WithSecondPassword(slea, c, false);
                break;
            case CLIENT_ERROR: {
                if (slea.available() < 8) {
                    return;
                }
                short type = slea.readShort();
                String type_str = "Unknown?!";
                if (type == 0x01) {
                    type_str = "SendBackupPacket";
                } else if (type == 0x02) {
                    type_str = "Crash Report";
                } else if (type == 0x03) {
                    type_str = "Exception";
                }
                int unk = slea.readInt();
                if (unk == 0) { // i don't wanna log error code 0 stuffs, (usually some bounceback to login)
                    return;
                }
                short data_length = slea.readShort();
                slea.skip(4); // ?				
                FileoutputUtil.log("ErrorCodes.rtf", "Client sent crashing packet: Type: " + type_str + "; Error code: " + unk + "; Length: " + data_length + "; Packet: " + slea.toString());
                break;
            }
            case ENABLE_SPECIAL_CREATION:
                c.sendPacket(LoginPacket.enableSpecialCreation(c.getAccID(), true));
                break;
            case RSA_KEY: // Fix this somehow
                //c.sendPacket(LoginPacket.getLoginAUTH());
                // c.sendPacket(LoginPacket.StrangeDATA());
                break;
            // END OF LOGIN SERVER
            case CHANGE_CHANNEL:
            case CHANGE_ROOM_CHANNEL:
                InterServerHandler.ChangeChannel(slea, c, c.getPlayer(), header == RecvPacketOpcode.CHANGE_ROOM_CHANNEL);
                break;
            case PLAYER_LOGGEDIN:
                InterServerHandler.Loggedin(slea, c);
                break;
            case ENTER_PVP:
            case ENTER_PVP_PARTY:
                PlayersHandler.EnterPVP(slea, c);
                break;
            case PVP_RESPAWN:
                PlayersHandler.RespawnPVP(slea, c);
                break;
            case LEAVE_PVP:
                PlayersHandler.LeavePVP(slea, c);
                break;
            case ENTER_AZWAN:
                PlayersHandler.EnterAzwan(slea, c);
                break;
            case ENTER_AZWAN_EVENT:
                PlayersHandler.EnterAzwanEvent(slea, c);
                break;
            case LEAVE_AZWAN:
                PlayersHandler.LeaveAzwan(slea, c);
                c.sendPacket(CField.showEffect("hillah/fail"));
                //c.getSession().write(UIPacket.sendAzwanResult());
                break;
            case PVP_ATTACK:
                PlayersHandler.AttackPVP(slea, c);
                break;
            case PVP_SUMMON:
                SummonHandler.SummonPVP(slea, c);
                break;
            case ENTER_CASH_SHOP:
                InterServerHandler.EnterCS(c, c.getPlayer(), false);
                break;
            case ENTER_MTS:
                InterServerHandler.EnterMTS(c, c.getPlayer());
                //InterServerHandler.EnterCS(c, c.getPlayer(), true);
                break;
            case MOVE_PLAYER:
                PlayerHandler.MovePlayer(slea, c, c.getPlayer());
                break;
            case CHAR_INFO_REQUEST:
                c.getPlayer().updateTick(slea.readInt());
                PlayerHandler.CharInfoRequest(slea.readInt(), c, c.getPlayer());
                break;
            case CLOSE_RANGE_ATTACK:
                PlayerHandler.closeRangeAttack(slea, c, c.getPlayer(), false);
                break;
            case RANGED_ATTACK:
                PlayerHandler.rangedAttack(slea, c, c.getPlayer());
                break;
            case MAGIC_ATTACK:
                PlayerHandler.MagicDamage(slea, c, c.getPlayer());
                break;
            case SPECIAL_MOVE:
                PlayerHandler.SpecialMove(slea, c, c.getPlayer());
                break;
            case PASSIVE_ENERGY:
                PlayerHandler.closeRangeAttack(slea, c, c.getPlayer(), true);
                break;
            case GET_BOOK_INFO:
                PlayersHandler.MonsterBookInfoRequest(slea, c, c.getPlayer());
                break;
            case MONSTER_BOOK_DROPS:
                PlayersHandler.MonsterBookDropsRequest(slea, c, c.getPlayer());
                break;
            case CHANGE_SET:
                PlayersHandler.ChangeSet(slea, c, c.getPlayer());
                break;
            case PROFESSION_INFO:
                ItemMakerHandler.ProfessionInfo(slea, c);
                break;
            case CRAFT_DONE:
                ItemMakerHandler.CraftComplete(slea, c, c.getPlayer());
                break;
            case CRAFT_MAKE:
                ItemMakerHandler.CraftMake(slea, c, c.getPlayer());
                break;
            case CRAFT_EFFECT:
                ItemMakerHandler.CraftEffect(slea, c, c.getPlayer());
                break;
            case CHOOSE_SKILL:
                PlayersHandler.ChooseSkill(slea, c);
                break;
            case SKILL_SWIPE:
                PlayersHandler.StealSkill(slea, c);
                break;
            case VIEW_SKILLS:
                PlayersHandler.viewSkills(slea, c);
                break;
            case CANCEL_OUT_SWIPE:
                slea.readInt();
                break;
            case UPDATE_CORE:
                InventoryHandler.resetCoreAura(slea.readByte(), c);
                break;
            case UPDATE_CORE_EXPIRE:
                InventoryHandler.addCoreExpire(slea.readByte(), c);
                break;
            case CosmicDustShifter:
                InventoryHandler.CosmicDustShifter(slea.readByte(), c);
                break;
            case UPDATE_RED_LEAF:
                PlayersHandler.updateRedLeafHigh(slea, c);
                break;
            case START_HARVEST:
                ItemMakerHandler.StartHarvest(slea, c, c.getPlayer());
                break;
            case STOP_HARVEST:
                ItemMakerHandler.StopHarvest(slea, c, c.getPlayer());
                break;
            case MAKE_EXTRACTOR:
                ItemMakerHandler.MakeExtractor(slea, c, c.getPlayer());
                break;
            case USE_BAG:
                ItemMakerHandler.UseBag(slea, c, c.getPlayer());
                break;
            case USE_FAMILIAR:
                MobHandler.UseFamiliar(slea, c, c.getPlayer());
                break;
            case SPAWN_FAMILIAR:
                MobHandler.SpawnFamiliar(slea, c, c.getPlayer());
                break;
            case RENAME_FAMILIAR:
                MobHandler.RenameFamiliar(slea, c, c.getPlayer());
                break;
            case MOVE_FAMILIAR:
                MobHandler.MoveFamiliar(slea, c, c.getPlayer());
                break;
            case ATTACK_FAMILIAR:
                MobHandler.AttackFamiliar(slea, c, c.getPlayer());
                break;
            case TOUCH_FAMILIAR:
                MobHandler.TouchFamiliar(slea, c, c.getPlayer());
                break;
            case USE_RECIPE:
                ItemMakerHandler.UseRecipe(slea, c, c.getPlayer());
                break;
            case MOVE_ANDROID:
                PlayerHandler.MoveAndroid(slea, c, c.getPlayer());
                break;
            case FACE_EXPRESSION:
                PlayerHandler.ChangeEmotion(slea.readInt(), c.getPlayer());
                break;
            case FACE_ANDROID:
                PlayerHandler.ChangeAndroidEmotion(slea.readInt(), c.getPlayer());
                break;
            case TAKE_DAMAGE:
                PlayerHandler.TakeDamage(slea, c, c.getPlayer());
                break;
            case HEAL_OVER_TIME:
                PlayerHandler.Heal(slea, c.getPlayer());
                break;
            case CANCEL_BUFF:
                PlayerHandler.CancelBuffHandler(slea.readInt(), c.getPlayer());
                break;
            case MECH_CANCEL:
                PlayerHandler.CancelMech(slea, c.getPlayer());
                break;
            case CANCEL_ITEM_EFFECT:
                PlayerHandler.CancelItemEffect(slea.readInt(), c.getPlayer());
                break;
            case USE_TITLE:
                PlayerHandler.UseTitle(slea.readInt(), c, c.getPlayer());
                break;
            case USE_CHAIR:
                PlayerHandler.UseChair(slea.readInt(), c, c.getPlayer());
                break;
            case CANCEL_CHAIR:
                PlayerHandler.CancelChair(slea.readShort(), c, c.getPlayer());
                break;
            case WHEEL_OF_FORTUNE:
                break; //whatever
            case USE_ITEMEFFECT:
                PlayerHandler.UseItemEffect(slea.readInt(), c, c.getPlayer());
                break;
            case SKILL_EFFECT:
                PlayerHandler.SkillEffect(slea, c.getPlayer());
                break;
            case QUICK_SLOT:
                PlayerHandler.QuickSlot(slea, c.getPlayer());
                break;
            case STOLEN_TICK:
                break;
            case MESO_DROP:
                c.getPlayer().updateTick(slea.readInt());
                PlayerHandler.DropMeso(slea.readInt(), c.getPlayer());
                break;
            case CHANGE_KEYMAP:
                PlayerHandler.ChangeKeymap(slea, c.getPlayer());
                break;
            case UPDATE_ENV:
                // We handle this in MapleMap
                break;
            case CHANGE_MAP:
                if (c.getPlayer().getOneTempValue("Transfer", "Channel") != null) {
                    CashShopOperation.LeaveCS(slea, c, c.getPlayer());
                } else {
                    PlayerHandler.ChangeMap(slea, c, c.getPlayer());
                }
                break;
            case CHANGE_MAP_SPECIAL:
                slea.skip(1);
                PlayerHandler.ChangeMapSpecial(slea.readMapleAsciiString(), c, c.getPlayer());
                break;
            case USE_INNER_PORTAL:
                slea.skip(1);
                PlayerHandler.InnerPortal(slea, c, c.getPlayer());
                break;
            case TROCK_ADD_MAP:
                PlayerHandler.TrockAddMap(slea, c, c.getPlayer());
                break;
            case ARAN_COMBO:
                PlayerHandler.AranCombo(c, c.getPlayer(), 1);
                break;
            case SKILL_MACRO:
                PlayerHandler.ChangeSkillMacro(slea, c.getPlayer());
                break;
            case GIVE_FAME:
                PlayersHandler.GiveFame(slea, c, c.getPlayer());
                break;
            case TRANSFORM_PLAYER:
                PlayersHandler.TransformPlayer(slea, c, c.getPlayer());
                break;
            case NOTE_ACTION:
                PlayersHandler.Note(slea, c.getPlayer());
                break;
            case USE_DOOR:
                PlayersHandler.UseDoor(slea, c.getPlayer());
                break;
            case USE_MECH_DOOR:
                PlayersHandler.UseMechDoor(slea, c.getPlayer());
                break;
            case DAMAGE_REACTOR:
                PlayersHandler.HitReactor(slea, c);
                break;
            case CLICK_REACTOR:
            case TOUCH_REACTOR:
                PlayersHandler.TouchReactor(slea, c);
                break;
            case CLOSE_CHALKBOARD:
                c.getPlayer().setChalkboard(null);
                break;
            case ITEM_SORT:
                InventoryHandler.ItemSort(slea, c);
                break;
            case ITEM_GATHER:
                InventoryHandler.ItemGather(slea, c);
                break;
            case ITEM_MOVE:
                InventoryHandler.ItemMove(slea, c);
                break;
            case ITEM_UNLOCK:
                InventoryHandler.UnlockItem(slea, c);
                break;
            case MOVE_BAG:
                InventoryHandler.MoveBag(slea, c);
                break;
            case SWITCH_BAG:
                InventoryHandler.SwitchBag(slea, c);
                break;
            case ITEM_MAKER:
                ItemMakerHandler.ItemMaker(slea, c);
                break;
            case ITEM_PICKUP:
                InventoryHandler.Pickup_Player(slea, c, c.getPlayer());
                break;
            case USE_CASH_ITEM:
                InventoryHandler.UseCashItem(slea, c);
                break;
            case USE_ITEM:
                InventoryHandler.UseItem(slea, c, c.getPlayer());
                break;
            case USE_COSMETIC:
                InventoryHandler.UseCosmetic(slea, c, c.getPlayer());
                break;
            case USE_MAGNIFY_GLASS:
                InventoryHandler.UseMagnify(slea, c);
                break;
            case USE_SCRIPTED_NPC_ITEM:
                InventoryHandler.UseScriptedNPCItem(slea, c, c.getPlayer());
                break;
            case USE_RETURN_SCROLL:
                InventoryHandler.UseReturnScroll(slea, c, c.getPlayer());
                break;
            case USE_NEBULITE:
                InventoryHandler.UseNebulite(slea, c);
                break;
            case USE_ALIEN_SOCKET:
                InventoryHandler.UseAlienSocket(slea, c);
                break;
            case USE_ALIEN_SOCKET_RESPONSE:
                slea.skip(4); // all 0
                c.sendPacket(MTSCSPacket.useAlienSocket(false));
                break;
            case VICIOUS_HAMMER:
                InventoryHandler.useViciousHammer(slea, c);
                break;
            case USE_NEBULITE_FUSION:
                InventoryHandler.UseNebuliteFusion(slea, c);
                break;
            case USE_UPGRADE_SCROLL:
                c.getPlayer().updateTick(slea.readInt());
                InventoryHandler.UseUpgradeScroll(slea.readShort(), slea.readShort(), slea.readShort(), c, c.getPlayer(), slea.readByte() > 0);
                break;
            case USE_FLAG_SCROLL:
            case USE_POTENTIAL_SCROLL:
            case USE_EQUIP_SCROLL:
                c.getPlayer().updateTick(slea.readInt());
                InventoryHandler.UseUpgradeScroll(slea.readShort(), slea.readShort(), (short) 0, c, c.getPlayer(), slea.readByte() > 0);
                break;
            case USE_CARVED_SEAL:
                InventoryHandler.UseCarvedSeal(slea, c);
                break;
            case USE_SUMMON_BAG:
                InventoryHandler.UseSummonBag(slea, c, c.getPlayer());
                break;
            case USE_TREASUER_CHEST:
                InventoryHandler.UseTreasureChest(slea, c, c.getPlayer());
                break;
            case USE_SKILL_BOOK:
                c.getPlayer().updateTick(slea.readInt());
                InventoryHandler.UseSkillBook((byte) slea.readShort(), slea.readInt(), c, c.getPlayer());
                break;
            case USE_EXP_POTION:
                InventoryHandler.UseExpPotion(slea, c, c.getPlayer());
                break;
            case USE_CATCH_ITEM:
                InventoryHandler.UseCatchItem(slea, c, c.getPlayer());
                break;
            case USE_MOUNT_FOOD:
                InventoryHandler.UseMountFood(slea, c, c.getPlayer());
                break;
            case REWARD_ITEM:
                InventoryHandler.UseRewardItem((byte) slea.readShort(), slea.readInt(), c, c.getPlayer());
                break;
            case HYPNOTIZE_DMG:
                MobHandler.HypnotizeDmg(slea, c.getPlayer());
                break;
            case MOB_NODE:
                MobHandler.MobNode(slea, c.getPlayer());
                break;
            case DISPLAY_NODE:
                MobHandler.DisplayNode(slea, c.getPlayer());
                break;
            case MOVE_LIFE:
                MobHandler.MoveMonster(slea, c, c.getPlayer());
                break;
            case AUTO_AGGRO:
                MobHandler.AutoAggro(slea.readInt(), c.getPlayer());
                break;
            case FRIENDLY_DAMAGE:
                MobHandler.FriendlyDamage(slea, c.getPlayer());
                break;
            case REISSUE_MEDAL:
                PlayerHandler.ReIssueMedal(slea, c, c.getPlayer());
                break;
            case MONSTER_BOMB:
                MobHandler.MonsterBomb(slea.readInt(), c.getPlayer());
                break;
            case MOB_BOMB:
                MobHandler.MobBomb(slea, c.getPlayer());
                break;
            case NPC_SHOP:
                NPCHandler.NPCShop(slea, c, c.getPlayer());
                break;
            case NPC_TALK:
                NPCHandler.NPCTalk(slea, c, c.getPlayer());
                break;
            case NPC_TALK_MORE:
                NPCHandler.NPCMoreTalk(slea, c);
                break;
            case NPC_ACTION:
                NPCHandler.NPCAnimation(slea, c);
                break;
            case QUEST_ACTION:
                NPCHandler.QuestAction(slea, c, c.getPlayer());
                break;
            case STORAGE:
                NPCHandler.Storage(slea, c, c.getPlayer());
                break;
            case GENERAL_CHAT:
                if (c.getPlayer() != null && c.getPlayer().getMap() != null) {
                    c.getPlayer().updateTick(slea.readInt());
                    ChatHandler.GeneralChat(slea.readMapleAsciiString(), slea.readByte(), c, c.getPlayer());
                }
                break;
            case PARTYCHAT:
                //c.getPlayer().updateTick(slea.readInt());
                ChatHandler.Others(slea, c, c.getPlayer());
                break;
            case WHISPER:
                ChatHandler.Whisper_Find(slea, c);
                break;
            case MESSENGER:
                ChatHandler.Messenger(slea, c);
                break;
            case AUTO_ASSIGN_AP:
                StatsHandling.AutoAssignAP(slea, c, c.getPlayer());
                break;
            case DISTRIBUTE_AP:
                StatsHandling.DistributeAP(slea, c, c.getPlayer());
                break;
            case TEACH_SKILL:
                PlayerHandler.TeachSkill(slea, c, c.getPlayer());
                break;
            case DISTRIBUTE_SP:
                c.getPlayer().updateTick(slea.readInt());
                StatsHandling.DistributeSP(slea.readInt(), c, c.getPlayer());
                break;
            case PLAYER_INTERACTION:
                PlayerInteractionHandler.PlayerInteraction(slea, c, c.getPlayer());
                break;
            case GUILD_OPERATION:
                GuildHandler.Guild(slea, c);
                break;
            case DENY_GUILD_REQUEST:
                slea.skip(1);
                GuildHandler.DenyGuildRequest(slea.readMapleAsciiString(), c);
                break;
            case ALLIANCE_OPERATION:
                AllianceHandler.HandleAlliance(slea, c, false);
                break;
            case DENY_ALLIANCE_REQUEST:
                AllianceHandler.HandleAlliance(slea, c, true);
                break;
            case PUBLIC_NPC:
                NPCHandler.OpenPublicNpc(slea, c);
                break;
            case BBS_OPERATION:
                BBSHandler.BBSOperation(slea, c);
                break;
            case PARTY_OPERATION:
                PartyHandler.PartyOperation(slea, c);
                break;
            case DENY_PARTY_REQUEST:
                PartyHandler.DenyPartyRequest(slea, c);
                break;
            case ALLOW_PARTY_INVITE:
                PartyHandler.AllowPartyInvite(slea, c);
                break;
            case BUDDYLIST_MODIFY:
                BuddyListHandler.BuddyOperation(slea, c);
                break;
            case CYGNUS_SUMMON:
                UserInterfaceHandler.CygnusSummon_NPCRequest(c);
                break;
            case SHIP_OBJECT:
                UserInterfaceHandler.ShipObjectRequest(slea.readInt(), c);
                break;
            case BUY_CS_ITEM:
                CashShopOperation.BuyCashItem(slea, c, c.getPlayer());
                break;
            case COUPON_CODE:
                //FileoutputUtil.log(FileoutputUtil.PacketEx_Log, "Coupon : \n" + slea.toString(true));
                //System.out.println(slea.toString());
                CashShopOperation.CouponCode(slea.readMapleAsciiString(), c);
                CashShopOperation.CouponCode(slea.readMapleAsciiString(), c);
                CashShopOperation.doCSPackets(c);
                break;
            case GIFT:
                CashShopOperation.sendGift(slea, c);
                break;
            case CS_UPDATE:
                CashShopOperation.CSUpdate(c);
                break;
            case TOUCHING_MTS:
                MTSOperation.MTSUpdate(MTSStorage.getInstance().getCart(c.getPlayer().getId()), c);
                break;
            case MTS_TAB:
                MTSOperation.MTSOperation(slea, c);
                break;
            case USE_POT:
                ItemMakerHandler.UsePot(slea, c);
                break;
            case CLEAR_POT:
                ItemMakerHandler.ClearPot(slea, c);
                break;
            case FEED_POT:
                ItemMakerHandler.FeedPot(slea, c);
                break;
            case CURE_POT:
                ItemMakerHandler.CurePot(slea, c);
                break;
            case REWARD_POT:
                ItemMakerHandler.RewardPot(slea, c);
                break;
            case DAMAGE_SUMMON:
                slea.skip(4);
                SummonHandler.DamageSummon(slea, c.getPlayer());
                break;
            case MOVE_SUMMON:
                SummonHandler.MoveSummon(slea, c.getPlayer());
                break;
            case SUMMON_ATTACK:
                SummonHandler.SummonAttack(slea, c, c.getPlayer());
                break;
            case MOVE_DRAGON:
                SummonHandler.MoveDragon(slea, c.getPlayer());
                break;
            case SUB_SUMMON:
                SummonHandler.SubSummon(slea, c.getPlayer());
                break;
            case REMOVE_SUMMON:
                SummonHandler.RemoveSummon(slea, c);
                break;
            case SPAWN_PET:
                PetHandler.SpawnPet(slea, c, c.getPlayer());
                break;
            case PET_AUTO_BUFF:
                PetHandler.Pet_AutoBuff(slea, c, c.getPlayer());
                break;
            case MOVE_PET:
                PetHandler.MovePet(slea, c.getPlayer());
                break;
            case PET_CHAT:
                //System.out.println("Pet chat: " + slea.toString());
                if (slea.available() < 12) {
                    break;
                }
                final int petid = slea.readInt();
                c.getPlayer().updateTick(slea.readInt());
                PetHandler.PetChat(petid, slea.readShort(), slea.readMapleAsciiString(), c.getPlayer());
                break;
            case PET_COMMAND:
                PetHandler.PetCommand(slea, c, c.getPlayer());
                break;
            case PET_FOOD:
                PetHandler.PetFood(slea, c, c.getPlayer());
                break;
            case PET_LOOT:
                InventoryHandler.Pickup_Pet(slea, c, c.getPlayer());
                break;
            case PET_AUTO_POT:
                PetHandler.Pet_AutoPotion(slea, c, c.getPlayer());
                break;
            case PET_LOOT_TOGGLE:
                PetHandler.Pet_LootToggle(slea, c, c.getPlayer());
                break;
            case MONSTER_CARNIVAL:
                MonsterCarnivalHandler.MonsterCarnival(slea, c);
                break;
            case DUEY_ACTION:
                DueyHandler.DueyOperation(slea, c);
                break;
            case USE_HIRED_MERCHANT:
                HiredMerchantHandler.UseHiredMerchant(c, true);
                break;
            case MERCH_ITEM_STORE:
                HiredMerchantHandler.MerchantItemStore(slea, c);
                break;
            case CANCEL_DEBUFF:
                // Ignore for now
                break;
            case MAPLETV:
                break;
            case LEFT_KNOCK_BACK:
                PlayerHandler.leftKnockBack(slea, c);
                break;
            case SNOWBALL:
                PlayerHandler.snowBall(slea, c);
                break;
            case COCONUT:
                PlayersHandler.hitCoconut(slea, c);
                break;
            case REPAIR:
                NPCHandler.repair(slea, c);
                break;
            case REPAIR_ALL:
                NPCHandler.repairAll(c);
                break;
            case GAME_POLL:
                UserInterfaceHandler.InGame_Poll(slea, c);
                break;
            case OWL:
                InventoryHandler.Owl(slea, c);
                break;
            case OWL_WARP:
                InventoryHandler.OwlWarp(slea, c);
                break;
            case USE_OWL_MINERVA:
                InventoryHandler.OwlMinerva(slea, c);
                break;
            case RPS_GAME:
                NPCHandler.RPSGame(slea, c);
                break;
            case UPDATE_QUEST:
                NPCHandler.UpdateQuest(slea, c);
                break;
            case USE_ITEM_QUEST:
                NPCHandler.UseItemQuest(slea, c);
                break;
            case FOLLOW_REQUEST:
                PlayersHandler.FollowRequest(slea, c);
                break;
            case AUTO_FOLLOW_REPLY:
            case FOLLOW_REPLY:
                PlayersHandler.FollowReply(slea, c);
                break;
            case RING_ACTION:
                PlayersHandler.RingAction(slea, c);
                break;
            case REQUEST_FAMILY:
                FamilyHandler.RequestFamily(slea, c);
                break;
            case OPEN_FAMILY:
                FamilyHandler.OpenFamily(slea, c);
                break;
            case FAMILY_OPERATION:
                FamilyHandler.FamilyOperation(slea, c);
                break;
            case DELETE_JUNIOR:
                FamilyHandler.DeleteJunior(slea, c);
                break;
            case DELETE_SENIOR:
                FamilyHandler.DeleteSenior(slea, c);
                break;
            case USE_FAMILY:
                FamilyHandler.UseFamily(slea, c);
                break;
            case FAMILY_PRECEPT:
                FamilyHandler.FamilyPrecept(slea, c);
                break;
            case FAMILY_SUMMON:
                FamilyHandler.FamilySummon(slea, c);
                break;
            case ACCEPT_FAMILY:
                FamilyHandler.AcceptFamily(slea, c);
                break;
            case SOLOMON:
                PlayersHandler.Solomon(slea, c);
                break;
            case GACH_EXP:
                PlayersHandler.GachExp(slea, c);
                break;
            case PARTY_SEARCH_START:
                PartyHandler.MemberSearch(slea, c);
                break;
            case PARTY_SEARCH_STOP:
                PartyHandler.PartySearch(slea, c);
                break;
            case EXPEDITION_LISTING:
                PartyHandler.PartyListing(slea, c);
                break;
            case EXPEDITION_OPERATION:
                PartyHandler.Expedition(slea, c);
                break;
            case USE_TELE_ROCK:
                InventoryHandler.TeleRock(slea, c);
                break;
            case AZWAN_REVIVE:
                PlayersHandler.reviveAzwan(slea, c);
                break;
            case INNER_CIRCULATOR:
                InventoryHandler.useInnerCirculator(slea, c);
                break;
            case PAM_SONG:
                InventoryHandler.PamSong(slea, c);
                break;
            case XMAS_SURPRISE:
                CashShopOperation.XmasSurprise(slea, c);
                break;
            case REPORT:
                PlayersHandler.Report(slea, c);
                break;
            case CHRONOSPHERE:
                PlayersHandler.UseChronosphere(slea, c, c.getPlayer());
                break;
            default:
                System.out.println("[UNHANDLED] Recv [" + header.toString() + "] found");
                break;
        }
    }
}
