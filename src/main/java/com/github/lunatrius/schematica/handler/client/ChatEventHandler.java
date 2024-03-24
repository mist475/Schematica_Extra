package com.github.lunatrius.schematica.handler.client;

import net.minecraftforge.client.event.ClientChatReceivedEvent;

import com.github.lunatrius.schematica.Schematica;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ChatEventHandler {

    public static final ChatEventHandler INSTANCE = new ChatEventHandler();

    public int chatLines = 0;

    private ChatEventHandler() {}

    @SubscribeEvent
    public void onClientChatReceivedEvent(ClientChatReceivedEvent event) {
        if (event.message != null && this.chatLines < 20) {
            String message = event.message.getFormattedText();
            if (message != null && !message.isEmpty()) {
                this.chatLines++;
                Reference.logger.debug("Message #{}: {}", this.chatLines, message);
                if (message.contains(Names.SBC.DISABLE_PRINTER)) {
                    Reference.logger.info("Printer is disabled on this server.");
                    SchematicPrinter.INSTANCE.setEnabled(false);
                }
                if (message.contains(Names.SBC.DISABLE_SAVE)) {
                    Reference.logger.info("Saving is disabled on this server.");
                    Schematica.proxy.isSaveEnabled = false;
                }
                if (message.contains(Names.SBC.DISABLE_LOAD)) {
                    Reference.logger.info("Loading is disabled on this server.");
                    Schematica.proxy.isLoadEnabled = false;
                }
            }
        }
    }
}
