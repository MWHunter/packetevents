/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2021 retrooper and contributors
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

package io.github.retrooper.packetevents.packetwrappers.play.out.blockaction;

import io.github.retrooper.packetevents.packettype.PacketTypeClasses;
import io.github.retrooper.packetevents.packetwrappers.NMSPacket;
import io.github.retrooper.packetevents.packetwrappers.WrappedPacket;
import io.github.retrooper.packetevents.packetwrappers.api.SendableWrapper;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.Material;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This packet is used for a number of actions and animations performed by blocks, usually non-persistent.
 *
 * @author Tecnio
 */
public class WrappedPacketOutBlockAction extends WrappedPacket implements SendableWrapper {

    private static boolean v_1_7_10;
    private static Constructor<?> packetConstructor;
    private static Method getNMSBlockMethodCache = null;
    private Vector3i blockPos;
    private int actionID, actionData;
    private Material blockType;

    public WrappedPacketOutBlockAction(NMSPacket packet) {
        super(packet);
    }

    public WrappedPacketOutBlockAction(Vector3i blockPos, int actionID, int actionData, Material blockType) {
        this.blockPos = blockPos;
        this.actionID = actionID;
        this.actionData = actionData;
        this.blockType = blockType;
    }

    @Override
    protected void load() {
        v_1_7_10 = version.isOlderThan(ServerVersion.v_1_8);
        getNMSBlockMethodCache = Reflection.getMethod(NMSUtils.iBlockDataClass, "getBlock", 0);
        if (getNMSBlockMethodCache == null) {
            Class<?> blockDataClass = NMSUtils.iBlockDataClass.getSuperclass();
            getNMSBlockMethodCache = Reflection.getMethod(blockDataClass, NMSUtils.blockClass, 0);
        }
        try {
            if (v_1_7_10) {
                packetConstructor = PacketTypeClasses.Play.Server.BLOCK_ACTION.getConstructor(NMSUtils.blockPosClass, NMSUtils.blockClass, int.class, int.class);
            } else {
                packetConstructor = PacketTypeClasses.Play.Server.BLOCK_ACTION.getConstructor(NMSUtils.blockPosClass, NMSUtils.iBlockDataClass, int.class, int.class);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public Vector3i getBlockPosition() {
        if (packet != null) {
            return readBlockPosition(0);
        } else {
            return this.blockPos;
        }
    }

    public void setBlockPosition(Vector3i blockPos) {
        if (packet != null) {
            writeBlockPosition(0, blockPos);
        } else {
            this.blockPos = blockPos;
        }
    }

    public int getActionId() {
        if (packet != null) {
            return readInt(0);
        } else {
            return this.actionID;
        }
    }

    public void setActionId(int actionID) {
        if (packet != null) {
            writeInt(0, actionID);
        } else {
            this.actionID = actionID;
        }
    }

    @Deprecated
    public int getActionParam() {
        return getActionData();
    }

    @Deprecated
    public void setActionParam(int actionParam) {
        setActionData(actionParam);
    }

    public int getActionData() {
        if (packet != null) {
            return readInt(1);
        } else {
            return this.actionData;
        }
    }

    public void setActionData(int actionData) {
        if (packet != null) {
            writeInt(1, actionData);
        } else {
            this.actionData = actionData;
        }
    }

    public Material getBlockType() {
        if (packet != null) {
            Object nmsBlock = null;
            if (v_1_7_10) {
                nmsBlock = readObject(0, NMSUtils.blockClass);
            } else {
                Object iBlockDataObj = readObject(0, NMSUtils.iBlockDataClass);
                try {
                    nmsBlock = getNMSBlockMethodCache.invoke(iBlockDataObj);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            return NMSUtils.getMaterialFromNMSBlock(nmsBlock);
        } else {
            return this.blockType;
        }
    }

    public void setBlockType(Material blockType) {
        if (packet != null) {
            Object nmsBlock = NMSUtils.getNMSBlockFromMaterial(blockType);
            if (v_1_7_10) {
                write(NMSUtils.blockClass, 0, nmsBlock);
            } else {
                WrappedPacket nmsBlockWrapper = new WrappedPacket(new NMSPacket(nmsBlock), NMSUtils.blockClass);
                Object iBlockData = nmsBlockWrapper.readObject(0, NMSUtils.iBlockDataClass);
                write(NMSUtils.iBlockDataClass, 0, iBlockData);
            }
        } else {
            this.blockType = blockType;
        }
    }

    @Override
    public Object asNMSPacket() throws Exception {
        Object nmsBlockPos = NMSUtils.generateNMSBlockPos(getBlockPosition());
        Object nmsBlock = NMSUtils.getNMSBlockFromMaterial(getBlockType());
        if (v_1_7_10) {
            return packetConstructor.newInstance(nmsBlockPos, nmsBlock, getActionId(), getActionData());
        } else {
            WrappedPacket nmsBlockWrapper = new WrappedPacket(new NMSPacket(nmsBlock), NMSUtils.blockClass);
            Object iBlockData = nmsBlockWrapper.readObject(0, NMSUtils.iBlockDataClass);
            return packetConstructor.newInstance(nmsBlockPos, iBlockData, getActionId(), getActionData());
        }
    }
}
