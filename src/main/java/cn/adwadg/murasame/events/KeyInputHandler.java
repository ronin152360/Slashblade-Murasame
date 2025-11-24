package cn.adwadg.murasame.events;

import cn.adwadg.murasame.Registry.KeyBindings;
import cn.adwadg.murasame.Murasame;
import cn.adwadg.murasame.network.NetworkHandler;
import cn.adwadg.murasame.network.ToggleKeyPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = Murasame.MOD_ID, value = Dist.CLIENT)
public class KeyInputHandler {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (KeyBindings.TOGGLE_KEY.consumeClick()) {
            // 发送切换请求到服务端
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.SERVER.noArg(),
                    new ToggleKeyPacket()
            );
        }
    }
}
