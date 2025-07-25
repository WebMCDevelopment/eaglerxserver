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

package net.lax1dude.eaglercraft.backend.server.base.handshake;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import net.lax1dude.eaglercraft.backend.server.adapter.EnumAdapterPlatformType;
import net.lax1dude.eaglercraft.backend.server.adapter.event.IEventDispatchCallback;
import net.lax1dude.eaglercraft.backend.server.api.EnumCapabilityType;
import net.lax1dude.eaglercraft.backend.server.api.collect.ObjectIntMap;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftAuthCheckRequiredEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftAuthCookieEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftAuthPasswordEvent;
import net.lax1dude.eaglercraft.backend.server.base.CapabilityBits;
import net.lax1dude.eaglercraft.backend.server.base.EaglerXServer;
import net.lax1dude.eaglercraft.backend.server.base.NettyPipelineData;
import net.lax1dude.eaglercraft.backend.server.base.collect.ObjectIntHashMap;
import net.lax1dude.eaglercraft.backend.server.base.config.ConfigDataSettings;
import net.lax1dude.eaglercraft.backend.server.base.pipeline.WebSocketEaglerInitialHandler;
import net.lax1dude.eaglercraft.backend.server.base.pipeline.WebSocketEaglerInitialHandler.IHandshaker;
import net.lax1dude.eaglercraft.v1_8.socket.protocol.GamePluginMessageProtocol;

public abstract class HandshakerInstance implements IHandshaker {

	public static final Pattern USERNAME_REGEX = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
	private static final byte[] OFFLINE_PLAYER_BYTES = "OfflinePlayer:".getBytes(StandardCharsets.US_ASCII);

	protected static final UUID[] NO_UUID = new UUID[0];
	protected static final int[] NO_VER = new int[0];

	protected final EaglerXServer<?> server;
	protected final NettyPipelineData pipelineData;
	protected final WebSocketEaglerInitialHandler inboundHandler;
	protected int state = HandshakePacketTypes.STATE_OPENED;

	protected HandshakerInstance(EaglerXServer<?> server, NettyPipelineData pipelineData,
			WebSocketEaglerInitialHandler inboundHandler) {
		this.server = server;
		this.pipelineData = pipelineData;
		this.inboundHandler = inboundHandler;
	}

	protected <T> IEventDispatchCallback<T> inEventLoop(Channel channel, IEventDispatchCallback<T> cb) {
		return (evt, err) -> {
			EventLoop eventLoop = channel.eventLoop();
			if (eventLoop.inEventLoop()) {
				if (!channel.isActive()) {
					return;
				}
				cb.complete(evt, err);
			} else {
				eventLoop.execute(() -> {
					if (!channel.isActive()) {
						return;
					}
					cb.complete(evt, err);
				});
			}
		};
	}

	protected void handleInvalidData(ChannelHandlerContext ctx) {
		inboundHandler.terminateErrorCode(ctx, getVersion(), HandshakePacketTypes.SERVER_ERROR_INVALID_PACKET,
				"Invalid Packet");
	}

	protected void handleUnknownPacket(ChannelHandlerContext ctx, int id) {
		inboundHandler.terminateErrorCode(ctx, getVersion(), HandshakePacketTypes.SERVER_ERROR_UNKNOWN_PACKET,
				"Unknown Packet #" + id);
	}

	protected void handlePacketInit(ChannelHandlerContext ctx, String eaglerBrand, String eaglerVersionString,
			int minecraftVersion, boolean auth, byte[] authUsername) {
		if (state == HandshakePacketTypes.STATE_OPENED) {
			state = HandshakePacketTypes.STATE_STALLING;
			pipelineData.eaglerBrandString = eaglerBrand;
			pipelineData.eaglerVersionString = eaglerVersionString;
			pipelineData.handshakeProtocol = getVersion();
			pipelineData.gameProtocol = getFinalVersion();
			pipelineData.minecraftProtocol = minecraftVersion;
			pipelineData.handshakeAuthEnabled = auth;
			pipelineData.handshakeAuthUsername = authUsername;
			server.eventDispatcher().dispatchClientBrandEvent(pipelineData.asPendingConnection(),
					inEventLoop(ctx.channel(), (evt, err) -> {
				if (err == null) {
					if (!evt.isCancelled()) {
						continueHandshakeInit(ctx);
					} else {
						Object obj = evt.getMessage();
						if (obj == null) {
							obj = server.componentBuilder().buildTranslationComponent().translation("disconnect.closed")
									.end();
						}
						state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
						inboundHandler.terminateErrorCode(ctx, getVersion(),
								HandshakePacketTypes.SERVER_ERROR_CUSTOM_MESSAGE, obj);
					}
				} else {
					state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
					inboundHandler.terminateInternalError(ctx, getVersion());
					pipelineData.connectionLogger.error("Caught exception dispatching client brand event", err);
				}
			}));
		} else {
			state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
			inboundHandler.terminateErrorCode(ctx, getVersion(), HandshakePacketTypes.SERVER_ERROR_WRONG_PACKET,
					"Wrong Initial Packet");
		}
	}

