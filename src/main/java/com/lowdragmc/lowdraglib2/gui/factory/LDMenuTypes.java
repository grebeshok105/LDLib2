package com.lowdragmc.lowdraglib2.gui.factory;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.editor.UIEditor;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

public final class LDMenuTypes {

    public static MenuType<ModularUIContainerMenu> PLAYER_UI;
    public static MenuType<ModularUIContainerMenu> HELD_ITEM_UI;
    public static MenuType<ModularUIContainerMenu> BLOCK_UI;

    public static void init() {
        PLAYER_UI = register("player_ui", new ExtendedScreenHandlerType<>(PlayerUIMenuType::create, PlayerUIMenuType.STREAM_CODEC));
        HELD_ITEM_UI = register("held_item_ui", new ExtendedScreenHandlerType<>(HeldItemUIMenuType::create, HeldItemUIMenuType.STREAM_CODEC));
        BLOCK_UI = register("block_ui", new ExtendedScreenHandlerType<>(BlockUIMenuType::create, BlockUIMenuType.STREAM_CODEC));

        PlayerUIMenuType.register(UIEditor.WINDOW_ID, ignored -> player -> {
            if (player.level().isClientSide) {
                return new ModularUI(UI.of(EditorWindow.open(UIEditor.WINDOW_ID, UIEditor::new)))
                        .shouldCloseOnEsc(false)
                        .shouldCloseOnKeyInventory(false);
            }
            return new ModularUI(UI.empty());
        });

        // KubeJS integration removed as per Phase 0
    }

    /**
     * Serializes the created menu's initial UI sync data to send inside the screen-open packet.
     * Fabric sets {@code player.containerMenu} to the created menu before calling
     * {@code getScreenOpeningData}, mirroring upstream where {@code writeInitialData} runs
     * after {@code createMenu}. Returns an empty array when there is no modular UI.
     */
    static byte[] captureInitialData(ServerPlayer player) {
        if (player.containerMenu instanceof IModularUIHolder holder) {
            var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
            try {
                holder.writeInitialData(buf);
                var data = new byte[buf.readableBytes()];
                buf.readBytes(data);
                return data;
            } finally {
                buf.release();
            }
        }
        return new byte[0];
    }

    /**
     * Reads the initial UI sync data carried by the screen-open packet into the created menu,
     * mirroring upstream where {@code readInitialData} runs after the menu factory.
     */
    static void readInitialData(ModularUIContainerMenu menu, byte[] data, Player player) {
        if (data.length == 0) return;
        var buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), player.registryAccess());
        try {
            menu.asModularUIHolderMenu().readInitialData(buf);
        } finally {
            buf.release();
        }
    }

    private static <T extends MenuType<?>> T register(String name, T type) {
        return Registry.register(BuiltInRegistries.MENU, LDLib2.id(name), type);
    }
}
