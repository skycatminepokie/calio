package io.github.apace100.calio.network.packet.s2c;

import io.github.apace100.calio.Calio;
import io.github.apace100.calio.registry.DataObjectRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncDataObjectRegistryS2CPacket(DataObjectRegistry<?> registry) implements CustomPayload {

    public static final Id<SyncDataObjectRegistryS2CPacket> PACKET_ID = new Id<>(Calio.identifier("s2c/sync_data_object_registry"));
    public static final PacketCodec<RegistryByteBuf, SyncDataObjectRegistryS2CPacket> PACKET_CODEC = PacketCodec.of(SyncDataObjectRegistryS2CPacket::write, SyncDataObjectRegistryS2CPacket::read);

    public static SyncDataObjectRegistryS2CPacket read(RegistryByteBuf buf) {

        Identifier registryId = buf.readIdentifier();
        DataObjectRegistry<?> registry = DataObjectRegistry.getRegistry(registryId);

        registry.receive(buf);
        return new SyncDataObjectRegistryS2CPacket(registry);

    }

    public void write(RegistryByteBuf buf) {
        buf.writeIdentifier(registry.getRegistryId());
        registry.send(buf);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