	private void continueHandshakeInit(ChannelHandlerContext ctx) {
		if (server.isAuthenticationEventsEnabled()) {
			if (getVersion() <= 1 || pipelineData.handshakeAuthUsername == null) {
				state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
				inboundHandler.terminateErrorCode(ctx, getVersion(), HandshakePacketTypes.SERVER_ERROR_CUSTOM_MESSAGE,
						"Outdated Client (Authentication Required)");
				return;
			}
			server.eventDispatcher().dispatchAuthCheckRequired(pipelineData.asPendingConnection(),
					pipelineData.handshakeAuthEnabled, pipelineData.handshakeAuthUsername,
					inEventLoop(ctx.channel(), (evt, err) -> {
				if (err == null) {
					IEaglercraftAuthCheckRequiredEvent.EnumAuthResponse response = evt.getAuthRequired();
					if (response == null) {
						state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
						inboundHandler.terminateInternalError(ctx, getVersion());
						pipelineData.connectionLogger.error("Auth required check event was not handled");
					} else {
						pipelineData.nicknameSelectionEnabled = evt.isNicknameSelectionEnabled();
						pipelineData.cookieAuthEventEnabled = evt.getEnableCookieAuth();
						pipelineData.authType = evt.getUseAuthTypeRaw();
						pipelineData.authMessage = evt.getAuthMessage();
						switch (response) {
						case SKIP:
							state = HandshakePacketTypes.STATE_CLIENT_VERSION;
							inboundHandler.canSendV3Kick = true;
							sendPacketVersionNoAuth(ctx, pipelineData.handshakeProtocol,
									pipelineData.minecraftProtocol, server.getServerBrand(),
									server.getServerVersion());
							break;
						case REQUIRE:
							if (pipelineData.authType == (byte) 0) {
								state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
								inboundHandler.terminateInternalError(ctx, getVersion());
								pipelineData.connectionLogger
										.error("Auth required check event handler did not provide auth type");
								break;
							}
							if (!pipelineData.handshakeAuthEnabled
									&& (getVersion() < 4 || !pipelineData.cookieAuthEventEnabled)) {
								inboundHandler.terminated = true;
								state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
								sendPacketAuthRequired(ctx, pipelineData.authType, evt.getAuthMessage())
										.addListener(ChannelFutureListener.CLOSE);
								break;
							}
							pipelineData.authEventEnabled = true;
							inboundHandler.canSendV3Kick = true;
							state = HandshakePacketTypes.STATE_CLIENT_VERSION;
							ChannelFuture future = sendPacketVersionAuth(ctx, pipelineData.handshakeProtocol,
									pipelineData.minecraftProtocol, server.getServerBrand(),
									server.getServerVersion(), pipelineData.authType, evt.getSaltingData(),
									evt.isNicknameSelectionEnabled());
							if (future == null) {
								state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
							}
							break;
						case DENY:
							Object obj = evt.getKickMessage();
							if (obj == null) {
								obj = server.componentBuilder().buildTranslationComponent()
										.translation("disconnect.closed").end();
							}
							state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
							inboundHandler.terminateErrorCode(ctx, getVersion(),
									HandshakePacketTypes.SERVER_ERROR_CUSTOM_MESSAGE, obj);
							break;
						}
					}
				} else {
					state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
					inboundHandler.terminateInternalError(ctx, getVersion());
					pipelineData.connectionLogger
							.error("Caught exception dispatching auth required check event", err);
				}
			}));
		} else {
			state = HandshakePacketTypes.STATE_CLIENT_VERSION;
			inboundHandler.canSendV3Kick = true;
			sendPacketVersionNoAuth(ctx, pipelineData.handshakeProtocol, pipelineData.minecraftProtocol,
					server.getServerBrand(), server.getServerVersion());
		}
	}

