package com.github.lunatrius.schematica.handler;

import java.util.ArrayDeque;
import java.util.Queue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;

import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.chunk.SchematicContainer;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class QueueTickHandler {

    public static final QueueTickHandler INSTANCE = new QueueTickHandler();

    private final Queue<SchematicContainer> queue = new ArrayDeque<>();

    private QueueTickHandler() {}

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        // TODO: find a better way... maybe?
        try {
            final EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
            if (player != null && player.sendQueue != null && !player.sendQueue.getNetworkManager().isLocalChannel()) {
                processQueue();
            }
        } catch (Exception e) {
            Reference.logger.error("Something went wrong...", e);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        processQueue();
    }

    private void processQueue() {
        if (this.queue.isEmpty()) {
            return;
        }

        final SchematicContainer container = this.queue.poll();
        if (container == null) {
            return;
        }

        if (container.hasNext()) {
            if (container.isFirst()) {
                final ChatComponentTranslation chatComponent = new ChatComponentTranslation(
                        Names.Command.Save.Message.SAVE_STARTED,
                        container.chunkCount,
                        container.file.getName());
                container.player.addChatMessage(chatComponent);
            }

            container.next();
        }

        if (container.hasNext()) {
            this.queue.offer(container);
        } else {
            if (container.world != null) {
                for (TileEntity entity : container.schematic.getTileEntities()) {
                    if (!entity.hasWorldObj()) {
                        entity.setWorldObj(container.world);
                    }
                }
            }

            final boolean success = SchematicFormat.writeToFile(container.file, container.schematic, container.world);
            final String message = success ? Names.Command.Save.Message.SAVE_SUCCESSFUL
                    : Names.Command.Save.Message.SAVE_FAILED;
            container.player.addChatMessage(new ChatComponentTranslation(message, container.file.getName()));
        }
    }

    public void queueSchematic(SchematicContainer container) {
        this.queue.offer(container);
    }
}
