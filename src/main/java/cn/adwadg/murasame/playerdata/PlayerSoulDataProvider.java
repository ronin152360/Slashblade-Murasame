package cn.adwadg.murasame.playerdata;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerSoulDataProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<PlayerSoulData> PLAYER_SOUL_DATA = CapabilityManager.get(new CapabilityToken<PlayerSoulData>() {});
    
    private PlayerSoulData playerSoulData = null;
    private final LazyOptional<PlayerSoulData> optionalData = LazyOptional.of(this::createPlayerSoulData);
    
    private PlayerSoulData createPlayerSoulData() {
        if (playerSoulData == null) {
            playerSoulData = new PlayerSoulData();
        }
        return playerSoulData;
    }
    
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == PLAYER_SOUL_DATA) {
            return optionalData.cast();
        }
        return LazyOptional.empty();
    }
    
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createPlayerSoulData().saveNBTData(nbt);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createPlayerSoulData().loadNBTData(nbt);
    }
}