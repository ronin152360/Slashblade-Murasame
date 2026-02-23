package cn.adwadg.tsukikage;

import com.mojang.logging.LogUtils;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod(TsukikageMod.MOD_ID)
public class TsukikageMod {
    public static final String MOD_ID = "tsukikage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TsukikageMod() {
        MinecraftForge.EVENT_BUS.register(FollowerHandler.class);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class FollowerHandler {
        private static final Map<UUID, ArmorStand> FOLLOWERS = new HashMap<>();

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
                return;
            }
            if (player.tickCount % 3 != 0) {
                return;
            }

            boolean holdingAwakened = isHoldingAwakenedTsukikage(player.getMainHandItem());
            ArmorStand stand = FOLLOWERS.get(player.getUUID());

            if (holdingAwakened) {
                if (stand == null || !stand.isAlive()) {
                    stand = spawnFollower(player);
                    if (stand != null) {
                        FOLLOWERS.put(player.getUUID(), stand);
                    }
                } else {
                    moveFollower(player, stand);
                }
            } else if (stand != null) {
                stand.discard();
                FOLLOWERS.remove(player.getUUID());
            }
        }

        @SubscribeEvent
        public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            removeFollower(event.getEntity().getUUID());
        }

        @SubscribeEvent
        public static void onClone(PlayerEvent.Clone event) {
            removeFollower(event.getOriginal().getUUID());
        }

        private static boolean isHoldingAwakenedTsukikage(ItemStack stack) {
            return stack.getItem() instanceof ItemSlashBlade
                    && stack.getCapability(ItemSlashBlade.BLADESTATE)
                    .map(state -> "item.tsukikage.tsukikage_awakened".equals(state.getTranslationKey()))
                    .orElse(false);
        }

        private static ArmorStand spawnFollower(ServerPlayer player) {
            Level level = player.level();
            ArmorStand stand = new ArmorStand(level, player.getX(), player.getY(), player.getZ());
            stand.setNoGravity(true);
            stand.setInvulnerable(true);
            stand.setMarker(true);
            stand.setInvisible(false);
            stand.setSilent(true);
            stand.setCustomNameVisible(true);
            stand.setCustomName(Component.literal("Tsukikage"));
            moveFollower(player, stand);
            return level.addFreshEntity(stand) ? stand : null;
        }

        private static void moveFollower(ServerPlayer player, ArmorStand stand) {
            Vec3 look = player.getLookAngle().normalize();
            Vec3 left = new Vec3(-look.z, 0, look.x).normalize();
            Vec3 target = player.getEyePosition()
                    .add(look.scale(3.0))
                    .add(left.scale(-2.0))
                    .subtract(0, 1.2, 0);

            stand.teleportTo(target.x, target.y, target.z);
            stand.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
        }

        private static void removeFollower(UUID id) {
            ArmorStand stand = FOLLOWERS.remove(id);
            if (stand != null && stand.isAlive()) {
                stand.discard();
            }
        }
    }
}
