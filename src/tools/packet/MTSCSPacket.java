package tools.packet;

import java.sql.SQLException;
import java.sql.ResultSet;

import java.util.List;

import client.MapleClient;
import client.MapleCharacter;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import server.CashShop;
import server.CashItemFactory;

import handling.SendPacketOpcode;
import constants.ServerConstants;

import java.util.ArrayList;

import tools.Pair;

import java.util.Map;
import java.util.Map.Entry;

import server.CashItem;
import server.CashItemFlag;
import server.CashModItem;
import server.MTSStorage.MTSItemInfo;
import tools.HexTool;
import tools.data.MaplePacketLittleEndianWriter;

public class MTSCSPacket {

    private static final byte Operation_Code = 0x5D; // We could just change this everytime a version updates
    private static final byte[] bestItems = HexTool.getByteArrayFromHexString("02 00 00 00 31 00 00 00 0A 00 10 00 12 00 0E 07 E0 3B 8B 0B 60 CE 8A 0B 69 00 6C 00 6C 00 2F 00 35 00 33 00 32 00 30 00 30 00 31 00 31 00 2F 00 73 00 75 00 6D 00 6D 00 6F 00 6E 00 2F 00 61 00 74 00 74 00 61 00 63 00 6B 00 31 00 2F 00 31 0000 00 00 00 00 00 00 00 02 00 1A 00 04 01 08 07 02 00 00 00 32 00 00 00 05 00 1C 00 06 00 08 07 A0 01 2E 00 58 CD 8A 0B");