	protected abstract int getVersion();

	protected abstract GamePluginMessageProtocol getFinalVersion();

	protected abstract ChannelFuture sendPacketAuthRequired(ChannelHandlerContext ctx, byte authMethod, String message);

	protected abstract ChannelFuture sendPacketVersionNoAuth(ChannelHandlerContext ctx, int selectedEaglerProtocol,
			int selectedMinecraftProtocol, String serverBrand, String serverVersions);

	protected abstract ChannelFuture sendPacketVersionAuth(ChannelHandlerContext ctx, int selectedEaglerProtocol,
			int selectedMinecraftProtocol, String serverBrand, String serverVersion, byte authMethod,
			byte[] authSaltingData, boolean nicknameSelection);

	protected void handlePacketRequestLogin(ChannelHandlerContext ctx, String requestedUsername, String requestedServer,
			byte[] authPassword, boolean enableCookie, byte[] authCookie, int standardCapabilities,
			int[] standardCapabilityVersions) {
		handlePacketRequestLogin(ctx, requestedUsername, requestedServer, authPassword, enableCookie, authCookie,
				standardCapabilities, standardCapabilityVersions, NO_UUID, NO_VER);
	}

	protected void handlePacketRequestLogin(ChannelHandlerContext ctx, String requestedUsername, String requestedServer,
			byte[] authPassword, boolean enableCookie, byte[] authCookie, int standardCapabilities,
			int[] standardCapabilityVersions, UUID[] extendedCapabilities, int[] extendedCapabilityVersions) {
		if (state == HandshakePacketTypes.STATE_CLIENT_VERSION) {
			state = HandshakePacketTypes.STATE_STALLING;

			processCapabilities(standardCapabilities, standardCapabilityVersions, extendedCapabilities,
					extendedCapabilityVersions);

			String username;

			byte[] b = pipelineData.handshakeAuthUsername;
			if (b == null) {
				// This shouldn't be null unless auth events are disabled anyway
				int strlen = requestedUsername.length();
				b = new byte[strlen];
				for (int i = 0; i < strlen; ++i) {
					b[i] = (byte) requestedUsername.charAt(i);
				}
				pipelineData.handshakeAuthUsername = b;
				username = requestedUsername;
			} else {
				if (pipelineData.nicknameSelectionEnabled) {
					username = requestedUsername;
					b = requestedUsername.getBytes(StandardCharsets.US_ASCII);
				} else {
					username = new String(b, StandardCharsets.US_ASCII);
				}
			}

			if (!USERNAME_REGEX.matcher(username).matches()) {
				state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
				inboundHandler.terminateErrorCode(ctx, getVersion(), HandshakePacketTypes.SERVER_ERROR_CUSTOM_MESSAGE,
						"Invalid Username");
				return;
			}

			pipelineData.requestedServer = requestedServer;
			pipelineData.cookieSupport = CapabilityBits.hasCapability(pipelineData.acceptedCapabilitiesMask,
					pipelineData.acceptedCapabilitiesVers, EnumCapabilityType.COOKIE.getId(), 0);
			pipelineData.cookieEnabled = enableCookie;
			pipelineData.cookieData = authCookie;

			byte[] uuidHashGenerator = new byte[OFFLINE_PLAYER_BYTES.length + b.length];
			System.arraycopy(OFFLINE_PLAYER_BYTES, 0, uuidHashGenerator, 0, OFFLINE_PLAYER_BYTES.length);
			System.arraycopy(b, 0, uuidHashGenerator, OFFLINE_PLAYER_BYTES.length, b.length);
			UUID clientUUID = UUID.nameUUIDFromBytes(uuidHashGenerator);

			pipelineData.username = username;
			pipelineData.uuid = clientUUID;

			if (pipelineData.authEventEnabled) {
				if (authPassword.length > 0) {
					continueLoginPasswordAuth(ctx, requestedUsername, authPassword);
				} else if (pipelineData.cookieAuthEventEnabled) {
					continueLoginCookieAuth(ctx, requestedUsername);
				} else {
					state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
					inboundHandler.terminateErrorCode(ctx, getVersion(), HandshakePacketTypes.SERVER_ERROR_WRONG_PACKET,
							"Missing Login Packet Password");
				}
			} else if (pipelineData.cookieAuthEventEnabled) {
				continueLoginCookieAuth(ctx, requestedUsername);
			} else {
				handleContinueLogin(ctx);
			}
		} else {
			state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
			inboundHandler.terminateErrorCode(ctx, getVersion(), HandshakePacketTypes.SERVER_ERROR_WRONG_PACKET,
					"Wrong Request Login Packet");
		}
	}

