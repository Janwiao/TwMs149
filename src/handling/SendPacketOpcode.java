package handling;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import tools.StringUtil;

public enum SendPacketOpcode implements WritableIntValueHolder {
    // GENERAL

    PING,
    // LOGIN
    LOGIN_STATUS,
    LOGIN_SECOND,
    PIN_OPERATION,
    SECONDPW_ERROR,
    SERVERLIST,
    SERVERSTATUS,
    SERVER_IP,
    CHARLIST,
    CHAR_NAME_RESPONSE,
    ADD_NEW_CHAR_ENTRY,
    DELETE_CHAR_RESPONSE,
    CHANNEL_SELECTED,
    ALL_CHARLIST,
    RSA_KEY,
    ENABLE_RECOMMENDED,
    SEND_RECOMMENDED,
    LOGIN_WELCOME,
    CHANGE_NAME_CHECK,
    CHANGE_NAME_RESPONSE,
    GACHAPON_STAMPS,
    FREE_CASH_ITEM,
    ONE_A_DAY,
    // CHANNEL
    CHANGE_CHANNEL, SHOW_TITLE,
    UPDATE_STATS,
    CS_CHARGE_CASH,
    FAME_RESPONSE,
    OWL_RESULT,
    USE_CASH_PET_FOOD,
    WEDDING_GIFT,
    UPDATE_SKILLS,
    UPDATE_STOLEN_SKILLS,
    TARGET_SKILL,
    YOUR_INFORMATION,
    CARD_DROPS,
    PINKBEAN_CHOCO,
    MULUNG_DOJO_RANKING,
    AUTO_CC_MSG,
    ALIEN_SOCKET_CREATOR,
    MULUNG_MESSAGE,
    REPLACE_SKILLS,
    INNER_ABILITY_MSG,
    ENABLE_INNER_ABILITY,
    DISABLE_INNER_ABILITY,
    UPDATE_HONOUR,
    AZWAN_KILLED,
    UPDATE_CORE_AURA,
    SHOW_FUSION_EFFECT,
    UPDATE_IMP_TIME,
    FIND_FRIEND,
    BOMB_LIE_DETECTOR,
    REPORT_RESPONSE,
    REPORT_TIME,
    RANDOM_MESOBAG_SUCCESS,
    BUFF_ZONE_EFFECT,
    TIME_BOMB_ATTACK,
    REISSUE_MEDAL,
    RANDOM_MESOBAG_FAILURE,
    POTION_BONUS,
    DISABLE_NPC,
    REPORT_STATUS,
    SHOW_FIREWORKS_EFFECT,
    SHOW_MAGNIFYING_EFFECT,
    SHOW_NEBULITE_EFFECT,
    SHEEP_RANCH_INFO,
    SHEEP_RANCH_CLOTHES,
    AP_RESET,
    EXP_POTION,
    ARIANT_SCOREBOARD,
    ARIANT_SCORE_UPDATE,
    BOAT_MOVE,
    BOAT_STATE,
    WITCH_TOWER,
    PYRAMID_KILL_COUNT,
    PVP_DAMAGED,
    ANDROID_UPDATE,
    SHOW_PQ_REWARD,
    PVP_DETAILS,
    NETT_PYRAMID,
    EXPAND_CHARACTER_SLOTS,
    WARP_TO_MAP,
    SERVERMESSAGE,
    ECHO_MESSAGE,
    AVATAR_MEGA,
    SPAWN_NPC,
    REMOVE_NPC,
    SPAWN_NPC_REQUEST_CONTROLLER,
    SPAWN_MONSTER,
    SPAWN_MONSTER_CONTROL,
    MOVE_MONSTER_RESPONSE,
    CHATTEXT,
    SHOW_STATUS_INFO,
    SHOW_MESO_GAIN,
    SHOW_QUEST_COMPLETION,
    WHISPER,
    SPAWN_PLAYER,
    ANNOUNCE_PLAYER_SHOP,
    SHOW_SCROLL_EFFECT,
    SHOW_ITEM_GAIN_INCHAT,
    CURRENT_MAP_WARP,
    KILL_MONSTER,
    DROP_ITEM_FROM_MAPOBJECT,
    FACIAL_EXPRESSION,
    FACIAL_EXPRESSION_2,
    MOVE_SCREEN,
    MOVE_PLAYER,
    MOVE_MONSTER,
    CLOSE_RANGE_ATTACK,
    RANGED_ATTACK,
    MAGIC_ATTACK,
    ENERGY_ATTACK,
    OPEN_NPC_SHOP,
    CONFIRM_SHOP_TRANSACTION,
    OPEN_STORAGE,
    INVENTORY_OPERATION,
    REMOVE_PLAYER_FROM_MAP,
    REMOVE_ITEM_FROM_MAP,
    UPDATE_CHAR_LOOK,
    SHOW_FOREIGN_EFFECT,
    GIVE_FOREIGN_BUFF,
    CANCEL_FOREIGN_BUFF,
    DAMAGE_PLAYER,
    CHAR_INFO,
    UPDATE_QUEST_INFO,
    GIVE_BUFF,
    CANCEL_BUFF,
    PLAYER_INTERACTION,
    UPDATE_CHAR_BOX,
    NPC_TALK,
    KEYMAP,
    SHOW_MONSTER_HP,
    PARTY_OPERATION,
    UPDATE_PARTYMEMBER_HP,
    MULTICHAT,
    APPLY_MONSTER_STATUS,
    CANCEL_MONSTER_STATUS,
    CLOCK,
    SPAWN_PORTAL,
    SPAWN_DOOR,
    REMOVE_DOOR,
    SPAWN_SUMMON,
    REMOVE_SUMMON,
    SUMMON_ATTACK,
    MOVE_SUMMON,
    SPAWN_KITE_ERROR,
    SPAWN_KITE,
    DESTROY_KITE,
    SPAWN_MIST,
    REMOVE_MIST,
    DAMAGE_SUMMON,
    DAMAGE_MONSTER,
    BUDDYLIST,
    SHOW_ITEM_EFFECT,
    SHOW_CHAIR,
    CANCEL_CHAIR,
    SKILL_EFFECT,
    CANCEL_SKILL_EFFECT,
    BOSS_ENV,
    REACTOR_SPAWN,
    REACTOR_HIT,
    REACTOR_DESTROY,
    MAP_EFFECT,
    GUILD_OPERATION,
    ALLIANCE_OPERATION,
    BBS_OPERATION,
    FAMILY,
    EARN_TITLE_MSG,
    SHOW_MAGNET,
    MERCH_ITEM_MSG,
    MERCH_ITEM_STORE,
    MESSENGER,
    NPC_ACTION,
    SPAWN_PET,
    MOVE_PET,
    PET_CHAT,
    SHOW_PET,
    PET_COMMAND,
    PET_NAMECHANGE,
    PET_AUTO_HP,
    PET_AUTO_MP,
    PET_AUTO_CURE,
    PET_UPDATE,
    COOLDOWN,
    PLAYER_HINT,
    SUMMON_HINT,
    SUMMON_HINT_MSG,
    CYGNUS_INTRO_DISABLE_UI,
    CYGNUS_INTRO_LOCK,
    USE_SKILL_BOOK,
    SHOW_EQUIP_EFFECT,
    SKILL_MACRO,
    CS_OPEN,
    CS_INFO,
    CS_UPDATE,
    CS_OPERATION,
    CS_ACCOUNT_NAME,
    MTS_OPEN,
    PLAYER_NPC,
    SHOW_NOTES,
    SUMMON_SKILL,
    ARIANT_PQ_START,
    CATCH_MONSTER,
    CATCH_MOB,
    ENGLISH_QUIZ,
    BOAT_EFFECT,
    CHALKBOARD,
    RENAME_FAMILIAR,
    DUEY,
    TROCK_LOCATIONS,
    MONSTER_CARNIVAL_START,
    MONSTER_CARNIVAL_OBTAINED_CP,
    MONSTER_CARNIVAL_PARTY_CP,
    MONSTER_CARNIVAL_SUMMON,
    MONSTER_CARNIVAL_DIED,
    SPAWN_HIRED_MERCHANT,
    UPDATE_HIRED_MERCHANT,
    SEND_TITLE_BOX,
    DESTROY_HIRED_MERCHANT,
    UPDATE_MOUNT,
    EXP_BONUS,
    VICIOUS_HAMMER,
    NEW_VICIOUS_HAMMER,
    VEGA_RESULT,
    FISHING_BOARD_UPDATE,
    FISHING_CAUGHT,
    OX_QUIZ,
    ROLL_SNOWBALL,
    HIT_SNOWBALL,
    SNOWBALL_MESSAGE,
    LEFT_KNOCK_BACK,
    FINISH_SORT,
    FINISH_GATHER,
    SEND_PEDIGREE,
    OPEN_FAMILY,
    FAMILY_MESSAGE,
    FAMILY_INVITE,
    FAMILY_JUNIOR,
    SENIOR_MESSAGE,
    REP_INCREASE,
    FAMILY_LOGGEDIN,
    FAMILY_BUFF,
    FAMILY_USE_REQUEST,
    YELLOW_CHAT,
    PIGMI_REWARD,
    GM_EFFECT,
    HIT_COCONUT,
    COCONUT_SCORE,
    LEVEL_UPDATE,
    MARRIAGE_UPDATE,
    JOB_UPDATE,
    HORNTAIL_SHRINE,
    STOP_CLOCK,
    MESOBAG_SUCCESS,
    MESOBAG_FAILURE,
    SERVER_BLOCKED,
    DRAGON_MOVE,
    DRAGON_REMOVE,
    DRAGON_SPAWN,
    ARAN_COMBO,
    TOP_MSG,
    TEMP_STATS,
    TEMP_STATS_RESET,
    OPEN_UI,
    OPEN_UI_OPTION,
    PYRAMID_UPDATE,
    PYRAMID_RESULT,
    SESSION_VALUE,
    GET_MTS_TOKENS,
    MTS_OPERATION,
    SHOW_POTENTIAL_EFFECT,
    SHOW_POTENTIAL_RESET,
    CHAOS_ZAKUM_SHRINE,
    CHAOS_HORNTAIL_SHRINE,
    GAME_POLL_QUESTION,
    GAME_POLL_REPLY,
    GMEVENT_INSTRUCTIONS,
    BOAT_EFF,
    OWL_OF_MINERVA,
    XMAS_SURPRISE,
    CASH_SONG,
    INVENTORY_GROW,
    FOLLOW_REQUEST,
    FOLLOW_EFFECT,
    FOLLOW_MOVE,
    FOLLOW_MSG,
    TALK_MONSTER,
    REMOVE_TALK_MONSTER,
    MONSTER_PROPERTIES,
    MOVE_PLATFORM,
    MOVE_ENV,
    UPDATE_ENV,
    ENGAGE_REQUEST,
    PARTY_VALUE,
    MAP_VALUE,
    ENGAGE_RESULT,
    UPDATE_JAGUAR,
    EXPEDITION_OPERATION,
    LOGIN_AUTH,
    TESLA_TRIANGLE,
    MECH_PORTAL,
    MECH_DOOR_SPAWN,
    MECH_DOOR_REMOVE,
    PET_FLAG_CHANGE,
    PAMS_SONG,
    PLAYER_DAMAGED,
    SP_RESET,
    REPORT,
    ULTIMATE_EXPLORER,
    GM_POLICE,
    PAM_SONG,
    CS_USE,
    DRAGON_BLINK,
    HARVESTED,
    SHOW_HARVEST,
    GAME_MESSAGE,
    ITEM_POT,
    GIVE_CHARACTER_SKILL,
    SPAWN_EXTRACTOR,
    REMOVE_EXTRACTOR,
    CRAFT_COMPLETE,
    CRAFT_EFFECT,
    HARVEST_MESSAGE,
    OPEN_BAG,
    BUFF_BAR,
    MID_MSG,
    NPC_SCRIPTABLE,
    NPC_SPECIAL_ACTION,
    NPC_SCRIPTABLEUP,
    SHOP_DISCOUNT,
    GET_CARD,
    CARD_SET,
    BOOK_STATS,
    ANDROID_SPAWN,
    ANDROID_MOVE,
    ANDROID_EMOTION,
    ANDROID_REMOVE,
    ANDROID_DEACTIVATED,
    PENDANT_SLOT,
    BOOK_INFO,
    PARTY_SEARCH,
    MEMBER_SEARCH,
    ARAN_COMBO_RECHARGE,
    R_MESOBAG_SUCCESS, //TODO use this
    R_MESOBAG_FAILURE, //TODO use this
    LOAD_GUILD_NAME,
    LOAD_GUILD_ICON,
    SPOUSE_CHAT, //TODO confirm
    MAP_BLOCKED, //TODO use this
    RESET_SCREEN, //TODO use this
    CHANGE_BACKGROUND, //TODO use this
    VISITOR,
    UPDATE_GENDER,
    REGISTER_FAMILIAR,
    SPAWN_FAMILIAR,
    MOVE_FAMILIAR,
    ATTACK_FAMILIAR,
    UPDATE_FAMILIAR,
    TOUCH_FAMILIAR,
    SIDEKICK_OPERATION,
    RESET_MINIMAP, //TODO use this
    ACHIEVEMENT_RATIO,
    CREATE_ULTIMATE,
    PROFESSION_INFO,
    QUICK_SLOT,
    BOOSTER_FAMILIAR,
    BOOSTER_PACK,
    FAMILIAR_INFO,
    PVP_INFO,
    PVP_SCOREBOARD,
    PVP_RESULT,
    PVP_ENABLED,
    PVP_MODE,
    PVP_TYPE,
    PVP_TEAM,
    PVP_SCORE,
    PVP_KILLED,
    PVP_SUMMON,
    PVP_ATTACK,
    PVP_POINTS,
    PVP_HP,
    PVP_MIST,
    PVP_ICEKNIGHT,
    PVP_COOL,
    PVP_ICEGAGE,
    PVP_TRANSFORM,
    LOAD_TEAM,
    CAPTURE_FLAGS,
    CAPTURE_POSITION,
    CAPTURE_RESET,
    CLEAR_MID_MSG,
    PUBLIC_NPC,
    PVP_BLOCKED,
    PLAY_MOVIE,
    PHANTOM_CARD,
    DIRECTION_INFO,
    DIRECTION_STATUS,
    GAIN_FORCE,
    CYGNUS_INTRO_ENABLE_UI,
    MONSTER_CARNIVAL_LEAVE,
    MONSTER_CARNIVAL_STATS,
    MONSTER_CARNIVAL_RESULT,
    MONSTER_CARNIVAL_RANKING,
    MOB_TO_MOB_DAMAGE,
    SKILL_EFFECT_MOB,
    ITEM_EFFECT_MOB,
    CYGNUS_ATTACK,
    SPECIAL_CREATION,
    PART_TIME,
    MONSTER_RESIST,
    PET_EXCEPTION_LIST,
    RPS_GAME,
    GET_SET_SECOND_PASSWORD,
    SECOND_PASSWORD_SET,
    CREATE_MENU,
    DAY_OF_CHRONOSPHERE,
    ERROR_CHRONOSPHERE;
    private short code = -2;

