/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2025 retrooper and contributors
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

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * Used on Paper 1.21.9+ because the event we previously used is now called in a different
 * place, during the configuration phase.<br/>
 * There is no replacement for this event we used, so now we are not able to get the player instance
 * before the server starts sending play packets to the player. This is not good.
 */
@NullMarked
@ApiStatus.Internal
public class InternalPaperJoinListener implements Listener {

    private final InternalBukkitListener delegate;

    public InternalPaperJoinListener(Plugin plugin) {
        this.delegate = new InternalBukkitListener(plugin);
    }

    // TODO somehow manage to the get player instance before the
    //   server starts sending play packets
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        this.delegate.onPostJoin(event.getPlayer());
    }
}