	protected void processCapabilities(int standardCapabilities, int[] standardCapabilityVersions,
			UUID[] extendedCapabilities, int[] extendedCapabilityVersions) {
		int acceptedMask = 0;
		byte[] acceptedVersions = new byte[8];
		int acceptedIdx = 0;
		int standardCount = Integer.bitCount(standardCapabilities);
		if (standardCount > standardCapabilityVersions.length) {
			standardCount = standardCapabilityVersions.length;
		}
		for (int i = 0; i < standardCount && acceptedIdx < acceptedVersions.length && standardCapabilities != 0; ++i) {
			int bit = Integer.numberOfTrailingZeros(standardCapabilities);
			int verBits = standardCapabilityVersions[i];
			switch (bit) {
			case 0: // UPDATE
				if ((verBits & 1) != 0) { // V0
					acceptedMask |= EnumCapabilityType.UPDATE.getBit();
					acceptedVersions[acceptedIdx++] = 0;
				}
				break;
			case 1: // VOICE
				if ((verBits & 1) != 0) { // V0
					acceptedMask |= EnumCapabilityType.VOICE.getBit();
					acceptedVersions[acceptedIdx++] = 0;
				}
				break;
			case 2: // REDIRECT
				if ((verBits & 1) != 0) { // V0
					acceptedMask |= EnumCapabilityType.REDIRECT.getBit();
					acceptedVersions[acceptedIdx++] = 0;
				}
				break;
			case 3: // NOTIFICATION
				if ((verBits & 1) != 0) { // V0
					acceptedMask |= EnumCapabilityType.NOTIFICATION.getBit();
					acceptedVersions[acceptedIdx++] = 0;
				}
				break;
			case 4: // PAUSE_MENU
				if ((verBits & 1) != 0) { // V0
					acceptedMask |= EnumCapabilityType.PAUSE_MENU.getBit();
					acceptedVersions[acceptedIdx++] = 0;
				}
				break;
			case 5: // WEBVIEW
				if ((verBits & 1) != 0) { // V0
					acceptedMask |= EnumCapabilityType.WEBVIEW.getBit();
					acceptedVersions[acceptedIdx++] = 0;
				}
				break;
			case 6: // COOKIE
				if ((verBits & 1) != 0) { // V0
					acceptedMask |= EnumCapabilityType.COOKIE.getBit();
					acceptedVersions[acceptedIdx++] = 0;
				}
				break;
//			case 7: // EAGLER_IP
//				if((verBits & 1) != 0) { // V0
//					acceptedMask |= EnumCapabilityType.EAGLER_IP.getBit();
//					acceptedVersions[acceptedIdx++] = 0;
//				}
//				break;
			}
			standardCapabilities &= (0xFFFFFFFF << (bit + 1));
		}
		pipelineData.acceptedCapabilitiesMask = acceptedMask;
		pipelineData.acceptedCapabilitiesVers = acceptedIdx == acceptedVersions.length ? acceptedVersions
				: Arrays.copyOf(acceptedVersions, acceptedIdx);
		int extCnt = extendedCapabilities.length;
		if (extCnt > 0) {
			ObjectIntMap<UUID> tmpMap = new ObjectIntHashMap<>(extCnt);
			for (int i = 0; i < extCnt; ++i) {
				tmpMap.put(extendedCapabilities[i], extendedCapabilityVersions[i]);
			}
			pipelineData.acceptedExtendedCapabilities = server.getExtCapabilityMap().acceptExtendedCapabilities(tmpMap);
		} else {
			pipelineData.acceptedExtendedCapabilities = Collections.emptyMap();
		}
	}

