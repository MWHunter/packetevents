/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2024 retrooper and contributors
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

package io.github.retrooper.packetevents.util.folia.task;

import org.bukkit.plugin.Plugin;

/**
 * A unified interface for interacting with tasks.
 * This wrapper provides consistent methods to interact with tasks across multiple platforms, such as {@code BukkitTask} and Paper's {@code ScheduledTask}.
 */
public interface TaskWrapper {

    /**
     * Retrieves the plugin that owns this task.
     *
     * @return The {@code Plugin} instance owning this task.
     */
    Plugin getOwner();

    /**
     * Checks if the task has been cancelled.
     *
     * @return {@code true} if the task is cancelled, {@code false} otherwise.
     */
    boolean isCancelled();

    /**
     * Cancels the task.
     */
    void cancel();
}