    public static byte[] warpCS(MapleClient c) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPEN.getValue());
        PacketHelper.addCharacterInfo(mplew, c.getPlayer());

        return mplew.getPacket();
    }

    public static byte[] getCSInfo(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_INFO.getValue());
        mplew.writeMapleAsciiString(c.getAccountName());
        mplew.writeInt(0); // Not Sale count. -> Send here removes the item from CS. For each: int(SN)
        // 商城道具
        CashItemFactory cif = CashItemFactory.getInstance();
        List<CashItem> csHideItems = cif.getHideAllDefaultItems(); // 關閉預設物品
        List<CashModItem> csItems = cif.getAllModItems();
        List<Integer> extraItems = new ArrayList<>();
        extraItems.add(10001205);
        extraItems.add(10001206);
        extraItems.add(80000354);
        extraItems.add(80000355);
        mplew.writeShort(csHideItems.size() + csItems.size() + extraItems.size());
        // 隱藏不出售的商品
        for (CashItem csItem : csHideItems) {
            mplew.writeInt(csItem.getSN());
            mplew.writeInt(0x400);
            mplew.write(0);
        }
        for (int sn : extraItems) {
            mplew.writeInt(sn);
            mplew.writeInt(0x400);
            mplew.write(0);
        }
        // 自定義商品寫入
        for (CashModItem csMod : csItems) {
            addCashModItem(mplew, csMod);
        }
        mplew.writeShort(0);
        mplew.write(0); // Category discount rate (For each: byte(category), byte(sub) byte(rate))
        final Map<Integer, List<Integer>> rmi = CashItemFactory.getInstance().getRandomItemInfo();
        mplew.writeInt(rmi.size()); // I don't know what does this do atm, but it seemed that it corresponds the items..
        for (final Entry<Integer, List<Integer>> i : rmi.entrySet()) {
            mplew.writeInt(i.getKey()); // Item Id
            if (i.getKey() / 1000 != 5533) {
                continue;
            }
            mplew.writeInt(i.getValue().size());
            for (final Integer x : i.getValue()) {
                mplew.writeInt(x); // SN
            }
        }
        mplew.write(bestItems);

        for (int i = 1; i <= 8; i++) {
            for (int j = 0; j <= 1; j++) {
                for (int hs = 0; hs < 5; hs++) {
                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(ServerConstants.hot_sell[hs]);
                }
            }
        }

        mplew.writeShort(0); // Stock [Disable/Enable Buy button] (For each: int(sn), int(amount left))
        mplew.writeShort(0); // Limited Goods 104 bytes-> A2 35 4D 00 CE FD FD 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 FF FF FF FF FF FF FF FF 06 00 00 00 1F 1C 32 01 A7 3F 32 01 FF FF FF FF FF FF FF FF 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00
        mplew.writeInt(0);
        mplew.write(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    private static void addCashModItem(MaplePacketLittleEndianWriter mplew, CashModItem csMod) {
        mplew.writeInt(csMod.getSN());
        if (csMod.getId() == 5040005
                || csMod.getId() == 5060003
                || csMod.getId() == 5060006
                || csMod.getId() == 5062102
                || csMod.getId() == 5064001
                || csMod.getId() == 5070000
                || csMod.getId() == 5200000
                || csMod.getId() == 5200001
                || csMod.getId() == 5200002
                || csMod.getId() == 5201001
                || csMod.getId() == 5201002
                || csMod.getId() == 5201003
                || csMod.getId() == 5220000
                || csMod.getId() == 5220001
                || csMod.getId() == 5220002
                || csMod.getId() == 5220003
                || csMod.getId() == 5220004
                || csMod.getId() == 5220008
                || csMod.getId() == 5220010
                || csMod.getId() == 5220082
                || csMod.getId() == 5222004
                || csMod.getId() == 5223000
                || csMod.getId() == 5252000
                || csMod.getId() == 5250003
                || csMod.getId() == 5252001
                || csMod.getId() == 5252003
                || csMod.getId() == 5320000
                || csMod.getId() == 5330000
                || csMod.getId() == 5350003
                || csMod.getId() == 5400000
                || csMod.getId() == 5401000
                || csMod.getId() == 5431000
                || csMod.getId() == 5431002
                || csMod.getId() == 5431003
                || csMod.getId() == 5431004
                || csMod.getId() == 5440000
                || csMod.getId() == 5451000
                || csMod.getId() == 5451001
                || csMod.getId() == 5470000
                || csMod.getId() == 5490005
                || csMod.getId() == 5490006
                || csMod.getId() == 5540000
                || csMod.getId() == 5560000
                || csMod.getId() == 5561000
                || csMod.getId() == 5600000
                || csMod.getId() == 5600001
                || csMod.getId() == 5660000
                || csMod.getId() == 5660001
                || csMod.getId() == 5670000
                || csMod.getId() == 5670001
                || csMod.getId() == 5680022
                || csMod.getId() == 5720000
                || csMod.getId() / 10000 == 553
                || csMod.getId() >= 5211087 && csMod.getId() <= 5211090
                || csMod.getId() >= 5211096 && csMod.getId() <= 5211107
                || csMod.getId() >= 5480000 && csMod.getId() <= 5480011
                || csMod.getId() == 5220040 && csMod.getCount() == 1
                || csMod.getId() == 5220040 && csMod.getCount() == 5
                || csMod.getId() == 5220040 && csMod.getCount() == 40
                || csMod.getId() == 5220040 && csMod.getCount() == 50) {
            mplew.writeInt(0x400);
            mplew.write(0);
            return;
        }
        int[] mask = new int[1];
        for (CashItemFlag cf : csMod.flags) {
            mask[cf.getPosition()] |= cf.getValue();
        }
        for (int i = 0; i < mask.length; i++) {
            mplew.writeInt(mask[i]);
        }
        // [0x1][V]
        if (csMod.flags.contains(CashItemFlag.ITEMID)) {
            mplew.writeInt(csMod.getId());
        }
        // [0x2][V]
        if (csMod.flags.contains(CashItemFlag.COUNT)) {
            mplew.writeShort(csMod.getCount());
        }
        // [0x4][V]
        if (csMod.flags.contains(CashItemFlag.PRICE)) {
            mplew.writeInt(csMod.getPrice());
        }
        // [0x8][V]
        if (csMod.flags.contains(CashItemFlag.UNK3)) {
            mplew.write(0);
        }
        // [0x10][V]
        if (csMod.flags.contains(CashItemFlag.PRIORITY)) {
            mplew.write(csMod.getPriority());
        }
        // [0x20][V]
        if (csMod.flags.contains(CashItemFlag.PERIOD)) {
            mplew.writeShort(csMod.getPeriod());
        }
        // [0x40][V]
        if (csMod.flags.contains(CashItemFlag.UNK6)) {
            mplew.writeInt(0);
        }
        // [0x80][V]
        if (csMod.flags.contains(CashItemFlag.MESO)) {
            mplew.writeInt(0);
        }
        // [0x100][V]
        if (csMod.flags.contains(CashItemFlag.UNK8)) {
            mplew.write(0);
        }
        // [0x200][V]
        if (csMod.flags.contains(CashItemFlag.GENDER)) {
            mplew.write(0);
        }
        // [0x400][V]
        if (csMod.flags.contains(CashItemFlag.ONSALE)) {
            mplew.write(csMod.isOnSale() ? 1 : 0);
        }
        // [0x800][V]
        if (csMod.flags.contains(CashItemFlag.FLAGE)) {
            mplew.write(csMod.getFlage());
        }
        // [0x1000][V]
        if (csMod.flags.contains(CashItemFlag.UNK12)) {
            mplew.write(0);
        }
        // [0x2000][V]
        if (csMod.flags.contains(CashItemFlag.UNK13)) {
            mplew.writeShort(0);
        }
        // [0x4000][V]
        if (csMod.flags.contains(CashItemFlag.UNK14)) {
            mplew.writeShort(0);
        }
        // [0x8000][V]
        if (csMod.flags.contains(CashItemFlag.UNK15)) {
            mplew.writeShort(0);
        }
        // [0x10000][V]
        if (csMod.flags.contains(CashItemFlag.PACKAGEZ)) {
            List pack = CashItemFactory.getInstance().getPackageItems(csMod.getSN());
            if (pack == null) {
                mplew.write(0);
            } else {
                mplew.write(pack.size());
                pack.stream().forEach((pack1) -> {
                    mplew.writeInt((Integer) pack1);
                });
            }
        }
        // [0x20000][V]
        if (csMod.flags.contains(CashItemFlag.UNK17)) {
            mplew.write(0);
        }
    }

    public static byte[] getCSAccountName(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_ACCOUNT_NAME.getValue());
        mplew.write(1);
        mplew.writeMapleAsciiString(c.getAccountName());

        return mplew.getPacket();
    }

    public static byte[] chargeCash() { // Useless, doesn't do anything.
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_CHARGE_CASH.getValue());
        mplew.writeMapleAsciiString("http://www.google.com");
        mplew.writeMapleAsciiString("http://www.google.com");

        return mplew.getPacket();
    }

    public static byte[] showNXMapleTokens(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_UPDATE.getValue());
        mplew.writeInt(chr.getCSPoints(0));
        mplew.writeInt(chr.getCSPoints(1));

        return mplew.getPacket();
    }

    public static byte[] LimitGoodsCountChanged() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code - 1);
        mplew.writeInt(0); // SN
        mplew.writeInt(0); // Count
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] getCSInventory(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 3); // 5 = Failed + transfer
        CashShop mci = c.getPlayer().getCashInventory();
        mplew.writeShort(mci.getItemsSize());
        if (mci.getItemsSize() > 0) {
            int size = 0;
            for (Item itemz : mci.getInventory()) {
                addCashItemInfo(mplew, itemz, c.getAccID(), 0);
                if (GameConstants.isPet(itemz.getItemId()) || GameConstants.getInventoryType(itemz.getItemId()) == MapleInventoryType.EQUIP) {
                    size++;
                }
            }
            mplew.writeInt(size);
            for (Item itemz : mci.getInventory()) {
                if (GameConstants.isPet(itemz.getItemId()) || GameConstants.getInventoryType(itemz.getItemId()) == MapleInventoryType.EQUIP) {
                    PacketHelper.addItemInfo(mplew, itemz);
                }
            }
        }
        mplew.writeShort(c.getPlayer().getStorage().getSlots());
        mplew.writeShort(c.getCharacterSlots());
        mplew.writeShort(0);
        mplew.writeShort(4); //04 00 <-- added?

        return mplew.getPacket();
    }

    public static byte[] getCSGifts(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 6); // 7 = Failed + transfer
        List<Pair<Item, String>> mci = c.getPlayer().getCashInventory().loadGifts();
        mplew.writeShort(mci.size());
        for (Pair<Item, String> mcz : mci) { // 70 Bytes, need to recheck.
            mplew.writeLong(mcz.getLeft().getUniqueId());
            mplew.writeInt(mcz.getLeft().getItemId());
            mplew.writeAsciiString(mcz.getLeft().getGiftFrom(), 15);
            mplew.writeAsciiString(mcz.getRight(), 74);
        }

        return mplew.getPacket();
    }

    public static byte[] sendWishList(MapleCharacter chr, boolean update) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + (update ? 10 : 8)); // 9 = Failed + transfer, 16 = Failed.
        int[] list = chr.getWishlist();
        for (int i = 0; i < 10; i++) {
            mplew.writeInt(list[i] != -1 ? list[i] : 0);
        }

        return mplew.getPacket();
    }

    public static byte[] showBoughtCSItem(Item item, int sn, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 12);
        addCashItemInfo(mplew, item, accid, sn);

        return mplew.getPacket();
    }

    public static byte[] showBoughtCSItem(int itemid, int sn, int uniqueid, int accid, int quantity, String giftFrom, long expire) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 12);
        addCashItemInfo(mplew, uniqueid, accid, itemid, sn, quantity, giftFrom, expire);

        return mplew.getPacket();
    }

    public static byte[] showBoughtCSItemFailed(final int mode, final int sn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 13);
        mplew.write(mode); // 0/1/2 = transfer, Rest = code
        switch (mode) {
            case 28:
            case 29:
                // Limit Goods update. this item is out of stock, and therefore not available for sale.
                mplew.writeInt(sn);
                break;
            case 48:
                // You cannot make any more purchases in %d.\r\nPlease try again in (%d + 1).
                mplew.write(1);    // Hour?
                break;
            case 64:
                // %s can only be purchased once a month.
                mplew.writeInt(sn);
                mplew.writeLong(System.currentTimeMillis());
                break;
            default:
                break;
        }

        return mplew.getPacket();
    }

    public static byte[] showBoughtCSPackage(Map<Integer, Item> ccc, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 66); // 72 = Similar structure to showBoughtCSItemFailed
        mplew.write(ccc.size());
        for (Entry<Integer, Item> sn : ccc.entrySet()) {
            addCashItemInfo(mplew, sn.getValue(), accid, sn.getKey());
        }
        mplew.writeShort(0); // Purchase Maple Points = 1, Item = 0
        mplew.writeInt(0); // SN
        //mplew.writeLong(System.currentTimeMillis());

        return mplew.getPacket();
    }

    public static byte[] sendGift(int price, int itemid, int quantity, String receiver, boolean packages) {
        // [ %s ] \r\nwas sent to %s. \r\n%d NX Prepaid \r\nwere spent in the process.
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + (packages ? 68 : 19)); // 74 = Similar structure to showBoughtCSItemFailed
        mplew.writeMapleAsciiString(receiver);
        mplew.writeInt(itemid);
        mplew.writeShort(quantity);
        if (packages) {
            mplew.writeShort(0); //maplePoints
        }
        mplew.writeInt(price);

        return mplew.getPacket();
    }

    public static byte[] showCouponRedeemedItem(Map<Integer, Item> items, int mesos, int maplePoints, MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 14);
        mplew.write(items.size());
        for (Entry<Integer, Item> item : items.entrySet()) {
            addCashItemInfo(mplew, item.getValue(), c.getAccID(), item.getKey());
        }
        mplew.writeInt(maplePoints);
        mplew.writeInt(0); // Normal items size
        //for (Pair<Integer, Integer> item : items2) {
        //    mplew.writeInt(item.getRight()); // Count
        //    mplew.writeInt(item.getLeft());  // Item ID
        //}
        mplew.writeInt(mesos);

        return mplew.getPacket();
    }

    public static byte[] showCouponGifted(Map<Integer, Item> items, String receiver, MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 16); // 22 = Failed. [Mode - 0/2 = transfer, 15 = invalid 3 times]
        mplew.writeMapleAsciiString(receiver); // Split by ;
        mplew.write(items.size());
        for (Entry<Integer, Item> item : items.entrySet()) {
            addCashItemInfo(mplew, item.getValue(), c.getAccID(), item.getKey());
        }
        mplew.writeInt(0); // (amount of receiver - 1)

        return mplew.getPacket();
    }

    public static byte[] increasedInvSlots(int inv, int slots) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 21);
        mplew.write(inv);
        mplew.writeShort(slots);

        return mplew.getPacket();
    }

    public static byte[] increasedStorageSlots(int slots, boolean characterSlots) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + (characterSlots ? 25 : 23)); // 32 = Buy Character. O.O
        mplew.writeShort(slots);

        return mplew.getPacket();
    }

    public static byte[] increasedPendantSlots() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 29); // 35 = Failed
        mplew.writeShort(0); // 0 = Add, 1 = Extend
        mplew.writeShort(100); // Related to time->Low/High fileTime
        // The time limit for the %s slot \r\nhas been extended to %d-%d-%d %d:%d.

        return mplew.getPacket();
    }

    public static byte[] confirmFromCSInventory(Item item, short pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 31); // 37 = Failed
        mplew.writeShort(pos);
        PacketHelper.addItemInfo(mplew, item);
        mplew.writeInt(0); // For each: 8 bytes(Could be 2 ints or 1 long)

        return mplew.getPacket();
    }

    public static byte[] confirmToCSInventory(Item item, int accId, int sn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 33); // 39 = Failed
        addCashItemInfo(mplew, item, accId, sn, false);

        return mplew.getPacket();
    }

    public static byte[] cashItemDelete(int uniqueid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 35); // 41 = Failed
        mplew.writeLong(uniqueid); // or SN?

        return mplew.getPacket();
    }

    public static byte[] rebateCashItem() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 62); // 41 = Failed
        mplew.writeLong(0); // UniqueID
        mplew.writeInt(0); // MaplePoints accumulated
        mplew.writeInt(0); // For each: 8 bytes.

        return mplew.getPacket();
    }

    public static byte[] sendBoughtRings(boolean couple, Item item, int sn, int accid, String receiver) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + (couple ? 64 : 74));
        addCashItemInfo(mplew, item, accid, sn);
        mplew.writeMapleAsciiString(receiver);
        mplew.writeInt(item.getItemId());
        mplew.writeShort(1); // Count

        return mplew.getPacket();
    }

    public static byte[] receiveFreeCSItem(Item item, int sn, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 76); // 105 = Buy Name Change, 107 = Transfer world
        addCashItemInfo(mplew, item, accid, sn);

        return mplew.getPacket();
    }

    public static byte[] cashItemExpired(int uniqueid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 37);
        mplew.writeLong(uniqueid);

        return mplew.getPacket();
    }

    public static byte[] showBoughtCSQuestItem(int price, short quantity, byte position, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 71); // 76 = Failed.
        mplew.writeInt(1); // size. below gets repeated for each.
        mplew.writeInt(quantity);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static byte[] updatePurchaseRecord(final int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 87); // 95 = Failed.
        mplew.writeInt(value);
        mplew.write(value != 0 ? 0 : 1); // boolean

        return mplew.getPacket();
    }

    public static byte[] sendCashRefund(final int cash) {
        // Your refund has been processed. \r\n(%d NX Refund)
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 90);
        mplew.writeInt(0); // Item Size.->For each 8 bytes.
        mplew.writeInt(cash); // NX

        return mplew.getPacket();
    }

    public static byte[] sendRandomBox(int uniqueid, Item item, short pos) { // have to revise this
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 92); // 100 = Failed
        mplew.writeLong(uniqueid);
        mplew.writeInt(1302000);
        PacketHelper.addItemInfo(mplew, item);
        mplew.writeShort(0);
        mplew.writeInt(0); // Item Size.->For each 8 bytes.

        return mplew.getPacket();
    }

    public static byte[] sendCashGachapon(final boolean cashItem, int idFirst, Item item, int accid) { // Xmas Surprise, Cash Shop Surprise
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 103); // 110 = Failed.		
        mplew.writeLong(idFirst); //uniqueid of the xmas surprise itself
        mplew.writeInt(0);
        mplew.write(cashItem ? 1 : 0);
        if (cashItem) {
            addCashItemInfo(mplew, item, accid, 0); //info of the new item, but packet shows 0 for sn?
        }
        mplew.writeInt(item.getItemId());
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] sendTwinDragonEgg(boolean test1, boolean test2, int idFirst, Item item, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 105); // 112 = Failed.		
        mplew.write(test1 ? 1 : 0);
        mplew.write(test2 ? 1 : 0);
        mplew.writeInt(1);
        mplew.writeInt(2);
        mplew.writeInt(3);
        mplew.writeInt(4);
        if (test1 && test2) {
            addCashItemInfo(mplew, item, accid, 0); //info of the new item, but packet shows 0 for sn?
        }

        return mplew.getPacket();
    }

    public static byte[] sendBoughtMaplePoints(final int maplePoints) {
        // You've received %d Maple Points.
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(Operation_Code + 107);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(maplePoints);

        return mplew.getPacket();
    }

    public static byte[] changeNameCheck(final String charname, final boolean nameUsed) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHANGE_NAME_CHECK.getValue());
        mplew.writeMapleAsciiString(charname);
        mplew.write(nameUsed ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] changeNameResponse(final int mode, final int pic) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // 0: Success
        // 1: The name change is already submitted \r\ndue to the item purchase
        // 2: This applies to the limitations on the request.\r\nPlease check if you were recently banned \r\nwithin 3 months.
        // 3: This applies to the limitations on the request.\r\nPlease check if you requested \r\nfor the name change within a month.
        // default: An unknown error has occured.
        mplew.writeShort(SendPacketOpcode.CHANGE_NAME_RESPONSE.getValue());
        mplew.writeInt(0);
        mplew.write(mode);
        mplew.writeInt(pic); // pic or birthdate

        return mplew.getPacket();
    }

    public static byte[] receiveGachaStamps(final boolean invfull, final int amount) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GACHAPON_STAMPS.getValue());
        mplew.write(invfull ? 0 : 1);
        if (!invfull) {
            mplew.writeInt(amount);
        }

        return mplew.getPacket();
    }

    public static byte[] freeCashItem(final int itemId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FREE_CASH_ITEM.getValue());
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    public static byte[] showXmasSurprise(boolean full, int idFirst, Item item, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.XMAS_SURPRISE.getValue());
        mplew.write(full ? 209 : 210);
        if (!full) {
            mplew.writeLong(idFirst); //uniqueid of the xmas surprise itself
            mplew.writeInt(0);
            addCashItemInfo(mplew, item, accid, 0); //info of the new item, but packet shows 0 for sn?
            mplew.writeInt(item.getItemId());
            mplew.write(1);
            mplew.write(1);
        }

        return mplew.getPacket();
    }

    public static byte[] showOneADayInfo(boolean show, int sn) { // hmmph->Buy regular item causes invalid pointer
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ONE_A_DAY.getValue());
        mplew.writeInt(100); //idk-related to main page
        mplew.writeInt(100000); // idk-related to main page
        mplew.writeInt(1); // size of items to buy, for each, repeat 3 ints below.
        mplew.writeInt(20121231); // yyyy-mm-dd
        mplew.writeInt(sn);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] playCashSong(int itemid, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CASH_SONG.getValue());
        mplew.writeInt(itemid);
        mplew.writeMapleAsciiString(name);
        return mplew.getPacket();
    }

    public static byte[] useAlienSocket(boolean start) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALIEN_SOCKET_CREATOR.getValue());
        mplew.write(start ? 0 : 2);

        return mplew.getPacket();
    }

    public static byte[] ViciousHammer(boolean start, int hammered) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.VICIOUS_HAMMER.getValue());
        mplew.write(start ? 2 : 3);
        mplew.writeInt(0);
        if (start) {
            mplew.writeInt(hammered);
        }

        return mplew.getPacket();
    }

    public static byte[] NewViciousHammer(boolean start, int hammered) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NEW_VICIOUS_HAMMER.getValue());
        mplew.write(start ? 78 : 79);
        mplew.writeInt(0);
        if (start) {
            mplew.writeInt(hammered);
        }

        return mplew.getPacket();
    }

    public static byte[] changePetFlag(int uniqueId, boolean added, int flagAdded) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PET_FLAG_CHANGE.getValue());

        mplew.writeLong(uniqueId);
        mplew.write(added ? 1 : 0);
        mplew.writeShort(flagAdded);

        return mplew.getPacket();
    }

    public static byte[] changePetName(MapleCharacter chr, String newname, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PET_NAMECHANGE.getValue());

        mplew.writeInt(chr.getId());
        mplew.write(0);
        mplew.writeMapleAsciiString(newname);
        mplew.write(slot);

        return mplew.getPacket();
    }

    public static byte[] OnMemoResult(final byte act, final byte mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        //04 // The note has successfully been sent 
        //05 00 // The other character is online now. Please use the whisper function. 
        //05 01 // Please check the name of the receiving character. 
        //05 02 // The receiver's inbox is full. Please try again. 
        mplew.writeShort(SendPacketOpcode.SHOW_NOTES.getValue());
        mplew.write(act);
        if (act == 5) {
            mplew.write(mode);
        }

        return mplew.getPacket();
    }

    public static byte[] showNotes(final ResultSet notes, final int count) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_NOTES.getValue());
        mplew.write(3);
        mplew.write(count);
        for (int i = 0; i < count; i++) {
            mplew.writeInt(notes.getInt("id"));
            mplew.writeMapleAsciiString(notes.getString("from"));
            mplew.writeMapleAsciiString(notes.getString("message"));
            mplew.writeLong(PacketHelper.getKoreanTimestamp(notes.getLong("timestamp")));
            mplew.write(notes.getInt("gift"));
            notes.next();
        }

        return mplew.getPacket();
    }

    public static byte[] useChalkboard(final int charid, final String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHALKBOARD.getValue());

        mplew.writeInt(charid);
        if (msg == null || msg.length() <= 0) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeMapleAsciiString(msg);
        }

        return mplew.getPacket();
    }

    public static byte[] OnMapTransferResult(MapleCharacter chr, byte vip, boolean delete) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // 29 00 05/08 00 // You cannot go to that place.
        // 29 00 06 00 // (null) is currently difficult to locate, so the teleport will not take place.
        // 29 00 09 00 // It's the map you're currently on.
        // 29 00 0A 00 // This map is not available to enter for the list.
        // 29 00 0B 00 // Users below level 7 are not allowed to go out from Maple Island.
        mplew.writeShort(SendPacketOpcode.TROCK_LOCATIONS.getValue());
        mplew.write(delete ? 2 : 3);
        mplew.write(vip);
        switch (vip) {
            case 1: {
                int[] map = chr.getRegRocks();
                for (int i = 0; i < 5; i++) {
                    mplew.writeInt(map[i]);
                }
                break;
            }
            case 2: {
                int[] map = chr.getRocks();
                for (int i = 0; i < 10; i++) {
                    mplew.writeInt(map[i]);
                }
                break;
            }
            case 3: {
                int[] map = chr.getHyperRocks();
                for (int i = 0; i < 13; i++) {
                    mplew.writeInt(map[i]);
                }
                break;
            }
            default:
                break;
        }

        return mplew.getPacket();
    }

    public static byte[] getTrockMessage(byte op) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TROCK_LOCATIONS.getValue());
        mplew.write(op);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static final void addCashItemInfo(MaplePacketLittleEndianWriter mplew, Item item, int accId, int sn) {
        addCashItemInfo(mplew, item, accId, sn, true);
    }

    public static final void addCashItemInfo(MaplePacketLittleEndianWriter mplew, Item item, int accId, int sn, boolean isFirst) {
        addCashItemInfo(mplew, item.getUniqueId(), accId, item.getItemId(), sn, item.getQuantity(), item.getGiftFrom(), item.getExpiration(), isFirst); //owner for the lulz
    }

    public static final void addCashItemInfo(MaplePacketLittleEndianWriter mplew, int uniqueid, int accId, int itemid, int sn, int quantity, String sender, long expire) {
        addCashItemInfo(mplew, uniqueid, accId, itemid, sn, quantity, sender, expire, true);
    }

    public static final void addCashItemInfo(MaplePacketLittleEndianWriter mplew, int uniqueid, int accId, int itemid, int sn, int quantity, String sender, long expire, boolean isFirst) {
        mplew.writeLong(uniqueid > 0 ? uniqueid : 0);
        mplew.writeLong(accId);
        mplew.writeInt(itemid);
        mplew.writeInt(isFirst ? sn : 0);
        mplew.writeShort(quantity);
        mplew.writeAsciiString(sender, 15); //owner for the lulzlzlzl
        PacketHelper.addExpirationTime(mplew, expire);
        mplew.writeLong(isFirst ? 0 : sn);
        mplew.writeZeroBytes(10);
        //additional 4 bytes for some stuff?
        //if (isFirst && uniqueid > 0 && GameConstants.isEffectRing(itemid)) {
        //	MapleRing ring = MapleRing.loadFromDb(uniqueid);
        //	if (ring != null) { //or is this only for friendship rings, i wonder. and does isFirst even matter
        //		mplew.writeMapleAsciiString(ring.getPartnerName());
        //		mplew.writeInt(itemid);
        //		mplew.writeShort(quantity);
        //	}
        //}
    }

    public static byte[] sendCSFail(int err) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x6C);
        mplew.write(err);

        return mplew.getPacket();
    }

    public static byte[] enableCSUse() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_USE.getValue());
        mplew.write(1);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] getBoosterPack(int f1, int f2, int f3) { //item IDs
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOOSTER_PACK.getValue());
        mplew.write(0xD7);
        mplew.writeInt(f1);
        mplew.writeInt(f2);
        mplew.writeInt(f3);

        return mplew.getPacket();
    }

    public static byte[] getBoosterPackClick() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOOSTER_PACK.getValue());
        mplew.write(0xD5);

        return mplew.getPacket();
    }

    public static byte[] getBoosterPackReveal() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOOSTER_PACK.getValue());
        mplew.write(0xD6);

        return mplew.getPacket();
    }

    public static byte[] sendMesobagFailed(final boolean random) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(random ? SendPacketOpcode.RANDOM_MESOBAG_FAILURE.getValue() : SendPacketOpcode.MESOBAG_FAILURE.getValue());

        return mplew.getPacket();
    }

    public static byte[] sendMesobagSuccess(int mesos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MESOBAG_SUCCESS.getValue());
        mplew.writeInt(mesos);
        return mplew.getPacket();
    }

    public static byte[] sendRandomMesobagSuccess(int size, int mesos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.RANDOM_MESOBAG_SUCCESS.getValue());
        mplew.write(size); // 1 = small, 2 = adequete, 3 = large, 4 = huge
        mplew.writeInt(mesos);

        return mplew.getPacket();
    }

    //======================================MTS===========================================
    public static final byte[] startMTS(final MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPEN.getValue());

        PacketHelper.addCharacterInfo(mplew, chr);
        mplew.writeInt(ServerConstants.MTS_MESO);
        mplew.writeInt(ServerConstants.MTS_TAX);
        mplew.writeInt(ServerConstants.MTS_BASE);
        mplew.writeInt(24);
        mplew.writeInt(168);
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        return mplew.getPacket();
    }

    public static final byte[] sendMTS(final List<MTSItemInfo> items, final int tab, final int type, final int page, final int pages) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x15); //operation
        mplew.writeInt(pages); //total items
        mplew.writeInt(items.size()); //number of items on this page
        mplew.writeInt(tab);
        mplew.writeInt(type);
        mplew.writeInt(page);
        mplew.write(1);
        mplew.write(1);

        for (MTSItemInfo item : items) {
            addMTSItemInfo(mplew, item);
        }
        mplew.write(0); //0 or 1?

        return mplew.getPacket();
    }

    public static final byte[] showMTSCash(final MapleCharacter p) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GET_MTS_TOKENS.getValue());
        mplew.writeInt(p.getCSPoints(0));
        mplew.writeInt(p.getCSPoints(1));
        return mplew.getPacket();
    }

    public static final byte[] getMTSWantedListingOver(final int nx, final int items) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x3D);
        mplew.writeInt(nx);
        mplew.writeInt(items);
        return mplew.getPacket();
    }

    public static final byte[] getMTSConfirmSell() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x1D);
        return mplew.getPacket();
    }

    public static final byte[] getMTSFailSell() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x1E);
        mplew.write(0x42);
        return mplew.getPacket();
    }

    public static final byte[] getMTSConfirmBuy() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x33);
        return mplew.getPacket();
    }

    public static final byte[] getMTSFailBuy() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x34);
        mplew.write(0x42);
        return mplew.getPacket();
    }

    public static final byte[] getMTSConfirmCancel() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x25);
        return mplew.getPacket();
    }

    public static final byte[] getMTSFailCancel() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x26);
        mplew.write(0x42);
        return mplew.getPacket();
    }

    public static final byte[] getMTSConfirmTransfer(final int quantity, final int pos) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x27);
        mplew.writeInt(quantity);
        mplew.writeInt(pos);
        return mplew.getPacket();
    }

    private static void addMTSItemInfo(final MaplePacketLittleEndianWriter mplew, final MTSItemInfo item) {
        PacketHelper.addItemInfo(mplew, item.getItem());
        mplew.writeInt(item.getId()); //id
        mplew.writeInt(item.getTaxes()); //this + below = price
        mplew.writeInt(item.getPrice()); //price
        mplew.writeZeroBytes(4);
        mplew.writeLong(PacketHelper.getTime(item.getEndingDate()));
        mplew.writeMapleAsciiString(item.getSeller()); //account name (what was nexon thinking?)
        mplew.writeMapleAsciiString(item.getSeller()); //char name
        mplew.writeZeroBytes(28);
    }

    public static final byte[] getNotYetSoldInv(final List<MTSItemInfo> items) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x23);

        mplew.writeInt(items.size());

        for (MTSItemInfo item : items) {
            addMTSItemInfo(mplew, item);
        }

        return mplew.getPacket();
    }

    public static final byte[] getTransferInventory(final List<Item> items, final boolean changed) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        mplew.write(0x21);

        mplew.writeInt(items.size());
        int i = 0;
        for (Item item : items) {
            PacketHelper.addItemInfo(mplew, item);
            mplew.writeInt(Integer.MAX_VALUE - i); //fake ID
            mplew.writeZeroBytes(52); //really just addMTSItemInfo
            i++;
        }
        mplew.writeInt(-47 + i - 1);
        mplew.write(changed ? 1 : 0);

        return mplew.getPacket();
    }

    public static final byte[] addToCartMessage(boolean fail, boolean remove) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.getValue());
        if (remove) {
            if (fail) {
                mplew.write(0x2C);
                mplew.writeInt(-1);
            } else {
                mplew.write(0x2B);
            }
        } else {
            if (fail) {
                mplew.write(0x2A);
                mplew.writeInt(-1);
            } else {
                mplew.write(0x29);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] VegaResult(byte result) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.VEGA_RESULT.getValue());
        //success1=81, fail=86, End=82, err2=84
        mplew.write(result);
        return mplew.getPacket();
    }
}
