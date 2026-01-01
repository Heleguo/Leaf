package org.leavesmc.leaves;

import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.configuration.GlobalConfiguration;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.bukkit.command.Command;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.leavesmc.leaves.command.LeavesCommand;
import org.leavesmc.leaves.config.GlobalConfigManager;
import org.leavesmc.leaves.config.annotations.GlobalConfig;
import org.leavesmc.leaves.config.annotations.GlobalConfigCategory;
import org.leavesmc.leaves.config.annotations.TransferConfig;
import org.leavesmc.leaves.config.api.ConfigTransformer;
import org.leavesmc.leaves.config.api.ConfigValidator;
import org.leavesmc.leaves.config.api.impl.ConfigValidatorImpl.BooleanConfigValidator;
import org.leavesmc.leaves.config.api.impl.ConfigValidatorImpl.DoubleConfigValidator;
import org.leavesmc.leaves.config.api.impl.ConfigValidatorImpl.EnumConfigValidator;
import org.leavesmc.leaves.config.api.impl.ConfigValidatorImpl.IntConfigValidator;
import org.leavesmc.leaves.config.api.impl.ConfigValidatorImpl.ListConfigValidator;
import org.leavesmc.leaves.config.api.impl.ConfigValidatorImpl.LongConfigValidator;
import org.leavesmc.leaves.config.api.impl.ConfigValidatorImpl.StringConfigValidator;
import org.leavesmc.leaves.protocol.CarpetServerProtocol.CarpetRule;
import org.leavesmc.leaves.protocol.CarpetServerProtocol.CarpetRules;
import org.leavesmc.leaves.protocol.bladeren.BladerenProtocol.LeavesFeature;
import org.leavesmc.leaves.protocol.bladeren.BladerenProtocol.LeavesFeatureSet;
import org.leavesmc.leaves.protocol.servux.logger.DataLogger;
import org.leavesmc.leaves.protocol.syncmatica.SyncmaticaProtocol;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Predicate;

public final class LeavesConfig {

    public static final String CONFIG_HEADER = "Configuration file for Leaves.";
    public static final int CURRENT_CONFIG_VERSION = 6;

    private static File configFile;
    public static YamlConfiguration config;

    public static void init(final @NotNull File file) {
        LeavesConfig.configFile = file;
        config = new YamlConfiguration();
        config.options().setHeader(Collections.singletonList(CONFIG_HEADER));
        config.options().copyDefaults(true);

        if (!file.exists()) {
            try {
                boolean is = file.createNewFile();
                if (!is) {
                    throw new IOException("Can't create file");
                }
            } catch (final Exception ex) {
                LeavesLogger.LOGGER.severe("Failure to create leaves config", ex);
            }
        } else {
            try {
                config.load(file);
            } catch (final Exception ex) {
                LeavesLogger.LOGGER.severe("Failure to load leaves config", ex);
                throw new RuntimeException(ex);
            }
        }

        LeavesConfig.config.set("config-version", CURRENT_CONFIG_VERSION);

        GlobalConfigManager.init();

        registerCommand("leaves", new LeavesCommand());
    }

    public static void reload() {
        if (!LeavesConfig.configFile.exists()) {
            throw new RuntimeException("Leaves config file not found, please restart the server");
        }

        try {
            config.load(LeavesConfig.configFile);
        } catch (final Exception ex) {
            LeavesLogger.LOGGER.severe("Failure to reload leaves config", ex);
            throw new RuntimeException(ex);
        }

        GlobalConfigManager.reload();
    }

    public static void save() {
        try {
            config.save(LeavesConfig.configFile);
        } catch (final Exception ex) {
            LeavesLogger.LOGGER.severe("Unable to save leaves config", ex);
        }
    }

    public static void registerCommand(String name, Command command) {
        MinecraftServer.getServer().server.getCommandMap().register(name, "leaves", command);
        MinecraftServer.getServer().server.syncCommands();
    }

    public static void unregisterCommand(String name) {
        name = name.toLowerCase(Locale.ENGLISH).trim();
        MinecraftServer.getServer().server.getCommandMap().getKnownCommands().remove(name);
        MinecraftServer.getServer().server.getCommandMap().getKnownCommands().remove("leaves:" + name);
        MinecraftServer.getServer().server.syncCommands();
    }

    public static ProtocolConfig protocol = new ProtocolConfig();

    @GlobalConfigCategory("protocol")
    public static class ProtocolConfig {

        public BladerenConfig bladeren = new BladerenConfig();

        @GlobalConfigCategory("bladeren")
        public static class BladerenConfig {
            @GlobalConfig("protocol")
            public boolean enable = true;

