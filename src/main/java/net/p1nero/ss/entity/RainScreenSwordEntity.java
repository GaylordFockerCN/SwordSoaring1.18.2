package net.p1nero.ss.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.p1nero.ss.SwordSoaring;
import net.p1nero.ss.capability.SSCapabilityProvider;
import net.p1nero.ss.capability.SSPlayer;
import yesman.epicfight.world.item.LongswordItem;
import yesman.epicfight.world.item.TachiItem;
import yesman.epicfight.world.item.UchigatanaItem;

import java.util.Optional;
import java.util.UUID;

/**
 * 都用EntityDataAccessor了还继承有点没必要..但是不知道怎么抛弃EntityDataAccessor
 */
public class RainScreenSwordEntity extends SwordEntity {

    private static final EntityDataAccessor<Optional<UUID>> RIDER_UUID = SynchedEntityData.defineId(
        RainScreenSwordEntity.class,
        EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<ItemStack> ITEM_STACK =
        SynchedEntityData.defineId(RainScreenSwordEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> RAIN_SCREEN_SWORD_ID = SynchedEntityData.defineId(
        RainScreenSwordEntity.class,
        EntityDataSerializers.INT
    );

    public RainScreenSwordEntity(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
        this.getEntityData().define(ITEM_STACK, ItemStack.EMPTY);
        this.getEntityData().define(RIDER_UUID, Optional.empty());
        this.getEntityData().define(RAIN_SCREEN_SWORD_ID, -1);
    }

    public RainScreenSwordEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.RAIN_SCREEN_SWORD.get(), level);
    }

    @Override
    public ItemStack getItemStack() {
        return this.getEntityData().get(ITEM_STACK);
    }

    @Override
    public void setItemStack(ItemStack itemStack) {
        this.getEntityData().set(ITEM_STACK, itemStack);
    }

    @Override
    public void setRider(Player rider) {
        this.rider = rider;
        this.getEntityData().set(RIDER_UUID, Optional.of(rider.getUUID()));
    }

    public void setSwordID(int swordID) {
        getEntityData().set(RAIN_SCREEN_SWORD_ID, swordID);
    }

    public int getRainScreenSwordId() {
        return getEntityData().get(RAIN_SCREEN_SWORD_ID);
    }

    @Override
    public void tick() {
        //想办法不让rider为null
        if (rider == null) {
            if (this.getEntityData().get(RIDER_UUID).isPresent()) {
                rider = level.getPlayerByUUID(this.getEntityData().get(RIDER_UUID).get());
            }
            SwordSoaring.LOGGER.info("sword entity {} doesn't have rider {}", getId(), level);
            discard();
            return;
        }

        //限制客户端执行，因为服务端客户端位置不知为何不同
        //围绕rider旋转
        Vec3 center = rider.position();
        double radians = tickCount * 0.1 + (getRainScreenSwordId() * Math.PI / 2);

        var targetPos = new Vec3(
            center.x + Math.cos(radians) * 1.3,
            center.y + Math.sin(radians) * 0.3,
            center.z + Math.sin(radians) * 1.3
        );

        moveTo(targetPos);

        SSPlayer ssPlayer = rider.getCapability(SSCapabilityProvider.SS_PLAYER).orElse(new SSPlayer());
        if (this.tickCount > 200) {
            if (!level.isClientSide) {
                ssPlayer.getSwordScreensID().remove(getId());
            }
            ssPlayer.setSwordScreenEntityCount(ssPlayer.getSwordScreenEntityCount() - 1);
            discard();
        }

    }

    /**
     * 痛苦地调位置
     * 雨帘剑的位置又他妈不一样...
     *
     * @param poseStack 来自Renderer的render
     *                  FIXME
     */
    @Override
    public void setPose(PoseStack poseStack) {
        Item sword = getItemStack().getItem();
        if ((sword instanceof UchigatanaItem || sword instanceof TachiItem || sword instanceof LongswordItem)) {
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(225));
            poseStack.translate(-0.8, -0.8, 0);
        } else {
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(-225));
            //碰撞箱偏高（调这个太折磨人了，xyz轴都不知道转成什么样了只能一个个试）
            poseStack.translate(0.8, -0.8, 0);
        }
    }
}
