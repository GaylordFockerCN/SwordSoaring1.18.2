package net.p1nero.ss.epicfight.skill;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.p1nero.ss.Config;
import net.p1nero.ss.SwordSoaring;
import net.p1nero.ss.capability.SSCapabilityProvider;
import net.p1nero.ss.capability.SSPlayer;
import net.p1nero.ss.enchantment.ModEnchantments;
import net.p1nero.ss.epicfight.animation.ModAnimations;
import net.p1nero.ss.keymapping.ModKeyMappings;
import net.p1nero.ss.network.PacketHandler;
import net.p1nero.ss.network.PacketRelay;
import net.p1nero.ss.network.packet.server.StartFlyPacket;
import net.p1nero.ss.network.packet.server.StopFlyPacket;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.gameasset.EpicFightSkills;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.UUID;

import static net.p1nero.ss.util.InertiaUtil.*;

public class SwordSoaringSkill extends Skill {

    private static final UUID EVENT_UUID = UUID.fromString("051a9bb2-7541-11ee-b962-0242ac114514");

    private static final SkillDataManager.SkillDataKey<Integer> COOL_DOWN_TIMER = SkillDataManager.SkillDataKey.createDataKey(SkillDataManager.ValueType.INTEGER);

    public SwordSoaringSkill(Builder<? extends Skill> builder) {
        super(builder);
    }

    public static float flySpeedLevel = 1;//还好只在客户端用，不然全服同步了。。

    @Override
    public boolean canExecute(PlayerPatch<?> executer) {
        return false;
    }

    /**
     * 注册监听器
     */
    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
        container.getDataManager().registerData(COOL_DOWN_TIMER);

        PlayerEventListener listener = container.getExecuter().getEventListener();
        listener.addEventListener(PlayerEventListener.EventType.SKILL_EXECUTE_EVENT, EVENT_UUID, (event) -> {
            event.getPlayerPatch().getOriginal().getCapability(SSCapabilityProvider.SS_PLAYER).ifPresent(ssPlayer -> {
                if(Config.FORCE_FLY_ANIM.get() && ssPlayer.isFlying() && (!event.getSkillContainer().hasSkill(ModSkills.SWORD_SOARING))){
                    event.setCanceled(true);
                }
            });
        });
//        listener.addEventListener(PlayerEventListener.EventType.ACTION_EVENT_CLIENT, EVENT_UUID, (event) -> {
//            event.getPlayerPatch().getOriginal().getCapability(SSCapabilityProvider.SS_PLAYER).ifPresent(ssPlayer -> {
//                if(Config.FORCE_FLY_ANIM.get() && ssPlayer.isFlying() && (event.getAnimation() != ModAnimations.FLY_ON_SWORD_ADVANCED || event.getAnimation() != ModAnimations.FLY_ON_SWORD_BASIC)){
//                    event.setCanceled(true);
//                }
//            });
//        });
//        listener.addEventListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, EVENT_UUID, (event) -> {
//            event.getPlayerPatch().getOriginal().getCapability(SSCapabilityProvider.SS_PLAYER).ifPresent(ssPlayer -> {
//                if(Config.FORCE_FLY_ANIM.get() && ssPlayer.isFlying() && (event.getAnimation() != ModAnimations.FLY_ON_SWORD_ADVANCED || event.getAnimation() != ModAnimations.FLY_ON_SWORD_BASIC)){
//                    event.setCanceled(true);
//                }
//            });
//        });

        //取消免疫摔落伤害
        listener.addEventListener(PlayerEventListener.EventType.HURT_EVENT_PRE, EVENT_UUID, (event) -> {
            if (event.getDamageSource().isFall() ) {
                Player player = event.getPlayerPatch().getOriginal();
                player.getCapability(SSCapabilityProvider.SS_PLAYER).ifPresent(ssPlayer -> {
                    if(ssPlayer.isProtectNextFall()){
                        ssPlayer.setProtectNextFall(false);
                    }
                });
            }
        });

