package com.lowdragmc.lowdraglib2.gui.factory;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

public class HeldItemUIMenuType {
    public record HeldItemUIOpeningData(InteractionHand hand, ItemStack itemStack, byte[] initialSync) {}
    public static final StreamCodec<RegistryFriendlyByteBuf, HeldItemUIOpeningData> STREAM_CODEC = StreamCodec.of(
        (buf, data) -> {
            buf.writeEnum(data.hand());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, data.itemStack());
            ByteBufCodecs.BYTE_ARRAY.encode(buf, data.initialSync());
        },
        buf -> new HeldItemUIOpeningData(buf.readEnum(InteractionHand.class), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), ByteBufCodecs.BYTE_ARRAY.decode(buf))
    );

    public static boolean openUI(ServerPlayer player, InteractionHand hand) {
        var heldItem = player.getItemInHand(hand);
        if (heldItem.getItem() instanceof HeldItemUI heldItemUI) {
            var holder = heldItemUI.createUIHolder(player, hand, heldItem);
            player.openMenu(holder);
            return true;
        }
        return false;
    }

    public static ModularUIContainerMenu create(int windowId, Inventory inv, HeldItemUIOpeningData data) {
        var player = inv.player;
        var hand = data.hand();
        var itemstack = data.itemStack();
        if (itemstack.getItem() instanceof HeldItemUI heldItemUI) {
            var holder = heldItemUI.createUIHolder(player, hand, itemstack);
            var menu = new ModularUIContainerMenu(LDMenuTypes.HELD_ITEM_UI, windowId, inv, holder);
            LDMenuTypes.readInitialData(menu, data.initialSync(), player);
            return menu;
        }
        throw new IllegalArgumentException("No held item ui found for item " + itemstack);
    }

    @FunctionalInterface
    public interface HeldItemUI {
        ModularUI createUI(HeldItemUIHolder holder);

        default HeldItemUIHolder createUIHolder(Player player, InteractionHand hand, ItemStack itemStack) {
            return new HeldItemUIHolder(this, player, hand, itemStack);
        }

        default boolean stillValid(HeldItemUIHolder holder) {
            var current = holder.player.getItemInHand(holder.hand);
            return ItemStack.matches(current, holder.itemStack);
        }

        default Component getUIDisplayName(HeldItemUIHolder holder) {
            return Component.translatable(holder.itemStack.getDescriptionId());
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static class HeldItemUIHolder implements ExtendedScreenHandlerFactory<HeldItemUIOpeningData>, IContainerUIHolder {
        public final HeldItemUI heldItemUI;
        public final Player player;
        public final InteractionHand hand;
        public final ItemStack itemStack;

        public HeldItemUIHolder(HeldItemUI heldItemUI, Player player, InteractionHand hand, ItemStack itemStack) {
            this.heldItemUI = heldItemUI;
            this.player = player;
            this.hand = hand;
            this.itemStack = itemStack;
        }

        @Override
        public boolean isStillValid(Player player) {
            return heldItemUI.stillValid(this);
        }

        @Override
        public Component getDisplayName() {
            return heldItemUI.getUIDisplayName(this);
        }

        @Override
        @Nullable
        public ModularUIContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new ModularUIContainerMenu(LDMenuTypes.HELD_ITEM_UI, containerId, playerInventory, this);
        }

        @Override
        public HeldItemUIOpeningData getScreenOpeningData(ServerPlayer player) { return new HeldItemUIOpeningData(hand, itemStack, LDMenuTypes.captureInitialData(player)); }

        @Override
        public ModularUI createUI(Player player) {
            return this.heldItemUI.createUI(this);
        }
    }
}
