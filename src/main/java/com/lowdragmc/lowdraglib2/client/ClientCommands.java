package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.font.LDFontStatsOverlay;
import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.client.shader.management.ShaderManager;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.uitest.UITestRunner;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author KilaBash
 * @date 2023/2/9
 * @implNote ClientCommands
 * @port ELB_GG
 * @date_port 2026/03/29
 * @port_to fabric
 */
public class ClientCommands {

    public static LiteralArgumentBuilder<FabricClientCommandSource> createLiteral(String command) {
        return ClientCommandManager.literal(command);
    }

    public static List<LiteralArgumentBuilder<FabricClientCommandSource>> createClientCommands() {
        var commands = new ArrayList<LiteralArgumentBuilder<FabricClientCommandSource>>();
        commands.add(createLiteral("ldlib2_client")
                .then(createLiteral("reload_shader")
                        .executes(context -> {
                            LDLibShaders.reload();
                            ShaderManager.getInstance().reload();
                            return 1;
                        })));
        if (Platform.isDevEnv()) {
            commands.add(createFontCommands());
        }
        if (LDLib2Registries.SCREEN_TESTS != null && !LDLib2Registries.SCREEN_TESTS.values().isEmpty()) {
            commands.add(createScreenTestCommands());
        }
        if (LDLib2Registries.UI_SCENARIOS != null && !LDLib2Registries.UI_SCENARIOS.values().isEmpty()) {
            commands.add(createAutoTestCommands());
        }
        return commands;
    }

    /**
     * Runs a UI test scenario inside the running game.
     *
     * <p>A command-line run pays for Gradle, mod loading and world creation before the first step,
     * and none of that changes between attempts. While iterating on a scenario, launch once with
     * {@code -PldTestKeepOpen} and re-run from here instead.
     */
    private static LiteralArgumentBuilder<FabricClientCommandSource> createAutoTestCommands() {
        return createLiteral("ldlib2_autotest")
                .then(createLiteral("list")
                        .executes(context -> {
                            var names = UITestRunner.registeredScenarioNames();
                            context.getSource().sendFeedback(Component.literal(
                                    names.size() + " scenario(s): " + String.join(", ", names)));
                            return names.size();
                        }))
                .then(createLiteral("run")
                        .then(ClientCommandManager.argument("selection", StringArgumentType.greedyString())
                                .suggests((context, builder) -> {
                                    builder.suggest("all");
                                    UITestRunner.registeredScenarioNames().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    var selection = StringArgumentType.getString(context, "selection");
                                    var error = UITestRunner.runInteractive(selection);
                                    if (error != null) {
                                        context.getSource().sendError(Component.literal(error));
                                        return 0;
                                    }
                                    context.getSource().sendFeedback(Component.literal(
                                            "Running UI scenarios: " + selection
                                                    + " (results go to the log and report.json)"));
                                    return 1;
                                })));
    }

    /**
     * Development only helpers for eyeballing the text renderer. Not registered outside a dev environment:
     * the settings they poke live in the client config, which is where users are meant to change them.
     */
    private static LiteralArgumentBuilder<FabricClientCommandSource> createFontCommands() {
        return createLiteral("ldlib2_font")
                .then(createLiteral("mode")
                        .executes(context -> {
                            var modes = LDLibClientConfig.FontRenderMode.values();
                            var next = modes[(LDLibClientConfig.fontRenderMode().ordinal() + 1) % modes.length];
                            LDLibClientConfig.setFontRenderMode(next);
                            // the renderers measure text slightly differently, so lay the screen out again
                            reinitCurrentScreen();
                            context.getSource().sendFeedback(
                                    Component.literal("LDLib text: " + next));
                            return 1;
                        }))
                .then(createLiteral("stats")
                        .executes(context -> {
                            LDFontStatsOverlay.toggle();
                            context.getSource().sendFeedback(
                                    Component.literal(LDFontStatsOverlay.describe()));
                            return 1;
                        }));
    }

    /**
     * Rebuilds the open screen so text is measured again with the renderer that is now active.
     */
    private static void reinitCurrentScreen() {
        var minecraft = Minecraft.getInstance();
        var screen = minecraft.screen;
        if (screen != null) {
            screen.resize(minecraft, screen.width, screen.height);
        }
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> createScreenTestCommands() {
        var builder = ClientCommandManager.literal("ldlib2_screen_test")
            .executes(context -> {
                int count = LDLib2Registries.SCREEN_TESTS == null ? -1 : LDLib2Registries.SCREEN_TESTS.values().size();
                context.getSource().sendFeedback(Component.literal("ldlib2_screen_test registered! Test count: " + count));
                return 1;
            });
        if (LDLib2Registries.SCREEN_TESTS == null) {
            return builder;
        }
        for (var uiTest : LDLib2Registries.SCREEN_TESTS) {
            builder = builder.then(createLiteral(uiTest.annotation().name())
                    .executes(context -> {
                        var test = uiTest.value().get();
                        var minecraft = Minecraft.getInstance();
                        var entityPlayer = minecraft.player;
                        if (entityPlayer == null) return 0;
                        var ui = test.createUI(entityPlayer);
                        minecraft.setScreen(new ModularUIScreen(ui, Component.empty()));
                        return 1;
                    }));
        }
        return builder;
    }
}
