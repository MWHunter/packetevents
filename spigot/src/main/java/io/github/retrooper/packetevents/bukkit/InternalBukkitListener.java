/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2022 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.retrooper.packetevents.bukkit;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.FakeChannelUtil;
import io.github.retrooper.packetevents.injector.SpigotChannelInjector;
import io.github.retrooper.packetevents.manager.player.PlayerManagerImpl;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;

@ApiStatus.Internal
public class InternalBukkitListener implements Listener {

    private static final String KICK_MESSAGE = "PacketEvents 2.0 failed to inject";

    private final Plugin plugin;

    public InternalBukkitListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLoginInstant(PlayerLoginEvent event) {
        PacketEventsAPI<?> api = PacketEvents.getAPI();
        if (api.getServerManager().getVersion().isOlderThan(ServerVersion.V_1_20_5)) {
            return; // only works for 1.20.5 and above
        }
        User user = api.getPlayerManager().getUser(event.getPlayer());
        if (user != null) {
            // if the user can be resolved from this player, save in encoder/decoder
            SpigotChannelInjector injector = (SpigotChannelInjector) api.getInjector();
            injector.updatePlayer(user, event.getPlayer());
            return; // we're done
        }
        Object channel = api.getPlayerManager().getChannel(event.getPlayer());
        if (channel != null && FakeChannelUtil.isFakeChannel(channel)
                || (api.isTerminated() && !api.getSettings().isKickIfTerminated())) {
            // either fake channel or api terminated (and we don't kick)
            return;
        }
        // since 1.20.5 and cookie packets, CraftBukkit associates the login listener with the player
        // before calling the login event; if this fails on 1.20.5+, something broke a lot
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, KICK_MESSAGE);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoginDelayed(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return; // don't care
        }
        PacketEventsAPI<?> api = PacketEvents.getAPI();
        if (api.getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_20_5)) {
            return; // if this is 1.20.5+, we have a direct way to access the channel, see method above
        }
        // save player in map for packet handler to consume
        Map<UUID, WeakReference<Player>> map = ((PlayerManagerImpl) api.getPlayerManager()).joiningPlayers;
        map.put(event.getPlayer().getUniqueId(), new WeakReference<>(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        PacketEventsAPI<?> api = PacketEvents.getAPI();
        if (api.getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_20_5)) {
            return; // if this is 1.20.5+, we have already processed everything in login event
        }
        Player player = event.getPlayer();
        User user = api.getPlayerManager().getUser(player);
        if (user != null) {
            // update player reference in encoder/decoder (doesn't matter if this was already done)
            SpigotChannelInjector injector = (SpigotChannelInjector) PacketEvents.getAPI().getInjector();
            injector.setPlayer(user.getChannel(), player);
            // remove from map; this is probably already removed anyway, but just make sure
            ((PlayerManagerImpl) api.getPlayerManager()).joiningPlayers.remove(player.getUniqueId());
            return;
        }

        // if this case occurs, we can't extract the connection from a fully joined player
        // or have internal connection for this player; this should not occur, kick them

        // remove from map just to be sure
        ((PlayerManagerImpl) api.getPlayerManager()).joiningPlayers.remove(player.getUniqueId());

        Object channel = api.getPlayerManager().getChannel(player);
        if (channel != null && FakeChannelUtil.isFakeChannel(channel)
                || (api.isTerminated() && !api.getSettings().isKickIfTerminated())) {
            // either fake channel or api terminated (and we don't kick)
            return;
        }

        // delay by a tick
        FoliaScheduler.getEntityScheduler().runDelayed(player, this.plugin, __ -> {
            // only kick if the player is actually still connected
            if (player.isConnected()) {
                player.kickPlayer(KICK_MESSAGE);
            }
        }, null, 0);
    }
}