	private void continueLoginPasswordAuth(ChannelHandlerContext ctx, String requestedUsername, byte[] authPassword) {
		server.eventDispatcher().dispatchAuthPasswordEvent(pipelineData.asLoginConnection(),
				pipelineData.handshakeAuthUsername, pipelineData.nicknameSelectionEnabled, pipelineData.authSalt,
				authPassword, pipelineData.cookieEnabled, pipelineData.cookieData, requestedUsername,
				pipelineData.username, pipelineData.uuid, pipelineData.authType, pipelineData.authMessage,
				pipelineData.requestedServer, inEventLoop(ctx.channel(), (evt, err) -> {
			IEaglercraftAuthPasswordEvent.EnumAuthResponse response = evt.getAuthResponse();
			if (response == null) {
				state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
				inboundHandler.terminateInternalError(ctx, getVersion());
				pipelineData.connectionLogger.error("Auth password event was not handled");
			} else if (response == IEaglercraftAuthPasswordEvent.EnumAuthResponse.ALLOW) {
				pipelineData.username = evt.getProfileUsername();
				if (server.getPlatform().getType() != EnumAdapterPlatformType.BUKKIT) {
					pipelineData.uuid = evt.getProfileUUID();
				}
				pipelineData.requestedServer = evt.getAuthRequestedServer();
				handleContinueLogin(ctx);
			} else {
				Object obj = evt.getKickMessage();
				if (obj == null) {
					obj = server.componentBuilder().buildTranslationComponent().translation("disconnect.closed")
							.end();
				}
				final Object obj2 = obj;
				state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
				sendPacketDenyLogin(ctx, obj2).addListener(ChannelFutureListener.CLOSE);
			}
		}));
	}

	private void continueLoginCookieAuth(ChannelHandlerContext ctx, String requestedUsername) {
		server.eventDispatcher().dispatchAuthCookieEvent(pipelineData.asLoginConnection(),
				pipelineData.handshakeAuthUsername, pipelineData.nicknameSelectionEnabled, pipelineData.cookieEnabled,
				pipelineData.cookieData, requestedUsername, pipelineData.username, pipelineData.uuid,
				pipelineData.authType, pipelineData.authMessage, pipelineData.requestedServer,
				inEventLoop(ctx.channel(), (evt, err) -> {
			IEaglercraftAuthCookieEvent.EnumAuthResponse response = evt.getAuthResponse();
			if (response == null) {
				state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
				inboundHandler.terminateInternalError(ctx, getVersion());
				pipelineData.connectionLogger.error("Auth cookie event was not handled");
				return;
			}
			switch (response) {
			case ALLOW:
				pipelineData.username = evt.getProfileUsername();
				if (server.getPlatform().getType() != EnumAdapterPlatformType.BUKKIT) {
					pipelineData.uuid = evt.getProfileUUID();
				}
				pipelineData.requestedServer = evt.getAuthRequestedServer();
				handleContinueLogin(ctx);
				break;
			case DENY:
				Object obj = evt.getKickMessage();
				if (obj == null) {
					obj = server.componentBuilder().buildTranslationComponent().translation("disconnect.closed")
							.end();
				}
				final Object obj2 = obj;
				state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
				sendPacketDenyLogin(ctx, obj2).addListener(ChannelFutureListener.CLOSE);
				break;
			case REQUIRE_AUTH:
				inboundHandler.terminated = true;
				state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
				sendPacketAuthRequired(ctx, pipelineData.authType, evt.getAuthMessage())
						.addListener(ChannelFutureListener.CLOSE);
				break;
			}
		}));
	}

	private void updateLoggerName() {
		pipelineData.connectionLogger.setName(pipelineData.connectionLogger.getName() + "|" + pipelineData.username);
	}

	private void handleContinueLogin(ChannelHandlerContext ctx) {
		server.eventDispatcher().dispatchLoginEvent(pipelineData.asLoginConnection(),
				pipelineData.hasLoginStateRedirectCap(), pipelineData.requestedServer,
				inEventLoop(ctx.channel(), (evt, err) -> {
			if (err == null) {
				if (evt.isCancelled()) {
					state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
					Object kickMsg = evt.getMessage();
					if (kickMsg == null) {
						String redirectAddr = evt.getRedirectAddress();
						if (redirectAddr != null) {
							if (evt.isLoginStateRedirectSupported()) {
								sendPacketLoginStateRedirect(ctx, redirectAddr).addListener(ChannelFutureListener.CLOSE);
							} else {
								inboundHandler.terminateInternalError(ctx, getVersion());
								pipelineData.connectionLogger
										.error("A plugin attempted to login-state redirect a client "
												+ "that does not support login-state redirects");
							}
							return;
						} else {
							kickMsg = server.componentBuilder().buildTranslationComponent()
									.translation("disconnect.closed").end();
						}
					}
					final Object msg2 = kickMsg;
					sendPacketDenyLogin(ctx, msg2).addListener(ChannelFutureListener.CLOSE);
				} else {
					pipelineData.username = evt.getProfileUsername();
					if (server.getPlatform().getType() != EnumAdapterPlatformType.BUKKIT) {
						pipelineData.uuid = evt.getProfileUUID();
					}
					pipelineData.requestedServer = evt.getRequestedServer();
					inboundHandler.beginBackendHandshake(ctx);
				}
			} else {
				state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
				inboundHandler.terminateInternalError(ctx, getVersion());
				pipelineData.connectionLogger.error("Caught exception dispatching login event", err);
			}
		}));
	}

