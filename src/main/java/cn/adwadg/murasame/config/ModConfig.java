package cn.adwadg.murasame.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ModConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Integer> GAINKILLNEEDED;
    public static final ForgeConfigSpec.ConfigValue<Integer> GAINLEVELNEEDED;
    public static final ForgeConfigSpec.ConfigValue<Integer> GAINEFFECTSNEEDED;
    public static final ForgeConfigSpec.ConfigValue<Integer> EVKILLNEEDED;
    public static final ForgeConfigSpec.ConfigValue<Integer> EVPSNEEDED;
    public static final ForgeConfigSpec.ConfigValue<Boolean> GAINCLEANLEVEL;

    static {
        BUILDER.push("general");
        GAINKILLNEEDED = BUILDER
                .comment("拔刀需求亡灵击杀数|0~99999")
                .defineInRange("killNeeded",500,0,99999);

        GAINLEVELNEEDED = BUILDER
                .comment("拔刀所需经验等级|0~500")
                .defineInRange("levelNeeded",50,0,500);

        GAINEFFECTSNEEDED = BUILDER
                .comment("拔刀所需正面效果数|0~24")
                .defineInRange("effectsNeeded",5,0,24);

        GAINCLEANLEVEL = BUILDER
                .comment("拔刀时是否清除对应等级经验")
                .define("gainCleanLevel",true);

        EVKILLNEEDED = BUILDER
                .comment("进化所需击杀数|0~99999")
                .defineInRange("evKillNeeded",1000,0,99999);

        EVPSNEEDED = BUILDER
                .comment("进化所需耀魂数|0~99999")
                .defineInRange("evPSNeeded",50000,0,99999);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
