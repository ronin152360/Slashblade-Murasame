package cn.adwadg.murasame.playerdata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class PlayerSoulData {
    private boolean showMurasameSoul = true; // 默认显示丛雨实体

    public boolean shouldShowMurasameSoul() {
        return showMurasameSoul;
    }

    public void setShowMurasameSoul(boolean show) {
        this.showMurasameSoul = show;
    }

    public void copyFrom(PlayerSoulData source) {
        this.showMurasameSoul = source.showMurasameSoul;
    }

    public void saveNBTData(CompoundTag compound) {
        compound.putBoolean("ShowMurasameSoul", showMurasameSoul);
    }

    public void loadNBTData(CompoundTag compound) {
        showMurasameSoul = compound.getBoolean("ShowMurasameSoul");
    }
}