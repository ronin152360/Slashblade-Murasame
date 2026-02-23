package cn.adwadg.murasame.events;

import cn.adwadg.murasame.Murasame;
import cn.adwadg.murasame.Registry.KeyBindings;
import cn.adwadg.murasame.Registry.SoundRegistry;
import cn.adwadg.murasame.playerdata.PlayerSoulDataManager;
import cn.adwadg.murasame.playerdata.PlayerSoulData;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Murasame.MOD_ID)
public class PlayerEventHandler {
    private static final Map<UUID, ItemStack> lastHeldBlades = new HashMap<>();
    private static final Random random = new Random();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {

            checkWeaponSwitch(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            lastHeldBlades.put(player.getUUID(), ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            lastHeldBlades.remove(player.getUUID());
        }
    }



    private static void checkWeaponSwitch(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ItemStack currentBlade = findAwakenedMurasame(player);
        ItemStack lastBlade = lastHeldBlades.getOrDefault(playerId, ItemStack.EMPTY);


        // 检查是否从非觉醒状态切换到觉醒状态，或反之
        boolean wasHolding = isAwakenedMurasame(lastBlade);
        boolean isHolding = isAwakenedMurasame(currentBlade);


        if (wasHolding != isHolding) {
            // 计算粒子生成位置（左前方3.5格，左2.3格）
            Vec3 lookVec = player.getLookAngle().normalize();
            Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();

            double x = player.getX() + lookVec.x * 3.5 - rightVec.x * 2.3;
            double y = player.getY() + player.getEyeHeight()+ 0.5 + lookVec.y * 3.5;
            double z = player.getZ() + lookVec.z * 3.5 - rightVec.z * 2.3;

            // 使用玩家特定的设置而不是全局设置
            PlayerSoulData playerData = PlayerSoulDataManager.getOrCreatePlayerData(player);
            if(playerData.shouldShowMurasameSoul()){
                spawnSpawnEffect(player.serverLevel(), x, y, z);
            }

            final SoundEvent[] SWITCH_SOUNDS = {
                    SoundRegistry.MURASAME_SWITCH.get(),
                    SoundRegistry.MURASAME_SWITCH2.get(),
                    SoundRegistry.MURASAME_SWITCH3.get(),
                    SoundRegistry.MURASAME_SWITCH4.get(),
                    SoundRegistry.MURASAME_SWITCH5.get(),
                    SoundRegistry.MURASAME_SWITCH6.get(),
                    SoundRegistry.MURASAME_SWITCH7.get(),
                    SoundRegistry.MURASAME_SWITCH8.get(),
                    SoundRegistry.MURASAME_SWITCH9.get()

            };

            SoundEvent sound = SWITCH_SOUNDS[random.nextInt(SWITCH_SOUNDS.length)];

            if(isHolding){
                Murasame.playSound(player, sound);
            }
            // 使用玩家特定的设置而不是全局设置
            if(playerData.shouldShowMurasameSoul()){
                Murasame.playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP);
            }
        }
        lastHeldBlades.put(playerId, currentBlade);
    }

    private static ItemStack findAwakenedMurasame(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (isAwakenedMurasame(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isAwakenedMurasame(ItemStack stack) {
        return stack.getItem() instanceof ItemSlashBlade &&
                stack.getCapability(ItemSlashBlade.BLADESTATE)
                        .map(state -> "item.murasame.tsukikage_awakened".equals(state.getTranslationKey()))
                        .orElse(false);
    }

    public static void spawnSpawnEffect(Level world, double x, double y, double z) {
        if (!world.isClientSide) {
            for (int i = 0; i < 36; i++) { // 36个方向
                double angle = Math.toRadians(i * 10);
                double speed = 0.4;

                double dx = Math.cos(angle) * speed;
                double dz = Math.sin(angle) * speed;

                ((ServerLevel)world).sendParticles(
                        ParticleTypes.POOF,
                        x, y, z,  // 生成位置
                        4,  // 每个方向5个粒子
                        dx, 0.05, dz,  // 粒子运动方向
                        0.1  // 速度变化范围
                );
            }
        }
    }
}