        //调整下落伤害，不然高飞低会扣血
        listener.addEventListener(PlayerEventListener.EventType.FALL_EVENT, EVENT_UUID, (event) -> {
            Player player = event.getPlayerPatch().getOriginal();
            player.getCapability(SSCapabilityProvider.SS_PLAYER).ifPresent(ssPlayer -> {
                //-1表示不作修改，高度变高也是错误的计算
                if(ssPlayer.flyHeight == -1 || ssPlayer.flyHeight > event.getForgeEvent().getDistance()){
                    return;
                }
                event.getForgeEvent().setDistance(((int) ssPlayer.flyHeight));
                ssPlayer.flyHeight = -1;
            });
        });

    }

    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        if(container.getExecuter().isLogicalClient()){

            Player player = container.getExecuter().getOriginal();
            ItemStack sword = player.getMainHandItem();

            player.getCapability(SSCapabilityProvider.SS_PLAYER).ifPresent(ssPlayer -> {
                int currentCoolDown = container.getDataManager().getDataValue(COOL_DOWN_TIMER);
                if(currentCoolDown > 0){
                    container.getDataManager().setData(COOL_DOWN_TIMER, currentCoolDown-1);
                    return;
                }
                // Check directly from the keybind because event.getMovementInput().isJumping doesn't allow to be set as true while player's jumping
                boolean fly = ModKeyMappings.FLY.isDown();
                if(ModKeyMappings.FLY.isRelease()){
                    container.getDataManager().setData(COOL_DOWN_TIMER, Config.SWORD_SOARING_COOLDOWN.get().intValue());
                }
                //最后一个条件是防止飞行的时候切物品会导致永久飞行不掉落。必须是剑或者被视为剑的物品才可以“御”。player.isInWater没吊用。。
                if (!fly || container.getExecuter().getOriginal().getVehicle() != null || container.getExecuter().getOriginal().getAbilities().flying || !container.getExecuter().isBattleMode()
                        || container.getExecuter().getStamina() <= 0.1f || player.isInLava() || player.isUnderWater() || !(SwordSoaring.isValidSword(sword) || ssPlayer.hasSwordEntity())) {
                    //停止飞行
                    PacketRelay.sendToServer(PacketHandler.INSTANCE, new StopFlyPacket());
                    //飞行结束后再获取末向量。因为此时isFlying还没设为false
                    if(Config.ENABLE_INERTIA.get() && ssPlayer.isFlying()){
                        Vec3 endVec = getViewVec(player.getPersistentData(),1).scale(Config.FLY_SPEED_SCALE.get() * flySpeedLevel);
                        setEndVec(player.getPersistentData(), endVec);
                        double leftTick = endVec.length() * maxRecordTick;
                        setLeftTick(player.getPersistentData(), ((int) leftTick));
                    }
                    ssPlayer.setFlying(false);
                    //重置飞行前摇时间
                    ssPlayer.setAnticipationTick(0);
                    return;
                }

                //进行前摇判断，按住空格0.5s后才起飞（不然就跳不了了..）
                if(ssPlayer.getAnticipationTick() == 0){
                    ssPlayer.setAnticipationTick(Config.MAX_ANTICIPATION_TICK.get().intValue());
                    return;
                }
                if(ssPlayer.getAnticipationTick() > 1){
                    ssPlayer.setAnticipationTick(ssPlayer.getAnticipationTick()-1);
                    return;
                }
                //设置飞行状态并设置免疫下次摔落伤害
                PacketRelay.sendToServer(PacketHandler.INSTANCE, new StartFlyPacket(flySpeedLevel));
                ssPlayer.setFlying(true);
//                event.getPlayerPatch().playAnimationSynchronized(ModAnimations.FLY_ON_SWORD_ADVANCED, 0);

            });

        }
    }

    @Override
    public boolean shouldDraw(SkillContainer container) {
        return container.getDataManager().getDataValue(COOL_DOWN_TIMER) > 0;
    }

    @Override
    public void drawOnGui(BattleModeGui gui, SkillContainer container, PoseStack poseStack, float x, float y) {
        poseStack.pushPose();
        poseStack.translate(0.0, (float)gui.getSlidingProgression(), 0.0);
        RenderSystem.setShaderTexture(0, getSkillTexture());
        GuiComponent.blit(poseStack, (int)x, (int)y, 24, 24, 0.0F, 0.0F, 1, 1, 1, 1);
        gui.font.drawShadow(poseStack, String.format("%.1f", (container.getDataManager().getDataValue(COOL_DOWN_TIMER) / 20.0)), x, y + 6.0F, 16777215);
    }

    /**
     * 控制飞行和耐力消耗
     * 并进行惯性判断。飞行结束时如果有缓冲时间则缓冲。
     * 缓冲时间设置请看：{@link StopFlyPacket#execute(Player)}
     */
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;

        player.getCapability(SSCapabilityProvider.SS_PLAYER).ifPresent(ssPlayer -> {
            if(ssPlayer.isFlying()){

                //速度切换
                if(player.isLocalPlayer()){
                    if(ModKeyMappings.CHANGE_SPEED.isRelease()){
                        if(ModKeyMappings.CHANGE_SPEED.isEvenNumber()){
                            flySpeedLevel = 2.0f;
                        } else {
                            flySpeedLevel = 1.0f;
                        }
                        player.displayClientMessage(Component.nullToEmpty(I18n.get("tip.sword_soaring.speed_level") + (int) flySpeedLevel), true);
                    }
                }

                //惯性控制。懒得重写就直接用getPersistentData了
                if(Config.ENABLE_INERTIA.get()){
                    Vec3 targetVec = getViewVec(player.getPersistentData(), Config.INERTIA_TICK_BEFORE.get().intValue()).scale(Config.FLY_SPEED_SCALE.get() * flySpeedLevel);
                    if(targetVec.length() != 0) {
                        player.setDeltaMovement(targetVec);
                    }
                } else {
                    player.setDeltaMovement(player.getViewVector(0.5f).scale(Config.FLY_SPEED_SCALE.get() * flySpeedLevel));
                }
                resetHeight(player,ssPlayer);

                //消耗耐力
                player.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY).ifPresent((entityPatch)->{
                    if(entityPatch instanceof ServerPlayerPatch playerPatch){
                        if(!player.isCreative()){
                            int enchantmentLevel = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.SWORD_SOARING.get(), player);
                            float scale = switch (enchantmentLevel) {
                                case 1 -> 0.75f;
                                case 2 -> 0.5f;
                                default -> 1;
                            };
                            playerPatch.consumeStamina(Config.STAMINA_CONSUME_PER_TICK.get().floatValue() * scale * flySpeedLevel);
                        }
                    }
                });
            } else if(Config.ENABLE_INERTIA.get()){
                double endVecLength = getEndVec(player.getPersistentData()).length();
                //惯性缓冲
                if (getLeftTick(player.getPersistentData()) > 0 && endVecLength != 0) {
                    resetHeight(player,ssPlayer);
                    int leftTick = getLeftTick(player.getPersistentData());
                    setLeftTick(player.getPersistentData(), leftTick - 1);
                    //用末速度来计算
                    double max = endVecLength * maxRecordTick;
                    player.setDeltaMovement(getEndVec(player.getPersistentData()).lerp(Vec3.ZERO, (max - leftTick) / max));
                }
            }
        });

        //更新方向向量队列
        updateViewVec(player.getPersistentData(), player.getViewVector(0));

    }

    /**
     * 这个是重置当前位置所处高度。因为飞行后会以初位置为初高度，摔落会有偏差
     * 每tick都消耗太浪费资源了，但是有无惯性都得重置高度。。
     */
    private static void resetHeight(Player player, SSPlayer ssPlayer){
        Vec3 from = player.getEyePosition(1.0F);
        Vec3 to = from.add(0, -500.0, 0);
        HitResult hitResult = player.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        if(hitResult.getType() != HitResult.Type.MISS){
            ssPlayer.flyHeight = hitResult.distanceTo(player);
        }else {
            ssPlayer.flyHeight = -1;
        }
    }

    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        PlayerEventListener listener = container.getExecuter().getEventListener();
        listener.removeListener(PlayerEventListener.EventType.HURT_EVENT_PRE, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.SKILL_EXECUTE_EVENT, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.FALL_EVENT, EVENT_UUID);
    }

}
