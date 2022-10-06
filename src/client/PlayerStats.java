/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client;

import client.MapleTrait.MapleTraitType;
import constants.GameConstants;
import client.inventory.MapleInventoryType;
import client.inventory.Item;
import client.inventory.Equip;
import client.inventory.EquipAdditions;
import client.inventory.MapleWeaponType;
import handling.world.World;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildSkill;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.HashMap;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.StructSetItem;
import server.StructSetItem.SetItem;
import server.StructItemOption;
import server.life.Element;
import tools.Pair;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.CField.EffectPacket;
import tools.packet.CWvsContext.InventoryPacket;

public class PlayerStats implements Serializable {

    private static final long serialVersionUID = -679541993413738569L;
    private Map<Integer, Integer> setHandling = new HashMap<>(), skillsIncrement = new HashMap<>(), damageIncrease = new HashMap<>();
    private EnumMap<Element, Integer> elemBoosts = new EnumMap<>(Element.class);
    private List<Equip> durabilityHandling = new ArrayList<>(), equipLevelHandling = new ArrayList<>();
    private transient float shouldHealHP, shouldHealMP;
    public short str, dex, luk, int_;
    public int hp, maxhp, mp, maxmp;
    private transient short passive_sharpeye_min_percent, passive_sharpeye_percent, passive_sharpeye_rate;
    private transient byte passive_mastery;
    private transient int localstr, localdex, localluk, localint_, localmaxhp, localmaxmp;
    public transient int incMaxDF;
    private transient int magic, watk, hands, accuracy;
    public transient boolean equippedWelcomeBackRing, hasClone, hasPartyBonus, Berserk, canFish, canFishVIP;
    public transient double expBuff, dropBuff, mesoBuff, cashBuff, mesoGuard, mesoGuardMeso, expMod, pickupRange;
    public transient double dam_r, bossdam_r;
    public transient int recoverHP, recoverMP, mpconReduce, mpconPercent, incMesoProp, reduceCooltime, DAMreflect, DAMreflect_rate, ignoreDAMr, ignoreDAMr_rate, ignoreDAM, ignoreDAM_rate, mpRestore,
            hpRecover, hpRecoverProp, hpRecoverPercent, mpRecover, mpRecoverProp, RecoveryUP, BuffUP, RecoveryUP_Skill, BuffUP_Skill,
            incAllskill, combatOrders, ignoreTargetDEF, defRange, BuffUP_Summon, evaR, dodgeChance, speed, jump, harvestingTool,
            equipmentBonusExp, dropMod, cashMod, levelBonus, ASR, TER, pickRate, decreaseDebuff, equippedFairy, equippedSummon,
            percent_hp, percent_mp, percent_str, percent_dex, percent_int, percent_luk, percent_acc, percent_atk, percent_matk, percent_wdef, percent_mdef,
            pvpDamage, hpRecoverTime = 0, mpRecoverTime = 0, dot, dotTime, questBonus, pvpRank, pvpExp, wdef, mdef, trueMastery;
    private transient float localmaxbasedamage, localmaxbasepvpdamage, localmaxbasepvpdamageL;
    public transient int def, element_ice, element_fire, element_light, element_psn;
    private static final ReentrantLock sethandlingLock = new ReentrantLock();

    // TODO: all psd skills (Passive)
    public final void init(MapleCharacter chra) {
        recalcLocalStats(chra);
    }

    public final short getStr() {
        return str;
    }

    public final short getDex() {
        return dex;
    }

    public final short getLuk() {
        return luk;
    }

    public final short getInt() {
        return int_;
    }

    public final void setStr(final short str, MapleCharacter chra) {
        this.str = str;
        recalcLocalStats(chra);
    }

    public final void setDex(final short dex, MapleCharacter chra) {
        this.dex = dex;
        recalcLocalStats(chra);
    }

    public final void setLuk(final short luk, MapleCharacter chra) {
        this.luk = luk;
        recalcLocalStats(chra);
    }

    public final void setInt(final short int_, MapleCharacter chra) {
        this.int_ = int_;
        recalcLocalStats(chra);
    }

    public final boolean setHp(final int newhp, MapleCharacter chra) {
        return setHp(newhp, false, chra);
    }

    public final boolean setHp(int newhp, boolean silent, MapleCharacter chra) {
        final int oldHp = hp;
        int thp = newhp;
        if (thp < 0) {
            thp = 0;
        }
        if (thp > localmaxhp) {
            thp = localmaxhp;
        }
        this.hp = thp;

        if (chra != null) {
            if (!silent) {
                chra.checkBerserk();
                chra.updatePartyMemberHP();
            }
            if (oldHp > hp && !chra.isAlive()) {
                chra.playerDead();
            }
        }
        return hp != oldHp;
    }

    public final boolean setMp(final int newmp, final MapleCharacter chra) {
        final int oldMp = mp;
        int tmp = newmp;
        if (tmp < 0) {
            tmp = 0;
        }
        if (tmp > localmaxmp) {
            tmp = localmaxmp;
        }
        this.mp = tmp;
        return mp != oldMp;
    }

    public final void setInfo(final int maxhp, final int maxmp, final int hp, final int mp) {
        this.maxhp = maxhp;
        this.maxmp = maxmp;
        this.hp = hp;
        this.mp = mp;
    }

    public final void setMaxHp(final int hp, MapleCharacter chra) {
        this.maxhp = hp;
        recalcLocalStats(chra);
    }

    public final void setMaxMp(final int mp, MapleCharacter chra) {
        this.maxmp = mp;
        recalcLocalStats(chra);
    }

    public final int getHp() {
        return hp;
    }

    public final int getMaxHp() {
        return maxhp;
    }

    public final int getMp() {
        return mp;
    }

    public final int getMaxMp() {
        return maxmp;
    }

    public final int getTotalDex() {
        return localdex;
    }

    public final int getTotalInt() {
        return localint_;
    }

    public final int getTotalStr() {
        return localstr;
    }

    public final int getTotalLuk() {
        return localluk;
    }

    public final int getTotalMagic() {
        return magic;
    }

    public final int getSpeed() {
        return speed;
    }

    public final int getJump() {
        return jump;
    }

    public final int getTotalWatk() {
        return watk;
    }

    public final int getCurrentMaxHp() {
        return localmaxhp;
    }

    public final int getCurrentMaxMp(final int job) {
        if (GameConstants.isDemon(job)) {
            return GameConstants.getMPByJob(job);
        }
        return localmaxmp;
    }

    public final int getHands() {
        return hands;
    }

    public final float getCurrentMaxBaseDamage() {
        return localmaxbasedamage;
    }

    public final float getCurrentMaxBasePVPDamage() {
        return localmaxbasepvpdamage;
    }

    public final float getCurrentMaxBasePVPDamageL() {
        return localmaxbasepvpdamageL;
    }

    public void recalcLocalStats(MapleCharacter chra) {
        recalcLocalStats(false, chra);
    }

