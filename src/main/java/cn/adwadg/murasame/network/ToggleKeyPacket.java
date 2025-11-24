package cn.adwadg.murasame.network;

import cn.adwadg.murasame.events.PlayerSoulHandler;
import cn.adwadg.murasame.playerdata.PlayerSoulDataManager;
import cn.adwadg.murasame.playerdata.PlayerSoulData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class ToggleKeyPacket {
    // 无参数构造函数
    public ToggleKeyPacket() {
    }

    public static void encode(ToggleKeyPacket msg, FriendlyByteBuf buffer) {
        // 无数据需要编码
    }

    public static ToggleKeyPacket decode(FriendlyByteBuf buffer) {
        return new ToggleKeyPacket();
    }

    public static void handle(ToggleKeyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // 获取当前玩家设置并切换状态
                PlayerSoulData playerData = PlayerSoulDataManager.getOrCreatePlayerData(player);
                boolean currentState = playerData.shouldShowMurasameSoul();
                boolean newState = !currentState;
                
                // 更新玩家个人设置
                playerData.setShowMurasameSoul(newState);
                
                // 强制更新玩家的实体状态（销毁或重新创建）
                PlayerSoulHandler.updateSoulState(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}