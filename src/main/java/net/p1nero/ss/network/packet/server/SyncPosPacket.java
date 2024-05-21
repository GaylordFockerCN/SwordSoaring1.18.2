package net.p1nero.ss.network.packet.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.p1nero.ss.network.packet.BasePacket;

import javax.annotation.Nullable;
import java.util.Objects;

public record SyncPosPacket(Vec3 pos, int entityID) implements BasePacket {
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        buf.writeInt(entityID);
    }

    public static SyncPosPacket decode(FriendlyByteBuf buf) {
        return new SyncPosPacket(new Vec3(buf.readDouble(),buf.readDouble(),buf.readDouble()), buf.readInt());
    }

    @Override
    public void execute(@Nullable Player player) {
        if(player != null){
            Objects.requireNonNull(player.level.getEntity(entityID)).setPos(pos);
        }
    }
}