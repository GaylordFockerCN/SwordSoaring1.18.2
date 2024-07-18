package net.p1nero.ss.keymapping;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = {Dist.CLIENT},bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModKeyMappings {
    public static final MyKeyMapping CHANGE_SPEED = new MyKeyMapping("key.sword_soaring.change_speed", GLFW.GLFW_KEY_TAB, "key.sword_soaring.common");
    public static final MyKeyMapping FLY = new MyKeyMapping("key.sword_soaring.fly", GLFW.GLFW_KEY_SPACE, "key.sword_soaring.common");

    @SubscribeEvent
    public static void registerKeys(FMLClientSetupEvent event) {
        ClientRegistry.registerKeyBinding(CHANGE_SPEED);
        ClientRegistry.registerKeyBinding(FLY);
    }

}
