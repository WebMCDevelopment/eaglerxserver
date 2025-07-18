/*
 * Copyright (c) 2025 lax1dude. All Rights Reserved.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package net.lax1dude.eaglercraft.backend.server.base;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import javax.net.ssl.SSLException;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.MapMaker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import net.lax1dude.eaglercraft.backend.server.adapter.AbortLoadException;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerImpl;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerListener;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatform;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformComponentBuilder;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformComponentHelper;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformLogger;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformPlayer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformServer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformTask;
import net.lax1dude.eaglercraft.backend.server.adapter.event.IEventDispatchAdapter;
import net.lax1dude.eaglercraft.backend.server.api.EnumPlatformType;
import net.lax1dude.eaglercraft.backend.server.api.ExtendedCapabilitySpec;
import net.lax1dude.eaglercraft.backend.server.api.IBasePlayer;
import net.lax1dude.eaglercraft.backend.server.api.IBinaryHTTPClient;
import net.lax1dude.eaglercraft.backend.server.api.IComponentSerializer;
import net.lax1dude.eaglercraft.backend.server.api.IComponentHelper;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerListenerInfo;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerPlayer;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.IPacketImageLoader;
import net.lax1dude.eaglercraft.backend.server.api.IScheduler;
import net.lax1dude.eaglercraft.backend.server.api.IServerIconLoader;
import net.lax1dude.eaglercraft.backend.server.api.IUpdateCertificate;
import net.lax1dude.eaglercraft.backend.server.api.attribute.IAttributeKey;
import net.lax1dude.eaglercraft.backend.server.api.attribute.IAttributeManager;
import net.lax1dude.eaglercraft.backend.server.api.collect.HPPC;
import net.lax1dude.eaglercraft.backend.server.api.internal.factory.IEaglerAPIFactory;
import net.lax1dude.eaglercraft.backend.server.api.nbt.INBTHelper;
import net.lax1dude.eaglercraft.backend.server.api.rewind.IEaglerXRewindProtocol;
import net.lax1dude.eaglercraft.backend.server.api.skins.TexturesProperty;
import net.lax1dude.eaglercraft.backend.server.base.collect.HPPCFactory;
import net.lax1dude.eaglercraft.backend.server.base.command.CommandBrand;
import net.lax1dude.eaglercraft.backend.server.base.command.CommandConfirmCode;
import net.lax1dude.eaglercraft.backend.server.base.command.CommandDomain;
import net.lax1dude.eaglercraft.backend.server.base.command.CommandProtocol;
import net.lax1dude.eaglercraft.backend.server.base.command.CommandUserAgent;
import net.lax1dude.eaglercraft.backend.server.base.command.CommandVersion;
import net.lax1dude.eaglercraft.backend.server.base.config.ConfigDataListener;
import net.lax1dude.eaglercraft.backend.server.base.config.ConfigDataPauseMenu;
import net.lax1dude.eaglercraft.backend.server.base.config.ConfigDataRoot;
import net.lax1dude.eaglercraft.backend.server.base.config.ConfigDataSupervisor;
import net.lax1dude.eaglercraft.backend.server.base.config.ConfigDataSettings.ConfigDataSkinService;
import net.lax1dude.eaglercraft.backend.server.base.config.ConfigDataSettings.ConfigDataVoiceService;
import net.lax1dude.eaglercraft.backend.server.base.message.MessageControllerFactory;
import net.lax1dude.eaglercraft.backend.server.base.message.PlayerChannelHelper;
import net.lax1dude.eaglercraft.backend.server.base.nbt.NBTHelper;
import net.lax1dude.eaglercraft.backend.server.base.notifications.NotificationService;
import net.lax1dude.eaglercraft.backend.server.base.pause_menu.PauseMenuService;
import net.lax1dude.eaglercraft.backend.server.base.config.EaglerConfigLoader;
import net.lax1dude.eaglercraft.backend.server.base.pipeline.PipelineTransformer;
import net.lax1dude.eaglercraft.backend.server.base.query.QueryServer;
import net.lax1dude.eaglercraft.backend.server.base.rpc.BackendChannelHelper;
import net.lax1dude.eaglercraft.backend.server.base.rpc.BackendRPCService;
import net.lax1dude.eaglercraft.backend.server.base.skins.ProfileResolver;
import net.lax1dude.eaglercraft.backend.server.base.skins.SimpleProfileCache;
import net.lax1dude.eaglercraft.backend.server.base.skins.SkinService;
import net.lax1dude.eaglercraft.backend.server.base.supervisor.ISupervisorServiceImpl;
import net.lax1dude.eaglercraft.backend.server.base.supervisor.SupervisorService;
import net.lax1dude.eaglercraft.backend.server.base.supervisor.SupervisorServiceDisabled;
import net.lax1dude.eaglercraft.backend.server.base.update.IUpdateCertificateImpl;
import net.lax1dude.eaglercraft.backend.server.base.update.UpdateCertificate;
import net.lax1dude.eaglercraft.backend.server.base.update.UpdateService;
import net.lax1dude.eaglercraft.backend.server.base.voice.IVoiceServiceImpl;
import net.lax1dude.eaglercraft.backend.server.base.voice.VoiceServiceDisabled;
import net.lax1dude.eaglercraft.backend.server.base.voice.VoiceServiceLocal;
import net.lax1dude.eaglercraft.backend.server.base.voice.VoiceServiceRemote;
import net.lax1dude.eaglercraft.backend.server.base.webserver.WebServer;
import net.lax1dude.eaglercraft.backend.server.base.webview.WebViewService;
import net.lax1dude.eaglercraft.backend.server.util.GsonLenient;
import net.lax1dude.eaglercraft.backend.server.util.Util;
import net.lax1dude.eaglercraft.backend.skin_cache.HTTPClient;
import net.lax1dude.eaglercraft.backend.skin_cache.IHTTPClient;
import net.lax1dude.eaglercraft.backend.skin_cache.SkinCacheDatastore;
import net.lax1dude.eaglercraft.backend.skin_cache.SkinCacheDownloader;
import net.lax1dude.eaglercraft.backend.skin_cache.SkinCacheService;
import net.lax1dude.eaglercraft.backend.util.EaglerDrivers;
import net.lax1dude.eaglercraft.v1_8.socket.protocol.GamePluginMessageProtocol;
import net.lax1dude.eaglercraft.v1_8.socket.protocol.pkt.server.SPacketClientStateFlagV5EAG;
import net.lax1dude.eaglercraft.v1_8.socket.protocol.pkt.server.SPacketOtherPlayerClientUUIDV4EAG;

public class EaglerXServer<PlayerObject> implements IEaglerXServerImpl<PlayerObject>, IEaglerAPIFactory,
		IEaglerXServerAPI<PlayerObject>, IEaglerXServerAPI.NettyUnsafe {

	public static final Gson GSON_PRETTY = GsonLenient.setLenient(new GsonBuilder()).setPrettyPrinting().create();
	public static final Interner<UUID> uuidInterner = Interners.newWeakInterner();

	private final EaglerAttributeManager attributeManager = APIFactoryImpl.INSTANCE.getEaglerAttribManager();
	private final EaglerAttributeManager.EaglerAttributeHolder attributeHolder = attributeManager.createEaglerHolder();

	private boolean hasStartedLoading = false;
	private IPlatform<PlayerObject> platform;
	private EnumPlatformType platformType;
	private Class<PlayerObject> playerClazz;
	private Set<Class<?>> playerClassSet;
	private ConfigDataRoot config;
	private IEventDispatchAdapter<PlayerObject, ?> eventDispatcher;
	private Set<EaglerPlayerInstance<PlayerObject>> eaglerPlayers;
	private BrandService<PlayerObject> brandRegistry;
	private Map<String, EaglerListener> listeners;
	private Map<SocketAddress, EaglerListener> listenersByAddress;
	private QueryServer queryServer;
	private WebServer webServer;
	private RewindService<PlayerObject> rewindService;
	private PipelineTransformer pipelineTransformer;
	private ExtCapabilityMap extCapabilityMap;
	private SSLCertificateManager certificateManager;
	private IPlatformTask certificateRefreshTask;
	private String serverListConfirmCode;
	private Class<?> componentType;
	private Set<Class<?>> componentTypeSet;
	private ComponentHelper<?> componentHelper;
	private IHTTPClient httpClient;
	private BinaryHTTPClient httpClientAPI;
	private ProfileResolver profileResolver;
	private TexturesProperty eaglerPlayersVanillaSkin;
	private boolean isEaglerPlayerProperyEnabled;
	private SkinService<PlayerObject> skinService;
	private DeferredStartSkinCache skinCacheService;
	private Connection skinCacheJDBCHandle;
	private IVoiceServiceImpl<PlayerObject> voiceService;
	private NotificationService<PlayerObject> notificationService;
	private WebViewService<PlayerObject> webViewService;
	private PauseMenuService<PlayerObject> pauseMenuService;
	private UpdateService updateService;
	private UpdateChecker updateChecker;
	private BackendRPCService<PlayerObject> backendRPCService;
	private ISupervisorServiceImpl<PlayerObject> supervisorService;
	private PlayerRateLimits.RateLimitParams ratelimitParams;

	public EaglerXServer() {
	}

	@Override
	public void load(IPlatform.Init<PlayerObject> init) {
		if (hasStartedLoading) {
			throw new IllegalStateException();
		}
		hasStartedLoading = true;
		eaglerPlayers = Collections.newSetFromMap((new MapMaker()).initialCapacity(512).concurrencyLevel(16).makeMap());
		platform = init.getPlatform();
		playerClazz = platform.getPlayerClass();
		playerClassSet = Collections.singleton(playerClazz);
		platformType = switch (platform.getType()) {
		case BUNGEE -> EnumPlatformType.BUNGEECORD;
		case BUKKIT -> EnumPlatformType.BUKKIT;
		case VELOCITY -> EnumPlatformType.VELOCITY;
		default -> EnumPlatformType.STANDALONE;
		};

		if (platformType != EnumPlatformType.BUKKIT) {
			logger().info("Loading " + getServerBrand() + " " + getServerVersion() + "...");
		}

		logger().info("(Platform: " + platformType.getName() + ")");

		if (platformType == EnumPlatformType.BUKKIT) {
			logger().warn("Note: Its highly recommended to install EaglerXServer on BungeeCord or "
					+ "Velocity instead, you will have a much better experience");
			logger().warn("Note: If you are not using Spigot (or a derivative like Paper) or a version "
					+ "above 1.12.2, things probably won't work right");
			if (platform.isModernPluginChannelNamesOnly()) {
				logger().error("Detected a modern server version, things probably won't work right, "
						+ "downgrade to 1.12.2 or below");
			}
		}

		eventDispatcher = platform.eventDispatcher();

		try {
			config = EaglerConfigLoader.loadConfig(platform);
		} catch (IOException e) {
			throw new AbortLoadException("Could not read one or more config files!", e);
		}

		logger().info("Server Name: \"" + config.getSettings().getServerName() + "\"");

		brandRegistry = new BrandService<>(this);
		queryServer = new QueryServer(this);
		webServer = new WebServer(this);
		rewindService = new RewindService<>(this);
		pipelineTransformer = new PipelineTransformer(this, rewindService);
		extCapabilityMap = new ExtCapabilityMap();
		certificateManager = new SSLCertificateManager(logger());
		componentType = componentHelper().getComponentType();
		componentTypeSet = Collections.singleton(componentType);
		componentHelper = new ComponentHelper<>(componentHelper());
		if (Util.classExists("io.netty.handler.ssl.SslContextBuilder")
				&& Util.classExists("io.netty.handler.codec.http.HttpHeaderNames")) {
			httpClient = new HTTPClient(() -> bootstrapClient(null), "Mozilla/5.0 " + getServerVersionString());
		} else {
			logger().warn("Using legacy JDK-based HTTP client because Netty is too outdated");
			httpClient = new LegacyInternalHTTPClient(platform.getScheduler(),
					"Mozilla/5.0 " + getServerVersionString());
		}
		httpClientAPI = new BinaryHTTPClient(httpClient);
		profileResolver = new ProfileResolver(this, httpClient);

		ConfigDataSkinService skinSvcConf = config.getSettings().getSkinService();
		ConfigDataSupervisor supervisorConf = config.getSupervisor();
		if (supervisorConf != null && supervisorConf.isEnableSupervisor()) {
			supervisorService = new SupervisorService<>(this);
			skinService = new SkinService<>(this, null, skinSvcConf.getFNAWSkinsPredicate(),
					skinSvcConf.isDownloadVanillaSkinsToClients());
		} else {
			supervisorService = new SupervisorServiceDisabled<>(this);
			if (skinSvcConf.isDownloadVanillaSkinsToClients()) {
				skinCacheService = new DeferredStartSkinCache();
				skinService = new SkinService<>(this, skinCacheService, skinSvcConf.getFNAWSkinsPredicate(), true);
			} else {
				skinService = new SkinService<>(this, null, skinSvcConf.getFNAWSkinsPredicate(), false);
			}
		}

		isEaglerPlayerProperyEnabled = config.getSettings().isEnableIsEaglerPlayerProperty();

		eaglerPlayersVanillaSkin = null;
		File vanillaSkinCache = new File("eagler_vanilla_skin_cache.json");
		String vanillaSkin = config.getSettings().getEaglerPlayersVanillaSkin();
		if (vanillaSkin != null) {
			SimpleProfileCache.loadProfile(this, vanillaSkinCache, vanillaSkin, 7l * 86400000l, (res) -> {
				if (res != null) {
					logger().info("Loaded vanilla profile: \"" + vanillaSkin + "\"");
					eaglerPlayersVanillaSkin = res;
				}
			});
		} else {
			vanillaSkinCache.delete();
		}

		ConfigDataVoiceService voiceConfig = config.getSettings().getVoiceService();
		if (voiceConfig.isEnableVoiceService()) {
			if (voiceConfig.isVoiceBackendRelayMode()) {
				voiceService = new VoiceServiceRemote<>(this);
			} else {
				voiceService = new VoiceServiceLocal<>(this, voiceConfig);
			}
			voiceService.setICEServers(config.getICEServers());
		} else {
			voiceService = new VoiceServiceDisabled<>(this);
		}

		notificationService = new NotificationService<>(this);

		webViewService = new WebViewService<>(this);
		webViewService.setTemplateGlobal("server_name", getServerName());
		webViewService.setTemplateGlobal("plugin_name", getServerBrand());
		webViewService.setTemplateGlobal("plugin_version", getServerVersion());
		webViewService.setTemplateGlobal("plugin_authors", EaglerXServerVersion.AUTHOR);
		config.getPauseMenu().getServerInfoButtonEmbedTemplateGlobals().forEach(webViewService::setTemplateGlobal);

		pauseMenuService = new PauseMenuService<>(this);

		ConfigDataPauseMenu pauseMenuConf = config.getPauseMenu();
		if (pauseMenuConf.isEnableCustomPauseMenu()) {
			try {
				pauseMenuService.reloadDefaultPauseMenu(platform.getDataFolder(), pauseMenuConf);
			} catch (IOException e) {
				logger().error("Could not load custom pause menu!", e);
				pauseMenuService.setDefaultPauseMenu(pauseMenuService.getVanillaPauseMenu());
			}
		}

		if (config.getSettings().getUpdateService().isEnableUpdateSystem()) {
			updateService = new UpdateService(this);
		}

		updateChecker = new UpdateChecker(this, config.getSettings().getUpdateChecker());

		if (config.getSettings().isEnableBackendRPCAPI() && platform.getType().proxy) {
			backendRPCService = new BackendRPCService<>(this);
		}

		ratelimitParams = new PlayerRateLimits.RateLimitParams(skinSvcConf.getSkinLookupRatelimit(),
				skinSvcConf.getCapeLookupRatelimit(), voiceConfig.getVoiceConnectRatelimit(),
				voiceConfig.getVoiceRequestRatelimit(), voiceConfig.getVoiceICERatelimit(),
				config.getSettings().getBrandLookupRatelimit(), config.getSettings().getWebviewDownloadRatelimit(),
				config.getSettings().getWebviewMessageRatelimit(), skinSvcConf.getSkinCacheAntagonistsRatelimit(),
				supervisorConf != null ? supervisorConf.getSupervisorSkinAntagonistsRatelimit() : 0,
				supervisorConf != null ? supervisorConf.getSupervisorBrandAntagonistsRatelimit() : 0);

		init.setOnServerEnable(this::enableHandler);
		init.setOnServerDisable(this::disableHandler);
		init.setPipelineInitializer(new EaglerXServerNettyPipelineInitializer<>(this));
		init.setConnectionInitializer(new EaglerXServerLoginInitializer<>(this));
		init.setPlayerInitializer(new EaglerXServerPlayerInitializer<>(this));
		init.setServerJoinListener(new EaglerXServerJoinListener<>(this));
		init.setCommandRegistry(
				Arrays.asList(new CommandVersion<>(this), new CommandBrand<>(this), new CommandProtocol<>(this),
						new CommandDomain<>(this), new CommandUserAgent<>(this), new CommandConfirmCode<>(this)));

		if (platform.getType().proxy) {
			loadProxying((IPlatform.InitProxying<PlayerObject>) init);
		} else {
			loadNonProxying((IPlatform.InitNonProxying<PlayerObject>) init);
		}

		eventDispatcher.setAPI(this);
		APIFactoryImpl.INSTANCE.initialize(playerClazz, this);
	}

	private void loadProxying(IPlatform.InitProxying<PlayerObject> init) {
		ImmutableMap.Builder<String, EaglerListener> listenersBuilder = ImmutableMap.builder();
		ImmutableMap.Builder<SocketAddress, EaglerListener> listenersByAddressBuilder = ImmutableMap.builder();
		ImmutableList.Builder<IEaglerXServerListener> listenersImpl = ImmutableList.builder();
		for (ConfigDataListener listener : config.getListeners().values()) {
			EaglerListener eagListener;
			try {
				eagListener = new EaglerListener(this, listener);
			} catch (SSLException ex) {
				throw new AbortLoadException("TLS configuration is invalid!", ex);
			} catch (IOException ex) {
				throw new AbortLoadException("Could not load server icon!", ex);
			}
			listenersBuilder.put(listener.getListenerName(), eagListener);
			listenersByAddressBuilder.put(listener.getInjectAddress(), eagListener);
			listenersImpl.add(eagListener);
		}
		listeners = listenersBuilder.build();
		listenersByAddress = listenersByAddressBuilder.build();
		init.setEaglerListeners(listenersImpl.build());
		init.setEaglerPlayerChannels(PlayerChannelHelper.getPlayerChannels(this));
		init.setEaglerBackendChannels(BackendChannelHelper.getBackendChannels(this));
	}

	private void loadNonProxying(IPlatform.InitNonProxying<PlayerObject> init) {
		EaglerListener eagListener;
		try {
			eagListener = new EaglerListener(this, init.getListenerAddress(),
					config.getListeners().values().iterator().next());
		} catch (SSLException ex) {
			throw new AbortLoadException("TLS configuration is invalid!", ex);
		} catch (IOException ex) {
			throw new AbortLoadException("Could not load server icon!", ex);
		}
		listeners = ImmutableMap.of("default", eagListener);
		listenersByAddress = ImmutableMap.of(init.getListenerAddress(), eagListener);
		init.setEaglerListener(eagListener);
		init.setEaglerPlayerChannels(PlayerChannelHelper.getPlayerChannels(this));
	}

	public ConfigDataRoot getConfig() {
		return config;
	}

	public IPlatform<PlayerObject> getPlatform() {
		return platform;
	}

	public PipelineTransformer getPipelineTransformer() {
		return pipelineTransformer;
	}

	public SSLCertificateManager getCertificateManager() {
		return certificateManager;
	}

	private void enableHandler() {
		if (platformType != EnumPlatformType.BUKKIT) {
			logger().info("Enabling " + getServerBrand() + " " + getServerVersion() + "...");
		}

		webServer.refreshBuiltinPages();

		if (certificateManager.hasRefreshableFiles()) {
			long refreshRate = Math.max(config.getSettings().getTLSCertRefreshRate(), 1) * 1000l;
			certificateRefreshTask = platform.getScheduler().executeAsyncRepeatingTask(certificateManager::update,
					refreshRate, refreshRate);
		}

		if (skinCacheService != null) {
			ConfigDataSkinService skinConf = config.getSettings().getSkinService();
			logger().info("Connecting to skin cache database \""
					+ Util.sanitizeJDBCURIForLogs(skinConf.getSkinCacheDBURI()) + "\"...");
			int threadCount = skinConf.getSkinCacheThreadCount();
			if (threadCount <= 0) {
				threadCount = Runtime.getRuntime().availableProcessors();
			}
			SkinCacheDatastore datastore;
			try {
				skinCacheJDBCHandle = EaglerDrivers.connectToDatabase(skinConf.getSkinCacheDBURI(),
						skinConf.getSkinCacheDriverClass(), skinConf.getSkinCacheDriverPath(), new Properties(),
						platform.getDataFolder(), logger());
				datastore = new SkinCacheDatastore(skinCacheJDBCHandle, threadCount,
						skinConf.getSkinCacheDiskKeepObjectsDays(), skinConf.getSkinCacheDiskMaxObjects(),
						Math.min(skinConf.getSkinCacheCompressionLevel(), 9), skinConf.isSkinCacheSQLiteCompatible(),
						logger());
				logger().info("Connected to skin cache database successfully!");
			} catch (SQLException e) {
				logger().error("Caught an exception while initializing the skin cache database", e);
				if (skinCacheJDBCHandle != null) {
					try {
						skinCacheJDBCHandle.close();
					} catch (SQLException ee) {
					}
					skinCacheJDBCHandle = null;
				}
				return;
			}
			skinCacheService.setDelegate(new SkinCacheService(
					new SkinCacheDownloader(httpClient, skinConf.getValidSkinDownloadURLs()), datastore,
					skinConf.getSkinCacheMemoryKeepSeconds(), skinConf.getSkinCacheMemoryMaxObjects(), logger()));
		}

		skinService.handleEnabled();

		if (updateService != null) {
			updateService.start();
		}

		updateChecker.handleEnable();

		supervisorService.handleEnable();

		platform.getScheduler().executeDelayed(pipelineTransformer::nagAgain, 10000);
	}

	private void disableHandler() {
		if (platformType != EnumPlatformType.BUKKIT) {
			logger().info("Disabling " + getServerBrand() + " " + getServerVersion() + "...");
		}

		webServer.releaseBuiltinPages();

		if (certificateRefreshTask != null) {
			certificateRefreshTask.cancel();
			certificateRefreshTask = null;
		}

		skinService.handleDisabled();

		if (skinCacheService != null) {
			if (skinCacheJDBCHandle != null) {
				logger().info("Disconnecting from skin cache database \""
						+ Util.sanitizeJDBCURIForLogs(config.getSettings().getSkinService().getSkinCacheDBURI())
						+ "\"...");
				try {
					skinCacheJDBCHandle.close();
				} catch (SQLException ee) {
				}
				skinCacheJDBCHandle = null;
				logger().info("Disconnected from skin cache database successfully!");
			}
			skinCacheService.setDelegate(null);
		}

		if (updateService != null) {
			updateService.stop();
		}

		updateChecker.handleDisable();

		supervisorService.handleDisable();
	}

	public void registerPlayer(BasePlayerInstance<PlayerObject> playerInstance) {
		if (backendRPCService != null) {
			playerInstance.backendRPCManager = backendRPCService.createVanillaPlayerRPCManager(playerInstance);
		}

		playerInstance.skinManager = skinService.createVanillaSkinManager(playerInstance);
	}

	public static class RegistrationStateException extends IllegalStateException {
	}

	public void registerEaglerPlayer(EaglerPlayerInstance<PlayerObject> playerInstance,
			NettyPipelineData.ProfileDataHolder profileData, Runnable onComplete) {
		if (!eaglerPlayers.add(playerInstance)) {
			throw new RegistrationStateException();
		}

		playerInstance.messageController = MessageControllerFactory.initializePlayer(playerInstance);

		if (updateService != null) {
			playerInstance.updateCertificate = updateService.createUpdateCertificate(playerInstance,
					profileData.updateCertInit);
		}

		playerInstance.voiceManager = voiceService.createVoiceManager(playerInstance);
		playerInstance.notifManager = notificationService.createPlayerManager(playerInstance);
		playerInstance.webViewManager = webViewService.createWebViewManager(playerInstance);
		playerInstance.pauseMenuManager = pauseMenuService.createPauseMenuManager(playerInstance);

		if (backendRPCService != null) {
			playerInstance.backendRPCManager = backendRPCService.createEaglerPlayerRPCManager(playerInstance);
		}

		int ver = playerInstance.getEaglerProtocol().ver;
		if (config.getSettings().isEnableIsEaglerPlayerProperty()) {
			if (ver >= 5) {
				playerInstance.sendEaglerMessage(new SPacketClientStateFlagV5EAG(
						ClientStateFlagUUIDs.EAGLER_PLAYER_FLAG_PRESENT.getMostSignificantBits(),
						ClientStateFlagUUIDs.EAGLER_PLAYER_FLAG_PRESENT.getLeastSignificantBits(),
						supervisorService.isSupervisorEnabled() ? 3 : 1));
			} else if (ver >= 4) {
				if (supervisorService.isSupervisorEnabled()) {
					playerInstance.sendEaglerMessage(new SPacketOtherPlayerClientUUIDV4EAG(-1,
							ClientStateFlagUUIDs.LEGACY_EAGLER_PLAYER_FLAG_PRESENT.getMostSignificantBits(),
							ClientStateFlagUUIDs.LEGACY_EAGLER_PLAYER_FLAG_PRESENT.getLeastSignificantBits()));
				}
			}
		}

		if (!skinService.isSkinDownloadEnabled()) {
			if (ver >= 5) {
				playerInstance.sendEaglerMessage(new SPacketClientStateFlagV5EAG(
						ClientStateFlagUUIDs.DISABLE_SKIN_URL_LOOKUP.getMostSignificantBits(),
						ClientStateFlagUUIDs.DISABLE_SKIN_URL_LOOKUP.getLeastSignificantBits(), 1));
			}
		}

		if (ver >= 5) {
			playerInstance.sendEaglerMessage(new SPacketClientStateFlagV5EAG(
					ClientStateFlagUUIDs.SET_MAX_MULTI_PACKET.getMostSignificantBits(),
					ClientStateFlagUUIDs.SET_MAX_MULTI_PACKET.getLeastSignificantBits(),
					config.getSettings().getProtocolV4DefragMaxPackets()));
		}

		skinService.createEaglerSkinManager(playerInstance, profileData, (mgr) -> {
			playerInstance.skinManager = mgr;

			try {
				if (playerInstance.isEaglerXRewindPlayer()) {
					((IEaglerXRewindProtocol<PlayerObject, Object>) playerInstance.getRewindProtocol())
							.handleCreatePlayer(playerInstance.getRewindAttachment(), playerInstance);
				}
			} catch (Exception ex) {
				logger().error("Uncaught exception initializing rewind player", ex);
				onComplete.run();
				return;
			}

			IPlatformPlayer<PlayerObject> platformPlayer = playerInstance.getPlatformPlayer();
			if (platformPlayer.isSetViewDistanceSupportedPaper()) {
				int distance = config.getSettings().getEaglerPlayersViewDistance();
				if (distance > 0) {
					platformPlayer.setViewDistancePaper(Math.max(distance, 3));
				}
			}

			updateChecker.sendUpdateMessage(platformPlayer);

			onComplete.run();
		});
	}

	public void unregisterPlayer(BasePlayerInstance<PlayerObject> playerInstance) {

	}

	public void unregisterEaglerPlayer(EaglerPlayerInstance<PlayerObject> playerInstance) {
		if (!eaglerPlayers.remove(playerInstance)) {
			throw new RegistrationStateException();
		}

		if (updateService != null) {
			updateService.removeUpdateCertificate(playerInstance);
			playerInstance.updateCertificate = null;
		}

		if (playerInstance.voiceManager != null) {
			playerInstance.voiceManager.destroyVoiceManager();
		}

		if (playerInstance.isEaglerXRewindPlayer()) {
			((IEaglerXRewindProtocol<PlayerObject, Object>) playerInstance.getRewindProtocol())
					.handleDestroyPlayer(playerInstance.getRewindAttachment());
		}
	}

	void handleServerPreConnect(BasePlayerInstance<PlayerObject> player) {
		if (player.backendRPCManager != null) {
			player.backendRPCManager.handleServerPreConnect();
		}
		if (player.isEaglerPlayer()) {
			EaglerPlayerInstance<PlayerObject> eaglerPlayer = player.asEaglerPlayer();
			if (eaglerPlayer.voiceManager != null) {
				eaglerPlayer.voiceManager.handleServerPreConnect();
			}
		}
	}

	void handleServerPostConnect(BasePlayerInstance<PlayerObject> player, IPlatformServer<PlayerObject> server) {
		String serverName = server.getServerConfName();
		if (player.backendRPCManager != null) {
			player.backendRPCManager.handleServerPostConnect();
		}
		if (player.isEaglerPlayer()) {
			EaglerPlayerInstance<PlayerObject> eaglerPlayer = player.asEaglerPlayer();
			eaglerPlayer.getSkinManager().handleServerPostConnect(serverName);
			if (eaglerPlayer.voiceManager != null) {
				eaglerPlayer.voiceManager.handleServerPostConnect(serverName);
			}
		}
	}

	@Override
	public Set<Class<?>> getPlayerTypes() {
		return playerClassSet;
	}

	@Override
	public IAttributeManager getGlobalAttributeManager() {
		return attributeManager;
	}

	public EaglerAttributeManager getEaglerAttribManager() {
		return attributeManager;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> IEaglerXServerAPI<T> getAPI(Class<T> playerClass) {
		if (!playerClass.isAssignableFrom(playerClazz)) {
			throw new ClassCastException(
					"Class " + playerClazz.getName() + " cannot be cast to " + playerClass.getName());
		}
		return (IEaglerXServerAPI<T>) this;
	}

	@Override
	public IEaglerXServerAPI<?> getDefaultAPI() {
		return this;
	}

	@Override
	public <T> T get(IAttributeKey<T> key) {
		return attributeHolder.get(key);
	}

	@Override
	public <T> void set(IAttributeKey<T> key, T value) {
		attributeHolder.set(key, value);
	}

	@Override
	public IEaglerAPIFactory getFactory() {
		return this;
	}

	@Override
	public EnumPlatformType getPlatformType() {
		return platformType;
	}

	@Override
	public Class<PlayerObject> getPlayerClass() {
		return playerClazz;
	}

	@Override
	public String getServerBrand() {
		return EaglerXServerVersion.BRAND;
	}

	@Override
	public String getServerVersion() {
		return EaglerXServerVersion.VERSION;
	}

	public String getServerVersionString() {
		return EaglerXServerVersion.BRAND + "/" + EaglerXServerVersion.VERSION;
	}

	@Override
	public String getServerName() {
		return config.getSettings().getServerName();
	}

	@Override
	public UUID getServerUUID() {
		return config.getSettings().getServerUUID();
	}

	public String getServerUUIDString() {
		return config.getSettings().getServerUUIDString();
	}

	@Override
	public boolean isAuthenticationEventsEnabled() {
		return config.getSettings().isEnableAuthenticationEvents();
	}

	@Override
	public boolean isEaglerHandshakeSupported(int vers) {
		return config.getSettings().getProtocols().isEaglerHandshakeSupported(vers);
	}

	@Override
	public boolean isEaglerProtocolSupported(GamePluginMessageProtocol vers) {
		return config.getSettings().getProtocols().isEaglerProtocolSupported(vers.ver);
	}

	@Override
	public boolean isMinecraftProtocolSupported(int vers) {
		return config.getSettings().getProtocols().isMinecraftProtocolSupported(vers);
	}

	@Override
	public BasePlayerInstance<PlayerObject> getPlayer(PlayerObject player) {
		if (player == null) {
			throw new NullPointerException("player");
		}
		IPlatformPlayer<PlayerObject> platformPlayer = platform.getPlayer(player);
		return platformPlayer != null ? platformPlayer.getPlayerAttachment() : null;
	}

	@Override
	public BasePlayerInstance<PlayerObject> getPlayerByName(String playerName) {
		if (playerName == null) {
			throw new NullPointerException("playerName");
		}
		IPlatformPlayer<PlayerObject> platformPlayer = platform.getPlayer(playerName);
		return platformPlayer != null ? platformPlayer.getPlayerAttachment() : null;
	}

	@Override
	public BasePlayerInstance<PlayerObject> getPlayerByUUID(UUID playerUUID) {
		if (playerUUID == null) {
			throw new NullPointerException("playerUUID");
		}
		IPlatformPlayer<PlayerObject> platformPlayer = platform.getPlayer(playerUUID);
		return platformPlayer != null ? platformPlayer.getPlayerAttachment() : null;
	}

	@Override
	public EaglerPlayerInstance<PlayerObject> getEaglerPlayer(PlayerObject player) {
		if (player == null) {
			throw new NullPointerException("player");
		}
		IPlatformPlayer<PlayerObject> platformPlayer = platform.getPlayer(player);
		if (platformPlayer != null) {
			return platformPlayer.<BasePlayerInstance<PlayerObject>>getPlayerAttachment().asEaglerPlayer();
		}
		return null;
	}

	@Override
	public EaglerPlayerInstance<PlayerObject> getEaglerPlayerByName(String playerName) {
		if (playerName == null) {
			throw new NullPointerException("playerName");
		}
		IPlatformPlayer<PlayerObject> platformPlayer = platform.getPlayer(playerName);
		if (platformPlayer != null) {
			return platformPlayer.<BasePlayerInstance<PlayerObject>>getPlayerAttachment().asEaglerPlayer();
		}
		return null;
	}

	@Override
	public EaglerPlayerInstance<PlayerObject> getEaglerPlayerByUUID(UUID playerUUID) {
		if (playerUUID == null) {
			throw new NullPointerException("playerUUID");
		}
		IPlatformPlayer<PlayerObject> platformPlayer = platform.getPlayer(playerUUID);
		if (platformPlayer != null) {
			return platformPlayer.<BasePlayerInstance<PlayerObject>>getPlayerAttachment().asEaglerPlayer();
		}
		return null;
	}

	@Override
	public boolean isPlayer(PlayerObject player) {
		if (player == null) {
			throw new NullPointerException("player");
		}
		return platform.getPlayer(player) != null;
	}

	@Override
	public boolean isPlayerByName(String playerName) {
		if (playerName == null) {
			throw new NullPointerException("playerName");
		}
		return platform.getPlayer(playerName) != null;
	}

	@Override
	public boolean isPlayerByUUID(UUID playerUUID) {
		if (playerUUID == null) {
			throw new NullPointerException("playerUUID");
		}
		return platform.getPlayer(playerUUID) != null;
	}

	@Override
	public boolean isEaglerPlayer(PlayerObject player) {
		if (player == null) {
			throw new NullPointerException("player");
		}
		IPlatformPlayer<PlayerObject> platformPlayer = platform.getPlayer(player);
		return platformPlayer != null
				&& platformPlayer.<BasePlayerInstance<PlayerObject>>getPlayerAttachment().isEaglerPlayer();
	}

	@Override
	public boolean isEaglerPlayerByName(String playerName) {
		if (playerName == null) {
			throw new NullPointerException("playerName");
		}
		IPlatformPlayer<PlayerObject> platformPlayer = platform.getPlayer(playerName);
		return platformPlayer != null
				&& platformPlayer.<BasePlayerInstance<PlayerObject>>getPlayerAttachment().isEaglerPlayer();
	}

	@Override
	public boolean isEaglerPlayerByUUID(UUID playerUUID) {
		if (playerUUID == null) {
			throw new NullPointerException("playerUUID");
		}
		IPlatformPlayer<PlayerObject> platformPlayer = platform.getPlayer(playerUUID);
		return platformPlayer != null
				&& platformPlayer.<BasePlayerInstance<PlayerObject>>getPlayerAttachment().isEaglerPlayer();
	}

	@Override
	public void forEachPlayer(Consumer<IBasePlayer<PlayerObject>> callback) {
		if (callback == null) {
			throw new NullPointerException("callback");
		}
		platform.forEachPlayer((player) -> {
			callback.accept(player.getPlayerAttachment());
		});
	}

	@Override
	public void forEachEaglerPlayer(Consumer<IEaglerPlayer<PlayerObject>> callback) {
		if (callback == null) {
			throw new NullPointerException("callback");
		}
		eaglerPlayers.forEach(callback);
	}

	public void forEachEaglerPlayerInternal(Consumer<EaglerPlayerInstance<PlayerObject>> callback) {
		eaglerPlayers.forEach(callback);
	}

	@Override
	public Collection<IBasePlayer<PlayerObject>> getAllPlayers() {
		return Collections2.transform(platform.getAllPlayers(),
				IPlatformPlayer<PlayerObject>::<BasePlayerInstance<PlayerObject>>getPlayerAttachment);
	}

	public Collection<BasePlayerInstance<PlayerObject>> getAllPlayersInternal() {
		return Collections2.transform(platform.getAllPlayers(),
				IPlatformPlayer<PlayerObject>::<BasePlayerInstance<PlayerObject>>getPlayerAttachment);
	}

	@Override
	public Collection<IEaglerPlayer<PlayerObject>> getAllEaglerPlayers() {
		return ImmutableList.copyOf(eaglerPlayers);
	}

	public Collection<EaglerPlayerInstance<PlayerObject>> getAllEaglerPlayersInternal() {
		return ImmutableList.copyOf(eaglerPlayers);
	}

	@Override
	public int getEaglerPlayerCount() {
		return eaglerPlayers.size();
	}

	@Override
	public Collection<IUpdateCertificate> getUpdateCertificates() {
		if (updateService != null) {
			return updateService.dumpAllCerts();
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public IUpdateCertificate createUpdateCertificate(byte[] data, int offset, int length) {
		if (data == null) {
			throw new NullPointerException("data");
		}
		byte[] copy = new byte[length];
		System.arraycopy(data, offset, copy, 0, length);
		return UpdateCertificate.intern(copy);
	}

	@Override
	public void addUpdateCertificate(IUpdateCertificate cert) {
		if (!(cert instanceof IUpdateCertificateImpl)) {
			throw new UnsupportedOperationException("Unknown certificate: " + cert);
		}
		if (updateService != null) {
			forEachEaglerPlayer((player) -> {
				player.offerUpdateCertificate(cert);
			});
		}
	}

	public UpdateService getUpdateService() {
		return updateService;
	}

	public BackendRPCService<PlayerObject> getBackendRPCService() {
		return backendRPCService;
	}

	@Override
	public Collection<IEaglerListenerInfo> getAllEaglerListeners() {
		return ImmutableList.copyOf(listeners.values());
	}

	@Override
	public IEaglerListenerInfo getListenerByName(String name) {
		if (name == null) {
			throw new NullPointerException("name");
		}
		return listeners.get(name);
	}

	@Override
	public IEaglerListenerInfo getListenerByAddress(SocketAddress address) {
		if (address == null) {
			throw new NullPointerException("name");
		}
		return listenersByAddress.get(address);
	}

	@Override
	public ProfileResolver getProfileResolver() {
		return profileResolver;
	}

	@Override
	public TexturesProperty getEaglerPlayersVanillaSkin() {
		return eaglerPlayersVanillaSkin;
	}

	@Override
	public void setEaglerPlayersVanillaSkin(TexturesProperty property) {
		eaglerPlayersVanillaSkin = property;
	}

	@Override
	public boolean isEaglerPlayerPropertyEnabled() {
		return isEaglerPlayerProperyEnabled;
	}

	@Override
	public void setEaglerPlayerProperyEnabled(boolean enable) {
		isEaglerPlayerProperyEnabled = enable;
	}

	@Override
	public void registerExtendedCapability(Object plugin, ExtendedCapabilitySpec capability) {
		if (plugin == null) {
			throw new NullPointerException("plugin");
		}
		if (capability == null) {
			throw new NullPointerException("capability");
		}
		extCapabilityMap.registerCapability(plugin, capability);
	}

	@Override
	public void unregisterExtendedCapability(Object plugin, ExtendedCapabilitySpec capability) {
		if (plugin == null) {
			throw new NullPointerException("plugin");
		}
		if (capability == null) {
			throw new NullPointerException("capability");
		}
		extCapabilityMap.unregisterCapability(plugin, capability);
	}

	@Override
	public boolean isExtendedCapabilityRegistered(UUID capabilityUUID, int version) {
		if (capabilityUUID == null) {
			throw new NullPointerException("capabilityUUID");
		}
		return extCapabilityMap.isCapabilityRegistered(capabilityUUID, version);
	}

	public ExtCapabilityMap getExtCapabilityMap() {
		return extCapabilityMap;
	}

	@Override
	public SkinService<PlayerObject> getSkinService() {
		return skinService;
	}

	@Override
	public IVoiceServiceImpl<PlayerObject> getVoiceService() {
		return voiceService;
	}

	@Override
	public BrandService<PlayerObject> getBrandService() {
		return brandRegistry;
	}

	@Override
	public NotificationService<PlayerObject> getNotificationService() {
		return notificationService;
	}

	@Override
	public PauseMenuService<PlayerObject> getPauseMenuService() {
		return pauseMenuService;
	}

	@Override
	public WebViewService<PlayerObject> getWebViewService() {
		return webViewService;
	}

	@Override
	public ISupervisorServiceImpl<PlayerObject> getSupervisorService() {
		return supervisorService;
	}

	@Override
	public RewindService<PlayerObject> getEaglerXRewindService() {
		return rewindService;
	}

	@Override
	public IPacketImageLoader getPacketImageLoader() {
		return PacketImageLoader.INSTANCE;
	}

	@Override
	public QueryServer getQueryServer() {
		return queryServer;
	}

	@Override
	public IServerIconLoader getServerIconLoader() {
		return ServerIconLoader.INSTANCE;
	}

	@Override
	public WebServer getWebServer() {
		return webServer;
	}

	@Override
	public IScheduler getScheduler() {
		return platform.getScheduler();
	}

	@Override
	public Set<Class<?>> getComponentTypes() {
		return componentTypeSet;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <ComponentObject> IComponentSerializer<ComponentObject> getComponentSerializer(
			Class<ComponentObject> componentType) {
		if (componentType != this.componentType) {
			throw new ClassCastException(
					"Component class " + componentType.getName() + " is not supported on this platform!");
		}
		return (IComponentSerializer<ComponentObject>) componentHelper;
	}

	@Override
	public IComponentHelper getComponentHelper() {
		return componentHelper;
	}

	@Override
	public INBTHelper getNBTHelper() {
		return NBTHelper.INSTANCE;
	}

	public IHTTPClient getInternalHTTPClient() {
		return httpClient;
	}

	@Override
	public IBinaryHTTPClient getBinaryHTTPClient() {
		return httpClientAPI;
	}

	@Override
	public UUID intern(UUID uuid) {
		if (uuid == null) {
			throw new NullPointerException("uuid");
		}
		return uuidInterner.intern(uuid);
	}

	@Override
	public IAttributeManager getAttributeManager() {
		return attributeManager;
	}

	@Override
	public HPPC getHPPC() {
		return HPPCFactory.INSTANCE;
	}

	@Override
	public boolean isNettyPlatform() {
		return true;
	}

	@Override
	public NettyUnsafe netty() {
		return this;
	}

	@Override
	public Bootstrap bootstrapClient(SocketAddress remoteAddress) {
		Bootstrap bootstrap = new Bootstrap().group(getWorkerEventLoopGroup());
		if (remoteAddress != null) {
			bootstrap.remoteAddress(remoteAddress);
		}
		return setChannelFactory(bootstrap, remoteAddress);
	}

	@Override
	public ServerBootstrap bootstrapServer(SocketAddress localAddress) {
		ServerBootstrap serverBootstrap = new ServerBootstrap();
		EventLoopGroup bossGroup = getBossEventLoopGroup();
		if (bossGroup != null) {
			serverBootstrap.group(bossGroup, getWorkerEventLoopGroup());
		} else {
			serverBootstrap.group(getWorkerEventLoopGroup());
		}
		if (localAddress != null) {
			serverBootstrap.localAddress(localAddress);
		}
		return setServerChannelFactory(serverBootstrap, localAddress);
	}

	@Override
	public Bootstrap setChannelFactory(Bootstrap boostrap, SocketAddress address) {
		return platform.setChannelFactory(boostrap, address);
	}

	@Override
	public ServerBootstrap setServerChannelFactory(ServerBootstrap boostrap, SocketAddress address) {
		return platform.setServerChannelFactory(boostrap, address);
	}

	@Override
	public EventLoopGroup getBossEventLoopGroup() {
		return platform.getBossEventLoopGroup();
	}

	@Override
	public EventLoopGroup getWorkerEventLoopGroup() {
		return platform.getWorkerEventLoopGroup();
	}

	public IPlatformLogger logger() {
		return platform.logger();
	}

	public IEventDispatchAdapter<PlayerObject, ?> eventDispatcher() {
		return platform.eventDispatcher();
	}

	public IPlatformComponentHelper componentHelper() {
		return platform.getComponentHelper();
	}

	public IPlatformComponentBuilder componentBuilder() {
		return platform.getComponentHelper().builder();
	}

	public PlayerRateLimits.RateLimitParams rateLimitParams() {
		return ratelimitParams;
	}

	public void setServerListConfirmCode(String code) {
		serverListConfirmCode = code;
	}

	public boolean testServerListConfirmCode(String code) {
		if (serverListConfirmCode != null) {
			if (code.equals(serverListConfirmCode)) {
				serverListConfirmCode = null;
				return true;
			}
		}
		return false;
	}

}
