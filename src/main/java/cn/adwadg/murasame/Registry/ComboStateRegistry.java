package cn.adwadg.murasame.Registry;

import cn.adwadg.murasame.Murasame;
import cn.adwadg.murasame.Slasharts.SpatialSlash;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ComboStateRegistry {
    public static final DeferredRegister<ComboState> COMBO_STATES =
            DeferredRegister.create(ComboState.REGISTRY_KEY, Murasame.MOD_ID);

    public static final RegistryObject<ComboState> SPATIAL_SLASH = COMBO_STATES.register("spatial_slash",
            ComboState.Builder.newInstance()
                    .startAndEnd(0, 60)  // 缩短为60帧，更紧凑
                    .priority(60)
                    .speed(1.5F)  // 进一步加快动画速度
                    .next(entity -> SlashBlade.prefix("none"))
                    .nextOfTimeout(entity -> SlashBlade.prefix("none"))

                    // 立即开始空间固定和第一轮斩击
                    .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                            .put(0, SpatialSlash::doSpatialSlash) // 立即空间固定
                            .put(0, (entityIn) -> entityIn.setDeltaMovement(Vec3.ZERO)) // 完全停止移动
                            .put(0, (entity) -> SpatialSlash.executeSingleSlash(entity, 0, 1.0F)) // 立即第1道斩击
                            .build())

                    // 连续斩击序列 - 从第0帧立即开始
                    .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                            // 第一轮快速斩击 (0-10帧)
                            .put(2, (entity) -> SpatialSlash.executeSingleSlash(entity, 72, 1.1F))
                            .put(4, (entity) -> SpatialSlash.executeSingleSlash(entity, 144, 1.2F))
                            .put(6, (entity) -> SpatialSlash.executeSingleSlash(entity, 216, 1.1F))
                            .put(8, (entity) -> SpatialSlash.executeSingleSlash(entity, 288, 1.0F))

                            // 第二轮强力斩击 (10-20帧)
                            .put(10, (entity) -> SpatialSlash.executeSingleSlash(entity, 36, 1.3F))
                            .put(12, (entity) -> SpatialSlash.executeSingleSlash(entity, 108, 1.4F))
                            .put(14, (entity) -> SpatialSlash.executeSingleSlash(entity, 180, 1.5F))
                            .put(16, (entity) -> SpatialSlash.executeSingleSlash(entity, 252, 1.4F))
                            .put(18, (entity) -> SpatialSlash.executeSingleSlash(entity, 324, 1.3F))
                            .build())

                    // 收招和恢复 - 斩击结束后立即开始
                    .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                            .put(25, SpatialSlash::releaseSpatialHold) // 提前释放空间固定
                            .put(40, (entityIn) -> {
                                // 快速恢复移动能力
                                Vec3 current = entityIn.getDeltaMovement();
                                entityIn.setDeltaMovement(current.scale(2.0));
                            })
                            .build())

                    // 命中效果
                    .addHitEffect((target, attacker) -> {
                        float distance = target.distanceTo(attacker);
                        int stunTime = distance < 3 ? 40 : 25;
                        StunManager.setStun(target, stunTime);
                    })
                    ::build);


}
