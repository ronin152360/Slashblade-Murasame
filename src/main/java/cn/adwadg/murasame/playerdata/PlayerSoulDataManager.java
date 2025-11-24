package cn.adwadg.murasame.playerdata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class PlayerSoulDataManager {
    private static final Map<UUID, PlayerSoulData> playerDataMap = new HashMap<>();
    
    // 定义能力的资源位置
    public static final ResourceLocation PLAYER_SOUL_DATA_ID = new ResourceLocation("murasame", "player_soul_data");

    public static PlayerSoulData getOrCreatePlayerData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUUID(), uuid -> new PlayerSoulData());
    }

    public static PlayerSoulData getPlayerData(Player player) {
        return playerDataMap.get(player.getUUID());
    }

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Player> event) {
        // 为玩家附加能力
        PlayerSoulDataProvider provider = new PlayerSoulDataProvider();
        event.addCapability(PLAYER_SOUL_DATA_ID, provider);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player originalPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        
        PlayerSoulData originalData = playerDataMap.get(originalPlayer.getUUID());
        if (originalData != null) {
            PlayerSoulData newData = getOrCreatePlayerData(newPlayer);
            newData.copyFrom(originalData);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // 确保玩家数据存在
        getOrCreatePlayerData(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 可以选择保留数据或清理数据
        // 这里我们保留数据，以便玩家重新登录时保持设置
    }
    
    @SubscribeEvent
    public static void onPlayerSave(PlayerEvent.SaveToFile event) {
        Player player = event.getEntity();
        PlayerSoulData playerData = getPlayerData(player);
        if (playerData != null) {
            // 保存数据到玩家文件
            // 这里可以添加具体的保存逻辑
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLoad(PlayerEvent.LoadFromFile event) {
        Player player = event.getEntity();
        PlayerSoulData playerData = getOrCreatePlayerData(player);
        // 从玩家文件加载数据
        // 这里可以添加具体的加载逻辑
    }
    
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(PlayerSoulData.class);
        }
    }
}