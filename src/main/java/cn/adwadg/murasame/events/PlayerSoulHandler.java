package cn.adwadg.murasame.events;

import cn.adwadg.murasame.Murasame;
import cn.adwadg.murasame.Registry.ModEntities;
import cn.adwadg.murasame.Entities.EntityMurasameSoul;
import cn.adwadg.murasame.playerdata.PlayerSoulDataManager;
import cn.adwadg.murasame.playerdata.PlayerSoulData;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = Murasame.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerSoulHandler {
    private static final Map<UUID, EntityMurasameSoul> soulMap = new HashMap<>();

    private static void spawnSoulEntity(Player player) {
        try {
            // 检查已有实体
            EntityMurasameSoul existing = soulMap.get(player.getUUID());
            if (existing != null && existing.isAlive()) {
                return; // 如果已有实体，不再创建新实体
            }
            
            PlayerSoulData playerData = PlayerSoulDataManager.getOrCreatePlayerData(player);
            // 如果玩家设置为隐藏，则不创建实体
            if (!playerData.shouldShowMurasameSoul()) {
                return;
            }
            
            ServerLevel level = (ServerLevel) player.level();
            EntityMurasameSoul soul = new EntityMurasameSoul(ModEntities.MURASAME_SOUL.get(), level);

            float distance = 3.5f;
            float horizontalOffset = -2.3f;

            // 计算位置
            Vec3 position = null;
            if (player != null) {
                position = soul.calculateLeftFrontPosition(player, distance, horizontalOffset);
            }

            // 更新实体位置
            soul.setPos(position.x, position.y, position.z);

            // 可选：让实体面朝玩家
            if (player != null) {
                soul.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
            }

            // 必须最后设置Owner
            soul.setOwner(player);

            if (level.addFreshEntity(soul)) {
                soulMap.put(player.getUUID(), soul);
            }
        } catch (Exception e) {
            Murasame.LOGGER.error("生成异常", e);
        }
    }

    public static void removeSoul(Player player) {
        EntityMurasameSoul soul = soulMap.remove(player.getUUID());
        if (soul != null && soul.isAlive()) {
            Murasame.LOGGER.debug("removed player soul: {}",player.getId());
            soul.discard();
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        removeSoul(player);
    }
    
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event){
        Player player = event.getEntity();
        removeSoul(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 使用玩家特定的设置而不是全局设置
        PlayerSoulData playerData = PlayerSoulDataManager.getOrCreatePlayerData(event.player);
        boolean showMurasameSoul = playerData.shouldShowMurasameSoul();
        
        if (event.phase == TickEvent.Phase.START &&
                event.player instanceof ServerPlayer player) {
            // 每3 tick检测一次（更快响应）
            if (player.tickCount % 3 == 0) {
                ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
                boolean shouldHave = false;
                if(stack.getItem() instanceof ItemSlashBlade && stack.getCapability(ItemSlashBlade.BLADESTATE)
                        .map(state -> "item.murasame.murasamemaru_awakened".equals(state.getTranslationKey()))
                        .orElse(false)){
                    shouldHave = true;
                }
                EntityMurasameSoul soul = soulMap.get(player.getUUID());

                if (shouldHave && showMurasameSoul) {
                    if (soul == null || !soul.isAlive()) {
                        spawnSoulEntity(player);
                    }
                } else {
                    // 如果玩家没有合适的剑或选择了隐藏，则移除实体
                    if (soul != null) {
                        removeSoul(player);
                    }
                }
            }
        }
    }
    
    // 添加一个公共方法来强制更新特定玩家的实体状态
    public static void updateSoulState(Player player) {
        PlayerSoulData playerData = PlayerSoulDataManager.getOrCreatePlayerData(player);
        boolean showMurasameSoul = playerData.shouldShowMurasameSoul();
        
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean shouldHave = false;
        if(stack.getItem() instanceof ItemSlashBlade && stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> "item.murasame.murasamemaru_awakened".equals(state.getTranslationKey()))
                .orElse(false)){
            shouldHave = true;
        }
        
        EntityMurasameSoul soul = soulMap.get(player.getUUID());
        
        if (shouldHave && showMurasameSoul) {
            // 需要显示实体但实体不存在时创建
            if (soul == null || !soul.isAlive()) {
                spawnSoulEntity(player);
            }
        } else {
            // 不需要显示实体时移除
            if (soul != null) {
                removeSoul(player);
            }
        }
    }
}