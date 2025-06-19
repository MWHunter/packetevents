package io.github.retrooper.packetevents.manager;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.LogManager;
import com.github.retrooper.packetevents.wrapper.handshaking.client.WrapperHandshakingClientHandshake;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerLoginSuccess;
import io.github.retrooper.packetevents.injector.SpigotChannelInjector;
import io.github.retrooper.packetevents.manager.player.PlayerManagerImpl;
import io.github.retrooper.packetevents.util.protocolsupport.ProtocolSupportUtil;
import io.github.retrooper.packetevents.util.viaversion.ViaVersionUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;

@ApiStatus.Internal
public class InternalBukkitPacketListener extends com.github.retrooper.packetevents.manager.InternalPacketListener {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        super.onPacketSend(event);

        // process after generic internal listener has processed this packet
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            WrapperLoginServerLoginSuccess packet = new WrapperLoginServerLoginSuccess(event);

            PacketEventsAPI<?> api = PacketEvents.getAPI();
            Map<UUID, WeakReference<Player>> map = ((PlayerManagerImpl) api.getPlayerManager()).joiningPlayers;
            WeakReference<Player> playerRef = map.remove(packet.getUserProfile().getUUID());
            Player player = playerRef != null ? playerRef.get() : null;

            // we don't care whether this player is null or not; if it is null,
            // our bukkit listener may have already handled everything or will be handling it
            if (player != null) {
                ((SpigotChannelInjector) api.getInjector()).updatePlayer(event.getUser(), player);
                if (api.getLogManager().isDebug()) {
                    api.getLogManager().debug("Updated player reference on packet handling for " + player.getUniqueId());
                }
            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Handshaking.Client.HANDSHAKE) {
            User user = event.getUser();
            WrapperHandshakingClientHandshake packet = new WrapperHandshakingClientHandshake(event);
            ClientVersion clientVersion = packet.getClientVersion();
            ConnectionState state = packet.getNextConnectionState();

            String feature;
            if (ViaVersionUtil.isAvailable()) {
                clientVersion = ClientVersion.getById(ViaVersionUtil.getProtocolVersion(user));
                feature = "ViaVersion";
            } else if (ProtocolSupportUtil.isAvailable()) {
                clientVersion = ClientVersion.getById(ProtocolSupportUtil.getProtocolVersion(user.getAddress()));
                feature = "ProtocolSupport";
            } else {
                feature = null;
            }

            LogManager logger = PacketEvents.getAPI().getLogManager();
            if (logger.isDebug()) {
                logger.debug("Processed handshake for " + event.getAddress() + ": "
                        + state.name() + " / " + packet.getClientVersion().getReleaseName()
                        + (feature != null ? " (using " + feature + ")" : ""));
            }

            user.setClientVersion(clientVersion);
            user.setConnectionState(state);
        } else {
            super.onPacketReceive(event);
        }
    }
}
