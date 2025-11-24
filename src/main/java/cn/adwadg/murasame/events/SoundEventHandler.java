package cn.adwadg.murasame.events;

import cn.adwadg.murasame.Murasame;
import cn.adwadg.murasame.Registry.SoundRegistry;
import cn.adwadg.murasame.client.utils.MurasameEvolutionTracker;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Murasame.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SoundEventHandler {
    private static final Random random = new Random();
    private static final Map<UUID, Boolean> wasInCombat = new HashMap<>();
    private static final Map<UUID, Integer> lowHpCooldown = new HashMap<>();
    private static final Map<UUID, Integer> frightenedCooldown = new HashMap<>();
    private static final Map<UUID, Vec3> lastPlayerPositions = new HashMap<>();
    private static final Map<UUID, Integer> afkCooldowns = new HashMap<>();

    // 检查玩家是否持有觉醒丛雨丸
    private static boolean isHoldingAwakenedMurasame(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof ItemSlashBlade) {
                return stack.getCapability(ItemSlashBlade.BLADESTATE)
                        .map(state -> "item.murasame.murasamemaru_awakened".equals(state.getTranslationKey()))
                        .orElse(false);
            }
        }
        return false;
    }

    // 播放音效的辅助方法（仅服务器端）


    // 1. 玩家起床时播放（随机选择早晨音效）
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        if (isHoldingAwakenedMurasame(player)) {
            SoundEvent sound = random.nextBoolean() ?
                    SoundRegistry.MURASAME_MORNING.get() :
                    SoundRegistry.MURASAME_MORNING2.get();
            Murasame.playSound(player, sound);
        }
        if (!event.updateLevel()) {
            //player.sendSystemMessage(Component.translatable("message.murasame.sleepwithblade"));
            checkHands(player);
        }
    }

    private static void checkHands(Player player) {
        checkHand(player, InteractionHand.MAIN_HAND);
        checkHand(player, InteractionHand.OFF_HAND);
    }

    private static void checkHand(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (MurasameEvolutionTracker.isValidBlade(stack)) {
            MurasameEvolutionTracker.checkEvolution(player, stack);
        }
    }

    // 2. 玩家睡觉时播放
    @SubscribeEvent
    public static void onPlayerSleep(PlayerSleepInBedEvent event) {
        Player player = event.getEntity();
        Level level = player.level();
        long time = level.getDayTime() % 24000;
        if(time >= 13000 && time < 23000){
            if (event.getResultStatus() == null && isHoldingAwakenedMurasame(player)) {
                Murasame.playSound(player, SoundRegistry.MURASAME_NIGHT.get());
            }
        }
    }

    // 3. 玩家血量小于50%时播放（带冷却）
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        UUID playerId = player.getUUID();


        if (isHoldingAwakenedMurasame(player)) {
            // 低血量检测
            if (player.getHealth() < player.getMaxHealth() * 0.4f) {
                int cooldown = lowHpCooldown.getOrDefault(playerId, 0);
                if (cooldown <= 0) {
                    Murasame.playSound(player, SoundRegistry.MURASAME_LOWHP.get());
                    lowHpCooldown.put(playerId, 600); // 30秒冷却（20 ticks/秒）
                } else {
                    lowHpCooldown.put(playerId, cooldown - 1);
                }
            } else {
                lowHpCooldown.remove(playerId);
            }

            // 7. 受惊吓音效（血量<20%且脱离战斗）
            boolean inCombat = player.getLastHurtByMob() != null &&
                    player.tickCount - player.getLastHurtByMobTimestamp() < 100;

            if (player.getHealth() < player.getMaxHealth() * 0.2f &&
                    wasInCombat.getOrDefault(playerId, false) &&
                    !inCombat) {

                int cooldown = frightenedCooldown.getOrDefault(playerId, 0);
                if (cooldown <= 0) {
                    Murasame.playSound(player, SoundRegistry.MURASAME_FRIGHTENED.get());
                    frightenedCooldown.put(playerId, 1200); // 60秒冷却
                }
            }
            wasInCombat.put(playerId, inCombat);
        }
        checkAfkState(player);
        // 新增武器切换检测
    }

    private static void checkAfkState(Player player) {
        // 只在持有觉醒丛雨丸时检测AFK
        if (!isHoldingAwakenedMurasame(player)) {
            player.getPersistentData().putInt("MurasameAfkTicks", 0); // 重置计时
            return;
        }
        UUID playerId = player.getUUID();
        Vec3 currentPos = player.position();
        Vec3 lastPos = lastPlayerPositions.get(playerId);
        // 更新位置记录
        if (!currentPos.equals(lastPos)) {
            lastPlayerPositions.put(playerId, currentPos);
            player.getPersistentData().putInt("MurasameAfkTicks", 0);
            return;
        }
        // 增加AFK计时
        int afkTicks = player.getPersistentData().getInt("MurasameAfkTicks") + 1;
        player.getPersistentData().putInt("MurasameAfkTicks", afkTicks);
        // 10秒(200 ticks)未移动且冷却结束
        if (afkTicks >= 200 && afkCooldowns.getOrDefault(playerId, 0) <= 0) {
            SoundEvent sound = random.nextBoolean() ?
                    SoundRegistry.MURASAME_AFK.get() :
                    SoundRegistry.MURASAME_AFK2.get();

            Murasame.playSound(player, sound);
            afkCooldowns.put(playerId, 600); // 30秒冷却
            player.getPersistentData().putInt("MurasameAfkTicks", 0);
        }
        // 冷却计时更新
        afkCooldowns.computeIfPresent(playerId, (k, v) -> v > 0 ? v - 1 : 0);
    }


    // 4. 丢弃武器音效（使用ItemTossPostEvent）
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = event.getEntity().getItem();

        if (stack.getItem() instanceof ItemSlashBlade &&
                stack.getCapability(ItemSlashBlade.BLADESTATE)
                        .map(state -> "item.murasame.murasamemaru_awakened".equals(state.getTranslationKey()))
                        .orElse(false)) {
            Murasame.playSound(player, SoundRegistry.MURASAME_DROP.get());
        }
    }

    // 5. 击杀Boss音效（血量≥300）
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity source = event.getSource().getEntity();
        if (source instanceof Player player &&
                isHoldingAwakenedMurasame(player) &&
                event.getEntity().getMaxHealth() >= 300) {
            Murasame.playSound(player, SoundRegistry.MURASAME_AFTER_BOSS.get());
        }
    }

    // 6. 重生安慰音效
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (isHoldingAwakenedMurasame(player)) {
            Murasame.playSound(player, SoundRegistry.MURASAME_COMFORT.get());
        }
    }
}