    public void recalcLocalStats(boolean first_login, MapleCharacter chra) {
        if (chra.isClone()) {
            return; //clones share PlayerStats objects and do not need to be recalculated
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int oldmaxhp = localmaxhp;
        int localmaxhp_ = getMaxHp();
        int localmaxmp_ = getMaxMp();
        accuracy = 0;
        wdef = 0;
        mdef = 0;
        localdex = getDex();
        localint_ = getInt();
        localstr = getStr();
        localluk = getLuk();
        speed = 100;
        jump = 100;
        pickupRange = 0.0;
        decreaseDebuff = 0;
        ASR = 0;
        TER = 0;
        dot = 0;
        questBonus = 1;
        dotTime = 0;
        trueMastery = 0;
        percent_wdef = 0;
        percent_mdef = 0;
        percent_hp = 0;
        percent_mp = 0;
        percent_str = 0;
        percent_dex = 0;
        percent_int = 0;
        percent_luk = 0;
        percent_acc = 0;
        percent_atk = 0;
        percent_matk = 0;
        passive_sharpeye_rate = 5;
        passive_sharpeye_min_percent = 20;
        passive_sharpeye_percent = 50;
        magic = 0;
        watk = 0;
        if (chra.getJob() == 500 || (chra.getJob() >= 520 && chra.getJob() <= 522)) {
            watk = 20; //bullet
        } else if (chra.getJob() == 400 || (chra.getJob() >= 410 && chra.getJob() <= 412) || (chra.getJob() >= 1400 && chra.getJob() <= 1412)) {
            watk = 30; //stars
        }
        StructItemOption soc;
        dodgeChance = 0;
        pvpDamage = 0;
        mesoGuard = 50.0;
        mesoGuardMeso = 0.0;
        dam_r = 100.0;
        bossdam_r = 100.0;
        expBuff = 100.0;
        cashBuff = 100.0;
        dropBuff = 100.0;
        mesoBuff = 100.0;
        recoverHP = 0;
        recoverMP = 0;
        mpconReduce = 0;
        mpconPercent = 100;
        incMesoProp = 0;
        reduceCooltime = 0;
        DAMreflect = 0;
        DAMreflect_rate = 0;
        ignoreDAMr = 0;
        ignoreDAMr_rate = 0;
        ignoreDAM = 0;
        ignoreDAM_rate = 0;
        ignoreTargetDEF = 0;
        hpRecover = 0;
        hpRecoverProp = 0;
        hpRecoverPercent = 0;
        mpRecover = 0;
        mpRecoverProp = 0;
        mpRestore = 0;
        pickRate = 0;
        equippedWelcomeBackRing = false;
        equippedFairy = 0;
        equippedSummon = 0;
        hasPartyBonus = false;
        hasClone = false;
        Berserk = false;
        canFish = true;
        canFishVIP = false;
        equipmentBonusExp = 0;
        RecoveryUP = 0;
        BuffUP = 0;
        RecoveryUP_Skill = 0;
        BuffUP_Skill = 0;
        BuffUP_Summon = 0;
        dropMod = 1;
        expMod = 1.0;
        cashMod = 1;
        levelBonus = 0;
        incMaxDF = 0;
        incAllskill = 0;
        combatOrders = 0;
        defRange = 0;
        durabilityHandling.clear();
        equipLevelHandling.clear();
        skillsIncrement.clear();
        damageIncrease.clear();
        try {
            sethandlingLock.lock();
            setHandling.clear();
        } finally {
            sethandlingLock.unlock();
        }
        harvestingTool = 0;
        element_fire = 100;
        element_ice = 100;
        element_light = 100;
        element_psn = 100;
        def = 100;
        for (MapleTraitType t : MapleTraitType.values()) {
            chra.getTrait(t).clearLocalExp();
        }
        final Map<Skill, SkillEntry> sData = new HashMap<>();
        final Iterator<Item> itera = chra.getInventory(MapleInventoryType.EQUIPPED).newList().iterator();
        while (itera.hasNext()) {
            final Equip equip = (Equip) itera.next();

            if (equip.getPosition() == -11) {
                if (GameConstants.isMagicWeapon(equip.getItemId())) {
                    final Map<String, Integer> eqstat = MapleItemInformationProvider.getInstance().getEquipStats(equip.getItemId());

                    if (eqstat != null) {
                        if (eqstat.containsKey("incRMAF")) {
                            element_fire = eqstat.get("incRMAF");
                        }
                        if (eqstat.containsKey("incRMAI")) {
                            element_ice = eqstat.get("incRMAI");
                        }
                        if (eqstat.containsKey("incRMAL")) {
                            element_light = eqstat.get("incRMAL");
                        }
                        if (eqstat.containsKey("incRMAS")) {
                            element_psn = eqstat.get("incRMAS");
                        }
                        if (eqstat.containsKey("elemDefault")) {
                            def = eqstat.get("elemDefault");
                        }
                    }
                }
            }
            if ((equip.getItemId() / 10000 == 166 && equip.getAndroid() != null
                    || equip.getItemId() / 10000 == 167) && chra.getAndroid() == null) {
                final Equip android = (Equip) chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -32);
                final Equip heart = (Equip) chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -33);
                if (android != null && heart != null) {
                    chra.setAndroid(equip.getAndroid());
                }
            }
            //if (equip.getItemId() / 1000 == 1099) {
            //    equippedForce += equip.getMp();
            //}
            chra.getTrait(MapleTraitType.craft).addLocalExp(equip.getHands());
            accuracy += equip.getAcc();
            localmaxhp_ += equip.getHp();
            localmaxmp_ += equip.getMp();
            localdex += equip.getDex();
            localint_ += equip.getInt();
            localstr += equip.getStr();
            localluk += equip.getLuk();
            magic += equip.getMatk();
            watk += equip.getWatk();
            wdef += equip.getWdef();
            mdef += equip.getMdef();
            speed += equip.getSpeed();
            jump += equip.getJump();
            pvpDamage += equip.getPVPDamage();
            switch (equip.getItemId()) {
                case 1112127: // Welcome Back
                    equippedWelcomeBackRing = true;
                    break;
                case 1122017: // 精靈墜飾
                    equippedFairy = 10;
                    break;
                case 1122158:
                    equippedFairy = 5;
                    break;
                case 1112585: // 天使祝福
                case 1112594: // 雪花天使祝福
                    equippedSummon = 1085;
                    break;
                case 1112586: // 黑天使祝福
                    equippedSummon = 1087;
                    break;
                case 1112663: // 白色精靈祝福
                    equippedSummon = 1179;
                    break;
                default:
                    for (int eb_bonus : GameConstants.Equipments_Bonus) {
                        if (equip.getItemId() == eb_bonus) {
                            equipmentBonusExp += GameConstants.Equipment_Bonus_EXP(eb_bonus);
                            break;
                        }
                    }
                    break;
            } //slow, poison, darkness, seal, freeze
            if (equip.getItemId() / 1000 == 1099) {
                this.incMaxDF += equip.getMp();
            }
            percent_hp += ii.getItemIncMHPr(equip.getItemId());
            percent_mp += ii.getItemIncMMPr(equip.getItemId());
            Integer set = ii.getSetItemID(equip.getItemId());
            try {
                sethandlingLock.lock();
                if (set != null && set > 0) {
                    int value = 1;
                    if (setHandling.containsKey(set)) {
                        value += setHandling.get(set);
                    }
                    setHandling.put(set, value); //id of Set, number of items to go with the set
                }
            } finally {
                sethandlingLock.unlock();
            }
            if (equip.getIncSkill() > 0 && ii.getEquipSkills(equip.getItemId()) != null) {
                for (int zzz : ii.getEquipSkills(equip.getItemId())) {
                    final Skill skil = SkillFactory.getSkill(zzz);
                    if (skil != null && skil.canBeLearnedBy(chra.getJob())) { //dont go over masterlevel :D
                        int value = 1;
                        if (skillsIncrement.get(skil.getId()) != null) {
                            value += skillsIncrement.get(skil.getId());
                        }
                        skillsIncrement.put(skil.getId(), value);
                    }
                }

            }
            EnumMap<EquipAdditions, Pair<Integer, Integer>> additions = ii.getEquipAdditions(equip.getItemId());
            if (additions != null) {
                for (Entry<EquipAdditions, Pair<Integer, Integer>> add : additions.entrySet()) {
                    switch (add.getKey()) {
                        case elemboost:
                            int value = add.getValue().right;
                            Element key = Element.getFromId(add.getValue().left);
                            if (elemBoosts.get(key) != null) {
                                value += elemBoosts.get(key);
                            }
                            elemBoosts.put(key, value);
                            break;
                        case mobcategory: //skip the category, thinkings too expensive to have yet another Map<Integer, Integer> for damage calculations
                            dam_r *= (add.getValue().right + 100.0) / 100.0;
                            bossdam_r += (add.getValue().right + 100.0) / 100.0;
                            break;
                        case critical:
                            passive_sharpeye_rate += add.getValue().left;
                            passive_sharpeye_min_percent += add.getValue().right;
                            passive_sharpeye_percent += add.getValue().right; //???CONFIRM - not sure if this is max or minCritDmg
                            break;
                        case boss:
                            bossdam_r *= (add.getValue().right + 100.0) / 100.0;
                            break;
                        case mobdie:
                            if (add.getValue().left > 0) {
                                hpRecover += add.getValue().left; //no indication of prop, so i made myself
                                hpRecoverProp += 5;
                            }
                            if (add.getValue().right > 0) {
                                mpRecover += add.getValue().right; //no indication of prop, so i made myself
                                mpRecoverProp += 5;
                            }
                            break;
                        case skill: //now, i'm a bit iffy on this one
                            if (first_login) {
                                sData.put(SkillFactory.getSkill(add.getValue().left), new SkillEntry((byte) (int) add.getValue().right, (byte) 0, -1, -1));
                            }
                            break;
                        case hpmpchange:
                            recoverHP += add.getValue().left;
                            recoverMP += add.getValue().right;
                            break;
                    }
                }
            }
            if (equip.getState() >= 17) {
                int[] potentials = {equip.getPotential1(), equip.getPotential2(), equip.getPotential3(), equip.getPotential4(), equip.getPotential5()};
                for (int i : potentials) {
                    if (i > 0) {
                        if (ii.getPotentialInfo(i) == null) {
                            continue;
                        }
                        soc = ii.getPotentialInfo(i).get(ii.getReqLevel(equip.getItemId()) / 10);
                        if (soc != null) {
                            localmaxhp_ += soc.get("incMHP");
                            localmaxmp_ += soc.get("incMMP");
                            handleItemOption(soc, chra, first_login, sData);
                        }
                    }
                }
            }
            if (equip.getSocketState() > 15) {
                int[] sockets = {equip.getSocket1(), equip.getSocket2(), equip.getSocket3()};
                for (int i : sockets) {
                    if (i > 0) {
                        soc = ii.getSocketInfo(i);
                        if (soc != null) {
                            localmaxhp_ += soc.get("incMHP");
                            localmaxmp_ += soc.get("incMMP");
                            handleItemOption(soc, chra, first_login, sData);
                        }
                    }
                }
            }

            if (equip.getDurability() > 0) {
                durabilityHandling.add((Equip) equip);
            }
            if (GameConstants.getMaxLevel(equip.getItemId()) > 0 && (GameConstants.getStatFromWeapon(equip.getItemId()) == null ? (equip.getEquipLevel() <= GameConstants.getMaxLevel(equip.getItemId())) : (equip.getEquipLevel() < GameConstants.getMaxLevel(equip.getItemId())))) {
                equipLevelHandling.add((Equip) equip);
            }
        }
        try {
            sethandlingLock.lock();
            final Iterator<Entry<Integer, Integer>> iter = setHandling.entrySet().iterator();
            while (iter.hasNext()) {
                final Entry<Integer, Integer> entry = iter.next();
                final StructSetItem set = ii.getSetItem(entry.getKey());
                if (set != null) {
                    final Map<Integer, SetItem> itemz = set.getItems();
                    for (Entry<Integer, SetItem> ent : itemz.entrySet()) {
                        if (ent.getKey() <= entry.getValue()) {
                            SetItem se = ent.getValue();
                            localstr += se.incSTR + se.incAllStat;
                            localdex += se.incDEX + se.incAllStat;
                            localint_ += se.incINT + se.incAllStat;
                            localluk += se.incLUK + se.incAllStat;
                            watk += se.incPAD;
                            magic += se.incMAD;
                            speed += se.incSpeed;
                            accuracy += se.incACC;
                            localmaxhp_ += se.incMHP;
                            localmaxmp_ += se.incMMP;
                            percent_hp += se.incMHPr;
                            percent_mp += se.incMMPr;
                            wdef += se.incPDD;
                            mdef += se.incMDD;
                            if (se.option1 > 0 && se.option1Level > 0) {
                                soc = ii.getPotentialInfo(se.option1).get(se.option1Level);
                                if (soc != null) {
                                    localmaxhp_ += soc.get("incMHP");
                                    localmaxmp_ += soc.get("incMMP");
                                    handleItemOption(soc, chra, first_login, sData);
                                }
                            }
                            if (se.option2 > 0 && se.option2Level > 0) {
                                soc = ii.getPotentialInfo(se.option2).get(se.option2Level);
                                if (soc != null) {
                                    localmaxhp_ += soc.get("incMHP");
                                    localmaxmp_ += soc.get("incMMP");
                                    handleItemOption(soc, chra, first_login, sData);
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            sethandlingLock.unlock();
        }
        handleProfessionTool(chra);
        double extraExpRate = 1.0;
        for (Item item : chra.getInventory(MapleInventoryType.CASH).newList()) {
            if (item.getItemId() / 10000 == 521) {
                double rate = ii.getExpCardRate(item.getItemId());
                if (item.getItemId() != 5210009 && rate > 1.0) {
                    if (!ii.isExpOrDropCardTime(item.getItemId()) || chra.getLevel() > ii.getExpCardMaxLevel(item.getItemId()) || (item.getExpiration() == -1L && !chra.isIntern())) {
                        if (item.getExpiration() == -1L && !chra.isIntern()) {
                            chra.dropMessage(5, ii.getName(item.getItemId()) + "屬性錯誤，經驗值加倍券無效。");
                        }
                        continue;
                    }
                    switch (item.getItemId()) {
                        case 5211000:
                        case 5211001:
                        case 5211002:
                            extraExpRate *= rate;
                            break;
                        default:
                            if (expMod < rate) {
                                expMod = rate;
                            }
                    }
                }
            } else if (dropMod == 1 && item.getItemId() / 10000 == 536) {
                if (item.getItemId() >= 5360000 && item.getItemId() < 5360100) {
                    if (!ii.isExpOrDropCardTime(item.getItemId()) || (item.getExpiration() == -1L && !chra.isIntern())) {
                        if (item.getExpiration() == -1L && !chra.isIntern()) {
                            chra.dropMessage(5, ii.getName(item.getItemId()) + "屬性錯誤，掉寶加倍券無效。");
                        }
                        continue;
                    }
                    dropMod = 2;
                }
            } else if (item.getItemId() == 5650000) {
                hasPartyBonus = true;
            } else if (item.getItemId() == 5590001) {
                levelBonus = 10;
            } else if (levelBonus == 0 && item.getItemId() == 5590000) {
                levelBonus = 5;
            } else if (item.getItemId() == 5710000) {
                questBonus = 2;
            } else if (item.getItemId() == 5340000) {
                canFish = true;
            } else if (item.getItemId() == 5340001) {
                canFish = true;
                canFishVIP = true;
            }
        }
        expMod = Math.max(extraExpRate, expMod);
        for (Item item : chra.getInventory(MapleInventoryType.ETC).list()) { //omfg;
            switch (item.getItemId()) {
                case 4030003:
                    pickupRange = Double.POSITIVE_INFINITY;
                    break;
//                case 4030004:
//                    hasClone = true;
//                    break;
//                case 4030005:
//                    cashMod = 2;
//                    break;
            }
        }
//        if (first_login && chra.getLevel() >= 30) { //yeah
//            if (chra.isGM()) { //!job lol
//                for (int i = 0; i < allJobs.length; i++) {
//                    sData.put(SkillFactory.getSkill(1085 + allJobs[i]), new SkillEntry((byte) 1, (byte) 0, -1, -1));
//                    sData.put(SkillFactory.getSkill(1087 + allJobs[i]), new SkillEntry((byte) 1, (byte) 0, -1, -1));
//                    sData.put(SkillFactory.getSkill(1179 + allJobs[i]), new SkillEntry((byte) 1, (byte) 0, -1, -1));
//                }
//            } else {
//                sData.put(SkillFactory.getSkill(getSkillByJob(1085, chra.getJob())), new SkillEntry((byte) 1, (byte) 0, -1, -1));
//                sData.put(SkillFactory.getSkill(getSkillByJob(1087, chra.getJob())), new SkillEntry((byte) 1, (byte) 0, -1, -1));
//                sData.put(SkillFactory.getSkill(getSkillByJob(1179, chra.getJob())), new SkillEntry((byte) 1, (byte) 0, -1, -1));
//            }
//        }
        if (equippedSummon > 0) {
            equippedSummon = getSkillByJob(equippedSummon, chra.getJob());
        }
        // 角色卡
        for (Pair<Integer, Integer> ix : chra.getCharacterCard().getCardEffects()) {
            final MapleStatEffect e = SkillFactory.getSkill(ix.getLeft()).getEffect(ix.getRight());
            percent_wdef += e.getPdr();
            watk += chra.getLevel();
            percent_hp += e.getPercentHP();
            percent_mp += e.getPercentMP();
            magic += chra.getLevel();
            RecoveryUP += e.getMPConsumeEff();
            percent_acc += e.getPercentAcc();
            passive_sharpeye_rate += e.getCr();
            jump += e.getPassiveJump();
            speed += e.getPassiveSpeed();
            dodgeChance += e.getPercentAvoid();
            BuffUP_Summon += e.getSummonTimeInc();
            ASR += e.getASRRate();
            BuffUP_Skill += e.getBuffTimeRate();
            incMesoProp += e.getMesoRate();
            passive_sharpeye_percent += e.getCriticalMax();
            ignoreTargetDEF += e.getIgnoreMob();
            localstr += e.getStrX();
            localdex += e.getDexX();
            localint_ += e.getIntX();
            localluk += e.getLukX();
            localmaxhp_ += e.getMaxHpX();
            localmaxmp_ += e.getMaxMpX();
            watk += e.getAttackX();
            magic += e.getMagicX();
            bossdam_r += e.getBossDamage();
        }
        // 角色內在能力
        for (InnerSkillValueHolder innerSkill : chra.getInnerSkills()) {
            MapleStatEffect innerEffect = SkillFactory.getSkill(innerSkill.getSkillId()).getEffect(innerSkill.getSkillLevel());
            if (innerEffect == null) {
                continue;
            }
            accuracy += innerEffect.getAccX(); // accX
            evaR += innerEffect.getPercentAvoid(); // evaR
            wdef += innerEffect.getWdefX(); // pddX
            mdef += innerEffect.getMdefX(); // mddX
            localstr += innerEffect.getStrFX(); // strFX
            localdex += innerEffect.getDexFX(); // dexFX
            localint_ += innerEffect.getIntFX(); // intFX
            localluk += innerEffect.getLukFX(); // lukFX
            percent_wdef += innerEffect.getWDEFRate(); // pddR
            percent_mdef += innerEffect.getMDEFRate(); // mddR
            percent_hp += innerEffect.getPercentHP(); // mhpR
            percent_mp += innerEffect.getPercentMP(); // mmpR
            dodgeChance += innerEffect.getPercentAvoid(); // evaR
            passive_sharpeye_rate = (short) (this.passive_sharpeye_rate + innerEffect.getCr()); // cr
            jump += innerEffect.getPassiveJump(); // psdJump
            speed += innerEffect.getPassiveSpeed(); // psdSpeed
            localmaxhp_ += innerEffect.getMaxHpX(); // mhpX
            localmaxmp_ += innerEffect.getMaxMpX(); // mmpX
            watk += innerEffect.getAttackX(); // padX
            magic += innerEffect.getMagicX(); // madX
            BuffUP_Skill += innerEffect.getBuffTimeRate(); // bufftimeR
            incMesoProp += innerEffect.getMesoRate(); // mesoR
            if (innerEffect.getLevelToWatk() > 0) { // lv2pad
                watk = (int) (this.watk + Math.floor(chra.getLevel() / innerEffect.getLevelToWatk()));
            }
            if (innerEffect.getLevelToMatk() > 0) { // lv2mad
                magic = (int) (this.magic + Math.floor(chra.getLevel() / innerEffect.getLevelToMatk()));
            }
            bossdam_r += innerEffect.getBossDamage(); // bdR
//            addTargetPlus(0, InnerEffect.getTargetPlus());
//            passivePlus += InnerEffect.getPassivePlus();
        }
        //dam_r += (chra.getJob() >= 430 && chra.getJob() <= 434 ? 70 : 0); //leniency on upper stab
        this.localstr += Math.floor((localstr * percent_str) / 100.0f);
        this.localdex += Math.floor((localdex * percent_dex) / 100.0f);
        this.localint_ += Math.floor((localint_ * percent_int) / 100.0f);
        this.localluk += Math.floor((localluk * percent_luk) / 100.0f);

        if (localint_ > localdex) {
            accuracy += localint_ + Math.floor(localluk * 1.2);
        } else {
            accuracy += localluk + Math.floor(localdex * 1.2);
        }
        this.wdef += Math.floor((localstr * 1.2) + ((localdex + localluk) * 0.5) + (localint_ * 0.4));
        this.mdef += Math.floor((localstr * 0.4) + ((localdex + localluk) * 0.5) + (localint_ * 1.2));
        this.accuracy += Math.floor((accuracy * percent_acc) / 100.0f);
        Skill bx;
        int bof;

        handleBuffStats(chra);

        Integer buff = chra.getBuffedValue(MapleBuffStat.ENHANCED_MAXHP);
        if (buff != null) {
            localmaxhp_ += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.ENHANCED_MAXMP);
        if (buff != null) {
            localmaxmp_ += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.HP_BOOST);
        if (buff != null) {
            localmaxhp_ += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MP_BOOST);
        if (buff != null) {
            localmaxmp_ += buff;
        }
        handlePassiveSkills(chra);
        MapleStatEffect eff;
        // 金剛不壞
        bx = SkillFactory.getSkill(5710004);
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            localmaxhp_ += eff.getMaxHpX();
            localmaxmp_ += eff.getMaxMpX();
        }

        if (chra.getGuildId() > 0) {
            final MapleGuild g = World.Guild.getGuild(chra.getGuildId());
            if (g != null && g.getSkills().size() > 0) {
                final long now = System.currentTimeMillis();
                for (MapleGuildSkill gs : g.getSkills()) {
                    if (gs.timestamp > now && gs.activator.length() > 0) {
                        final MapleStatEffect e = SkillFactory.getSkill(gs.skillID).getEffect(gs.level);
                        passive_sharpeye_rate += e.getCr();
                        watk += e.getAttackX();
                        magic += e.getMagicX();
                        expBuff *= (e.getEXPRate() + 100.0) / 100.0;
                        dodgeChance += e.getER();
                        percent_wdef += e.getWDEFRate();
                        percent_mdef += e.getMDEFRate();
                    }
                }
            }
        }

        localmaxhp_ += Math.floor((percent_hp * localmaxhp_) / 100.0f);
        localmaxmp_ += Math.floor((percent_mp * localmaxmp_) / 100.0f);
        wdef += Math.min(30000, Math.floor((wdef * percent_wdef) / 100.0f));
        mdef += Math.min(30000, Math.floor((wdef * percent_mdef) / 100.0f));
        //magic = Math.min(magic, 1999); //buffs can make it higher

        hands = this.localdex + this.localint_ + this.localluk;
        calculateFame(chra);
        ignoreTargetDEF += chra.getTrait(MapleTraitType.charisma).getLevel() / 10;
        pvpDamage += chra.getTrait(MapleTraitType.charisma).getLevel() / 10;

        localmaxmp_ += chra.getTrait(MapleTraitType.sense).getLevel() * 20;

        localmaxhp_ += chra.getTrait(MapleTraitType.will).getLevel() * 20;
        ASR += chra.getTrait(MapleTraitType.will).getLevel() / 5;

        accuracy += chra.getTrait(MapleTraitType.insight).getLevel() * 15 / 10;

        localmaxhp = Math.min(99999, Math.abs(Math.max(-99999, localmaxhp_)));
        localmaxmp = Math.min(99999, Math.abs(Math.max(-99999, localmaxmp_)));

        if (chra.getEventInstance() != null && chra.getEventInstance().getName().startsWith("PVP")) { //hack
//            MapleStatEffect eff;
//            localmaxhp = Math.min(40000, localmaxhp * 3); //approximate.
//            localmaxmp = Math.min(20000, localmaxmp * 2);
            //not sure on 20000 cap
            for (int i : pvpSkills) {
                Skill skil = SkillFactory.getSkill(i);
                if (skil != null && skil.canBeLearnedBy(chra.getJob())) {
                    sData.put(skil, new SkillEntry((byte) 1, (byte) 0, -1, -1));
                    eff = skil.getEffect(1);
                    switch ((i / 1000000) % 10) {
                        case 1:
                            if (eff.getX() > 0) {
                                pvpDamage += (wdef / eff.getX());
                            }
                            break;
                        case 3:
                            hpRecoverProp += eff.getProb();
                            hpRecover += eff.getX();
                            mpRecoverProp += eff.getProb();
                            mpRecover += eff.getX();
                            break;
                        case 5:
                            passive_sharpeye_rate += eff.getProb();
                            passive_sharpeye_percent = 100;
                            break;
                    }
                    break;
                }
            }
//            eff = chra.getStatForBuff(MapleBuffStat.MORPH);
//            if (eff != null && eff.getSourceId() % 10000 == 1105) { //ice knight
//                localmaxhp = 99999;
//                localmaxmp = 99999;
//            }
        }
        chra.changeSkillLevel_Skip(sData, false);
        if (GameConstants.isDemon(chra.getJob())) {
            localmaxmp = GameConstants.getMPByJob(chra.getJob());
        }

        CalcPassive_SharpEye(chra);
        CalcPassive_Mastery(chra);
        recalcPVPRank(chra);
        if (first_login) {
            chra.silentEnforceMaxHpMp();
            relocHeal(chra);
        } else {
            chra.enforceMaxHpMp();
        }

        calculateMaxBaseDamage(Math.max(magic, watk), pvpDamage, chra);
        trueMastery = Math.min(100, trueMastery);
        passive_sharpeye_min_percent = (short) Math.min(passive_sharpeye_min_percent, passive_sharpeye_percent);
        if (oldmaxhp != 0 && oldmaxhp != localmaxhp) {
            chra.updatePartyMemberHP();
        }
    }

    public boolean checkEquipLevels(final MapleCharacter chr, int gain) {
        if (chr.isClone()) {
            return false;
        }
        boolean changed = false;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        List<Equip> all = new ArrayList<>(equipLevelHandling);
        for (Equip eq : all) {
            int lvlz = eq.getEquipLevel();
            eq.setItemEXP(eq.getItemEXP() + gain);

            if (eq.getEquipLevel() > lvlz) { //lvlup
                for (int i = eq.getEquipLevel() - lvlz; i > 0; i--) {
                    //now for the equipment increments...
                    final Map<Integer, Map<String, Integer>> inc = ii.getEquipIncrements(eq.getItemId());
                    if (inc != null && inc.containsKey(lvlz + i)) { //flair = 1
                        eq = ii.levelUpEquip(eq, inc.get(lvlz + i));
                    }
                    //UGH, skillz
                    if (GameConstants.getStatFromWeapon(eq.getItemId()) == null && GameConstants.getMaxLevel(eq.getItemId()) < (lvlz + i) && Math.random() < 0.1 && eq.getIncSkill() <= 0 && ii.getEquipSkills(eq.getItemId()) != null) {
                        for (int zzz : ii.getEquipSkills(eq.getItemId())) {
                            final Skill skil = SkillFactory.getSkill(zzz);
                            if (skil != null && skil.canBeLearnedBy(chr.getJob())) { //dont go over masterlevel :D
                                eq.setIncSkill(skil.getId());
                                chr.dropMessage(5, "Your skill has gained a levelup: " + skil.getName() + " +1");
                            }
                        }
                    }
                }
                changed = true;
            }
            chr.forceReAddItem(eq.copy(), MapleInventoryType.EQUIPPED);
        }
        if (changed) {
            chr.equipChanged();
            chr.getClient().sendPacket(EffectPacket.showItemLevelupEffect());
            chr.getMap().broadcastMessage(chr, EffectPacket.showForeignItemLevelupEffect(chr.getId()), false);
        }
        return changed;
    }

    public boolean checkEquipDurabilitys(final MapleCharacter chr, int gain) {
        return checkEquipDurabilitys(chr, gain, false);
    }

    public boolean checkEquipDurabilitys(final MapleCharacter chr, int gain, boolean aboveZero) {
        if (chr.isClone() || chr.inPVP()) {
            return true;
        }
        List<Equip> all = new ArrayList<>(durabilityHandling);
        for (Equip item : all) {
            if (item != null && ((item.getPosition() >= 0) == aboveZero)) {
                item.setDurability(item.getDurability() + gain);
                if (item.getDurability() < 0) { //shouldnt be less than 0
                    item.setDurability(0);
                }
            }
        }
        for (Equip eqq : all) {
            if (eqq != null && eqq.getDurability() == 0 && eqq.getPosition() < 0) { //> 0 went to negative
                if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                    chr.getClient().sendPacket(InventoryPacket.getInventoryFull());
                    chr.getClient().sendPacket(InventoryPacket.getShowInventoryFull());
                    return false;
                }
                durabilityHandling.remove(eqq);
                final short pos = chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot();
                MapleInventoryManipulator.unequip(chr.getClient(), eqq.getPosition(), pos);
            } else if (eqq != null) {
                chr.forceReAddItem(eqq.copy(), MapleInventoryType.EQUIPPED);
            }
        }
        return true;
    }

    public final void handleProfessionTool(final MapleCharacter chra) {
        if (chra.getProfessionLevel(92000000) > 0 || chra.getProfessionLevel(92010000) > 0) {
            final Iterator<Item> itera = chra.getInventory(MapleInventoryType.EQUIP).newList().iterator();
            while (itera.hasNext()) { //goes to first harvesting tool and stops
                final Equip equip = (Equip) itera.next();
                if (equip.getDurability() != 0 && (equip.getItemId() / 10000 == 150 && chra.getProfessionLevel(92000000) > 0) || (equip.getItemId() / 10000 == 151 && chra.getProfessionLevel(92010000) > 0)) {
                    if (equip.getDurability() > 0) {
                        durabilityHandling.add(equip);
                    }
                    harvestingTool = equip.getPosition();
                    break;
                }
            }
        }
    }

    private void CalcPassive_Mastery(final MapleCharacter player) {
        if (player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11) == null) {
            passive_mastery = 0;
            return;
        }
        final int skil;
        final MapleWeaponType weaponType = GameConstants.getWeaponType(player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11).getItemId());
        boolean acc = true;
        switch (weaponType) {
            case 弓:
                skil = GameConstants.isKOC(player.getJob()) ? 13100000 : 3100000;
                break;
            case 拳套:
                skil = MapleJob.is暗夜行者(player.getJob()) ? 14100000 : 4100000;
                break;
            case 手杖:
                skil = player.getTotalSkillLevel(24120006) > 0 ? 24120006 : 24100004;
                break;
            case 火砲:
                skil = 5300005;
                break;
            case 雙刀:
            case 短劍:
                skil = player.getJob() >= 430 && player.getJob() <= 434 ? 4300000 : 4200000;
                break;
            case 弩:
                skil = GameConstants.isResist(player.getJob()) ? 33100000 : 3200000;
                break;
            case 單手斧:
            case 單手棍:
                skil = GameConstants.isResist(player.getJob()) ? 31100004 : (GameConstants.isKOC(player.getJob()) ? 11100000 : (player.getJob() > 112 ? 1200000 : 1100000)); //hero/pally
                break;
            case 雙手斧:
            case 單手劍:
            case 雙手劍:
            case 雙手棍:
                skil = GameConstants.isKOC(player.getJob()) ? 11100000 : (player.getJob() > 112 ? 1200000 : 1100000); //hero/pally
                break;
            case 矛:
                skil = GameConstants.isAran(player.getJob()) ? 21100000 : 1300000;
                break;
            case 槍:
                skil = 1300000;
                break;
            case 指虎:
                skil = GameConstants.isKOC(player.getJob()) ? 15100001 : 5100001;
                break;
            case 火槍:
                skil = MapleJob.is蒼龍俠客(player.getJob()) ? 5700000 : MapleJob.is機甲戰神(player.getJob()) ? 35100000 : 5900000;
                break;
            case 雙弩:
                skil = 23100005;
                break;
            case 短杖:
            case 長杖:
                acc = false;
                skil = GameConstants.isResist(player.getJob()) ? 32100006 : (player.getJob() <= 212 ? 2100006 : (player.getJob() <= 222 ? 2200006 : (player.getJob() <= 232 ? 2300006 : (player.getJob() <= 2000 ? 12100007 : 22120002))));
                break;
            default:
                passive_mastery = 0;
                return;

        }
        if (player.getSkillLevel(skil) <= 0) {
            passive_mastery = 0;
            return;
        }
        final MapleStatEffect eff = SkillFactory.getSkill(skil).getEffect(player.getTotalSkillLevel(skil));
        if (acc) {
            accuracy += eff.getX();
            if (skil == 35100000) {
                watk += eff.getX();
            }
        } else {
            magic += eff.getX();
        }
        passive_sharpeye_rate += eff.getCr();
        passive_mastery = (byte) eff.getMastery(); //after bb, simpler?
        trueMastery += eff.getMastery() + weaponType.getBaseMastery();
    }

    private void calculateFame(final MapleCharacter player) {
        player.getTrait(MapleTraitType.charm).addLocalExp(player.getFame());
        for (MapleTraitType t : MapleTraitType.values()) {
            player.getTrait(t).recalcLevel();
        }
    }

    private void CalcPassive_SharpEye(final MapleCharacter player) {
        Skill critSkill;
        int critlevel;
        if (GameConstants.isResist(player.getJob())) {
            critSkill = SkillFactory.getSkill(30000022);
            critlevel = player.getTotalSkillLevel(critSkill);
            if (critlevel > 0) {
                passive_sharpeye_rate += critSkill.getEffect(critlevel).getProb();
                this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
            }
        }
        switch (player.getJob()) { // Apply passive Critical bonus
            case 211: // 魔導士(火、毒)
            case 212: { // 大魔導士(火、毒)
                // 魔法暴擊
                critSkill = SkillFactory.getSkill(2110009);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCr());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 221: // 魔導士(冰、雷)
            case 222: { // 大魔導士(冰、雷)
                // 魔法暴擊
                critSkill = SkillFactory.getSkill(2210009);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCr());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 231: // 祭司
            case 232: { // 主教
                // 魔法暴擊
                critSkill = SkillFactory.getSkill(2310010);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCr());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 410:
            case 411:
            case 412: { // Assasin/ Hermit / NL
                critSkill = SkillFactory.getSkill(4100001);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getProb());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 1410: // 暗夜行者(2轉)
            case 1411: // 暗夜行者(3轉)
            case 1412: { // 暗夜行者(4轉)
                critSkill = SkillFactory.getSkill(14100001);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getProb());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 3100:
            case 3110:
            case 3111:
            case 3112: {
                // 憤怒
                critSkill = SkillFactory.getSkill(31100006); //TODO LEGEND, not final
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCr());
                    this.watk += critSkill.getEffect(critlevel).getAttackX();
                }
                break;
            }
            case 2300:
            case 2310:
            case 2311:
            case 2312: {
                critSkill = SkillFactory.getSkill(23000003);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCr());
                }
                break;
            }
            case 434: {
                critSkill = SkillFactory.getSkill(4340010);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getProb());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                    return;
                }
                break;
            }
            case 1211: // 烈焰巫師(3轉)
            case 1212: { // 烈焰巫師(4轉)
                critSkill = SkillFactory.getSkill(12110000);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCr());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 530:
            case 531:
            case 532: {
                critSkill = SkillFactory.getSkill(5300004);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCr());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                    return;
                }
                break;
            }
            case 500: // 海盜
            case 510: // 打手
            case 511: // 格鬥家
            case 512: { // 拳霸
//                // 致命暗襲
//                critSkill = SkillFactory.getSkill(5110000);
//                critlevel = player.getTotalSkillLevel(critSkill);
//                if (critlevel > 0) {
//                    this.passive_sharpeye_rate += (short) critSkill.getEffect(critlevel).getProb();
//                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
//                }
                // 初階爆擊
                critSkill = SkillFactory.getSkill(5000007);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) critSkill.getEffect(critlevel).getCr();
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                // 爆擊鬥氣
                critSkill = SkillFactory.getSkill(5110011);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) critSkill.getEffect(critlevel).getCr();
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                return;
            }
            case 520: // 槍手
            case 521: // 神槍手
            case 522: { // 槍神
                // 初階爆擊
                critSkill = SkillFactory.getSkill(5000007);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) critSkill.getEffect(critlevel).getCr();
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                // 金屬外殼
                critSkill = SkillFactory.getSkill(5210013);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) critSkill.getEffect(critlevel).getCr();
                }
                break;
            }
            case 508:
            case 570:
            case 571:
            case 572: {
                // 猛虎之力
                critSkill = SkillFactory.getSkill(5080004);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) critSkill.getEffect(critlevel).getCr();
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                // 無堅不摧
                critSkill = SkillFactory.getSkill(5710005);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) critSkill.getEffect(critlevel).getCr();
                }
                // 蒼龍之力
                critSkill = SkillFactory.getSkill(5720008);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel <= 0) {
                    break;
                }
                this.passive_sharpeye_rate += (short) critSkill.getEffect(critlevel).getCr();
                this.passive_sharpeye_percent += critSkill.getEffect(critlevel).getCriticalMin();
                this.bossdam_r += critSkill.getEffect(critlevel).getProb();
                break;
            }
            case 1500: // 閃雷悍將(1轉)
            case 1510: // 閃雷悍將(2轉)
            case 1511: // 閃雷悍將(3轉)
            case 1512: { // 閃雷悍將(4轉)
                // 初階爆擊
                critSkill = SkillFactory.getSkill(15000006);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) critSkill.getEffect(critlevel).getCr();
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                // 衝壓暴擊
                critSkill = SkillFactory.getSkill(15110000);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getProb());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                    return;
                }
                // 爆擊鬥氣
                critSkill = SkillFactory.getSkill(15110009);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) critSkill.getEffect(critlevel).getCr();
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 2111:
            case 2112: {
                critSkill = SkillFactory.getSkill(21110000);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) ((critSkill.getEffect(critlevel).getX() * critSkill.getEffect(critlevel).getY()) + critSkill.getEffect(critlevel).getCr());
                }
                break;
            }
            case 300:
            case 310:
            case 311:
            case 312:
            case 320:
            case 321:
            case 322: { // Bowman
                critSkill = SkillFactory.getSkill(3000001);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getProb());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                    return;
                }
                break;
            }
            case 1300:
            case 1310:
            case 1311:
            case 1312: { // Bowman
                critSkill = SkillFactory.getSkill(13000000);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getProb());
                }
                break;
            }
            case 2003: // 幻影俠盜(0轉)
            case 2400: // 幻影俠盜(1轉)
            case 2410: // 幻影俠盜(2轉)
            case 2411: // 幻影俠盜(3轉)
            case 2412: // 幻影俠盜(4轉)
                // 致命本能
                critSkill = SkillFactory.getSkill(20030204);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCr());
                }
                // 爆擊天賦
                critSkill = SkillFactory.getSkill(24110007);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCr());
                }
                // 進階手杖精通
                critSkill = SkillFactory.getSkill(24120006);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            case 2214: // 龍魔導士(6轉)
            case 2215: // 龍魔導士(7轉)
            case 2216: // 龍魔導士(8轉)
            case 2217: // 龍魔導士(9轉)
            case 2218: { // 龍魔導士(10轉)
                // 魔力爆擊
                critSkill = SkillFactory.getSkill(22140000);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getProb());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
        }
    }

    public final short passive_sharpeye_min_percent() {
        return passive_sharpeye_min_percent;
    }

    public final short passive_sharpeye_percent() {
        return passive_sharpeye_percent;
    }

    public final short passive_sharpeye_rate() {
        return passive_sharpeye_rate;
    }

    public final byte passive_mastery() {
        return passive_mastery; //* 5 + 10 for mastery %
    }

    public final void calculateMaxBaseDamage(final int watk, final int pvpDamage, MapleCharacter chra) {
        if (watk <= 0) {
            localmaxbasedamage = 1;
            localmaxbasepvpdamage = 1;
        } else {
            final Item weapon_item = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
            final Item weapon_item2 = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10);
            final int job = chra.getJob();
            final MapleWeaponType weapon = weapon_item == null ? MapleWeaponType.沒有武器 : GameConstants.getWeaponType(weapon_item.getItemId());
            final MapleWeaponType weapon2 = weapon_item2 == null ? MapleWeaponType.沒有武器 : GameConstants.getWeaponType(weapon_item2.getItemId());
            int mainstat, secondarystat, mainstatpvp, secondarystatpvp;
            final boolean mage = (job >= 200 && job <= 232) || (job >= 1200 && job <= 1212) || (job >= 2200 && job <= 2218) || (job >= 3200 && job <= 3212);
            switch (weapon) {
                case 弓:
                case 雙弩:
                case 火槍:
                    mainstat = localdex;
                    secondarystat = localstr;
                    mainstatpvp = dex;
                    secondarystatpvp = str;
                    break;
                case 短劍:
                case 雙刀:
                case 拳套:
                case 手杖:
                    mainstat = localluk;
                    secondarystat = localdex + localstr;
                    mainstatpvp = luk;
                    secondarystatpvp = dex + str;
                    break;
                default:
                    if (mage) {
                        mainstat = localint_;
                        secondarystat = localluk;
                        mainstatpvp = int_;
                        secondarystatpvp = luk;
                    } else {
                        mainstat = localstr;
                        secondarystat = localdex;
                        mainstatpvp = str;
                        secondarystatpvp = dex;
                    }
                    break;
            }
            localmaxbasepvpdamage = weapon.getMaxDamageMultiplier() * (4 * mainstatpvp + secondarystatpvp) * (100.0f + (pvpDamage / 100.0f));
            localmaxbasepvpdamageL = weapon.getMaxDamageMultiplier() * (4 * mainstat + secondarystat) * (100.0f + (pvpDamage / 100.0f));
            if (weapon2 != MapleWeaponType.沒有武器 && weapon_item != null && weapon_item2 != null) {
                Equip we1 = (Equip) weapon_item;
                Equip we2 = (Equip) weapon_item2;
                localmaxbasedamage = weapon.getMaxDamageMultiplier() * (4 * mainstat + secondarystat) * ((watk - (mage ? we2.getMatk() : we2.getWatk())) / 100.0f);
                localmaxbasedamage += weapon2.getMaxDamageMultiplier() * (4 * mainstat + secondarystat) * ((watk - (mage ? we1.getMatk() : we1.getWatk())) / 100.0f);
            } else {
                localmaxbasedamage = weapon.getMaxDamageMultiplier() * (4 * mainstat + secondarystat) * (watk / 100.0f);
            }
        }
    }

    public final float getHealHP() {
        return shouldHealHP;
    }

    public final float getHealMP() {
        return shouldHealMP;
    }

    public final void relocHeal(MapleCharacter chra) {
        if (chra.isClone()) {
            return;
        }
        final int playerjob = chra.getJob();

        shouldHealHP = 10 + recoverHP; // Reset
        shouldHealMP = GameConstants.isDemon(chra.getJob()) ? 0 : (3 + mpRestore + recoverMP + (localint_ / 10)); // i think
        mpRecoverTime = 0;
        hpRecoverTime = 0;
        if (playerjob == 111 || playerjob == 112) {
            final Skill effect = SkillFactory.getSkill(1110000); // Improving MP Recovery
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                MapleStatEffect eff = effect.getEffect(lvl);
                if (eff.getHp() > 0) {
                    shouldHealHP += eff.getHp();
                    hpRecoverTime = 4000;
                }
                shouldHealMP += eff.getMp();
                mpRecoverTime = 4000;
            }
        } else if (playerjob == 510 || playerjob == 511 || playerjob == 512) {
            final Skill effect =  SkillFactory.getSkill(5100013);
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                shouldHealHP += (effect.getEffect(lvl).getX() * localmaxhp) / 100.0f;
                hpRecoverTime = effect.getEffect(lvl).getY();
                shouldHealMP += (effect.getEffect(lvl).getX() * localmaxmp) / 100.0f;
                mpRecoverTime = effect.getEffect(lvl).getY();
            }
        } else if (playerjob == 1111 || playerjob == 1112) {
            final Skill effect = SkillFactory.getSkill(11110000); // 魔力恢復
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                shouldHealMP += effect.getEffect(lvl).getMp();
                mpRecoverTime = 4000;
            }
        } else if (GameConstants.isMercedes(playerjob)) {
            final Skill effect = SkillFactory.getSkill(20020109); // Improving MP Recovery
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                shouldHealHP += (effect.getEffect(lvl).getX() * localmaxhp) / 100;
                hpRecoverTime = 4000;
                shouldHealMP += (effect.getEffect(lvl).getX() * localmaxmp) / 100;
                mpRecoverTime = 4000;
            }
        } else if (playerjob == 3111 || playerjob == 3112) {
            final Skill effect = SkillFactory.getSkill(31110009); // Improving MP Recovery
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                shouldHealMP += effect.getEffect(lvl).getY();
                mpRecoverTime = 4000;
            }
        } else if (playerjob == 5111 || playerjob == 5112) {
            final Skill effect = SkillFactory.getSkill(51110000); // 魔力恢復
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                MapleStatEffect eff = effect.getEffect(lvl);
                if (eff.getHp() > 0) {
                    shouldHealHP += eff.getHp();
                    hpRecoverTime = 4000;
                }
                shouldHealMP += eff.getMp();
                mpRecoverTime = 4000;
            }
        }
        if (chra.getChair() != 0) { // Is sitting on a chair.
            shouldHealHP += 99; // Until the values of Chair heal has been fixed,
            shouldHealMP += 99; // MP is different here, if chair data MP = 0, heal + 1.5
        } else if (chra.getMap() != null) { // Because Heal isn't multipled when there's a chair :)
            final float recvRate = chra.getMap().getRecoveryRate();
            if (recvRate > 0) {
                shouldHealHP *= recvRate;
                shouldHealMP *= recvRate;
            }
        }
    }

    public final void connectData(final MaplePacketLittleEndianWriter mplew) {
        mplew.writeShort(str); // str
        mplew.writeShort(dex); // dex
        mplew.writeShort(int_); // int
        mplew.writeShort(luk); // luk

        mplew.writeInt(hp); // hp -- INT after bigbang
        mplew.writeInt(maxhp); // maxhp
        mplew.writeInt(mp); // mp
        mplew.writeInt(maxmp); // maxmp
    }

    private final static int[] allJobs = {0, 10000, 10000000, 20000000, 20010000, 20020000, 30000000, 30010000};
    public final static int[] pvpSkills = {1000007, 2000007, 3000006, 4000010, 5000006, 5010004, 11000006, 12000006, 13000005, 14000006, 15000005, 21000005, 22000002, 23000004, 31000005, 32000012, 33000004, 35000005};

    public static int getSkillByJob(final int skillID, final int job) {
        return skillID + (MapleJob.getBeginner((short) job) * 10000);
    }

    public final int getSkillIncrement(final int skillID) {
        if (skillsIncrement.containsKey(skillID)) {
            return skillsIncrement.get(skillID);
        }
        return 0;
    }

    public final int getElementBoost(final Element key) {
        if (elemBoosts.containsKey(key)) {
            return elemBoosts.get(key);
        }
        return 0;
    }

    public final int getDamageIncrease(final int key) {
        if (damageIncrease.containsKey(key)) {
            return damageIncrease.get(key);
        }
        return 0;
    }

    public final int getAccuracy() {
        return accuracy;
    }

    public void heal_noUpdate(MapleCharacter chra) {
        setHp(getCurrentMaxHp(), chra);
        setMp(getCurrentMaxMp(chra.getJob()), chra);
    }

    public void heal(MapleCharacter chra) {
        heal_noUpdate(chra);
        chra.updateSingleStat(MapleStat.HP, getCurrentMaxHp());
        chra.updateSingleStat(MapleStat.MP, getCurrentMaxMp(chra.getJob()));
    }

    public void handleItemOption(StructItemOption soc, MapleCharacter chra, boolean first_login, Map<Skill, SkillEntry> hmm) {
        localstr += soc.get("incSTR");
        localdex += soc.get("incDEX");
        localint_ += soc.get("incINT");
        localluk += soc.get("incLUK");
        accuracy += soc.get("incACC");
        // incEVA -> increase dodge
        speed += soc.get("incSpeed");
        jump += soc.get("incJump");
        watk += soc.get("incPAD");
        magic += soc.get("incMAD");
        wdef += soc.get("incPDD");
        mdef += soc.get("incMDD");
        percent_str += soc.get("incSTRr");
        percent_dex += soc.get("incDEXr");
        percent_int += soc.get("incINTr");
        percent_luk += soc.get("incLUKr");
        percent_hp += soc.get("incMHPr");
        percent_mp += soc.get("incMMPr");
        percent_acc += soc.get("incACCr");
        dodgeChance += soc.get("incEVAr");
        percent_atk += soc.get("incPADr");
        percent_matk += soc.get("incMADr");
        percent_wdef += soc.get("incPDDr");
        percent_mdef += soc.get("incMDDr");
        passive_sharpeye_rate += soc.get("incCr");
        bossdam_r *= (soc.get("incDAMr") + 100.0) / 100.0;
        if (soc.get("boss") <= 0) {
            dam_r *= (soc.get("incDAMr") + 100.0) / 100.0;
        }
        recoverHP += soc.get("RecoveryHP"); // This shouldn't be here, set 4 seconds.
        recoverMP += soc.get("RecoveryMP"); // This shouldn't be here, set 4 seconds.
        //if (soc.get("HP") > 0) { // Should be heal upon attacking
        //	hpRecover += soc.get("HP");
        //	hpRecoverProp += soc.get("prop");
        //}
        //if (soc.get("MP") > 0 && !GameConstants.isDemon(chra.getJob())) {
        //	mpRecover += soc.get("MP");
        //	mpRecoverProp += soc.get("prop");
        //}
        ignoreTargetDEF += soc.get("ignoreTargetDEF");
        if (soc.get("ignoreDAM") > 0) {
            ignoreDAM += soc.get("ignoreDAM");
            ignoreDAM_rate += soc.get("prop");
        }
        incAllskill += soc.get("incAllskill");
        if (soc.get("ignoreDAMr") > 0) {
            ignoreDAMr += soc.get("ignoreDAMr");
            ignoreDAMr_rate += soc.get("prop");
        }
        RecoveryUP += soc.get("RecoveryUP"); // only for hp items and skills
        passive_sharpeye_min_percent += soc.get("incCriticaldamageMin");
        passive_sharpeye_percent += soc.get("incCriticaldamageMax");
        TER += soc.get("incTerR"); // elemental resistance = avoid element damage from monster
        ASR += soc.get("incAsrR"); // abnormal status = disease
        if (soc.get("DAMreflect") > 0) {
            DAMreflect += soc.get("DAMreflect");
            DAMreflect_rate += soc.get("prop");
        }
        mpconReduce += soc.get("mpconReduce");
        reduceCooltime += soc.get("reduceCooltime"); // in seconds
        incMesoProp += soc.get("incMesoProp"); // mesos + %
        dropBuff *= (100 + soc.get("incRewardProp")) / 100.0; // extra drop rate for item
        if (first_login && soc.get("skillID") > 0) {
            hmm.put(SkillFactory.getSkill(getSkillByJob(soc.get("skillID"), chra.getJob())), new SkillEntry((byte) 1, (byte) 0, -1, -1));
        }
        // TODO: Auto Steal potentials (modify handleSteal), potentials with invincible stuffs, abnormal status duration decrease,
        // poison, stun, etc (uses level field -> cast disease to mob/player), face?
    }

    public void recalcPVPRank(MapleCharacter chra) {
        this.pvpRank = 10;
        this.pvpExp = chra.getTotalBattleExp();
        for (int i = 0; i < 10; i++) {
            if (pvpExp > GameConstants.getPVPExpNeededForLevel(i + 1)) {
                pvpRank--;
                pvpExp -= GameConstants.getPVPExpNeededForLevel(i + 1);
            }
        }
    }

    public int getHPPercent() {
        return (int) Math.ceil((hp * 100.0) / localmaxhp);
    }

    private void handlePassiveSkills(MapleCharacter chra) {
        Skill bx;
        int bof;
        MapleStatEffect eff;
        if (GameConstants.isKOC(chra.getJob())) {
            bx = SkillFactory.getSkill(10000074);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getX();
                percent_mp += eff.getX();
            }
        }
        switch (chra.getJob()) {
            case 200:
            case 210:
            case 211:
            case 212:
            case 220:
            case 221:
            case 222:
            case 230:
            case 231:
            case 232: {
                // MP 增加
                bx = SkillFactory.getSkill(2000006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_mp += bx.getEffect(bof).getPercentMP();
                }
                break;
            }
            case 1200: // 烈焰巫師(1轉)
            case 1210: // 烈焰巫師(2轉)
            case 1211: // 烈焰巫師(3轉)
            case 1212: { // 烈焰巫師(4轉)
                // MP 增加
                bx = SkillFactory.getSkill(12000005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_mp += bx.getEffect(bof).getPercentMP();
                }
                // 智慧昇華
                bx = SkillFactory.getSkill(12100008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                break;
            }
            case 1100: // 聖魂劍士(1轉)
            case 1110: // 聖魂劍士(2轉)
            case 1111: // 聖魂劍士(3轉)
            case 1112: { // 聖魂劍士(4轉)
                // HP增加
                bx = SkillFactory.getSkill(11000005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                // 體能訓練
                bx = SkillFactory.getSkill(11100007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                break;
            }
            case 2003: // 幻影俠盜(0轉)
            case 2400: // 幻影俠盜(1轉)
            case 2410: // 幻影俠盜(2轉)
            case 2411: // 幻影俠盜(3轉)
            case 2412: { // 幻影俠盜(4轉)
                // 高洞察力
                bx = SkillFactory.getSkill(20030206);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localdex += eff.getDexX();
                    dodgeChance += eff.getER();
                    chra.getTrait(MapleTraitType.craft).addLocalExp(GameConstants.getTraitExpNeededForLevel(20));
                    chra.getTrait(MapleTraitType.sense).addLocalExp(GameConstants.getTraitExpNeededForLevel(20));
                }
                bx = SkillFactory.getSkill(24001002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    speed += eff.getPassiveSpeed();
                    jump += eff.getPassiveJump();
                }
                bx = SkillFactory.getSkill(24000003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dodgeChance += eff.getER();
                }
                bx = SkillFactory.getSkill(24100006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localluk += eff.getLukX();
                }
                bx = SkillFactory.getSkill(24111002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localluk += eff.getLukX();
                }
                bx = SkillFactory.getSkill(24111006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(24101002, (int) eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(24121003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(24111006, (int) eff.getDAMRate());
                    damageIncrease.put(24111008, (int) eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(24120006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    trueMastery += eff.getMastery();
                }
                bx = SkillFactory.getSkill(24120002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dodgeChance += eff.getSubProp();
                }
                break;
            }
            case 501:
            case 530:
            case 531:
            case 532:
                defRange = 200;
                bx = SkillFactory.getSkill(5010003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    watk += bx.getEffect(bof).getAttackX();
                }
                bx = SkillFactory.getSkill(5300008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(5311001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    damageIncrease.put(5301001, (int) bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(5310007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                    percent_wdef += eff.getWDEFRate();
                }
                bx = SkillFactory.getSkill(5310006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    watk += bx.getEffect(bof).getAttackX();
                }
                bx = SkillFactory.getSkill(5321009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                    ignoreTargetDEF += eff.getIgnoreMob();
                }
                break;
            case 508: // 蒼龍俠客(一轉)
            case 570: // 蒼龍俠客(二轉)
            case 571: // 蒼龍俠客(三轉)
            case 572: // 蒼龍俠客(四轉)
                // 寶盒的保佑
                bx = SkillFactory.getSkill(1214);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    MapleCoreAura core = chra.getCoreAura();
                    localstr += core.getStr();
                    localdex += core.getDex();
                    localint_ += core.getInt();
                    localluk += core.getLuk();
                    watk += core.getAtt();
                    magic += core.getMagic();
                }
                // 體能訓練
                bx = SkillFactory.getSkill(5700003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                // 金剛不壞
                bx = SkillFactory.getSkill(5710004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getWDEFRate();
                    percent_mdef += eff.getMDEFRate();
//                    localmaxhp += eff.getMaxHpX();
//                    localmaxmp += eff.getMaxMpX();
                }
                break;
            case 3001:
            case 3100:
            case 3110:
            case 3111:
            case 3112:
                mpRecoverProp = 100;
                // 黑暗敏捷
                bx = SkillFactory.getSkill(31000002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    accuracy += eff.getAccX();
                    speed += eff.getPassiveSpeed();
                    jump += eff.getPassiveJump();
                }
                // HP增加
                bx = SkillFactory.getSkill(31000003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                // 惡魔狂斬 1次強化
                bx = SkillFactory.getSkill(31100007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(31000004, (int) eff.getDAMRate());
                    damageIncrease.put(31001006, (int) eff.getDAMRate());
                    damageIncrease.put(31001007, (int) eff.getDAMRate());
                    damageIncrease.put(31001008, (int) eff.getDAMRate());
                }
                // 體能訓練
                bx = SkillFactory.getSkill(31100005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                // 惡魔狂斬 2次強化
                bx = SkillFactory.getSkill(31110010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(31000004, (int) eff.getX());
                    damageIncrease.put(31001006, (int) eff.getX());
                    damageIncrease.put(31001007, (int) eff.getX());
                    damageIncrease.put(31001008, (int) eff.getX());
                }
                // 精神集中
                bx = SkillFactory.getSkill(31110007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                }
                // 力量防禦
                bx = SkillFactory.getSkill(31110008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dodgeChance += eff.getX();
                    // HACK: shouldn't be here
                    hpRecoverPercent += eff.getY();
                    hpRecoverProp += eff.getX();
                    //mpRecover += eff.getY(); // handle in takeDamage
                    //mpRecoverProp += eff.getX();
                }
                // 強化惡魔之力
                bx = SkillFactory.getSkill(31110009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mpRecover += 1;
                    mpRecoverProp += eff.getProb();
                }
                // 邪惡酷刑
                bx = SkillFactory.getSkill(31110006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getX() + 100.0) / 100.0;
                    bossdam_r *= (eff.getX() + 100.0) / 100.0;
//                    passive_sharpeye_rate += eff.getY();
                }
                // 黑暗拘束
                bx = SkillFactory.getSkill(31121006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    ignoreTargetDEF += bx.getEffect(bof).getIgnoreMob();
                }
                // 惡魔狂斬最終強化
                bx = SkillFactory.getSkill(31120011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(31000004, (int) eff.getX());
                    damageIncrease.put(31001006, (int) eff.getX());
                    damageIncrease.put(31001007, (int) eff.getX());
                    damageIncrease.put(31001008, (int) eff.getX());
                }
                // 進階武器精通
                bx = SkillFactory.getSkill(31120008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                // 堅硬肌膚
                bx = SkillFactory.getSkill(31120009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_wdef += bx.getEffect(bof).getT();
                }
                // 惡魔之怒
                bx = SkillFactory.getSkill(30010112);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    bossdam_r += eff.getBossDamage();
                    mpRecover += eff.getX();
                    mpRecoverProp += eff.getBossDamage(); //yes
                }
                // 魔族之血
                bx = SkillFactory.getSkill(30010185);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    chra.getTrait(MapleTraitType.will).addLocalExp(GameConstants.getTraitExpNeededForLevel(eff.getY()));
                    chra.getTrait(MapleTraitType.charisma).addLocalExp(GameConstants.getTraitExpNeededForLevel(eff.getZ()));
                }
                // 死亡詛咒
                bx = SkillFactory.getSkill(30010111);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    hpRecoverPercent += eff.getX();
                    hpRecoverProp += eff.getProb(); //yes
                }
                break;
            case 510:
            case 511:
            case 512: {
                // HP增加
                bx = SkillFactory.getSkill(5100009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                // 體能訓練
                bx = SkillFactory.getSkill(5100010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                // 拳霸大師
                bx = SkillFactory.getSkill(5121015);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    trueMastery += eff.getMastery();
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                // 防禦撞擊
                bx = SkillFactory.getSkill(5120014);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    ignoreTargetDEF += eff.x;
                }
                break;
            }
            case 1500: // 閃雷悍將(1轉)
            case 1510: // 閃雷悍將(2轉)
            case 1511: // 閃雷悍將(3轉)
            case 1512: { // 閃雷悍將(4轉)
                // 極限迴避
                bx = SkillFactory.getSkill(15000000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    accuracy += eff.getAccX();
                    jump += eff.getPassiveJump();
                }
                // 增加生命
                bx = SkillFactory.getSkill(15000008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                // 百烈訓練
                bx = SkillFactory.getSkill(15100009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                break;
            }
            case 400:
            case 410:
            case 411:
            case 412: {
                defRange = 200;
                // 鷹之眼
                bx = SkillFactory.getSkill(4000001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    defRange += bx.getEffect(bof).getRange();
                }
                // 迴避
                bx = SkillFactory.getSkill(4000012);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                // 影之抵抗
                bx = SkillFactory.getSkill(4100006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                // 體能訓練
                bx = SkillFactory.getSkill(4100007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localdex += eff.getDexX();
                    localluk += eff.getLukX();
                }
                break;
            }
            case 420:
            case 421:
            case 422: {
                bx = SkillFactory.getSkill(4200006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                // 體能訓練
                bx = SkillFactory.getSkill(4200007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localdex += eff.getDexX();
                    localluk += eff.getLukX();
                }
                bx = SkillFactory.getSkill(4210000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getX();
                    percent_mdef += eff.getX();
                }
                break;
            }
            case 430: // 下忍
            case 431: // 中忍
            case 432: // 上忍
            case 433: // 隱忍
            case 434: { // 影武者
                // 迴避
                bx = SkillFactory.getSkill(4000012);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                // 影之抵抗
                bx = SkillFactory.getSkill(4310004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                // 體能訓練
                bx = SkillFactory.getSkill(4310006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localdex += eff.getDexX();
                    localluk += eff.getLukX();
                }
                // 激進黑暗
                bx = SkillFactory.getSkill(4330008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                // 幻影替身
                bx = SkillFactory.getSkill(4341006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getWDEFRate();
                    percent_mdef += eff.getMDEFRate();
                }
                break;
            }
            case 100:
            case 110:
            case 111:
            case 112:
            case 120:
            case 121:
            case 122:
            case 130:
            case 131:
            case 132: {
                // HP增加
                bx = SkillFactory.getSkill(1000006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                // 盾防精通
                bx = SkillFactory.getSkill(1210001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getX();
                    percent_mdef += eff.getX();
                }
                // 武神防禦
                bx = SkillFactory.getSkill(1220005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_wdef += bx.getEffect(bof).getT();
                }
                // 屬性強化
                bx = SkillFactory.getSkill(1220010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    trueMastery += bx.getEffect(bof).getMastery();
                }
                // 魔法抵抗
                bx = SkillFactory.getSkill(1310000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    TER += bx.getEffect(bof).getX();
                }
                break;
            }
            case 322: { // Crossbowman
                bx = SkillFactory.getSkill(3220004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                bx = SkillFactory.getSkill(3220009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ignoreTargetDEF += eff.getIgnoreMob();
                }
                bx = SkillFactory.getSkill(3220005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && chra.getBuffedValue(MapleBuffStat.SPIRIT_LINK) != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getX();
                    dam_r *= (eff.getDamage() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDamage() + 100.0) / 100.0;
                }
                break;
            }
            case 312: { // Bowmaster
                bx = SkillFactory.getSkill(3120005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    watk += bx.getEffect(bof).getX();
                }
                bx = SkillFactory.getSkill(3120011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ignoreTargetDEF += eff.getIgnoreMob();
                }
                bx = SkillFactory.getSkill(3120006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && chra.getBuffedValue(MapleBuffStat.SPIRIT_LINK) != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getX();
                    dam_r *= (eff.getDamage() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDamage() + 100.0) / 100.0;
                }
                break;
            }
            case 3510:
            case 3511:
            case 3512:
                defRange = 200;
                // 機甲戰神精通
                bx = SkillFactory.getSkill(35100000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    watk += bx.getEffect(bof).getAttackX();
                }
                // 體能訓練
                bx = SkillFactory.getSkill(35100011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                // 合金盔甲終極
                bx = SkillFactory.getSkill(35120000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    trueMastery += bx.getEffect(bof).getMastery();
                }
                break;
            case 3210:
            case 3211:
            case 3212:
                // 智慧昇華
                bx = SkillFactory.getSkill(32100007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                // 進階藍色繩索
                bx = SkillFactory.getSkill(32110000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    ASR += bx.getEffect(bof).getASRRate();
                    TER += bx.getEffect(bof).getTERRate();
                }
                // 戰鬥精通
                bx = SkillFactory.getSkill(32110001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                // 進階黑色繩索
                bx = SkillFactory.getSkill(32120000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    magic += bx.getEffect(bof).getMagicX();
                }
                // 進階黃色繩索
                bx = SkillFactory.getSkill(32120001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                // 勁能
                bx = SkillFactory.getSkill(32120009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                    percent_mp += bx.getEffect(bof).getPercentMP();
                    percent_wdef += bx.getEffect(bof).getWDEFRate();
                    percent_mdef += bx.getEffect(bof).getMDEFRate();
                }
                break;
            case 3300:
            case 3310:
            case 3311:
            case 3312:
                defRange = 200;
                // 體能訓練
                bx = SkillFactory.getSkill(33100010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                // 騎乘精通
                bx = SkillFactory.getSkill(33110000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDamage() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDamage() + 100.0) / 100.0;
                }
                // 弩術精通
                bx = SkillFactory.getSkill(33120000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                // 狂暴天性
                bx = SkillFactory.getSkill(33120010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    ignoreTargetDEF += eff.getIgnoreMob();
                    dodgeChance += eff.getER();
                }
                // 進階終極攻擊
                bx = SkillFactory.getSkill(33120011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    damageIncrease.put(33100009, Integer.valueOf(eff.getDamage()));
                }
                break;
            case 2001: // 龍魔導士(0轉)
            case 2200: // 龍魔導士(1轉)
            case 2210: // 龍魔導士(2轉)
            case 2211: // 龍魔導士(3轉)
            case 2212: // 龍魔導士(4轉)
            case 2213: // 龍魔導士(5轉)
            case 2214: // 龍魔導士(6轉)
            case 2215: // 龍魔導士(7轉)
            case 2216: // 龍魔導士(8轉)
            case 2217: // 龍魔導士(9轉)
            case 2218: { // 龍魔導士(10轉)
                // 繼承的意志
                bx = SkillFactory.getSkill(20010194);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    magic += eff.getMagicX();
                    mdef += eff.getMdefX();
                    percent_hp += eff.getPercentHP();
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                    localint_ += eff.getIntX();
                    localluk += eff.getLukX();
                    bossdam_r += eff.getBossDamage();
                }
                // 龍之魂
                bx = SkillFactory.getSkill(22000000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    magic += eff.getMatk();
                    wdef += eff.getWdefX();
                    mdef += eff.getMdefX();
                    speed += eff.getPassiveSpeed();
                }
                // 智慧昇華
                bx = SkillFactory.getSkill(22120001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                // 守護之力
                bx = SkillFactory.getSkill(22131001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                // 魔力激發
                bx = SkillFactory.getSkill(22150000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mpconPercent += eff.getX() - 100;
                    dam_r *= eff.getY() / 100.0;
                    bossdam_r *= eff.getY() / 100.0;
                }
                // 龍神的護佑
                bx = SkillFactory.getSkill(22160000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDamage() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDamage() + 100.0) / 100.0;
                }
                // 魔法激發
                bx = SkillFactory.getSkill(22170001); // magic mastery, this is an invisible skill
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    magic += eff.getX();
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                break;
            }
            case 2112: {
                // 攻擊戰術
                bx = SkillFactory.getSkill(21120001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                // 進階終極攻擊
                bx = SkillFactory.getSkill(21120012);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    damageIncrease.put(21100010, Integer.valueOf(eff.getDamage()));
                }
                // 防禦戰術
                bx = SkillFactory.getSkill(21120004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                    percent_wdef += bx.getEffect(bof).getT();
                }
                break;
            }
            case 5000: // 米哈逸(0轉)
            case 5100: // 米哈逸(1轉)
            case 5110: // 米哈逸(2轉)
            case 5111: // 米哈逸(3轉)
            case 5112: // 米哈逸(4轉)
                // 女皇的呼喚
                bx = SkillFactory.getSkill(50000074);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getX();
                    percent_mp += eff.getX();
                }
                // 增加HP
                bx = SkillFactory.getSkill(51000000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                // 靈魂盾牌
                bx = SkillFactory.getSkill(51000001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    wdef += eff.getWdefX();
                    mdef += eff.getMdefX();
                }
                // 靈魂迅捷
                bx = SkillFactory.getSkill(51000002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    accuracy += eff.getAccX();
                    speed += eff.getPassiveSpeed();
                    jump += eff.getPassiveJump();
                }
                // 體能訓練
                bx = SkillFactory.getSkill(51100000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                // 癒合
                bx = SkillFactory.getSkill(51110001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    // Add Attack Speed here
                }
                // 靈魂重擊
                bx = SkillFactory.getSkill(51110002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_atk += eff.getX();
                    passive_sharpeye_min_percent += eff.getY();
                }
                // 戰鬥大師
                bx = SkillFactory.getSkill(51120000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    ignoreTargetDEF += eff.getIgnoreMob();
                }
                // 進階精準之劍
                bx = SkillFactory.getSkill(51120001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    trueMastery += eff.getMastery();
                    watk += eff.getAttackX();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                // 進階終極攻擊
                bx = SkillFactory.getSkill(51120002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    damageIncrease.put(51100002, Integer.valueOf(eff.getDamage()));
                }
                // 進階靈魂盾牌
                bx = SkillFactory.getSkill(51120003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_wdef += bx.getEffect(bof).getT();
                }
                break;
        }
        this.watk += Math.floor((watk * percent_atk) / 100.0f);
        this.magic += Math.floor((magic * percent_matk) / 100.0f); //or should this go before
        this.localint_ += Math.floor((localint_ * percent_matk) / 100.0f); //overpowered..
        bx = SkillFactory.getSkill(80000000);
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            localstr += eff.getStrX();
            localdex += eff.getDexX();
            localint_ += eff.getIntX();
            localluk += eff.getLukX();
            percent_hp += eff.getHpR();
            percent_mp += eff.getMpR();
        }
        bx = SkillFactory.getSkill(80000001);
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            bossdam_r += eff.getBossDamage();
        }
        bx = SkillFactory.getSkill(80001040);
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            expBuff *= (bx.getEffect(bof).getEXPRate() + 100.0) / 100.0;
        }
        if (GameConstants.isAdventurer(chra.getJob())) {
            bx = SkillFactory.getSkill(74);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                levelBonus += bx.getEffect(bof).getX();
            }

            bx = SkillFactory.getSkill(80);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                levelBonus += bx.getEffect(bof).getX();
            }

            bx = SkillFactory.getSkill(10074);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                levelBonus += bx.getEffect(bof).getX();
            }

            bx = SkillFactory.getSkill(10080);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                levelBonus += bx.getEffect(bof).getX();
            }

            // 百烈祝福
            bx = SkillFactory.getSkill(110);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                localstr += eff.getStrX();
                localdex += eff.getDexX();
                localint_ += eff.getIntX();
                localluk += eff.getLukX();
                percent_hp += eff.getPercentHP();
                percent_mp += eff.getPercentMP();
            }

            bx = SkillFactory.getSkill(10110);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                localstr += eff.getStrX();
                localdex += eff.getDexX();
                localint_ += eff.getIntX();
                localluk += eff.getLukX();
                percent_hp += eff.getHpR();
                percent_mp += eff.getMpR();
            }
        }

        //damage increase
        switch (chra.getJob()) {
            case 210:
            case 211:
            case 212: {
                // 智慧昇華
                bx = SkillFactory.getSkill(2100007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                // 終極魔術(火，毒)
                bx = SkillFactory.getSkill(2110000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dotTime += eff.getX();
                    dot += eff.getZ();
                }
                // 魔力激發
                bx = SkillFactory.getSkill(2110001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mpconPercent += eff.getX() - 100;
                    dam_r *= eff.getY() / 100.0;
                    bossdam_r *= eff.getY() / 100.0;
                }
                // 地獄爆發
                bx = SkillFactory.getSkill(2121003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(2111003, (int) eff.getX());
                }
                // 召喚冰魔
                bx = SkillFactory.getSkill(2121005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    TER += bx.getEffect(bof).getTERRate();
                }
                // 火毒大師魔法
                bx = SkillFactory.getSkill(2121009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    magic += eff.getMagicX();
                    BuffUP_Skill += eff.getX();
                }
                // 神秘狙擊
                bx = SkillFactory.getSkill(2120010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                    bossdam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                    ignoreTargetDEF += eff.getIgnoreMob();
                }
                break;
            }
            case 220:
            case 221:
            case 222: { // IL
                bx = SkillFactory.getSkill(2200007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                bx = SkillFactory.getSkill(2210000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dot += bx.getEffect(bof).getZ();
                }
                bx = SkillFactory.getSkill(2210001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mpconPercent += eff.getX() - 100;
                    dam_r *= eff.getY() / 100.0;
                    bossdam_r *= eff.getY() / 100.0;
                }
                bx = SkillFactory.getSkill(2220009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    magic += eff.getMagicX();
                    BuffUP_Skill += eff.getX();
                }
                bx = SkillFactory.getSkill(2221005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    TER += bx.getEffect(bof).getTERRate();
                }
                // 冰雷 大師魔法
                bx = SkillFactory.getSkill(2221009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    magic += eff.getMagicX();
                    BuffUP_Skill += eff.getX();
                }
                bx = SkillFactory.getSkill(2220010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                    bossdam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                    ignoreTargetDEF += eff.getIgnoreMob();
                }
                break;
            }
            case 1211:
            case 1212: { // flame
                // 魔力激發
                bx = SkillFactory.getSkill(12110001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mpconPercent += eff.getX() - 100;
                    dam_r *= eff.getY() / 100.0;
                    bossdam_r *= eff.getY() / 100.0;
                }
                // 召喚冰魔
                bx = SkillFactory.getSkill(12111004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    TER += bx.getEffect(bof).getTERRate();
                }
                break;
            }
            case 230:
            case 231:
            case 232: { // Bishop
                bx = SkillFactory.getSkill(2300007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                bx = SkillFactory.getSkill(2310008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    passive_sharpeye_rate += bx.getEffect(bof).getCr();
                }
                // 主教 大師魔法
                bx = SkillFactory.getSkill(2321010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    magic += eff.getMagicX();
                    BuffUP_Skill += eff.getX();
                }
                bx = SkillFactory.getSkill(2320005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    ASR += bx.getEffect(bof).getASRRate();
                }
                bx = SkillFactory.getSkill(2320011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                    bossdam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                    ignoreTargetDEF += eff.getIgnoreMob();
                }
                break;
            }
            case 2002: // 精靈遊俠(0轉)
            case 2300: // 精靈遊俠(1轉)
            case 2310: // 精靈遊俠(2轉)
            case 2311: // 精靈遊俠(3轉)
            case 2312: // 精靈遊俠(4轉)
                defRange = 200;
                // 精靈的祝福
                bx = SkillFactory.getSkill(20021110);
                bof = chra.getSkillLevel(bx);
                if (bof > 0) {
                    expBuff *= (bx.getEffect(bof).getEXPRate() + 100.0) / 100.0;
                }
                // 王的資格
                bx = SkillFactory.getSkill(20020112);
                bof = chra.getSkillLevel(bx);
                if (bof > 0) {
                    chra.getTrait(MapleTraitType.charm).addLocalExp(GameConstants.getTraitExpNeededForLevel(30));
                }
                // 潛在力量
                bx = SkillFactory.getSkill(23000001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                // 體能訓練
                bx = SkillFactory.getSkill(23100008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                // 依古尼斯咆哮
                bx = SkillFactory.getSkill(23111004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dodgeChance += bx.getEffect(bof).getProb();
                }
                // 騰空踢擊
                bx = SkillFactory.getSkill(23110006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    damageIncrease.put(23101001, (int) bx.getEffect(bof).getDAMRate());
                }
                // 遠古意志
                bx = SkillFactory.getSkill(23121004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dodgeChance += bx.getEffect(bof).getProb();
                }
                // 進階雙弩槍精通
                bx = SkillFactory.getSkill(23120009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                // 破防射擊
                bx = SkillFactory.getSkill(23120010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    ignoreTargetDEF += bx.getEffect(bof).getX(); //or should we do 100?
                }
                // 旋風月光翻轉
                bx = SkillFactory.getSkill(23120011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    damageIncrease.put(23101001, (int) bx.getEffect(bof).getDAMRate());
                }
                // 進階終極攻擊
                bx = SkillFactory.getSkill(23120012);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    watk += bx.getEffect(bof).getAttackX();
                    damageIncrease.put(23100006, Integer.valueOf(bx.getEffect(bof).getDamage()));
                }
                // 伊修塔爾之環
                bx = SkillFactory.getSkill(23121000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    damageIncrease.put(23111000, (int) bx.getEffect(bof).getDAMRate());
                }
                // 傳說之槍
                bx = SkillFactory.getSkill(23121002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    damageIncrease.put(23111001, (int) bx.getEffect(bof).getDAMRate());
                }
                break;
            case 1300: // 破風使者(1轉)
            case 1310: // 破風使者(2轉)
            case 1311: // 破風使者(3轉)
            case 1312: // 破風使者(4轉)
                defRange = 200;
                // 精通射手
                bx = SkillFactory.getSkill(13000001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    accuracy += eff.getAcc();
                    // eva
                    defRange += eff.getRange();
                    speed += eff.getSpeed();
                }
                // 體能訓練
                bx = SkillFactory.getSkill(13100008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                // 躲避
                bx = SkillFactory.getSkill(13110008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                // 弓術精通
                bx = SkillFactory.getSkill(13110003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    trueMastery += eff.getMastery();
                    watk += eff.getX();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                break;
            case 300:
            case 310:
            case 311:
            case 312:
                defRange = 200;
                bx = SkillFactory.getSkill(3000002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    defRange += bx.getEffect(bof).getRange();
                }
                // 體能訓練
                bx = SkillFactory.getSkill(3100006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(3110007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                bx = SkillFactory.getSkill(3120005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                break;
            case 320:
            case 321:
            case 322:
                defRange = 200;
                bx = SkillFactory.getSkill(3000002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    defRange += bx.getEffect(bof).getRange();
                }
                // 體能訓練
                bx = SkillFactory.getSkill(3200006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(3220010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    damageIncrease.put(3211006, bx.getEffect(bof).getDamage() - 150);
                }
                bx = SkillFactory.getSkill(3210007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                break;

            case 422:
                bx = SkillFactory.getSkill(4221007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Savage Blow, Steal, and Assaulter
                    eff = bx.getEffect(bof);
                    damageIncrease.put(4201005, (int) eff.getDAMRate());
                    damageIncrease.put(4201004, (int) eff.getDAMRate());
                    damageIncrease.put(4211002, (int) eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(4220009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mesoBuff *= (eff.getMesoRate() + 100.0) / 100.0;
                    pickRate += eff.getU();
                    mesoGuard -= eff.getV();
                    mesoGuardMeso -= eff.getW();
                    damageIncrease.put(4211006, eff.getX());
                }
                break;
            case 433:
            case 434:
                bx = SkillFactory.getSkill(4330007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    hpRecoverProp += eff.getProb();
                    hpRecoverPercent += eff.getX();
                }
                bx = SkillFactory.getSkill(4341002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Fatal Blow, Slash Storm, Tornado Spin, Bloody Storm, Upper Stab, and Flying Assaulter
                    eff = bx.getEffect(bof);
                    damageIncrease.put(4311002, (int) eff.getDAMRate());
                    damageIncrease.put(4311003, (int) eff.getDAMRate());
                    damageIncrease.put(4321000, (int) eff.getDAMRate());
                    damageIncrease.put(4321001, (int) eff.getDAMRate());
                    damageIncrease.put(4331000, (int) eff.getDAMRate());
                    damageIncrease.put(4331004, (int) eff.getDAMRate());
                    damageIncrease.put(4331005, (int) eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(4341006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                break;
            case 2110: // 狂狼勇士(2轉)
            case 2111: // 狂狼勇士(3轉)
            case 2112: { // 狂狼勇士(4轉)
                // 體能訓練
                bx = SkillFactory.getSkill(21100008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                // 寒冰屬性
                bx = SkillFactory.getSkill(21101006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                }
                // 伺機攻擊
                bx = SkillFactory.getSkill(21110002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    damageIncrease.put(21000004, bx.getEffect(bof).getW());
                }
                // 攀爬 攻擊
                bx = SkillFactory.getSkill(21110010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    ignoreTargetDEF += bx.getEffect(bof).getIgnoreMob();
                }
                // 終極攻擊
                bx = SkillFactory.getSkill(21120002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    damageIncrease.put(21100007, bx.getEffect(bof).getZ());
                }
                // 快速移動
                bx = SkillFactory.getSkill(21120011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(21100002, (int) eff.getDAMRate());
                    damageIncrease.put(21110003, (int) eff.getDAMRate());
                }
                break;
            }
            case 3511:
            case 3512:
                // 金屬拳精通
                bx = SkillFactory.getSkill(35110014);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //ME-07 Drillhands, Atomic Hammer
                    eff = bx.getEffect(bof);
                    damageIncrease.put(35001003, (int) eff.getDAMRate());
                    damageIncrease.put(35101003, (int) eff.getDAMRate());
                }
                // 終極賽特拉特
                bx = SkillFactory.getSkill(35121006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Satellite
                    eff = bx.getEffect(bof);
                    damageIncrease.put(35111001, (int) eff.getDAMRate());
                    damageIncrease.put(35111009, (int) eff.getDAMRate());
                    damageIncrease.put(35111010, (int) eff.getDAMRate());
                }
                // 機器人精通
                bx = SkillFactory.getSkill(35120001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Satellite
                    eff = bx.getEffect(bof);
                    damageIncrease.put(35111005, eff.getX());
                    damageIncrease.put(35111011, eff.getX());
                    damageIncrease.put(35121009, eff.getX());
                    damageIncrease.put(35121010, eff.getX());
                    damageIncrease.put(35121011, eff.getX());
                    BuffUP_Summon += eff.getY();
                }
                break;
            case 110:
            case 111:
            case 112:
                // 體能訓練
                bx = SkillFactory.getSkill(1100009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(1110009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= eff.getDamage() / 100.0;
                    bossdam_r *= eff.getDamage() / 100.0;
                }
                bx = SkillFactory.getSkill(1120012);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    ignoreTargetDEF += bx.getEffect(bof).getIgnoreMob();
                }
                bx = SkillFactory.getSkill(1120013);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    damageIncrease.put(1100002, (int) eff.getDamage());
                }
                break;
            case 120:
            case 121:
            case 122:
                // 體能訓練
                bx = SkillFactory.getSkill(1200009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(1220006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    ASR += bx.getEffect(bof).getASRRate();
                    TER += bx.getEffect(bof).getTERRate();
                }
                break;
            case 511:
            case 512:
                bx = SkillFactory.getSkill(5110008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Backspin Blow, Double Uppercut, and Corkscrew Blow
                    eff = bx.getEffect(bof);
                    damageIncrease.put(5101002, eff.getX());
                    damageIncrease.put(5101003, eff.getY());
                    damageIncrease.put(5101004, eff.getZ());
                }
                break;
            case 520:
            case 521:
            case 522:
                defRange = 200;
                bx = SkillFactory.getSkill(5220001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Flamethrower and Ice Splitter
                    eff = bx.getEffect(bof);
                    damageIncrease.put(5211004, (int) eff.getDamage());
                    damageIncrease.put(5211005, (int) eff.getDamage());
                }
                // 體能訓練
                bx = SkillFactory.getSkill(5200009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                // 金屬外殼
                bx = SkillFactory.getSkill(5210013);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    ignoreTargetDEF += eff.getIgnoreMob();
                }
                break;
            case 130: // 槍騎兵
            case 131: // 龍騎士
            case 132: // 黑騎士
                // 體能訓練
                bx = SkillFactory.getSkill(1300009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                // 龍之審判
                bx = SkillFactory.getSkill(1310009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    passive_sharpeye_rate += eff.getCr();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                    hpRecoverProp += eff.getProb();
                    hpRecoverPercent += eff.getX();
                }
                // 黑暗力量
                bx = SkillFactory.getSkill(1320006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDamage() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDamage() + 100.0) / 100.0;
                    passive_sharpeye_rate += eff.getCr();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                break;
            case 411:
            case 412:
                bx = SkillFactory.getSkill(4110000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    RecoveryUP += eff.getX() - 100;
                    BuffUP += eff.getY() - 100;
                }
                bx = SkillFactory.getSkill(4120010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Lucky Seven, Drain, Avenger, Shadow Meso
                    eff = bx.getEffect(bof);
                    damageIncrease.put(4001344, (int) eff.getDAMRate());
                    damageIncrease.put(4101005, (int) eff.getDAMRate());
                    damageIncrease.put(4111004, (int) eff.getDAMRate());
                    damageIncrease.put(4111005, (int) eff.getDAMRate());
                }
                break;
            case 1400:
            case 1410:
            case 1411:
            case 1412:
                // 幻體功
                bx = SkillFactory.getSkill(14000000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    accuracy += eff.getX();
                    // 迴避值
                }
                defRange = 200;
                // 鷹之眼
                bx = SkillFactory.getSkill(14000001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    defRange += bx.getEffect(bof).getRange();
                }
                // 體能訓練
                bx = SkillFactory.getSkill(14100010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localdex += eff.getDexX();
                    localluk += eff.getLukX();
                }
                // 激進黑暗
                bx = SkillFactory.getSkill(14110009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                // 藥劑精通
                bx = SkillFactory.getSkill(14110011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    RecoveryUP += eff.getX() - 100;
                }
                break;
        }

        // 精靈的祝福
        bx = SkillFactory.getSkill(GameConstants.getBOF_ForJob(chra.getJob()));
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            watk += eff.getX();
            magic += eff.getY();
            accuracy += eff.getZ();
        }
        // 女皇的祝福
        bx = SkillFactory.getSkill(GameConstants.getEmpress_ForJob(chra.getJob()));
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            watk += eff.getX();
            magic += eff.getY();
            accuracy += eff.getZ();
        }
        // 聯盟的意志
        bx = SkillFactory.getSkill(GameConstants.getWOTA_ForJob(chra.getJob()));
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            localstr += eff.getStrX();
            localdex += eff.getDexX();
            localint_ += eff.getIntX();
            localluk += eff.getLukX();
            watk += eff.getAttackX();
            magic += eff.getMagicX();
        }
        // 寶盒的護佑
        bx = SkillFactory.getSkill(80001151);
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            MapleCoreAura core = chra.getCoreAura();
            localstr += core.getStr();
            localdex += core.getDex();
            localint_ += core.getInt();
            localluk += core.getLuk();
            watk += core.getAtt();
            magic += core.getMagic();
        }
        if (GameConstants.isResist(chra.getJob())) {
            bx = SkillFactory.getSkill(30000002);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                RecoveryUP += bx.getEffect(bof).getX() - 100;
            }
        }

        // 不知道什麼鬼東西任何職業的全屬性+5
        localstr += 5;
        localdex += 5;
        localint_ += 5;
        localluk += 5;
    }

    private void handleBuffStats(MapleCharacter chra) {
        MapleStatEffect eff = chra.getStatForBuff(MapleBuffStat.MONSTER_RIDING);
        if (eff != null && eff.getSourceId() == 33001001) { //jaguar
            passive_sharpeye_rate += eff.getW();
            percent_hp += eff.getZ();
        }
        Integer buff = chra.getBuffedValue(MapleBuffStat.DICE_ROLL);
        if (buff != null) {
            percent_wdef += GameConstants.getDiceStat(buff, 2);
            percent_mdef += GameConstants.getDiceStat(buff, 2);
            percent_hp += GameConstants.getDiceStat(buff, 3);
            percent_mp += GameConstants.getDiceStat(buff, 3);
            passive_sharpeye_rate += GameConstants.getDiceStat(buff, 4);
            dam_r *= (GameConstants.getDiceStat(buff, 5) + 100.0) / 100.0;
            bossdam_r *= (GameConstants.getDiceStat(buff, 5) + 100.0) / 100.0;
            expBuff *= (GameConstants.getDiceStat(buff, 6) + 100.0) / 100.0;
            if (chra.getBuffSource(MapleBuffStat.DICE_ROLL) == 5320007) {
//                percent_wdef += GameConstants.getDiceStat(buff, 2);
//                percent_mdef += GameConstants.getDiceStat(buff, 2);
//                percent_hp += GameConstants.getDiceStat(buff, 3);
//                percent_mp += GameConstants.getDiceStat(buff, 3);
//                passive_sharpeye_rate += GameConstants.getDiceStat(buff, 4);
//                dam_r *= (GameConstants.getDiceStat(buff, 5) + 100.0) / 100.0;
//                bossdam_r *= (GameConstants.getDiceStat(buff, 5) + 100.0) / 100.0;
//                expBuff *= (GameConstants.getDiceStat(buff, 6) + 100.0) / 100.0;
            }
        }
        buff = chra.getBuffedValue(MapleBuffStat.HP_BOOST_PERCENT);
        if (buff != null) {
            percent_hp += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MP_BOOST_PERCENT);
        if (buff != null) {
            percent_mp += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.DEFENCE_BOOST_R);
        if (buff != null) {
            percent_wdef += buff;
            percent_mdef += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.AsrR);
        if (buff != null) {
            ASR += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.TerR);
        if (buff != null) {
            TER += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.INFINITY);
        if (buff != null) {
            percent_matk += buff - 1;
        }
        buff = chra.getBuffedValue(MapleBuffStat.ONYX_SHROUD);
        if (buff != null) {
            dodgeChance += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.PVP_DAMAGE);
        if (buff != null) {
            pvpDamage += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.PVP_ATTACK);
        if (buff != null) {
            pvpDamage += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.FELINE_BERSERK);
        if (buff != null) {
            percent_hp += buff;
        }
        eff = chra.getStatForBuff(MapleBuffStat.BLUE_AURA);
        if (eff != null) {
            percent_wdef += eff.getZ() + eff.getY();
            percent_mdef += eff.getZ() + eff.getY();

        }
        buff = chra.getBuffedValue(MapleBuffStat.CONVERSION);
        if (buff != null) {
            percent_hp += buff;
        } else {
            buff = chra.getBuffedValue(MapleBuffStat.MAXHP);
            if (buff != null) {
                percent_hp += buff;
            }
        }
        buff = chra.getBuffedValue(MapleBuffStat.MAXMP);
        if (buff != null) {
            percent_mp += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MP_BUFF);
        if (buff != null) {
            percent_mp += buff;
        }
        buff = chra.getBuffedSkill_X(MapleBuffStat.BUFF_MASTERY);
        if (buff != null) {
            BuffUP_Skill += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.STR);
        if (buff != null) {
            localstr += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.DEX);
        if (buff != null) {
            localdex += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.INT);
        if (buff != null) {
            localint_ += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.LUK);
        if (buff != null) {
            localluk += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.ANGEL_STAT);
        if (buff != null) {
            localstr += buff;
            localdex += buff;
            localint_ += buff;
            localluk += buff;
        }

        buff = chra.getBuffedValue(MapleBuffStat.ENHANCED_WDEF);
        if (buff != null) {
            wdef += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.ENHANCED_MDEF);
        if (buff != null) {
            mdef += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.WDEF);
        if (buff != null) {
            wdef += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.WDEF);
        if (buff != null) {
            mdef += buff;
        }

        buff = chra.getBuffedValue(MapleBuffStat.MAPLE_WARRIOR);
        if (buff != null) {
            final double d = buff.doubleValue() / 100.0;
            localstr += d * str; //base only
            localdex += d * dex;
            localluk += d * luk;
            localint_ += d * int_;
        }
        buff = chra.getBuffedValue(MapleBuffStat.ECHO_OF_HERO);
        if (buff != null) {
            final double d = buff.doubleValue() / 100.0;
            watk += (int) (watk * d);
            magic += (int) (magic * d);
        }
        buff = chra.getBuffedValue(MapleBuffStat.ARAN_COMBO);
        if (buff != null) {
            watk += buff / 10;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MESOGUARD);
        if (buff != null) {
            mesoGuardMeso += buff.doubleValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.EXPRATE);
        if (buff != null) {
            expBuff *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.DROP_RATE);
        if (buff != null) {
            dropBuff *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.ACASH_RATE);
        if (buff != null) {
            cashBuff *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MESO_RATE);
        if (buff != null) {
            mesoBuff *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MESOUP);
        if (buff != null) {
            mesoBuff *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.ACC);
        if (buff != null) {
            accuracy += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.ANGEL_ACC);
        if (buff != null) {
            accuracy += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.ANGEL_ATK);
        if (buff != null) {
            watk += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.ANGEL_MATK);
        if (buff != null) {
            magic += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.WATK);
        if (buff != null) {
            watk += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.SPIRIT_SURGE);
        if (buff != null) {
            dam_r *= (buff + 100.0) / 100.0;
            bossdam_r *= (buff + 100.0) / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.CRITICAL_INC);
        if (buff != null) {
            passive_sharpeye_rate += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.ENHANCED_WATK);
        if (buff != null) {
            watk += buff;
        }
        eff = chra.getStatForBuff(MapleBuffStat.ENERGY_CHARGE);
        if (eff != null) {
            watk += eff.getWatk();
            accuracy += eff.getAcc();
        }
        buff = chra.getBuffedValue(MapleBuffStat.MATK);
        if (buff != null) {
            magic += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.SPEED);
        if (buff != null) {
            speed += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.JUMP);
        if (buff != null) {
            jump += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.DASH_SPEED);
        if (buff != null) {
            speed += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.DASH_JUMP);
        if (buff != null) {
            jump += buff;
        }
        eff = chra.getStatForBuff(MapleBuffStat.HIDDEN_POTENTIAL);
        if (eff != null) {
            passive_sharpeye_rate = 100; //INTENSE
            ASR = 100; //INTENSE

            wdef += eff.getX();
            mdef += eff.getX();
            watk += eff.getX();
            magic += eff.getX();
        }
        buff = chra.getBuffedValue(MapleBuffStat.DAMAGE_BUFF);
        if (buff != null) {
            dam_r *= (buff.doubleValue() + 100.0) / 100.0;
            bossdam_r *= (buff.doubleValue() + 100.0) / 100.0;
        }
        buff = chra.getBuffedSkill_Y(MapleBuffStat.FINAL_CUT);
        if (buff != null) {
            dam_r *= buff.doubleValue() / 100.0;
            bossdam_r *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedSkill_Y(MapleBuffStat.OWL_SPIRIT);
        if (buff != null) {
            dam_r *= buff.doubleValue() / 100.0;
            bossdam_r *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedSkill_X(MapleBuffStat.BERSERK_FURY);
        if (buff != null) {
            dam_r *= buff.doubleValue() / 100.0;
            bossdam_r *= buff.doubleValue() / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.BLESS);
        if (eff != null) {
            watk += eff.getX();
            magic += eff.getY();
            accuracy += eff.getV();
        }
        buff = chra.getBuffedSkill_X(MapleBuffStat.CONCENTRATE);
        if (buff != null) {
            mpconReduce += buff;
        }
        eff = chra.getStatForBuff(MapleBuffStat.HOLY_SHIELD);
        if (eff != null) {
            watk += eff.getX();
            magic += eff.getY();
            accuracy += eff.getV();
            mpconReduce += eff.getMPConReduce();
        }
        eff = chra.getStatForBuff(MapleBuffStat.MAGIC_RESISTANCE);
        if (eff != null) {
            ASR += eff.getX();
        }

        eff = chra.getStatForBuff(MapleBuffStat.COMBO);
        buff = chra.getBuffedValue(MapleBuffStat.COMBO);
        if (eff != null && buff != null) {
            dam_r *= ((100.0 + ((eff.getV() + eff.getDAMRate()) * (buff - 1))) / 100.0);
            bossdam_r *= ((100.0 + ((eff.getV() + eff.getDAMRate()) * (buff - 1))) / 100.0);
        }
        eff = chra.getStatForBuff(MapleBuffStat.SUMMON);
        if (eff != null) {
            if (eff.getSourceId() == 35121010) { //amp
                dam_r *= (eff.getX() + 100.0) / 100.0;
                bossdam_r *= (eff.getX() + 100.0) / 100.0;
            }
        }
        eff = chra.getStatForBuff(MapleBuffStat.DARK_AURA);
        if (eff != null) {
            dam_r *= (eff.getX() + 100.0) / 100.0;
            bossdam_r *= (eff.getX() + 100.0) / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.BODY_BOOST);
        if (eff != null) {
            dam_r *= (eff.getV() + 100.0) / 100.0;
            bossdam_r *= (eff.getV() + 100.0) / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.BEHOLDER);
        if (eff != null) {
            trueMastery += eff.getMastery();
        }
        eff = chra.getStatForBuff(MapleBuffStat.MECH_CHANGE);
        if (eff != null) {
            passive_sharpeye_rate += eff.getCr();
        }
        eff = chra.getStatForBuff(MapleBuffStat.PYRAMID_PQ);
        if (eff != null && eff.getBerserk() > 0) {
            dam_r *= eff.getBerserk() / 100.0;
            bossdam_r *= eff.getBerserk() / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.WK_CHARGE);
        if (eff != null) {
            dam_r *= eff.getDamage() / 100.0;
            bossdam_r *= eff.getDamage() / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.PICKPOCKET);
        if (eff != null) {
            pickRate = eff.getProb();
        }
        eff = chra.getStatForBuff(MapleBuffStat.DamR);
        if (eff != null) {
            dam_r *= (eff.getDAMRate() + 100.0) / 100.0;
            bossdam_r *= (eff.getDAMRate() + 100.0) / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.LIGHTNING_CHARGE);
        if (eff != null) {
            dam_r *= eff.getDamage() / 100.0;
            bossdam_r *= eff.getDamage() / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.WIND_WALK);
        if (eff != null) {
            dam_r *= eff.getDamage() / 100.0;
            bossdam_r *= eff.getDamage() / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.DIVINE_SHIELD);
        if (eff != null) {
            watk += eff.getEnhancedWatk();
        }
        buff = chra.getBuffedSkill_Y(MapleBuffStat.DARKSIGHT);
        if (buff != null) {
            dam_r *= (buff + 100.0) / 100.0;
            bossdam_r *= (buff + 100.0) / 100.0;
        }
        buff = chra.getBuffedSkill_X(MapleBuffStat.ENRAGE);
        if (buff != null) {
            dam_r *= (buff + 100.0) / 100.0;
            bossdam_r *= (buff + 100.0) / 100.0;
        }
        buff = chra.getBuffedSkill_X(MapleBuffStat.COMBAT_ORDERS);
        if (buff != null) {
            combatOrders += buff;
        }
        eff = chra.getStatForBuff(MapleBuffStat.SHARP_EYES);
        if (eff != null) {
            passive_sharpeye_rate += eff.getX();
            passive_sharpeye_percent += eff.getY();
        }
        buff = chra.getBuffedValue(MapleBuffStat.CRITICAL_RATE_BUFF);
        if (buff != null) {
            passive_sharpeye_rate += buff;
        }
        if (speed > 140) {
            speed = 140;
        }
        if (jump > 123) {
            jump = 123;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MONSTER_RIDING);
        if (buff != null) {
            jump = 120;
            switch (buff) {
                case 1:
                    speed = 150;
                    break;
                case 2:
                    speed = 170;
                    break;
                case 3:
                    speed = 180;
                    break;
                default:
                    speed = 200; //lol
                    break;
            }
        }
    }

    public final int d(int variable) {
        return (int) Math.floor(variable);
    }
}
