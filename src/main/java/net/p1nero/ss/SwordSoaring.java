package net.p1nero.ss;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.p1nero.ss.enchantment.ModEnchantments;
import net.p1nero.ss.entity.ModEntities;
import net.p1nero.ss.entity.SwordEntityRenderer;
import net.p1nero.ss.epicfight.skill.*;
import net.p1nero.ss.network.PacketHandler;
import org.slf4j.Logger;

import java.util.stream.Collectors;

@Mod(SwordSoaring.MOD_ID)
public class SwordSoaring {

    public static final String MOD_ID = "sword_soaring";
    public static final Logger LOGGER = LogUtils.getLogger();
    public SwordSoaring(){
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::commonSetup);
        IEventBus fg_bus = MinecraftForge.EVENT_BUS;
        MinecraftForge.EVENT_BUS.register(this);
        ModEntities.ENTITIES.register(bus);
        ModEnchantments.ENCHANTMENTS.register(bus);
        fg_bus.addListener(ModSkills::BuildSkills);
        fg_bus.addListener(SwordSoaringSkill::onPlayerFall);
        fg_bus.addListener(SwordSoaringSkill::onPlayerTick);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        PacketHandler.register();
    }

    /**
     * 调整是否在有WOM的环境下
     */
    public static boolean isWOMLoaded(){
        return ModList.get().isLoaded("wom");
//        return false;
    }

    /**
     * 判断物品是否属于剑或者被视为剑。
     * 无法监听事件，干脆直接在这里初始化剑物品表。
     */
    public static boolean isValidSword(ItemStack sword){
        //不知为何无法监听
        if(Config.swordItems.isEmpty()){
            Config.swordItems = Config.ITEMS_CAN_FLY.get().stream()
                    .map(itemName -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName)))
                    .collect(Collectors.toSet());
            Config.notSwordItems = Config.ITEMS_CAN_NOT_FLY.get().stream()
                    .map(itemName -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName)))
                    .collect(Collectors.toSet());
            Config.swordItems.removeAll(Config.notSwordItems);
        }
        if(Config.notSwordItems.contains(sword.getItem())){
            return false;
        }
        return sword.getItem() instanceof SwordItem  || Config.swordItems.contains(sword.getItem());
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents{
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event){
            EntityRenderers.register(ModEntities.SWORD.get(), SwordEntityRenderer::new);
        }

    }

}
