package cn.adwadg.murasame.Registry;

import cn.adwadg.murasame.Murasame;
import cn.adwadg.murasame.Specialeffects.MurasameBlessing;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Murasame.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SERegistry {
    public static DeferredRegister<SpecialEffect> REGISTRY_KEY2;
    public static RegistryObject<SpecialEffect> MURASAME_BLESSING;
    public SERegistry(){

    }
    // 延迟初始化方法
    /*public static void init() {
        // 安全检查
        if (SpecialEffect.REGISTRY_KEY == null) {
            Murasame.LOGGER.error("SpecialEffect registry key is null! Slashblade mod may not be loaded properly.");
            return;
        }

        REGISTRY_KEY2 = DeferredRegister.create(SpecialEffect.REGISTRY_KEY, Murasame.MOD_ID);
        MURASAME_BLESSING = REGISTRY_KEY2.register("murasame_blessing", MurasameBlessing::new);
    }*/

    // 在模组事件中初始化
    /*@SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                init();
                Murasame.LOGGER.info("Special Effects registry initialized successfully");
            } catch (Exception e) {
                Murasame.LOGGER.error("Failed to initialize Special Effects registry", e);
            }
        });
    }*/
    static {
        REGISTRY_KEY2 = DeferredRegister.create(SpecialEffect.REGISTRY_KEY, Murasame.MOD_ID);
        MURASAME_BLESSING = REGISTRY_KEY2.register("murasame_blessing", MurasameBlessing::new);
    }
}
