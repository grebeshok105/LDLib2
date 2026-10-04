package com.lowdragmc.lowdraglib2.networking;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.networking.both.PacketModularUISync;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCBlockEntity;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCPacket;
import com.lowdragmc.lowdraglib2.networking.both.PacketUIRPCEvent;
import com.lowdragmc.lowdraglib2.networking.both.PacketUIRPCEventReturn;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Author: KilaBash
 * Date: 2022/04/27
 * Description:
 */
public class LDLNetworking {


    public static void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (net.fabricmc.api.EnvType.CLIENT == net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType()) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
        }
    }

    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
    }

    public static void register() {
        // S2C
        PayloadTypeRegistry.playS2C().register(SPacketAutoSyncBlockEntity.TYPE, SPacketAutoSyncBlockEntity.CODEC);

        // C2S
        PayloadTypeRegistry.playC2S().register(PacketUIRPCEvent.TYPE, PacketUIRPCEvent.CODEC);

        // Bidirectional (registering both registries, matching upstream playBidirectional)
        PayloadTypeRegistry.playS2C().register(PacketUIRPCEventReturn.TYPE, PacketUIRPCEventReturn.CODEC);
        PayloadTypeRegistry.playC2S().register(PacketUIRPCEventReturn.TYPE, PacketUIRPCEventReturn.CODEC);

        PayloadTypeRegistry.playS2C().register(PacketRPCBlockEntity.TYPE, PacketRPCBlockEntity.CODEC);
        PayloadTypeRegistry.playC2S().register(PacketRPCBlockEntity.TYPE, PacketRPCBlockEntity.CODEC);

        PayloadTypeRegistry.playS2C().register(PacketModularUISync.TYPE, PacketModularUISync.CODEC);
        PayloadTypeRegistry.playC2S().register(PacketModularUISync.TYPE, PacketModularUISync.CODEC);

        PayloadTypeRegistry.playS2C().register(PacketRPCPacket.TYPE, PacketRPCPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(PacketRPCPacket.TYPE, PacketRPCPacket.CODEC);
    }

}
