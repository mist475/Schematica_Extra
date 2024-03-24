package com.github.lunatrius.schematica.command;

import java.io.File;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;

import com.github.lunatrius.core.util.vector.Vector3i;
import com.github.lunatrius.schematica.Schematica;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;

public class CommandSchematicaSave extends CommandSchematicaBase {

    @Override
    public String getCommandName() {
        return Names.Command.Save.NAME;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return Names.Command.Save.Message.USAGE;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] arguments) {
        if (arguments.length < 7) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        if (!(sender instanceof EntityPlayer player)) {
            throw new CommandException(Names.Command.Save.Message.PLAYERS_ONLY);
        }

        if (Schematica.proxy.isPlayerQuotaExceeded(player)) {
            throw new CommandException(Names.Command.Save.Message.QUOTA_EXCEEDED);
        }

        Vector3i from = new Vector3i();
        Vector3i to = new Vector3i();
        String filename;
        String name;

        try {
            from.set(Integer.parseInt(arguments[0]), Integer.parseInt(arguments[1]), Integer.parseInt(arguments[2]));
            to.set(Integer.parseInt(arguments[3]), Integer.parseInt(arguments[4]), Integer.parseInt(arguments[5]));

            name = arguments[6];
            filename = String.format("%s.schematic", name);
        } catch (NumberFormatException exception) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        Reference.logger.debug("Saving schematic from {} to {} to {}", from, to, filename);
        final File schematicDirectory = Schematica.proxy.getPlayerSchematicDirectory(player, true);
        if (schematicDirectory == null) {
            // Chances are that if this is null, we could not retrieve their UUID.
            Reference.logger.warn("Unable to determine the schematic directory for player {}", player);
            throw new CommandException(Names.Command.Save.Message.PLAYER_SCHEMATIC_DIR_UNAVAILABLE);
        }

        if (!schematicDirectory.exists()) {
            if (!schematicDirectory.mkdirs()) {
                Reference.logger
                        .warn("Could not create player schematic directory {}", schematicDirectory.getAbsolutePath());
                throw new CommandException(Names.Command.Save.Message.PLAYER_SCHEMATIC_DIR_UNAVAILABLE);
            }
        }

        try {
            Schematica.proxy.saveSchematic(player, schematicDirectory, filename, player.getEntityWorld(), from, to);
            sender.addChatMessage(new ChatComponentTranslation(Names.Command.Save.Message.SAVE_SUCCESSFUL, name));
        } catch (Exception e) {
            throw new CommandException(Names.Command.Save.Message.SAVE_FAILED, name);
        }
    }
}
