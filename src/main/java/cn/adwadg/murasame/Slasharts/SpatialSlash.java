package cn.adwadg.murasame.Slasharts;

import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import java.util.List;
import java.util.WeakHashMap;

public class SpatialSlash {

    private static final WeakHashMap<LivingEntity, Vec3> frozenEntities = new WeakHashMap<>();
    private static final WeakHashMap<LivingEntity, Vec3> playerOriginalSpeeds = new WeakHashMap<>();

    public static void doSpatialSlash(LivingEntity entity) {
        if (entity.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) entity.level();
        Vec3 center = entity.position();

        // 获取8格半径内的所有生物
        AABB area = new AABB(center.x - 8, center.y - 4, center.z - 8,
                center.x + 8, center.y + 4, center.z + 8);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area);

        // 保存玩家原始速度
        playerOriginalSpeeds.put(entity, entity.getDeltaMovement());

        // 减缓玩家移动速度到40%
        Vec3 currentMovement = entity.getDeltaMovement();
        entity.setDeltaMovement(new Vec3(
                currentMovement.x * 0.4,
                currentMovement.y,
                currentMovement.z * 0.4
        ));

        // 固定周围生物
        for (LivingEntity target : entities) {
            if (target != entity && target.isAlive()) {
                frozenEntities.put(target, target.getDeltaMovement());
                target.setDeltaMovement(Vec3.ZERO);
            }
        }

        // 添加时间定格粒子特效
        SpatialParticleEffects.createTimeFreezeEffect(entity);
    }

    public static void releaseSpatialHold(LivingEntity entity) {
        if (entity.level().isClientSide()) return;

        // 添加时间恢复粒子特效
        SpatialParticleEffects.createTimeResumeEffect(entity);

        // 释放所有被固定的生物
        for (LivingEntity target : frozenEntities.keySet()) {
            if (target != null && target.isAlive()) {
                Vec3 originalMovement = frozenEntities.get(target);
                if (originalMovement != null) {
                    target.setDeltaMovement(originalMovement);
                }
                target.setNoGravity(false);
            }
        }
        frozenEntities.clear();

        // 恢复玩家速度
        Vec3 originalSpeed = playerOriginalSpeeds.get(entity);
        if (originalSpeed != null) {
            entity.setDeltaMovement(originalSpeed);
        }
        playerOriginalSpeeds.remove(entity);
    }

    public static void executeSingleSlash(LivingEntity entity, float angle, float power) {
        if (entity.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) entity.level();

        // 执行单道斩击，根据power调整威力
        AttackManager.doSlash(entity, angle, Vec3.ZERO, true, false, power);

        // 同步粒子效果 - 根据威力调整粒子强度
        int particleCount = (int) (power * 10);

        // 轻微屏幕震动效果（近距离）
        if (power >= 1.3F) {
        }
    }

}