	public void handleBackendHandshakeSuccess(ChannelHandlerContext ctx, String acceptedUsername, UUID acceptedUUID) {
		pipelineData.username = acceptedUsername;
		updateLoggerName();
		if (server.getPlatform().getType() == EnumAdapterPlatformType.BUKKIT
				&& !acceptedUUID.equals(pipelineData.uuid)) {
			pipelineData.connectionLogger
					.warn("The underlying server assigned a UUID to this player that does not match "
							+ "the expected offline-mode UUID for their username");
		}
		pipelineData.uuid = acceptedUUID;
		ConfigDataSettings settings = server.getConfig().getSettings();
		if (settings.isDebugLogOriginHeaders()) {
			if (pipelineData.headerOrigin != null) {
				pipelineData.connectionLogger.info("Player is from origin: " + pipelineData.headerOrigin);
			} else {
				pipelineData.connectionLogger.info("Player has no origin header");
			}
		}
		if (settings.isDebugLogClientBrands()) {
			pipelineData.connectionLogger.info("Player is using client: " + pipelineData.eaglerBrandString + " - "
					+ pipelineData.eaglerVersionString);
		}
		state = HandshakePacketTypes.STATE_CLIENT_LOGIN;
		sendPacketAllowLogin(ctx, pipelineData.username, pipelineData.uuid, pipelineData.acceptedCapabilitiesMask,
				pipelineData.acceptedCapabilitiesVers, pipelineData.acceptedExtendedCapabilities);
	}

	protected abstract ChannelFuture sendPacketAllowLogin(ChannelHandlerContext ctx, String setUsername, UUID setUUID,
			int standardCapabilities, byte[] standardCapabilityVersions, Map<UUID, Byte> extendedCapabilities);

	protected abstract ChannelFuture sendPacketDenyLogin(ChannelHandlerContext ctx, Object component);

	protected abstract ChannelFuture sendPacketDenyLogin(ChannelHandlerContext ctx, String message);

	protected abstract ChannelFuture sendPacketLoginStateRedirect(ChannelHandlerContext ctx, String address);

	protected void handlePacketProfileData(ChannelHandlerContext ctx, String key, byte[] value) {
		if (state == HandshakePacketTypes.STATE_CLIENT_LOGIN) {
			if (pipelineData.profileDatas == null) {
				pipelineData.profileDatas = new HashMap<>(4);
				pipelineData.profileDatas.put(key, value);
			} else if (pipelineData.profileDatas.size() >= 8) {
				state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
				inboundHandler.terminateErrorCode(ctx, getVersion(),
						HandshakePacketTypes.SERVER_ERROR_EXCESSIVE_PROFILE_DATA, "Too Many Profile Datas");
			} else if (pipelineData.profileDatas.putIfAbsent(key, value) != null) {
				state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
				inboundHandler.terminateErrorCode(ctx, getVersion(),
						HandshakePacketTypes.SERVER_ERROR_DUPLICATE_PROFILE_DATA, "Duplicate Profile Data");
			}
		} else {
			state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
			inboundHandler.terminateErrorCode(ctx, getVersion(), HandshakePacketTypes.SERVER_ERROR_WRONG_PACKET,
					"Wrong Profile Data Packet");
		}
	}

	protected void handlePacketFinishLogin(ChannelHandlerContext ctx) {
		if (state == HandshakePacketTypes.STATE_CLIENT_LOGIN) {
			state = HandshakePacketTypes.STATE_STALLING;
			inboundHandler.enterPlayState(ctx);
		} else {
			state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
			inboundHandler.terminateErrorCode(ctx, getVersion(), HandshakePacketTypes.SERVER_ERROR_WRONG_PACKET,
					"Wrong Finish Login Packet");
		}
	}

	protected abstract ChannelFuture sendPacketFinishLogin(ChannelHandlerContext ctx);

	public void finish(ChannelHandlerContext ctx) {
		state = HandshakePacketTypes.STATE_CLIENT_COMPLETE;
		sendPacketFinishLogin(ctx);
	}

}