    @Override
    public void setValue(short code) {
        this.code = code;
    }

    @Override
    public short getValue() {
        //System.out.println("Packet to send: " + this.name() + " Value: " + this.code + "\r\nCaller: " + Thread.currentThread().getStackTrace()[2]);
        return code;
    }

    public static String nameOf(int value) {
        for (SendPacketOpcode opcode : SendPacketOpcode.values()) {
            if (opcode.getValue() == value) {
                return opcode.name();
            }
        }
        return "UNKNOWN";
    }

    public static boolean isSpamHeader(SendPacketOpcode opcode) {
        switch (opcode.name()) {
            case "WARP_TO_MAP":
            case "PING":
            case "NPC_ACTION":
//            case "AUTH_RESPONSE":
//            case "SERVERLIST":
            case "UPDATE_STATS":
            case "MOVE_PLAYER":
            case "SPAWN_NPC":
            case "SPAWN_NPC_REQUEST_CONTROLLER":
            case "REMOVE_NPC":
            case "MOVE_MONSTER":
            case "MOVE_MONSTER_RESPONSE":
            case "SPAWN_MONSTER":
            case "SPAWN_MONSTER_CONTROL":
//            case "HAKU_MOVE":
//            case "MOVE_SUMMON":
//            case "MOVE_FAMILIAR":
            case "ANDROID_MOVE":
//            case "INVENTORY_OPERATION":
//            case "MOVE_PET":
//            case "SHOW_SPECIAL_EFFECT":
//            case "DROP_ITEM_FROM_MAPOBJECT":
//            case "REMOVE_ITEM_FROM_MAP":
//            case "UPDATE_PARTYMEMBER_HP":
//            case "DAMAGE_PLAYER":
//            case "SHOW_MONSTER_HP":
//            case "CLOSE_RANGE_ATTACK":
//            case "RANGED_ATTACK":
//            case "ARAN_COMBO":
//            case "REMOVE_BG_LAYER":
//            case "SPECIAL_STAT":
//            case "TOP_MSG":
//            case "ANGELIC_CHANGE":
//            case "UPDATE_CHAR_LOOK":
//            case "KILL_MONSTER":
                return true;
            default:
                return false;
        }
    }

    public static final void reloadValues() {
        String fileName = "send.properties";
        Properties props = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(fileName); BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream, StringUtil.codeString(fileName)))) {
            props.load(br);
        } catch (IOException ex) {
            InputStream in = SendPacketOpcode.class.getClassLoader().getResourceAsStream("properties/" + fileName);
            if (in == null) {
                System.err.println("錯誤: 未加載 " + fileName + " 檔案");
                return;
            }
            try {
                props.load(in);
                in.close();
            } catch (IOException e) {
                throw new RuntimeException("加載 " + fileName + " 檔案出錯", e);
            }
        }
        ExternalCodeTableGetter.populateValues(props, values());
    }

    static {
        reloadValues();
    }
}
