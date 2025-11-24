package cn.adwadg.murasame.Entities;

import cn.adwadg.murasame.Murasame;
import cn.adwadg.murasame.client.utils.CombatTracker;
import com.mojang.datafixers.kinds.IdF;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

import static net.minecraft.util.Mth.lerp;

public class EntityMurasameSoul extends PathfinderMob {

    private int aliveTicks = 0;
    private double lastX = 0;
    private double lastY = 0;


    // 同步数据定义
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(
            EntityMurasameSoul.class,
            EntityDataSerializers.OPTIONAL_UUID
    );

    public static final EntityDataAccessor<Boolean> VISIBLE = SynchedEntityData.defineId(
            EntityMurasameSoul.class,
            EntityDataSerializers.BOOLEAN
    );

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Nullable
    private Player cachedOwner;
    public int hideCooldown = 0;



    public EntityMurasameSoul(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setNoAi(true);
        this.setSilent(true);
        this.setInvisible(false);
        this.noPhysics = true; // 添加物理禁用
        this.setNoGravity(true);
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.murasame.soul"); // 自定义名称
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(VISIBLE, true);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(OWNER_UUID).ifPresent(uuid ->
                tag.putUUID("OwnerUUID", uuid)
        );
        tag.putBoolean("Visible", this.entityData.get(VISIBLE));
        tag.putInt("HideCooldown", this.hideCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) {
            this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("OwnerUUID")));
        }
        if (tag.contains("Visible")) {
            this.entityData.set(VISIBLE, tag.getBoolean("Visible"));
        }
        if (tag.contains("HideCooldown")) {
            this.hideCooldown = tag.getInt("HideCooldown");
        }
    }

    public void setVisible(boolean visibility){
        this.setInvisible(!visibility);
        this.entityData.set(VISIBLE, visibility);
    }
    
    public boolean isVisible() {
        return this.entityData.get(VISIBLE);
    }

    // 添加持久化保护
    @Override
    public boolean isPersistenceRequired() {
        return true; // 防止被自然清除
    }

    // 修改tick方法
    @Override
    public void tick() {
        super.tick();

        aliveTicks++;

        // 只在服务端处理
        if (!this.level().isClientSide) {
            // Owner检查增强版

            if (this.cachedOwner == null) {
                this.entityData.get(OWNER_UUID).ifPresent(uuid -> {
                    Entity entity = ((ServerLevel)this.level()).getEntity(uuid);
                    if (entity instanceof Player player) {
                        this.cachedOwner = player;
                        Murasame.LOGGER.debug("强制绑定Owner: {}", player);
                    } else {
                        Murasame.LOGGER.warn("Owner实体不存在，准备移除");
                        this.discard();
                    }
                });
            }
            if (aliveTicks > 20) {
                if (this.cachedOwner == null || !this.cachedOwner.isAlive()) {
                    this.discard();
                    return;
                }

                // 严格距离检查（加入0.5格容差）
                if (this.distanceToSqr(this.cachedOwner) > 64.25) {
                    this.discard();
                    return;
                }
            }


            // 存活检查
            if (this.cachedOwner != null) {
                // 如果Owner死亡或距离过远(>64格)
                if (!this.cachedOwner.isAlive() ||
                        this.distanceToSqr(this.cachedOwner) > 4096) {
                    this.discard();
                    return;
                }
                if(cachedOwner.getXRot()!=lastX || cachedOwner.getY()!=lastY){
                    updatePosition();
                }
            }
        }

        /*if (cachedOwner != null && !CombatTracker.isInCombat(cachedOwner)) {
            this.setVisible(false);
        }else{
            this.setVisible(true);
        }*/
        if(cachedOwner != null){
            lastX = cachedOwner.getXRot();
            lastY = cachedOwner.getYRot();
        }
    }

    // 修改updatePosition方法
    private void updatePosition() {

        float distance = 3.5f;
        float horizontalOffset = -2.3f;

        // 计算位置
        Vec3 position = null;
        if (cachedOwner != null) {
            position = calculateLeftFrontPosition(cachedOwner, distance, horizontalOffset);
        }

        // 更新实体位置
        this.setPos(position.x, position.y, position.z);

        // 可选：让实体面朝玩家
        if (cachedOwner != null) {
            this.lookAt(EntityAnchorArgument.Anchor.EYES, cachedOwner.getEyePosition());
        }
        // 强制位置同步
        this.hasImpulse = true;
    }

    public Vec3 calculateLeftFrontPosition(Player player, float distance, float horizontalOffset) {
        // 获取玩家视角向量
        Vec3 lookVec = player.getLookAngle();

        // 计算左方向向量（视角向量绕Y轴旋转-90度）
        Vec3 leftVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();

        // 计算目标位置
        Vec3 frontOffset = lookVec.scale(distance); // 前方偏移
        Vec3 leftOffset = leftVec.scale(horizontalOffset); // 左侧偏移

        // 组合位置
        return player.getEyePosition()
                .add(frontOffset)
                .add(leftOffset)
                .subtract(0, this.getBbHeight() / 2, 0); // 调整高度使实体站在地面上
    }



    // 添加安全校验
    @Override
    public void setYRot(float yRot) {
        if (Float.isFinite(yRot)) {
            super.setYRot(yRot);
        } else {
            Murasame.LOGGER.error("尝试设置无效yRot: {}", yRot);
        }
    }

    @Override
    public void setXRot(float xRot) {
        if (Float.isFinite(xRot)) {
            super.setXRot(xRot);
        } else {
            Murasame.LOGGER.error("尝试设置无效xRot: {}", xRot);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        Murasame.LOGGER.warn("实体被移除 | 原因: {} | Tick: {} | 位置: {},{},{}",
                reason, aliveTicks, getX(), getY(), getZ());
        super.remove(reason);
    }


    public void setOwner(Player player) {
        this.cachedOwner = player;
        UUID uuid = player.getUUID();
        this.entityData.set(OWNER_UUID, Optional.of(uuid));
    }


    public void hideTemporarily(int ticks) {
        this.hideCooldown = ticks;
    }

    @Override
    public boolean is(Entity entity) {
        // 确保不会被误认为其他实体
        return this == entity;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (OWNER_UUID.equals(key)) {
            this.cachedOwner = null; // 强制重新获取owner
        }
        if (VISIBLE.equals(key)) {
            // 当可见性数据更新时，同步到实体状态
            this.setInvisible(!this.entityData.get(VISIBLE));
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        // 确保使用Forge的网络系统
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        // 强制同步关键数据
        this.setInvisible(!this.entityData.get(VISIBLE));
    }

    @Override
    public boolean isPickable() {
        return false; // 不可被选中
    }
    @Override
    public boolean isAttackable() {
        return false; // 不可被攻击
    }
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false; // 完全免疫伤害
    }
}
