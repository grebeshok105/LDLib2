package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.font.LDFontManager;
import com.lowdragmc.lowdraglib2.client.font.LDFontStatsOverlay;
import com.lowdragmc.lowdraglib2.editor.resource.EditorResourceEvent;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.utils.CursorOverlay;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.OreSprites;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;


/**
 * @author KilaBash
 * @date 2022/5/12
 * @implNote EventListener
 * @port ELB_GG
 * @date_port 2026/03/29
 * @port_to fabric
 */
public class ClientEventListener {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            ClientCommands.createClientCommands().forEach(dispatcher::register);
        });

        /**
         * The two things about the text renderer that can only be noticed by looking: the font related video
         * settings, which vanilla gives mods no event for, and rasterized glyph sizes going unused, which is time
         * based by nature. Both free textures, so both belong between frames rather than inside one.
         */
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LDFontManager.INSTANCE.refreshVanillaFontOptions();
            LDFontManager.INSTANCE.evictStaleRasterSizes();
        });

        /**
         * TEMPORARY: the statistics overlay is a HUD layer, and HUD layers are drawn before the open screen rather
         * than over it, so on a screen it would sit behind the very interface it is reporting on. Drawing it again
         * here puts it on top. See {@link LDFontStatsOverlay}.
         */
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, tickDelta) -> {
                    if (Platform.isDevEnv()) {
                        LDFontStatsOverlay.INSTANCE.render(graphics, Minecraft.getInstance().getTimer());
                    }
                    // Only draws while something is driving the cursor from inside the process; see the class doc.
                    CursorOverlay.render(graphics, tickDelta);
                }));
    }

    public static void init() {
        EditorResourceEvent.LOAD_BUILTIN.register(ClientEventListener::onResourceLoad);
    }

    @SuppressWarnings("unchecked")
    public static void onResourceLoad(ResourceInstance<?> resourceInstance) {
        if (resourceInstance.resource instanceof TexturesResource texturesResource) {
            if (texturesResource.getName().equals("texture")) {
                Sprites.init((ResourceInstance<IGuiTexture>) resourceInstance);
                MCSprites.init((ResourceInstance<IGuiTexture>) resourceInstance);
                OreSprites.init((ResourceInstance<IGuiTexture>) resourceInstance);
            }
        }
    }

//    @SubscribeEvent
//    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
//        // memoize and delay, to make sure ui is generated after the world loading
//        var muiCache = Suppliers.memoize(() -> ModularUI.of(UI.of(
//                new UIElement().layout(l -> l.widthPercent(100).heightPercent(100).paddingAll(10).gapAll(4))
//                        .addChildren(
//                                new UIElement()
//                                        .layout(l -> l.width(50).height(50).paddingAll(5))
//                                        .style(s -> s.background(Sprites.BORDER1_RT1))
//                                        .addChild(new UIElement()
//                                                .layout(l -> l.widthPercent(100).heightPercent(100))
//                                                .style(s -> s.background(new ItemStackTexture(Items.DIAMOND)))
//                                        ),
//                                new ProgressBar().bindDataSource(SupplierDataSource.of(() -> Optional.ofNullable(Minecraft.getInstance().player)
//                                                .map(p -> p.getHealth() / p.getMaxHealth()).orElse(1f)))
//                                        .label(l -> l.setText("health"))
//                                        .layout(l -> l.width(100))
//                        )
//        )));
//        event.registerAboveAll(LDLib2.id("test_hud"), (ModularHudLayer) muiCache::get);
//    }
}
