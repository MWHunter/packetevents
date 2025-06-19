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
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

@ApiStatus.Internal
public class InternalBukkitListener implements Listener {

    private final Plugin plugin;

    public InternalBukkitListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
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
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "PacketEvents 2.0 failed to inject");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpawnInGame(PlayerSpawnLocationEvent event) {
        PacketEventsAPI<?> api = PacketEvents.getAPI();
        Player player = event.getPlayer();
        SpigotChannelInjector injector = (SpigotChannelInjector) api.getInjector();
        User user = api.getPlayerManager().getUser(event.getPlayer());
        if (user == null) {
            //We did not inject this user
            Object channel = api.getPlayerManager().getChannel(player);
            if (channel == null) {
                return;
            }
            //Check if it is a fake connection...
            if (!FakeChannelUtil.isFakeChannel(channel) && (!api.isTerminated() || api.getSettings().isKickIfTerminated())) {
                //Kick them, if they are not a fake player.
                FoliaScheduler.getEntityScheduler().runDelayed(player, plugin, (o) -> {
                    player.kickPlayer("PacketEvents 2.0 failed to inject");
                }, null, 0);
            }
            return;
        }

        // Set bukkit player object in the injectors
        injector.updatePlayer(user, player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        Object channel = PacketEvents.getAPI().getPlayerManager().getChannel(player);
        SpigotChannelInjector injector = (SpigotChannelInjector) PacketEvents.getAPI().getInjector();
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) {
            //We did not inject this user
            //Check if it is a fake connection...
            if (channel == null || !FakeChannelUtil.isFakeChannel(channel) && (!PacketEvents.getAPI().isTerminated() || PacketEvents.getAPI().getSettings().isKickIfTerminated())) {
                //Kick them, if they are not a fake player.
                FoliaScheduler.getEntityScheduler().runDelayed(player, plugin, (o) -> {
                    if (player.isConnected()) {
                        player.kickPlayer("PacketEvents 2.0 failed to inject");
                    }
                }, null, 0);
            }
            // Set bukkit player object in the injectors
            return;
        }

        // It's possible that the player reference was set in the prior event.
        if (!injector.isPlayerSet(channel)) {
            injector.updatePlayer(user, player);
        }
    }
}
