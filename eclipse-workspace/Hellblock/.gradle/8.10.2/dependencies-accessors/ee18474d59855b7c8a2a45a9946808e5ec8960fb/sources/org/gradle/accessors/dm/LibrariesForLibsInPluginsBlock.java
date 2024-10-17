package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.artifacts.dsl.CapabilityNotationParser;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the {@code libs} extension.
 */
@NonNullApi
public class LibrariesForLibsInPluginsBlock extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final ComLibraryAccessors laccForComLibraryAccessors = new ComLibraryAccessors(owner);
    private final DeLibraryAccessors laccForDeLibraryAccessors = new DeLibraryAccessors(owner);
    private final IoLibraryAccessors laccForIoLibraryAccessors = new IoLibraryAccessors(owner);
    private final MeLibraryAccessors laccForMeLibraryAccessors = new MeLibraryAccessors(owner);
    private final NetLibraryAccessors laccForNetLibraryAccessors = new NetLibraryAccessors(owner);
    private final OrgLibraryAccessors laccForOrgLibraryAccessors = new OrgLibraryAccessors(owner);
    private final RedisLibraryAccessors laccForRedisLibraryAccessors = new RedisLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibsInPluginsBlock(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

    /**
     * Group of libraries at <b>com</b>
     *
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public ComLibraryAccessors getCom() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
        return laccForComLibraryAccessors;
    }

    /**
     * Group of libraries at <b>de</b>
     *
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public DeLibraryAccessors getDe() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
        return laccForDeLibraryAccessors;
    }

    /**
     * Group of libraries at <b>io</b>
     *
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public IoLibraryAccessors getIo() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
        return laccForIoLibraryAccessors;
    }

    /**
     * Group of libraries at <b>me</b>
     *
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public MeLibraryAccessors getMe() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
        return laccForMeLibraryAccessors;
    }

    /**
     * Group of libraries at <b>net</b>
     *
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public NetLibraryAccessors getNet() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
        return laccForNetLibraryAccessors;
    }

    /**
     * Group of libraries at <b>org</b>
     *
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public OrgLibraryAccessors getOrg() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
        return laccForOrgLibraryAccessors;
    }

    /**
     * Group of libraries at <b>redis</b>
     *
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public RedisLibraryAccessors getRedis() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
        return laccForRedisLibraryAccessors;
    }

    /**
     * Group of versions at <b>versions</b>
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Group of bundles at <b>bundles</b>
     *
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public BundleAccessors getBundles() {
        org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
        return baccForBundleAccessors;
    }

    /**
     * Group of plugins at <b>plugins</b>
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class ComLibraryAccessors extends SubDependencyFactory {
        private final ComComphenixLibraryAccessors laccForComComphenixLibraryAccessors = new ComComphenixLibraryAccessors(owner);
        private final ComGithubLibraryAccessors laccForComGithubLibraryAccessors = new ComGithubLibraryAccessors(owner);
        private final ComJeffLibraryAccessors laccForComJeffLibraryAccessors = new ComJeffLibraryAccessors(owner);
        private final ComSk89qLibraryAccessors laccForComSk89qLibraryAccessors = new ComSk89qLibraryAccessors(owner);
        private final ComZaxxerLibraryAccessors laccForComZaxxerLibraryAccessors = new ComZaxxerLibraryAccessors(owner);

        public ComLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.comphenix</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public ComComphenixLibraryAccessors getComphenix() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForComComphenixLibraryAccessors;
        }

        /**
         * Group of libraries at <b>com.github</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public ComGithubLibraryAccessors getGithub() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForComGithubLibraryAccessors;
        }

        /**
         * Group of libraries at <b>com.jeff</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public ComJeffLibraryAccessors getJeff() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForComJeffLibraryAccessors;
        }

        /**
         * Group of libraries at <b>com.sk89q</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public ComSk89qLibraryAccessors getSk89q() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForComSk89qLibraryAccessors;
        }

        /**
         * Group of libraries at <b>com.zaxxer</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public ComZaxxerLibraryAccessors getZaxxer() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForComZaxxerLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class ComComphenixLibraryAccessors extends SubDependencyFactory {
        private final ComComphenixProtocolLibraryAccessors laccForComComphenixProtocolLibraryAccessors = new ComComphenixProtocolLibraryAccessors(owner);

        public ComComphenixLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.comphenix.protocol</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public ComComphenixProtocolLibraryAccessors getProtocol() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForComComphenixProtocolLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class ComComphenixProtocolLibraryAccessors extends SubDependencyFactory {

        public ComComphenixProtocolLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>protocollib</b> with <b>com.comphenix.protocol:ProtocolLib</b> coordinates and
         * with version reference <b>com.comphenix.protocol.protocollib</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getProtocollib() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("com.comphenix.protocol.protocollib");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class ComGithubLibraryAccessors extends SubDependencyFactory {
        private final ComGithubMilkbowlLibraryAccessors laccForComGithubMilkbowlLibraryAccessors = new ComGithubMilkbowlLibraryAccessors(owner);

        public ComGithubLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.github.milkbowl</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public ComGithubMilkbowlLibraryAccessors getMilkbowl() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForComGithubMilkbowlLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class ComGithubMilkbowlLibraryAccessors extends SubDependencyFactory {

        public ComGithubMilkbowlLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>vaultapi</b> with <b>com.github.MilkBowl:VaultAPI</b> coordinates and
         * with version reference <b>com.github.milkbowl.vaultapi</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getVaultapi() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("com.github.milkbowl.vaultapi");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class ComJeffLibraryAccessors extends SubDependencyFactory {
        private final ComJeffMediaLibraryAccessors laccForComJeffMediaLibraryAccessors = new ComJeffMediaLibraryAccessors(owner);

        public ComJeffLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.jeff.media</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public ComJeffMediaLibraryAccessors getMedia() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForComJeffMediaLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class ComJeffMediaLibraryAccessors extends SubDependencyFactory {
        private final ComJeffMediaCustomLibraryAccessors laccForComJeffMediaCustomLibraryAccessors = new ComJeffMediaCustomLibraryAccessors(owner);

        public ComJeffMediaLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.jeff.media.custom</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public ComJeffMediaCustomLibraryAccessors getCustom() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForComJeffMediaCustomLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class ComJeffMediaCustomLibraryAccessors extends SubDependencyFactory {
        private final ComJeffMediaCustomBlockLibraryAccessors laccForComJeffMediaCustomBlockLibraryAccessors = new ComJeffMediaCustomBlockLibraryAccessors(owner);

        public ComJeffMediaCustomLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.jeff.media.custom.block</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public ComJeffMediaCustomBlockLibraryAccessors getBlock() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForComJeffMediaCustomBlockLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class ComJeffMediaCustomBlockLibraryAccessors extends SubDependencyFactory {

        public ComJeffMediaCustomBlockLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>data</b> with <b>com.jeff-media:custom-block-data</b> coordinates and
         * with version reference <b>com.jeff.media.custom.block.data</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getData() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("com.jeff.media.custom.block.data");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class ComSk89qLibraryAccessors extends SubDependencyFactory {
        private final ComSk89qWorldeditLibraryAccessors laccForComSk89qWorldeditLibraryAccessors = new ComSk89qWorldeditLibraryAccessors(owner);
        private final ComSk89qWorldguardLibraryAccessors laccForComSk89qWorldguardLibraryAccessors = new ComSk89qWorldguardLibraryAccessors(owner);

        public ComSk89qLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.sk89q.worldedit</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public ComSk89qWorldeditLibraryAccessors getWorldedit() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForComSk89qWorldeditLibraryAccessors;
        }

        /**
         * Group of libraries at <b>com.sk89q.worldguard</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public ComSk89qWorldguardLibraryAccessors getWorldguard() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForComSk89qWorldguardLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class ComSk89qWorldeditLibraryAccessors extends SubDependencyFactory {
        private final ComSk89qWorldeditWorldeditLibraryAccessors laccForComSk89qWorldeditWorldeditLibraryAccessors = new ComSk89qWorldeditWorldeditLibraryAccessors(owner);

        public ComSk89qWorldeditLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.sk89q.worldedit.worldedit</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public ComSk89qWorldeditWorldeditLibraryAccessors getWorldedit() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForComSk89qWorldeditWorldeditLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class ComSk89qWorldeditWorldeditLibraryAccessors extends SubDependencyFactory {

        public ComSk89qWorldeditWorldeditLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>bukkit</b> with <b>com.sk89q.worldedit:worldedit-bukkit</b> coordinates and
         * with version reference <b>com.sk89q.worldedit.worldedit.bukkit</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getBukkit() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("com.sk89q.worldedit.worldedit.bukkit");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class ComSk89qWorldguardLibraryAccessors extends SubDependencyFactory {
        private final ComSk89qWorldguardWorldguardLibraryAccessors laccForComSk89qWorldguardWorldguardLibraryAccessors = new ComSk89qWorldguardWorldguardLibraryAccessors(owner);

        public ComSk89qWorldguardLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>com.sk89q.worldguard.worldguard</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public ComSk89qWorldguardWorldguardLibraryAccessors getWorldguard() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForComSk89qWorldguardWorldguardLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class ComSk89qWorldguardWorldguardLibraryAccessors extends SubDependencyFactory {

        public ComSk89qWorldguardWorldguardLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>bukkit</b> with <b>com.sk89q.worldguard:worldguard-bukkit</b> coordinates and
         * with version reference <b>com.sk89q.worldguard.worldguard.bukkit</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getBukkit() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("com.sk89q.worldguard.worldguard.bukkit");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class ComZaxxerLibraryAccessors extends SubDependencyFactory {

        public ComZaxxerLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>hikaricp</b> with <b>com.zaxxer:HikariCP</b> coordinates and
         * with version reference <b>com.zaxxer.hikaricp</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getHikaricp() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("com.zaxxer.hikaricp");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class DeLibraryAccessors extends SubDependencyFactory {
        private final DeTr7zwLibraryAccessors laccForDeTr7zwLibraryAccessors = new DeTr7zwLibraryAccessors(owner);

        public DeLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>de.tr7zw</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public DeTr7zwLibraryAccessors getTr7zw() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForDeTr7zwLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class DeTr7zwLibraryAccessors extends SubDependencyFactory {
        private final DeTr7zwItemLibraryAccessors laccForDeTr7zwItemLibraryAccessors = new DeTr7zwItemLibraryAccessors(owner);

        public DeTr7zwLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>de.tr7zw.item</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public DeTr7zwItemLibraryAccessors getItem() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForDeTr7zwItemLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class DeTr7zwItemLibraryAccessors extends SubDependencyFactory {
        private final DeTr7zwItemNbtLibraryAccessors laccForDeTr7zwItemNbtLibraryAccessors = new DeTr7zwItemNbtLibraryAccessors(owner);

        public DeTr7zwItemLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>de.tr7zw.item.nbt</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public DeTr7zwItemNbtLibraryAccessors getNbt() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForDeTr7zwItemNbtLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class DeTr7zwItemNbtLibraryAccessors extends SubDependencyFactory {
        private final DeTr7zwItemNbtApiLibraryAccessors laccForDeTr7zwItemNbtApiLibraryAccessors = new DeTr7zwItemNbtApiLibraryAccessors(owner);

        public DeTr7zwItemNbtLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>de.tr7zw.item.nbt.api</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public DeTr7zwItemNbtApiLibraryAccessors getApi() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForDeTr7zwItemNbtApiLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class DeTr7zwItemNbtApiLibraryAccessors extends SubDependencyFactory {

        public DeTr7zwItemNbtApiLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>plugin</b> with <b>de.tr7zw:item-nbt-api-plugin</b> coordinates and
         * with version reference <b>de.tr7zw.item.nbt.api.plugin</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getPlugin() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("de.tr7zw.item.nbt.api.plugin");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class IoLibraryAccessors extends SubDependencyFactory {
        private final IoPapermcLibraryAccessors laccForIoPapermcLibraryAccessors = new IoPapermcLibraryAccessors(owner);

        public IoLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>io.papermc</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public IoPapermcLibraryAccessors getPapermc() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForIoPapermcLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class IoPapermcLibraryAccessors extends SubDependencyFactory {
        private final IoPapermcPaperLibraryAccessors laccForIoPapermcPaperLibraryAccessors = new IoPapermcPaperLibraryAccessors(owner);

        public IoPapermcLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>io.papermc.paper</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public IoPapermcPaperLibraryAccessors getPaper() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForIoPapermcPaperLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class IoPapermcPaperLibraryAccessors extends SubDependencyFactory {

        public IoPapermcPaperLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>api</b> with <b>io.papermc:paper-api</b> coordinates and
         * with version reference <b>io.papermc.paper.api</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getApi() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("io.papermc.paper.api");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class MeLibraryAccessors extends SubDependencyFactory {
        private final MeClipLibraryAccessors laccForMeClipLibraryAccessors = new MeClipLibraryAccessors(owner);

        public MeLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>me.clip</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public MeClipLibraryAccessors getClip() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForMeClipLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class MeClipLibraryAccessors extends SubDependencyFactory {

        public MeClipLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>placeholderapi</b> with <b>me.clip:placeholderapi</b> coordinates and
         * with version reference <b>me.clip.placeholderapi</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getPlaceholderapi() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("me.clip.placeholderapi");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class NetLibraryAccessors extends SubDependencyFactory {
        private final NetKyoriLibraryAccessors laccForNetKyoriLibraryAccessors = new NetKyoriLibraryAccessors(owner);
        private final NetObjecthunterLibraryAccessors laccForNetObjecthunterLibraryAccessors = new NetObjecthunterLibraryAccessors(owner);

        public NetLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>net.kyori</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public NetKyoriLibraryAccessors getKyori() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForNetKyoriLibraryAccessors;
        }

        /**
         * Group of libraries at <b>net.objecthunter</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public NetObjecthunterLibraryAccessors getObjecthunter() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForNetObjecthunterLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class NetKyoriLibraryAccessors extends SubDependencyFactory {
        private final NetKyoriAdventureLibraryAccessors laccForNetKyoriAdventureLibraryAccessors = new NetKyoriAdventureLibraryAccessors(owner);

        public NetKyoriLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>net.kyori.adventure</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public NetKyoriAdventureLibraryAccessors getAdventure() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForNetKyoriAdventureLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class NetKyoriAdventureLibraryAccessors extends SubDependencyFactory {
        private final NetKyoriAdventurePlatformLibraryAccessors laccForNetKyoriAdventurePlatformLibraryAccessors = new NetKyoriAdventurePlatformLibraryAccessors(owner);
        private final NetKyoriAdventureTextLibraryAccessors laccForNetKyoriAdventureTextLibraryAccessors = new NetKyoriAdventureTextLibraryAccessors(owner);

        public NetKyoriAdventureLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>net.kyori.adventure.platform</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public NetKyoriAdventurePlatformLibraryAccessors getPlatform() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForNetKyoriAdventurePlatformLibraryAccessors;
        }

        /**
         * Group of libraries at <b>net.kyori.adventure.text</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public NetKyoriAdventureTextLibraryAccessors getText() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForNetKyoriAdventureTextLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class NetKyoriAdventurePlatformLibraryAccessors extends SubDependencyFactory {

        public NetKyoriAdventurePlatformLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>bukkit</b> with <b>net.kyori:adventure-platform-bukkit</b> coordinates and
         * with version reference <b>net.kyori.adventure.platform.bukkit</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getBukkit() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("net.kyori.adventure.platform.bukkit");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class NetKyoriAdventureTextLibraryAccessors extends SubDependencyFactory {

        public NetKyoriAdventureTextLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>minimessage</b> with <b>net.kyori:adventure-text-minimessage</b> coordinates and
         * with version reference <b>net.kyori.adventure.text.minimessage</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getMinimessage() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("net.kyori.adventure.text.minimessage");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class NetObjecthunterLibraryAccessors extends SubDependencyFactory {

        public NetObjecthunterLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>exp4j</b> with <b>net.objecthunter:exp4j</b> coordinates and
         * with version reference <b>net.objecthunter.exp4j</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getExp4j() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("net.objecthunter.exp4j");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class OrgLibraryAccessors extends SubDependencyFactory {
        private final OrgBstatsLibraryAccessors laccForOrgBstatsLibraryAccessors = new OrgBstatsLibraryAccessors(owner);
        private final OrgMongodbLibraryAccessors laccForOrgMongodbLibraryAccessors = new OrgMongodbLibraryAccessors(owner);
        private final OrgProjectlombokLibraryAccessors laccForOrgProjectlombokLibraryAccessors = new OrgProjectlombokLibraryAccessors(owner);

        public OrgLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.bstats</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public OrgBstatsLibraryAccessors getBstats() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForOrgBstatsLibraryAccessors;
        }

        /**
         * Group of libraries at <b>org.mongodb</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public OrgMongodbLibraryAccessors getMongodb() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForOrgMongodbLibraryAccessors;
        }

        /**
         * Group of libraries at <b>org.projectlombok</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public OrgProjectlombokLibraryAccessors getProjectlombok() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForOrgProjectlombokLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class OrgBstatsLibraryAccessors extends SubDependencyFactory {
        private final OrgBstatsBstatsLibraryAccessors laccForOrgBstatsBstatsLibraryAccessors = new OrgBstatsBstatsLibraryAccessors(owner);

        public OrgBstatsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.bstats.bstats</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public OrgBstatsBstatsLibraryAccessors getBstats() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForOrgBstatsBstatsLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class OrgBstatsBstatsLibraryAccessors extends SubDependencyFactory {

        public OrgBstatsBstatsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>bukkit</b> with <b>org.bstats:bstats-bukkit</b> coordinates and
         * with version reference <b>org.bstats.bstats.bukkit</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getBukkit() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("org.bstats.bstats.bukkit");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class OrgMongodbLibraryAccessors extends SubDependencyFactory {
        private final OrgMongodbMongodbLibraryAccessors laccForOrgMongodbMongodbLibraryAccessors = new OrgMongodbMongodbLibraryAccessors(owner);

        public OrgMongodbLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.mongodb.mongodb</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public OrgMongodbMongodbLibraryAccessors getMongodb() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForOrgMongodbMongodbLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class OrgMongodbMongodbLibraryAccessors extends SubDependencyFactory {
        private final OrgMongodbMongodbDriverLibraryAccessors laccForOrgMongodbMongodbDriverLibraryAccessors = new OrgMongodbMongodbDriverLibraryAccessors(owner);

        public OrgMongodbMongodbLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.mongodb.mongodb.driver</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public OrgMongodbMongodbDriverLibraryAccessors getDriver() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForOrgMongodbMongodbDriverLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class OrgMongodbMongodbDriverLibraryAccessors extends SubDependencyFactory {

        public OrgMongodbMongodbDriverLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>sync</b> with <b>org.mongodb:mongodb-driver-sync</b> coordinates and
         * with version reference <b>org.mongodb.mongodb.driver.sync</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getSync() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("org.mongodb.mongodb.driver.sync");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class OrgProjectlombokLibraryAccessors extends SubDependencyFactory {

        public OrgProjectlombokLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>lombok</b> with <b>org.projectlombok:lombok</b> coordinates and
         * with version reference <b>org.projectlombok.lombok</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getLombok() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("org.projectlombok.lombok");
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class RedisLibraryAccessors extends SubDependencyFactory {
        private final RedisClientsLibraryAccessors laccForRedisClientsLibraryAccessors = new RedisClientsLibraryAccessors(owner);

        public RedisLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>redis.clients</b>
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public RedisClientsLibraryAccessors getClients() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return laccForRedisClientsLibraryAccessors;
        }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class RedisClientsLibraryAccessors extends SubDependencyFactory {

        public RedisClientsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>jedis</b> with <b>redis.clients:jedis</b> coordinates and
         * with version reference <b>redis.clients.jedis</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         *
         * @deprecated Will be removed in Gradle 9.0.
         */
        @Deprecated
        public Provider<MinimalExternalModuleDependency> getJedis() {
            org.gradle.internal.deprecation.DeprecationLogger.deprecateBehaviour("Accessing libraries or bundles from version catalogs in the plugins block.").withAdvice("Only use versions or plugins from catalogs in the plugins block.").willBeRemovedInGradle9().withUpgradeGuideSection(8, "kotlin_dsl_deprecated_catalogs_plugins_block").nagUser();
            return create("redis.clients.jedis");
        }

    }

    public static class VersionAccessors extends VersionFactory  {

        private final ComVersionAccessors vaccForComVersionAccessors = new ComVersionAccessors(providers, config);
        private final DeVersionAccessors vaccForDeVersionAccessors = new DeVersionAccessors(providers, config);
        private final IoVersionAccessors vaccForIoVersionAccessors = new IoVersionAccessors(providers, config);
        private final MeVersionAccessors vaccForMeVersionAccessors = new MeVersionAccessors(providers, config);
        private final NetVersionAccessors vaccForNetVersionAccessors = new NetVersionAccessors(providers, config);
        private final OrgVersionAccessors vaccForOrgVersionAccessors = new OrgVersionAccessors(providers, config);
        private final RedisVersionAccessors vaccForRedisVersionAccessors = new RedisVersionAccessors(providers, config);
        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com</b>
         */
        public ComVersionAccessors getCom() {
            return vaccForComVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.de</b>
         */
        public DeVersionAccessors getDe() {
            return vaccForDeVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.io</b>
         */
        public IoVersionAccessors getIo() {
            return vaccForIoVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.me</b>
         */
        public MeVersionAccessors getMe() {
            return vaccForMeVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.net</b>
         */
        public NetVersionAccessors getNet() {
            return vaccForNetVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.org</b>
         */
        public OrgVersionAccessors getOrg() {
            return vaccForOrgVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.redis</b>
         */
        public RedisVersionAccessors getRedis() {
            return vaccForRedisVersionAccessors;
        }

    }

    public static class ComVersionAccessors extends VersionFactory  {

        private final ComComphenixVersionAccessors vaccForComComphenixVersionAccessors = new ComComphenixVersionAccessors(providers, config);
        private final ComGithubVersionAccessors vaccForComGithubVersionAccessors = new ComGithubVersionAccessors(providers, config);
        private final ComJeffVersionAccessors vaccForComJeffVersionAccessors = new ComJeffVersionAccessors(providers, config);
        private final ComSk89qVersionAccessors vaccForComSk89qVersionAccessors = new ComSk89qVersionAccessors(providers, config);
        private final ComZaxxerVersionAccessors vaccForComZaxxerVersionAccessors = new ComZaxxerVersionAccessors(providers, config);
        public ComVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.comphenix</b>
         */
        public ComComphenixVersionAccessors getComphenix() {
            return vaccForComComphenixVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.com.github</b>
         */
        public ComGithubVersionAccessors getGithub() {
            return vaccForComGithubVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.com.jeff</b>
         */
        public ComJeffVersionAccessors getJeff() {
            return vaccForComJeffVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.com.sk89q</b>
         */
        public ComSk89qVersionAccessors getSk89q() {
            return vaccForComSk89qVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.com.zaxxer</b>
         */
        public ComZaxxerVersionAccessors getZaxxer() {
            return vaccForComZaxxerVersionAccessors;
        }

    }

    public static class ComComphenixVersionAccessors extends VersionFactory  {

        private final ComComphenixProtocolVersionAccessors vaccForComComphenixProtocolVersionAccessors = new ComComphenixProtocolVersionAccessors(providers, config);
        public ComComphenixVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.comphenix.protocol</b>
         */
        public ComComphenixProtocolVersionAccessors getProtocol() {
            return vaccForComComphenixProtocolVersionAccessors;
        }

    }

    public static class ComComphenixProtocolVersionAccessors extends VersionFactory  {

        public ComComphenixProtocolVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>com.comphenix.protocol.protocollib</b> with value <b>5.3.0-SNAPSHOT</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getProtocollib() { return getVersion("com.comphenix.protocol.protocollib"); }

    }

    public static class ComGithubVersionAccessors extends VersionFactory  {

        private final ComGithubMilkbowlVersionAccessors vaccForComGithubMilkbowlVersionAccessors = new ComGithubMilkbowlVersionAccessors(providers, config);
        public ComGithubVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.github.milkbowl</b>
         */
        public ComGithubMilkbowlVersionAccessors getMilkbowl() {
            return vaccForComGithubMilkbowlVersionAccessors;
        }

    }

    public static class ComGithubMilkbowlVersionAccessors extends VersionFactory  {

        public ComGithubMilkbowlVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>com.github.milkbowl.vaultapi</b> with value <b>1.7</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getVaultapi() { return getVersion("com.github.milkbowl.vaultapi"); }

    }

    public static class ComJeffVersionAccessors extends VersionFactory  {

        private final ComJeffMediaVersionAccessors vaccForComJeffMediaVersionAccessors = new ComJeffMediaVersionAccessors(providers, config);
        public ComJeffVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.jeff.media</b>
         */
        public ComJeffMediaVersionAccessors getMedia() {
            return vaccForComJeffMediaVersionAccessors;
        }

    }

    public static class ComJeffMediaVersionAccessors extends VersionFactory  {

        private final ComJeffMediaCustomVersionAccessors vaccForComJeffMediaCustomVersionAccessors = new ComJeffMediaCustomVersionAccessors(providers, config);
        public ComJeffMediaVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.jeff.media.custom</b>
         */
        public ComJeffMediaCustomVersionAccessors getCustom() {
            return vaccForComJeffMediaCustomVersionAccessors;
        }

    }

    public static class ComJeffMediaCustomVersionAccessors extends VersionFactory  {

        private final ComJeffMediaCustomBlockVersionAccessors vaccForComJeffMediaCustomBlockVersionAccessors = new ComJeffMediaCustomBlockVersionAccessors(providers, config);
        public ComJeffMediaCustomVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.jeff.media.custom.block</b>
         */
        public ComJeffMediaCustomBlockVersionAccessors getBlock() {
            return vaccForComJeffMediaCustomBlockVersionAccessors;
        }

    }

    public static class ComJeffMediaCustomBlockVersionAccessors extends VersionFactory  {

        public ComJeffMediaCustomBlockVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>com.jeff.media.custom.block.data</b> with value <b>2.2.3</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getData() { return getVersion("com.jeff.media.custom.block.data"); }

    }

    public static class ComSk89qVersionAccessors extends VersionFactory  {

        private final ComSk89qWorldeditVersionAccessors vaccForComSk89qWorldeditVersionAccessors = new ComSk89qWorldeditVersionAccessors(providers, config);
        private final ComSk89qWorldguardVersionAccessors vaccForComSk89qWorldguardVersionAccessors = new ComSk89qWorldguardVersionAccessors(providers, config);
        public ComSk89qVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.sk89q.worldedit</b>
         */
        public ComSk89qWorldeditVersionAccessors getWorldedit() {
            return vaccForComSk89qWorldeditVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.com.sk89q.worldguard</b>
         */
        public ComSk89qWorldguardVersionAccessors getWorldguard() {
            return vaccForComSk89qWorldguardVersionAccessors;
        }

    }

    public static class ComSk89qWorldeditVersionAccessors extends VersionFactory  {

        private final ComSk89qWorldeditWorldeditVersionAccessors vaccForComSk89qWorldeditWorldeditVersionAccessors = new ComSk89qWorldeditWorldeditVersionAccessors(providers, config);
        public ComSk89qWorldeditVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.sk89q.worldedit.worldedit</b>
         */
        public ComSk89qWorldeditWorldeditVersionAccessors getWorldedit() {
            return vaccForComSk89qWorldeditWorldeditVersionAccessors;
        }

    }

    public static class ComSk89qWorldeditWorldeditVersionAccessors extends VersionFactory  {

        public ComSk89qWorldeditWorldeditVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>com.sk89q.worldedit.worldedit.bukkit</b> with value <b>7.2.7-SNAPSHOT</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getBukkit() { return getVersion("com.sk89q.worldedit.worldedit.bukkit"); }

    }

    public static class ComSk89qWorldguardVersionAccessors extends VersionFactory  {

        private final ComSk89qWorldguardWorldguardVersionAccessors vaccForComSk89qWorldguardWorldguardVersionAccessors = new ComSk89qWorldguardWorldguardVersionAccessors(providers, config);
        public ComSk89qWorldguardVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.com.sk89q.worldguard.worldguard</b>
         */
        public ComSk89qWorldguardWorldguardVersionAccessors getWorldguard() {
            return vaccForComSk89qWorldguardWorldguardVersionAccessors;
        }

    }

    public static class ComSk89qWorldguardWorldguardVersionAccessors extends VersionFactory  {

        public ComSk89qWorldguardWorldguardVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>com.sk89q.worldguard.worldguard.bukkit</b> with value <b>7.0.13-SNAPSHOT</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getBukkit() { return getVersion("com.sk89q.worldguard.worldguard.bukkit"); }

    }

    public static class ComZaxxerVersionAccessors extends VersionFactory  {

        public ComZaxxerVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>com.zaxxer.hikaricp</b> with value <b>6.0.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getHikaricp() { return getVersion("com.zaxxer.hikaricp"); }

    }

    public static class DeVersionAccessors extends VersionFactory  {

        private final DeTr7zwVersionAccessors vaccForDeTr7zwVersionAccessors = new DeTr7zwVersionAccessors(providers, config);
        public DeVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.de.tr7zw</b>
         */
        public DeTr7zwVersionAccessors getTr7zw() {
            return vaccForDeTr7zwVersionAccessors;
        }

    }

    public static class DeTr7zwVersionAccessors extends VersionFactory  {

        private final DeTr7zwItemVersionAccessors vaccForDeTr7zwItemVersionAccessors = new DeTr7zwItemVersionAccessors(providers, config);
        public DeTr7zwVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.de.tr7zw.item</b>
         */
        public DeTr7zwItemVersionAccessors getItem() {
            return vaccForDeTr7zwItemVersionAccessors;
        }

    }

    public static class DeTr7zwItemVersionAccessors extends VersionFactory  {

        private final DeTr7zwItemNbtVersionAccessors vaccForDeTr7zwItemNbtVersionAccessors = new DeTr7zwItemNbtVersionAccessors(providers, config);
        public DeTr7zwItemVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.de.tr7zw.item.nbt</b>
         */
        public DeTr7zwItemNbtVersionAccessors getNbt() {
            return vaccForDeTr7zwItemNbtVersionAccessors;
        }

    }

    public static class DeTr7zwItemNbtVersionAccessors extends VersionFactory  {

        private final DeTr7zwItemNbtApiVersionAccessors vaccForDeTr7zwItemNbtApiVersionAccessors = new DeTr7zwItemNbtApiVersionAccessors(providers, config);
        public DeTr7zwItemNbtVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.de.tr7zw.item.nbt.api</b>
         */
        public DeTr7zwItemNbtApiVersionAccessors getApi() {
            return vaccForDeTr7zwItemNbtApiVersionAccessors;
        }

    }

    public static class DeTr7zwItemNbtApiVersionAccessors extends VersionFactory  {

        public DeTr7zwItemNbtApiVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>de.tr7zw.item.nbt.api.plugin</b> with value <b>2.13.2</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getPlugin() { return getVersion("de.tr7zw.item.nbt.api.plugin"); }

    }

    public static class IoVersionAccessors extends VersionFactory  {

        private final IoPapermcVersionAccessors vaccForIoPapermcVersionAccessors = new IoPapermcVersionAccessors(providers, config);
        public IoVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.io.papermc</b>
         */
        public IoPapermcVersionAccessors getPapermc() {
            return vaccForIoPapermcVersionAccessors;
        }

    }

    public static class IoPapermcVersionAccessors extends VersionFactory  {

        private final IoPapermcPaperVersionAccessors vaccForIoPapermcPaperVersionAccessors = new IoPapermcPaperVersionAccessors(providers, config);
        public IoPapermcVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.io.papermc.paper</b>
         */
        public IoPapermcPaperVersionAccessors getPaper() {
            return vaccForIoPapermcPaperVersionAccessors;
        }

    }

    public static class IoPapermcPaperVersionAccessors extends VersionFactory  {

        public IoPapermcPaperVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>io.papermc.paper.api</b> with value <b>1.21.1-R0.1-SNAPSHOT</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getApi() { return getVersion("io.papermc.paper.api"); }

    }

    public static class MeVersionAccessors extends VersionFactory  {

        private final MeClipVersionAccessors vaccForMeClipVersionAccessors = new MeClipVersionAccessors(providers, config);
        public MeVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.me.clip</b>
         */
        public MeClipVersionAccessors getClip() {
            return vaccForMeClipVersionAccessors;
        }

    }

    public static class MeClipVersionAccessors extends VersionFactory  {

        public MeClipVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>me.clip.placeholderapi</b> with value <b>2.11.6</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getPlaceholderapi() { return getVersion("me.clip.placeholderapi"); }

    }

    public static class NetVersionAccessors extends VersionFactory  {

        private final NetKyoriVersionAccessors vaccForNetKyoriVersionAccessors = new NetKyoriVersionAccessors(providers, config);
        private final NetObjecthunterVersionAccessors vaccForNetObjecthunterVersionAccessors = new NetObjecthunterVersionAccessors(providers, config);
        public NetVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.net.kyori</b>
         */
        public NetKyoriVersionAccessors getKyori() {
            return vaccForNetKyoriVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.net.objecthunter</b>
         */
        public NetObjecthunterVersionAccessors getObjecthunter() {
            return vaccForNetObjecthunterVersionAccessors;
        }

    }

    public static class NetKyoriVersionAccessors extends VersionFactory  {

        private final NetKyoriAdventureVersionAccessors vaccForNetKyoriAdventureVersionAccessors = new NetKyoriAdventureVersionAccessors(providers, config);
        public NetKyoriVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.net.kyori.adventure</b>
         */
        public NetKyoriAdventureVersionAccessors getAdventure() {
            return vaccForNetKyoriAdventureVersionAccessors;
        }

    }

    public static class NetKyoriAdventureVersionAccessors extends VersionFactory  {

        private final NetKyoriAdventurePlatformVersionAccessors vaccForNetKyoriAdventurePlatformVersionAccessors = new NetKyoriAdventurePlatformVersionAccessors(providers, config);
        private final NetKyoriAdventureTextVersionAccessors vaccForNetKyoriAdventureTextVersionAccessors = new NetKyoriAdventureTextVersionAccessors(providers, config);
        public NetKyoriAdventureVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.net.kyori.adventure.platform</b>
         */
        public NetKyoriAdventurePlatformVersionAccessors getPlatform() {
            return vaccForNetKyoriAdventurePlatformVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.net.kyori.adventure.text</b>
         */
        public NetKyoriAdventureTextVersionAccessors getText() {
            return vaccForNetKyoriAdventureTextVersionAccessors;
        }

    }

    public static class NetKyoriAdventurePlatformVersionAccessors extends VersionFactory  {

        public NetKyoriAdventurePlatformVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>net.kyori.adventure.platform.bukkit</b> with value <b>4.3.5-SNAPSHOT</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getBukkit() { return getVersion("net.kyori.adventure.platform.bukkit"); }

    }

    public static class NetKyoriAdventureTextVersionAccessors extends VersionFactory  {

        public NetKyoriAdventureTextVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>net.kyori.adventure.text.minimessage</b> with value <b>4.18.0-SNAPSHOT</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getMinimessage() { return getVersion("net.kyori.adventure.text.minimessage"); }

    }

    public static class NetObjecthunterVersionAccessors extends VersionFactory  {

        public NetObjecthunterVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>net.objecthunter.exp4j</b> with value <b>0.4.8</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getExp4j() { return getVersion("net.objecthunter.exp4j"); }

    }

    public static class OrgVersionAccessors extends VersionFactory  {

        private final OrgBstatsVersionAccessors vaccForOrgBstatsVersionAccessors = new OrgBstatsVersionAccessors(providers, config);
        private final OrgMongodbVersionAccessors vaccForOrgMongodbVersionAccessors = new OrgMongodbVersionAccessors(providers, config);
        private final OrgProjectlombokVersionAccessors vaccForOrgProjectlombokVersionAccessors = new OrgProjectlombokVersionAccessors(providers, config);
        public OrgVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.bstats</b>
         */
        public OrgBstatsVersionAccessors getBstats() {
            return vaccForOrgBstatsVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.org.mongodb</b>
         */
        public OrgMongodbVersionAccessors getMongodb() {
            return vaccForOrgMongodbVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.org.projectlombok</b>
         */
        public OrgProjectlombokVersionAccessors getProjectlombok() {
            return vaccForOrgProjectlombokVersionAccessors;
        }

    }

    public static class OrgBstatsVersionAccessors extends VersionFactory  {

        private final OrgBstatsBstatsVersionAccessors vaccForOrgBstatsBstatsVersionAccessors = new OrgBstatsBstatsVersionAccessors(providers, config);
        public OrgBstatsVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.bstats.bstats</b>
         */
        public OrgBstatsBstatsVersionAccessors getBstats() {
            return vaccForOrgBstatsBstatsVersionAccessors;
        }

    }

    public static class OrgBstatsBstatsVersionAccessors extends VersionFactory  {

        public OrgBstatsBstatsVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>org.bstats.bstats.bukkit</b> with value <b>3.1.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getBukkit() { return getVersion("org.bstats.bstats.bukkit"); }

    }

    public static class OrgMongodbVersionAccessors extends VersionFactory  {

        private final OrgMongodbMongodbVersionAccessors vaccForOrgMongodbMongodbVersionAccessors = new OrgMongodbMongodbVersionAccessors(providers, config);
        public OrgMongodbVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.mongodb.mongodb</b>
         */
        public OrgMongodbMongodbVersionAccessors getMongodb() {
            return vaccForOrgMongodbMongodbVersionAccessors;
        }

    }

    public static class OrgMongodbMongodbVersionAccessors extends VersionFactory  {

        private final OrgMongodbMongodbDriverVersionAccessors vaccForOrgMongodbMongodbDriverVersionAccessors = new OrgMongodbMongodbDriverVersionAccessors(providers, config);
        public OrgMongodbMongodbVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.org.mongodb.mongodb.driver</b>
         */
        public OrgMongodbMongodbDriverVersionAccessors getDriver() {
            return vaccForOrgMongodbMongodbDriverVersionAccessors;
        }

    }

    public static class OrgMongodbMongodbDriverVersionAccessors extends VersionFactory  {

        public OrgMongodbMongodbDriverVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>org.mongodb.mongodb.driver.sync</b> with value <b>5.2.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getSync() { return getVersion("org.mongodb.mongodb.driver.sync"); }

    }

    public static class OrgProjectlombokVersionAccessors extends VersionFactory  {

        public OrgProjectlombokVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>org.projectlombok.lombok</b> with value <b>1.18.34</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getLombok() { return getVersion("org.projectlombok.lombok"); }

    }

    public static class RedisVersionAccessors extends VersionFactory  {

        private final RedisClientsVersionAccessors vaccForRedisClientsVersionAccessors = new RedisClientsVersionAccessors(providers, config);
        public RedisVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of versions at <b>versions.redis.clients</b>
         */
        public RedisClientsVersionAccessors getClients() {
            return vaccForRedisClientsVersionAccessors;
        }

    }

    public static class RedisClientsVersionAccessors extends VersionFactory  {

        public RedisClientsVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>redis.clients.jedis</b> with value <b>5.1.5</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJedis() { return getVersion("redis.clients.jedis"); }

    }

    /**
     * @deprecated Will be removed in Gradle 9.0.
     */
    @Deprecated
    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

    }

    public static class PluginAccessors extends PluginFactory {

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

    }

}
