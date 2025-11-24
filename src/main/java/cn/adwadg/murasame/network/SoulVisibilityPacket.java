package cn.adwadg.murasame.network;

import cn.adwadg.murasame.Entities.EntityMurasameSoul;
import cn.adwadg.murasame.events.PlayerSoulHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class SoulVisibilityPacket {
    private final int entityId;
    private final boolean visible;

    public SoulVisibilityPacket(int entityId, boolean visible) {
        this.entityId = entityId;
        this.visible = visible;
    }

    public SoulVisibilityPacket(EntityMurasameSoul soul, boolean visible) {
        this.entityId = soul.getId();
        this.visible = visible;
    }

    public static void encode(SoulVisibilityPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.entityId);
        buffer.writeBoolean(msg.visible);
    }

    public static SoulVisibilityPacket decode(FriendlyByteBuf buffer) {
        return new SoulVisibilityPacket(buffer.readInt(), buffer.readBoolean());
    }

    public static void handle(SoulVisibilityPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 在客户端处理可见性更新
            if (Minecraft.getInstance().level != null) {
                Entity entity = Minecraft.getInstance().level.getEntity(msg.entityId);
                if (entity instanceof EntityMurasameSoul soul) {
                    soul.setVisible(msg.visible);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}