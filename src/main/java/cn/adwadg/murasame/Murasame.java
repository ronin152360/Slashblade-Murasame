package cn.adwadg.murasame;


import cn.adwadg.murasame.Registry.*;
import cn.adwadg.murasame.Specialeffects.MurasameBlessing;
import cn.adwadg.murasame.client.model.ModModelProvider;
import cn.adwadg.murasame.client.renderer.EmbeddedBladeStoneRenderer;
import cn.adwadg.murasame.client.renderer.MurasameSoulRenderer;
import cn.adwadg.murasame.data.ModAdvancementProvider;
import cn.adwadg.murasame.data.ModWorldGenProvider;
import cn.adwadg.murasame.network.NetworkHandler;
import cn.adwadg.murasame.playerdata.PlayerSoulDataManager;
import com.mojang.logging.LogUtils;
import mods.flammpfeil.slashblade.SlashBladeCreativeGroup;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mod(Murasame.MOD_ID)
public class Murasame {
    public static final String MOD_ID = "murasame";
    public static final Logger LOGGER = LogUtils.getLogger();
    // 添加创造模式标签栏注册器
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    // 注册Murasame创造模式标签栏
    public static final RegistryObject<CreativeModeTab> MURASAME_TAB = CREATIVE_MODE_TABS.register("murasame_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.murasame_tab"))
                    .withTabsBefore(SlashBladeCreativeGroup.SLASHBLADE_GROUP.getId())
                    .icon(() -> {
                        ItemStack stack = new ItemStack(mods.flammpfeil.slashblade.init.SBItems.slashblade);
                        stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(s -> {
                            s.setModel(new ResourceLocation(MOD_ID, "models/murasame/murasamemaru_awakened.obj"));
                            s.setTexture(new ResourceLocation(MOD_ID, "models/murasame/murasamemaru_awakened.png"));
                        });
                        return stack;
                    })
                    .displayItems((parameters, output) -> {
                        fillBlades(output);
                        // 添加其他模组物品到创造标签栏
                        output.accept(ModBlocks.EMBEDDED_STONE_ITEM.get());
                        output.accept(ModBlocks.EMPTY_STONE_ITEM.get());
                        // 可以继续添加其他物品
                    })
                    .build());
    private static void fillBlades(CreativeModeTab.Output output) {
        if (Minecraft.getInstance().getConnection() != null) {
            BladeModelManager.getClientSlashBladeRegistry()
                    .entrySet().stream()
                    // 过滤空值
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    // 解析字符串为ResourceLocation并过滤命名空间
                    .filter(entry -> {
                        ResourceLocation loc = ResourceLocation.tryParse(entry.getKey().location().toString());
                        return loc != null && loc.getNamespace().equals(MOD_ID);
                    })
                    // 按字符串键排序
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        LOGGER.info("Registering Murasame Slashblade: {}", entry.getKey());
                        output.accept(entry.getValue().getBlade());
                    });
        }
    }
    public Murasame() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // 注册创造模式标签栏
        CREATIVE_MODE_TABS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        // 注册玩家数据管理器
        MinecraftForge.EVENT_BUS.register(PlayerSoulDataManager.class);

        SERegistry.REGISTRY_KEY2.register(modEventBus);
        ComboStateRegistry.COMBO_STATES.register(modEventBus);
        SlashArtsRegistry.SLASH_ARTS.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, cn.adwadg.murasame.config.ModConfig.SPEC);

        SoundRegistry.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);  // 先注册方块
        ModEntities.ENTITIES.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);   // 然后注册方块物品
        ModEntities.BLOCK_ENTITIES.register(modEventBus); // 接着注册方块实体
        ModItems.ITEMS.register(modEventBus);    // 最后注册其他物品
        ModEntities.registerAttributes(FMLJavaModLoadingContext.get().getModEventBus());
        modEventBus.addListener(ModModelProvider::registerLayers);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onGatherData);
        modEventBus.addListener(this::onCommonSetup);

    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(SoundRegistry::logRegistration);
        NetworkHandler.init();
    }

    private void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> registries = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        if (event.includeServer()) {
            // 注册成就数据生成器
            generator.addProvider(event.includeServer(),
                    new ModAdvancementProvider(output, registries, existingFileHelper));
            generator.addProvider(event.includeServer(),
                    new ModWorldGenProvider(output, registries, existingFileHelper));
        }
    }




    private void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Client setup started");

        if (!ModEntities.MURASAME_SOUL.isPresent()) {
            LOGGER.error("Murasame Soul entity not registered!");
            return;
        }

        event.enqueueWork(() -> {
            try {
                EntityRenderers.register(ModEntities.MURASAME_SOUL.get(), MurasameSoulRenderer::new);
                Murasame.LOGGER.info("Murasame Soul renderer registered successfully");
            } catch (Exception e) {
                Murasame.LOGGER.error("Failed to register Murasame Soul renderer", e);
            }
            LOGGER.info("Registering block entity renderer");
            BlockEntityRenderers.register(ModEntities.EMBEDDED_STONE_ENTITY.get(),
                    EmbeddedBladeStoneRenderer::new);
        });
    }

    public static void playSound(Player player, SoundEvent sound) {
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.serverLevel().playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    sound,
                    player.getSoundSource(),
                    1.0F, 1.0F
            );
        }
    }


    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class KeyRegistration {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            KeyBindings.init(); // 初始化客户端按键
            event.register(KeyBindings.TOGGLE_KEY);
        }
    }
}
