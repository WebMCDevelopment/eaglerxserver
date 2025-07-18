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

package net.lax1dude.eaglercraft.backend.server.base.config;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class ConfigDataSettings {

	public static class ConfigDataProtocols {

		private final int minMinecraftProtocol;
		private final int maxMinecraftProtocol;
		private final int maxMinecraftProtocolV5;
		private final boolean eaglerXRewindAllowed;
		private final boolean protocolLegacyAllowed;
		private final boolean protocolV3Allowed;
		private final boolean protocolV4Allowed;
		private final boolean protocolV5Allowed;
		private final int minEaglerProtocol;
		private final int maxEaglerProtocol;

		public ConfigDataProtocols(int minMinecraftProtocol, int maxMinecraftProtocol, int maxMinecraftProtocolV5,
				boolean eaglerXRewindAllowed, boolean protocolLegacyAllowed, boolean protocolV3Allowed,
				boolean protocolV4Allowed, boolean protocolV5Allowed) {
			this.minMinecraftProtocol = minMinecraftProtocol;
			this.maxMinecraftProtocol = maxMinecraftProtocol;
			this.maxMinecraftProtocolV5 = maxMinecraftProtocolV5;
			this.eaglerXRewindAllowed = eaglerXRewindAllowed;
			this.protocolLegacyAllowed = protocolLegacyAllowed;
			this.protocolV3Allowed = protocolV3Allowed;
			this.protocolV4Allowed = protocolV4Allowed;
			this.protocolV5Allowed = protocolV5Allowed;
			if (protocolLegacyAllowed) {
				minEaglerProtocol = 1;
			} else if (protocolV3Allowed) {
				minEaglerProtocol = 3;
			} else if (protocolV4Allowed) {
				minEaglerProtocol = 4;
			} else if (protocolV5Allowed) {
				minEaglerProtocol = 5;
			} else {
				minEaglerProtocol = Integer.MAX_VALUE;
			}
			if (protocolV5Allowed) {
				maxEaglerProtocol = 5;
			} else if (protocolV4Allowed) {
				maxEaglerProtocol = 4;
			} else if (protocolV3Allowed) {
				maxEaglerProtocol = 3;
			} else if (protocolLegacyAllowed) {
				maxEaglerProtocol = 1;
			} else {
				maxEaglerProtocol = Integer.MIN_VALUE;
			}
		}

		public int getMinMinecraftProtocol() {
			return minMinecraftProtocol;
		}

		public int getMaxMinecraftProtocol() {
			return maxMinecraftProtocol;
		}

		public int getMaxMinecraftProtocolV5() {
			return maxMinecraftProtocolV5;
		}

		public boolean isEaglerXRewindAllowed() {
			return eaglerXRewindAllowed;
		}

		public boolean isProtocolLegacyAllowed() {
			return protocolLegacyAllowed;
		}

		public boolean isProtocolV3Allowed() {
			return protocolV3Allowed;
		}

		public boolean isProtocolV4Allowed() {
			return protocolV4Allowed;
		}

		public boolean isProtocolV5Allowed() {
			return protocolV5Allowed;
		}

		public int getMinEaglerProtocol() {
			return minEaglerProtocol;
		}

		public int getMaxEaglerProtocol() {
			return maxEaglerProtocol;
		}

		public boolean isEaglerHandshakeSupported(int vers) {
			return switch (vers) {
			case 1, 2 -> protocolLegacyAllowed;
			case 3 -> protocolV3Allowed;
			case 4 -> protocolV4Allowed;
			case 5 -> protocolV5Allowed;
			default -> false;
			};
		}

		public boolean isEaglerProtocolSupported(int vers) {
			return switch (vers) {
			case 3 -> protocolLegacyAllowed || protocolV3Allowed;
			case 4 -> protocolV4Allowed;
			case 5 -> protocolV5Allowed;
			default -> false;
			};
		}

		public boolean isMinecraftProtocolSupported(int vers) {
			return minMinecraftProtocol >= vers && maxMinecraftProtocol <= vers;
		}

	}

	public static class ConfigDataSkinService {

		private final int skinLookupRatelimit;
		private final int capeLookupRatelimit;
		private final boolean downloadVanillaSkinsToClients;
		private final Set<String> validSkinDownloadURLs;
		private final String skinCacheDBURI;
		private final String skinCacheDriverClass;
		private final String skinCacheDriverPath;
		private final boolean skinCacheSQLiteCompatible;
		private final int skinCacheThreadCount;
		private final int skinCacheCompressionLevel;
		private final int skinCacheMemoryKeepSeconds;
		private final int skinCacheMemoryMaxObjects;
		private final int skinCacheDiskKeepObjectsDays;
		private final int skinCacheDiskMaxObjects;
		private final int skinCacheAntagonistsRatelimit;
		private final boolean enableFNAWSkinModelsGlobal;
		private final Set<String> enableFNAWSkinModelsOnServers;
		private final boolean enableSkinsRestorerApplyHook;

		public ConfigDataSkinService(int skinLookupRatelimit, int capeLookupRatelimit,
				boolean downloadVanillaSkinsToClients, Set<String> validSkinDownloadURLs, String skinCacheDBURI,
				String skinCacheDriverClass, String skinCacheDriverPath, boolean skinCacheSQLiteCompatible,
				int skinCacheThreadCount, int skinCacheCompressionLevel, int skinCacheMemoryKeepSeconds,
				int skinCacheMemoryMaxObjects, int skinCacheDiskKeepObjectsDays, int skinCacheDiskMaxObjects,
				int skinCacheAntagonistsRatelimit, boolean enableFNAWSkinModelsGlobal,
				Set<String> enableFNAWSkinModelsOnServers, boolean enableSkinsRestorerApplyHook) {
			this.skinLookupRatelimit = skinLookupRatelimit;
			this.capeLookupRatelimit = capeLookupRatelimit;
			this.downloadVanillaSkinsToClients = downloadVanillaSkinsToClients;
			this.validSkinDownloadURLs = validSkinDownloadURLs;
			this.skinCacheDBURI = skinCacheDBURI;
			this.skinCacheDriverClass = skinCacheDriverClass;
			this.skinCacheDriverPath = skinCacheDriverPath;
			this.skinCacheSQLiteCompatible = skinCacheSQLiteCompatible;
			this.skinCacheThreadCount = skinCacheThreadCount;
			this.skinCacheCompressionLevel = skinCacheCompressionLevel;
			this.skinCacheMemoryKeepSeconds = skinCacheMemoryKeepSeconds;
			this.skinCacheMemoryMaxObjects = skinCacheMemoryMaxObjects;
			this.skinCacheDiskKeepObjectsDays = skinCacheDiskKeepObjectsDays;
			this.skinCacheDiskMaxObjects = skinCacheDiskMaxObjects;
			this.skinCacheAntagonistsRatelimit = skinCacheAntagonistsRatelimit;
			this.enableFNAWSkinModelsGlobal = enableFNAWSkinModelsGlobal;
			this.enableFNAWSkinModelsOnServers = enableFNAWSkinModelsOnServers;
			this.enableSkinsRestorerApplyHook = enableSkinsRestorerApplyHook;
		}

		public int getSkinLookupRatelimit() {
			return skinLookupRatelimit;
		}

		public int getCapeLookupRatelimit() {
			return capeLookupRatelimit;
		}

		public boolean isDownloadVanillaSkinsToClients() {
			return downloadVanillaSkinsToClients;
		}

		public Set<String> getValidSkinDownloadURLs() {
			return validSkinDownloadURLs;
		}

		public String getSkinCacheDBURI() {
			return skinCacheDBURI;
		}

		public String getSkinCacheDriverClass() {
			return skinCacheDriverClass;
		}

		public String getSkinCacheDriverPath() {
			return skinCacheDriverPath;
		}

		public boolean isSkinCacheSQLiteCompatible() {
			return skinCacheSQLiteCompatible;
		}

		public int getSkinCacheThreadCount() {
			return skinCacheThreadCount;
		}

		public int getSkinCacheCompressionLevel() {
			return skinCacheCompressionLevel;
		}

		public int getSkinCacheMemoryKeepSeconds() {
			return skinCacheMemoryKeepSeconds;
		}

		public int getSkinCacheMemoryMaxObjects() {
			return skinCacheMemoryMaxObjects;
		}

		public int getSkinCacheDiskKeepObjectsDays() {
			return skinCacheDiskKeepObjectsDays;
		}

		public int getSkinCacheDiskMaxObjects() {
			return skinCacheDiskMaxObjects;
		}

		public int getSkinCacheAntagonistsRatelimit() {
			return skinCacheAntagonistsRatelimit;
		}

		public boolean isEnableFNAWSkinModelsGlobal() {
			return enableFNAWSkinModelsGlobal;
		}

		public Set<String> getEnableFNAWSkinModelsOnServers() {
			return enableFNAWSkinModelsOnServers;
		}

		public Predicate<String> getFNAWSkinsPredicate() {
			if (enableFNAWSkinModelsGlobal) {
				return (str) -> true;
			} else if (!enableFNAWSkinModelsOnServers.isEmpty()) {
				return enableFNAWSkinModelsOnServers::contains;
			} else {
				return (str) -> false;
			}
		}

		public boolean isEnableSkinsRestorerApplyHook() {
			return enableSkinsRestorerApplyHook;
		}

	}

	public static class ConfigDataVoiceService {

		private final boolean enableVoiceService;
		private final boolean enableVoiceChatAllServers;
		private final Set<String> enableVoiceChatOnServers;
		private final boolean separateVoiceChannelsPerServer;
		private final boolean voiceBackendRelayMode;
		private final int voiceConnectRatelimit;
		private final int voiceRequestRatelimit;
		private final int voiceICERatelimit;

		public ConfigDataVoiceService(boolean enableVoiceService, boolean enableVoiceChatAllServers,
				Set<String> enableVoiceChatOnServers, boolean separateVoiceChannelsPerServer,
				boolean voiceBackendRelayMode, int voiceConnectRatelimit, int voiceRequestRatelimit,
				int voiceICERatelimit) {
			this.enableVoiceService = enableVoiceService;
			this.enableVoiceChatAllServers = enableVoiceChatAllServers;
			this.enableVoiceChatOnServers = enableVoiceChatOnServers;
			this.separateVoiceChannelsPerServer = separateVoiceChannelsPerServer;
			this.voiceBackendRelayMode = voiceBackendRelayMode;
			this.voiceConnectRatelimit = voiceConnectRatelimit;
			this.voiceRequestRatelimit = voiceRequestRatelimit;
			this.voiceICERatelimit = voiceICERatelimit;
		}

		public boolean isEnableVoiceService() {
			return enableVoiceService;
		}

		public boolean isEnableVoiceChatAllServers() {
			return enableVoiceChatAllServers;
		}

		public Set<String> getEnableVoiceChatOnServers() {
			return enableVoiceChatOnServers;
		}

		public boolean isSeparateVoiceChannelsPerServer() {
			return separateVoiceChannelsPerServer;
		}

		public boolean isVoiceBackendRelayMode() {
			return voiceBackendRelayMode;
		}

		public int getVoiceConnectRatelimit() {
			return voiceConnectRatelimit;
		}

		public int getVoiceRequestRatelimit() {
			return voiceRequestRatelimit;
		}

		public int getVoiceICERatelimit() {
			return voiceICERatelimit;
		}

	}

	public static class ConfigDataUpdateService {

		private final boolean enableUpdateSystem;
		private final boolean discardLoginPacketCerts;
		private final int certPacketDataRateLimit;
		private final boolean enableEagcertFolder;
		private final boolean downloadLatestCerts;
		private final List<URI> downloadCertsFrom;
		private final int checkForUpdateEvery;

		public ConfigDataUpdateService(boolean enableUpdateSystem, boolean discardLoginPacketCerts,
				int certPacketDataRateLimit, boolean enableEagcertFolder, boolean downloadLatestCerts,
				List<URI> downloadCertsFrom, int checkForUpdateEvery) {
			this.enableUpdateSystem = enableUpdateSystem;
			this.discardLoginPacketCerts = discardLoginPacketCerts;
			this.certPacketDataRateLimit = certPacketDataRateLimit;
			this.enableEagcertFolder = enableEagcertFolder;
			this.downloadLatestCerts = downloadLatestCerts;
			this.downloadCertsFrom = downloadCertsFrom;
			this.checkForUpdateEvery = checkForUpdateEvery;
		}

		public boolean isEnableUpdateSystem() {
			return enableUpdateSystem;
		}

		public boolean isDiscardLoginPacketCerts() {
			return discardLoginPacketCerts;
		}

		public int getCertPacketDataRateLimit() {
			return certPacketDataRateLimit;
		}

		public boolean isEnableEagcertFolder() {
			return enableEagcertFolder;
		}

		public boolean isDownloadLatestCerts() {
			return downloadLatestCerts;
		}

		public List<URI> getDownloadCertsFrom() {
			return downloadCertsFrom;
		}

		public int getCheckForUpdateEvery() {
			return checkForUpdateEvery;
		}

	}

	public static class ConfigDataUpdateChecker {

		private final boolean enableUpdateChecker;
		private final int checkForServerUpdateEvery;
		private final boolean updateChatMessages;

		public ConfigDataUpdateChecker(boolean enableUpdateChecker, int checkForServerUpdateEvery,
				boolean updateChatMessages) {
			this.enableUpdateChecker = enableUpdateChecker;
			this.checkForServerUpdateEvery = checkForServerUpdateEvery;
			this.updateChatMessages = updateChatMessages;
		}

		public boolean isEnableUpdateChecker() {
			return enableUpdateChecker;
		}

		public int getCheckForServerUpdateEvery() {
			return checkForServerUpdateEvery;
		}

		public boolean isUpdateChatMessages() {
			return updateChatMessages;
		}

	}

	private final String serverName;
	private final UUID serverUUID;
	private final String serverUUIDString;
	private final int eaglerLoginTimeout;
	private final int httpMaxInitialLineLength;
	private final int httpMaxHeaderSize;
	private final int httpMaxChunkSize;
	private final int httpMaxContentLength;
	private final int httpWebSocketCompressionLevel;
	private final int httpWebSocketFragmentSize;
	private final int httpWebSocketMaxFrameLength;
	private final boolean httpWebSocketPingIntervention;
	private final int tlsCertRefreshRate;
	private final boolean enableAuthenticationEvents;
	private final boolean enableBackendRPCAPI;
	private final boolean useModernizedChannelNames;
	private final int eaglerPlayersViewDistance;
	private final String eaglerPlayersVanillaSkin;
	private final boolean enableIsEaglerPlayerPropery;
	private final int protocolV4DefragSendDelay;
	private final int protocolV4DefragMaxPackets;
	private final int brandLookupRatelimit;
	private final int webviewDownloadRatelimit;
	private final int webviewMessageRatelimit;
	private final boolean debugLogNewChannels;
	private final boolean debugLogRealIPHeaders;
	private final boolean debugLogOriginHeaders;
	private final boolean debugLogClientBrands;
	private final ConfigDataProtocols protocols;
	private final ConfigDataSkinService skinService;
	private final ConfigDataVoiceService voiceService;
	private final ConfigDataUpdateService updateService;
	private final ConfigDataUpdateChecker updateChecker;

	public ConfigDataSettings(String serverName, UUID serverUUID, int eaglerLoginTimeout, int httpMaxInitialLineLength,
			int httpMaxHeaderSize, int httpMaxChunkSize, int httpMaxContentLength, int httpWebSocketCompressionLevel,
			int httpWebSocketFragmentSize, int httpWebSocketMaxFrameLength, boolean httpWebSocketPingIntervention,
			int tlsCertRefreshRate, boolean enableAuthenticationEvents, boolean enableBackendRPCAPI,
			boolean useModernizedChannelNames, int eaglerPlayersViewDistance, String eaglerPlayersVanillaSkin,
			boolean enableIsEaglerPlayerPropery, int protocolV4DefragSendDelay, int protocolV4DefragMaxPackets,
			int brandLookupRatelimit, int webviewDownloadRatelimit, int webviewMessageRatelimit,
			boolean debugLogNewChannels, boolean debugLogRealIPHeaders, boolean debugLogOriginHeaders,
			boolean debugLogClientBrands, ConfigDataProtocols protocols, ConfigDataSkinService skinService,
			ConfigDataVoiceService voiceService, ConfigDataUpdateService updateService,
			ConfigDataUpdateChecker updateChecker) {
		this.serverName = serverName;
		this.serverUUID = serverUUID;
		this.serverUUIDString = serverUUID.toString();
		this.eaglerLoginTimeout = eaglerLoginTimeout;
		this.httpMaxInitialLineLength = httpMaxInitialLineLength;
		this.httpMaxHeaderSize = httpMaxHeaderSize;
		this.httpMaxChunkSize = httpMaxChunkSize;
		this.httpMaxContentLength = httpMaxContentLength;
		this.httpWebSocketCompressionLevel = httpWebSocketCompressionLevel;
		this.httpWebSocketFragmentSize = httpWebSocketFragmentSize;
		this.httpWebSocketMaxFrameLength = httpWebSocketMaxFrameLength;
		this.httpWebSocketPingIntervention = httpWebSocketPingIntervention;
		this.tlsCertRefreshRate = tlsCertRefreshRate;
		this.enableAuthenticationEvents = enableAuthenticationEvents;
		this.enableBackendRPCAPI = enableBackendRPCAPI;
		this.useModernizedChannelNames = useModernizedChannelNames;
		this.eaglerPlayersViewDistance = eaglerPlayersViewDistance;
		this.eaglerPlayersVanillaSkin = eaglerPlayersVanillaSkin;
		this.enableIsEaglerPlayerPropery = enableIsEaglerPlayerPropery;
		this.protocolV4DefragSendDelay = protocolV4DefragSendDelay;
		this.protocolV4DefragMaxPackets = protocolV4DefragMaxPackets;
		this.brandLookupRatelimit = brandLookupRatelimit;
		this.webviewDownloadRatelimit = webviewDownloadRatelimit;
		this.webviewMessageRatelimit = webviewMessageRatelimit;
		this.debugLogNewChannels = debugLogNewChannels;
		this.debugLogRealIPHeaders = debugLogRealIPHeaders;
		this.debugLogOriginHeaders = debugLogOriginHeaders;
		this.debugLogClientBrands = debugLogClientBrands;
		this.protocols = protocols;
		this.skinService = skinService;
		this.voiceService = voiceService;
		this.updateService = updateService;
		this.updateChecker = updateChecker;
	}

	public String getServerName() {
		return serverName;
	}

	public UUID getServerUUID() {
		return serverUUID;
	}

	public String getServerUUIDString() {
		return serverUUIDString;
	}

	public int getEaglerLoginTimeout() {
		return eaglerLoginTimeout;
	}

	public int getHTTPMaxInitialLineLength() {
		return httpMaxInitialLineLength;
	}

	public int getHTTPMaxHeaderSize() {
		return httpMaxHeaderSize;
	}

	public int getHTTPMaxChunkSize() {
		return httpMaxChunkSize;
	}

	public int getHTTPMaxContentLength() {
		return httpMaxContentLength;
	}

	public int getHTTPWebSocketCompressionLevel() {
		return httpWebSocketCompressionLevel;
	}

	public int getHTTPWebSocketFragmentSize() {
		return httpWebSocketFragmentSize;
	}

	public int getHTTPWebSocketMaxFrameLength() {
		return httpWebSocketMaxFrameLength;
	}

	public boolean getHTTPWebSocketPingIntervention() {
		return httpWebSocketPingIntervention;
	}

	public int getTLSCertRefreshRate() {
		return tlsCertRefreshRate;
	}

	public boolean isEnableAuthenticationEvents() {
		return enableAuthenticationEvents;
	}

	public boolean isEnableBackendRPCAPI() {
		return enableBackendRPCAPI;
	}

	public boolean isUseModernizedChannelNames() {
		return useModernizedChannelNames;
	}

	public int getEaglerPlayersViewDistance() {
		return eaglerPlayersViewDistance;
	}

	public String getEaglerPlayersVanillaSkin() {
		return eaglerPlayersVanillaSkin;
	}

	public boolean isEnableIsEaglerPlayerProperty() {
		return enableIsEaglerPlayerPropery;
	}

	public int getProtocolV4DefragSendDelay() {
		return protocolV4DefragSendDelay;
	}

	public int getProtocolV4DefragMaxPackets() {
		return protocolV4DefragMaxPackets;
	}

	public int getBrandLookupRatelimit() {
		return brandLookupRatelimit;
	}

	public int getWebviewDownloadRatelimit() {
		return webviewDownloadRatelimit;
	}

	public int getWebviewMessageRatelimit() {
		return webviewMessageRatelimit;
	}

	public boolean isDebugLogNewChannels() {
		return debugLogNewChannels;
	}

	public boolean isDebugLogRealIPHeaders() {
		return debugLogRealIPHeaders;
	}

	public boolean isDebugLogOriginHeaders() {
		return debugLogOriginHeaders;
	}

	public boolean isDebugLogClientBrands() {
		return debugLogClientBrands;
	}

	public ConfigDataProtocols getProtocols() {
		return protocols;
	}

	public ConfigDataSkinService getSkinService() {
		return skinService;
	}

	public ConfigDataVoiceService getVoiceService() {
		return voiceService;
	}

	public ConfigDataUpdateService getUpdateService() {
		return updateService;
	}

	public ConfigDataUpdateChecker getUpdateChecker() {
		return updateChecker;
	}

}