            @GlobalConfig(value = "mspt-sync-protocol", validator = MSPTSyncValidator.class)
            public boolean msptSyncProtocol = false;

            private static class MSPTSyncValidator extends BooleanConfigValidator {
                @Override
                public void verify(Boolean old, Boolean value) throws IllegalArgumentException {
                    LeavesFeatureSet.register(LeavesFeature.of("mspt_sync", value));
                }
            }

            @GlobalConfig(value = "mspt-sync-tick-interval", validator = MSPTSyncIntervalValidator.class)
            public int msptSyncTickInterval = 20;

            private static class MSPTSyncIntervalValidator extends IntConfigValidator {
                @Override
                public void verify(Integer old, Integer value) throws IllegalArgumentException {
                    if (value <= 0) {
                        throw new IllegalArgumentException("mspt-sync-tick-interval need > 0");
                    }
                }
            }
        }


        public ServuxConfig servux = new ServuxConfig();

        @GlobalConfigCategory("servux")
        public static class ServuxConfig {
            @TransferConfig("protocol.servux-protocol")
            @GlobalConfig("structure-protocol")
            public boolean structureProtocol = false;

            @GlobalConfig("entity-protocol")
            public boolean entityProtocol = false;

            @GlobalConfig("hud-metadata-protocol")
            public boolean hudMetadataProtocol = false;

            @GlobalConfig("hud-logger-protocol")
            public boolean hudLoggerProtocol = false;

            @GlobalConfig("hud-enabled-loggers")
            public List<DataLogger.Type> hudEnabledLoggers = List.of(DataLogger.Type.TPS, DataLogger.Type.MOB_CAPS);

            @GlobalConfig("hud-update-interval")
            public int hudUpdateInterval = 1;

            @GlobalConfig("hud-metadata-protocol-share-seed")
            public boolean hudMetadataShareSeed = true;

            public LitematicsConfig litematics = new LitematicsConfig();

            @GlobalConfigCategory("litematics")
            public static class LitematicsConfig {

                @TransferConfig("protocol.servux.litematics-protocol")
                @GlobalConfig(value = "enable", validator = LitematicsProtocolValidator.class)
                public boolean enable = false;

                @GlobalConfig(value = "max-nbt-size", validator = MaxNbtSizeValidator.class)
                public long maxNbtSize = 2097152;

                public static class MaxNbtSizeValidator extends LongConfigValidator {

                    @Override
                    public void verify(Long old, Long value) throws IllegalArgumentException {
                        if (value <= 0) {
                            throw new IllegalArgumentException("Max nbt size can not be <= 0");
                        }
                    }
                }

                public static class LitematicsProtocolValidator extends BooleanConfigValidator {
                    @Override
                    public void verify(Boolean old, Boolean value) throws IllegalArgumentException {
                        PluginManager pluginManager = MinecraftServer.getServer().server.getPluginManager();
                        if (value) {
                            if (pluginManager.getPermission("leaves.protocol.litematics") == null) {
                                pluginManager.addPermission(new Permission("leaves.protocol.litematics", PermissionDefault.OP));
                            }
                        } else {
                            pluginManager.removePermission("leaves.protocol.litematics");
                        }
                    }
                }
            }
        }

        @GlobalConfigCategory("pca")
        public static class PCAConfig {
            @TransferConfig("protocol.pca-sync-protocol")
            @GlobalConfig(value = "pca-sync-protocol", validator = PcaValidator.class)
            public boolean enable = false;

            public static class PcaValidator extends BooleanConfigValidator {
                @Override
                public void verify(Boolean old, Boolean value) throws IllegalArgumentException {
                    if (old != null && old != value) {
                        PcaSyncProtocol.onConfigModify(value);
                    }
                }
            }

            @TransferConfig("protocol.pca-sync-player-entity")
            @GlobalConfig(value = "pca-sync-player-entity")
            public PcaPlayerEntityType syncPlayerEntity = PcaPlayerEntityType.OPS;

            public enum PcaPlayerEntityType {
                NOBODY, OPS, OPS_AND_SELF, EVERYONE
            }
        }

        @GlobalConfig(value = "alternative-block-placement", validator = AlternativePlaceValidator.class)
        public AlternativePlaceType alternativeBlockPlacement = AlternativePlaceType.NONE;

        public enum AlternativePlaceType {
            NONE, CARPET, CARPET_FIX, LITEMATICA
        }


        @GlobalConfig("leaves-carpet-support")
        public boolean leavesCarpetSupport = false;

    }
}
