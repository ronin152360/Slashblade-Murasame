package cn.adwadg.murasame.Registry;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;

public class KeyBindings {
    @OnlyIn(Dist.CLIENT)
    public static KeyMapping TOGGLE_KEY;

    @OnlyIn(Dist.CLIENT)
    public static void init() {
        TOGGLE_KEY = new KeyMapping(
                "key.murasame.toggle_soul",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_R,
                "category.murasame"
        );
    }
}
