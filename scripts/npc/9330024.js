﻿var status = 0;
var beauty = 0;
var mface = Array(20000, 20001, 20002, 20003, 20004, 20005, 20006, 20007, 20008, 20012, 20014);
var fface = Array(21000, 21001, 21002, 21003, 21004, 21005, 21006, 21007, 21008, 21012, 21014);
var facenew = Array();

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1)
            status++;
        else
            status--;
        if (status == 0) {
            cm.sendSimple("你好，我是#p9330024#！如果你有#b#t5152018##k，我就能幫你#r隨機整形#k哦。\r\n#L2##b使用 #t5152018##k");
        } else if (status == 1) {
            if (selection == 2) {
                facenew = Array();
                if (cm.getPlayer().getGender() == 0) {
                    for (var i = 0; i < mface.length; i++) {
                        facenew.push(mface[i] + cm.getPlayer().getFace() %
                            1000 - (cm.getPlayer().getFace() %
                                100));
                    }
                }
                if (cm.getPlayer().getGender() == 1) {
                    for (var i = 0; i < fface.length; i++) {
                        facenew.push(fface[i] + cm.getPlayer().getFace() %
                            1000 - (cm.getPlayer().getFace() %
                                100));
                    }
                }
                cm.sendYesNo("你真的要使用 #b#t5152018##k #r隨機整形#k嗎？");
            }
        } else if (status == 2) {
            cm.dispose();
            if (cm.haveItem(5152018) == true) {
                cm.gainItem(5152018, -1);
                cm.setFace(facenew[Math.floor(Math.random() * facenew.length)]);
                cm.sendOk("整形成功！");
            } else {
                cm.sendOk("你沒有#b#t5152018##k，因而無法整形。");
            }
        }
    }
}