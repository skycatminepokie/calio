package io.github.apace100.calio.network;

import io.github.apace100.calio.network.packet.s2c.SyncDataObjectRegistryS2CPacket;
import io.github.apace100.calio.registry.DataObjectRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public class CalioNetworkingClient {

    public static void registerReceivers() {
        ClientPlayConnectionEvents.INIT.register((handler, client) ->
            ClientPlayNetworking.registerReceiver(SyncDataObjectRegistryS2CPacket.PACKET_ID, CalioNetworkingClient::onDataObjectRegistrySync)
        );
    }

    private static void onDataObjectRegistrySync(SyncDataObjectRegistryS2CPacket payload, ClientPlayNetworking.Context context) {
        DataObjectRegistry.updateRegistry(payload.registry());
    }

}
