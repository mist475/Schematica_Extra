package com.github.lunatrius.schematica.client.gui.load;

import static com.github.lunatrius.schematica.client.util.WorldServerName.worldServerName;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.lwjgl.Sys;

import com.github.lunatrius.core.client.gui.GuiScreenBase;
import com.github.lunatrius.schematica.FileFilterSchematic;
import com.github.lunatrius.schematica.Schematica;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.schematic.SchematicUtil;

public class GuiSchematicLoad extends GuiScreenBase {

    private static final FileFilterSchematic FILE_FILTER_FOLDER = new FileFilterSchematic(true);
    private static final FileFilterSchematic FILE_FILTER_SCHEMATIC = new FileFilterSchematic(false);
    protected final List<GuiSchematicEntry> schematicFiles = new ArrayList<GuiSchematicEntry>();
    private final String strTitle = I18n.format(Names.Gui.Load.TITLE);
    private final String strFolderInfo = I18n.format(Names.Gui.Load.FOLDER_INFO);
    protected File currentDirectory = ConfigurationHandler.schematicDirectory;
    private GuiSchematicLoadSlot guiSchematicLoadSlot;
    private GuiButton btnOpenDir = null;
    private GuiButton btnDone = null;

    public GuiSchematicLoad(GuiScreen guiScreen) {
        super(guiScreen);
    }

    @Override
    public void initGui() {
        int id = 0;

        this.btnOpenDir = new GuiButton(
                id++,
                this.width / 2 - 154,
                this.height - 36,
                150,
                20,
                I18n.format(Names.Gui.Load.OPEN_FOLDER));
        this.buttonList.add(this.btnOpenDir);

        this.btnDone = new GuiButton(id++, this.width / 2 + 4, this.height - 36, 150, 20, I18n.format(Names.Gui.DONE));
        this.buttonList.add(this.btnDone);

        this.guiSchematicLoadSlot = new GuiSchematicLoadSlot(this);

        reloadSchematics();
    }

    @Override
    protected void actionPerformed(GuiButton guiButton) {
        if (guiButton.enabled) {
            if (guiButton.id == this.btnOpenDir.id) {
                boolean retry = false;

                try {
                    Class c = Class.forName("java.awt.Desktop");
                    Object m = c.getMethod("getDesktop").invoke(null);
                    c.getMethod("browse", URI.class).invoke(m, ConfigurationHandler.schematicDirectory.toURI());
                } catch (Throwable e) {
                    retry = true;
                }

                if (retry) {
                    Reference.logger.info("Opening via Sys class!");
                    Sys.openURL("file://" + ConfigurationHandler.schematicDirectory.getAbsolutePath());
                }
            } else if (guiButton.id == this.btnDone.id) {
                if (Schematica.proxy.isLoadEnabled) {
                    loadSchematic();
                }
                this.mc.displayGuiScreen(this.parentScreen);
            } else {
                this.guiSchematicLoadSlot.actionPerformed(guiButton);
            }
        }
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {
        this.guiSchematicLoadSlot.drawScreen(x, y, partialTicks);

        drawCenteredString(this.fontRendererObj, this.strTitle, this.width / 2, 4, 0x00FFFFFF);
        drawCenteredString(this.fontRendererObj, this.strFolderInfo, this.width / 2 - 78, this.height - 12, 0x00808080);

        super.drawScreen(x, y, partialTicks);
    }

    @Override
    public void onGuiClosed() {
        // loadSchematic();
    }

    protected void changeDirectory(String directory) {
        this.currentDirectory = new File(this.currentDirectory, directory);

        reloadSchematics();
    }

    protected void reloadSchematics() {
        String name = null;
        Item item = null;

        this.schematicFiles.clear();

        try {
            if (!this.currentDirectory.getCanonicalPath()
                    .equals(ConfigurationHandler.schematicDirectory.getCanonicalPath())) {
                this.schematicFiles.add(new GuiSchematicEntry("..", Items.lava_bucket, 0, true));
            }
        } catch (IOException e) {
            Reference.logger.error("Failed to add GuiSchematicEntry!", e);
        }

        File[] filesFolders = this.currentDirectory.listFiles(FILE_FILTER_FOLDER);
        if (filesFolders == null) {
            Reference.logger.error("listFiles returned null (directory: {})!", this.currentDirectory);
        } else {
            for (File file : filesFolders) {
                if (file == null) {
                    continue;
                }

                name = file.getName();

                File[] files = file.listFiles();
                item = (files == null || files.length == 0) ? Items.bucket : Items.water_bucket;

                this.schematicFiles.add(new GuiSchematicEntry(name, item, 0, file.isDirectory()));
            }
        }

        File[] filesSchematics = this.currentDirectory.listFiles(FILE_FILTER_SCHEMATIC);
        if (filesSchematics == null || filesSchematics.length == 0) {
            this.schematicFiles
                    .add(new GuiSchematicEntry(I18n.format(Names.Gui.Load.NO_SCHEMATIC), Blocks.dirt, 0, false));
        } else {
            for (File file : filesSchematics) {
                name = file.getName();

                this.schematicFiles
                        .add(new GuiSchematicEntry(name, SchematicUtil.getIconFromFile(file), file.isDirectory()));
            }
        }
    }

    private void loadSchematic() {
        int selectedIndex = this.guiSchematicLoadSlot.selectedIndex;

        try {
            if (selectedIndex >= 0 && selectedIndex < this.schematicFiles.size()) {
                GuiSchematicEntry schematicEntry = this.schematicFiles.get(selectedIndex);
                if (Schematica.proxy.loadSchematic(null, this.currentDirectory, schematicEntry.getName())) {
                    SchematicWorld schematic = ClientProxy.schematic;
                    if (schematic != null) {
                        ImmutableTriple<Boolean, Integer, ImmutableTriple<Integer, Integer, Integer>> schematicCoordinate = ClientProxy
                                .getCoordinates(worldServerName(this.mc), schematic.name);
                        if (schematicCoordinate.left) {
                            ClientProxy.moveSchematic(
                                    schematic,
                                    schematicCoordinate.right.left,
                                    schematicCoordinate.right.middle,
                                    schematicCoordinate.right.right);
                            for (int i = 0; i < schematicCoordinate.middle; i++) {
                                schematic.rotate();
                            }
                            RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(schematic);
                            SchematicPrinter.INSTANCE.refresh();
                        } else {
                            ClientProxy.moveSchematicToPlayer(schematic);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Reference.logger.error("Failed to load schematic!", e);
        }
    }
}
