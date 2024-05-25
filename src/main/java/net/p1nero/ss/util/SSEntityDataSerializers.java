package net.p1nero.ss.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class SSEntityDataSerializers {
    public static final EntityDataSerializer<Vec3> POS = new EntityDataSerializer<>() {
        @Override
        public void write(final FriendlyByteBuf buf, final Vec3 vec3) {
            buf.writeDouble(vec3.x);
            buf.writeDouble(vec3.y);
            buf.writeDouble(vec3.z);
        }

        @Override
        public @NotNull Vec3 read(final FriendlyByteBuf buf) {
            return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        @Override
        public Vec3 copy(final Vec3 vec3) {
            return vec3.add(Vec3.ZERO);
        }
    };

    static {
        EntityDataSerializers.registerSerializer(POS);
    }
}
