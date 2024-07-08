package net.p1nero.ss.epicfight.animation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.p1nero.ss.SwordSoaring;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.api.client.model.ClientModels;
import yesman.epicfight.api.forgeevent.AnimationRegistryEvent;
import yesman.epicfight.api.model.Model;
import yesman.epicfight.gameasset.Models;

@Mod.EventBusSubscriber(modid = SwordSoaring.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModAnimations {
    public static StaticAnimation FLY_ON_SWORD_BASIC;
    public static StaticAnimation FLY_ON_SWORD_ADVANCED;

    @SubscribeEvent
    public static void registerAnimations(AnimationRegistryEvent event) {
        event.getRegistryMap().put(SwordSoaring.MOD_ID, ModAnimations::build);
    }

    private static void build() {
        Models<?> models = FMLEnvironment.dist == Dist.CLIENT ? ClientModels.LOGICAL_CLIENT : Models.LOGICAL_SERVER;
        Model biped = models.biped;
        FLY_ON_SWORD_BASIC = new StaticAnimation(false, "biped/fly_on_sword_beginner", biped);
        FLY_ON_SWORD_ADVANCED = new StaticAnimation(false, "biped/fly_on_sword_master", biped);
    }

}
