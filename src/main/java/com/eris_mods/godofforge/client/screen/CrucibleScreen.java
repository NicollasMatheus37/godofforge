package com.eris_mods.godofforge.client.screen;

import com.eris_mods.godofforge.blockentity.CrucibleBlockEntity;
import com.eris_mods.godofforge.material.ForgeMaterial;
import com.eris_mods.godofforge.menu.CrucibleMenu;
import com.eris_mods.godofforge.network.MenuActionPayload;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Crucible GUI: temperature gauge (left), molten metal gauge (middle), input/mold slots, cast progress and output.
 * Placeholder visuals (flat colors); a real texture is in the visual backlog.
 */
public class CrucibleScreen extends AbstractContainerScreen<CrucibleMenu> {
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 180;
    /** Highest temperature shown by the gauge. */
    private static final int GAUGE_MAX_TEMP = 1500;

    // temperature gauge
    private static final int TEMP_X = 10;
    private static final int TEMP_Y = 16;
    private static final int TEMP_W = 8;
    private static final int TEMP_H = 54;
    // molten metal gauge
    private static final int TANK_X = 70;
    private static final int TANK_Y = 16;
    private static final int TANK_W = 22;
    private static final int TANK_H = 54;
    // cast progress arrow
    private static final int ARROW_X = 100;
    private static final int ARROW_Y = 40;
    private static final int ARROW_W = 26;

    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_DARK = 0xFF555555;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_GAUGE_BG = 0xFF2A2A2A;
    private static final int COLOR_HEAT = 0xFFE0502A;
    private static final int COLOR_MELT_MARK = 0xFFFFFFFF;
    private static final int COLOR_PROGRESS = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFF404040;

    private Button dumpButton;

    public CrucibleScreen(CrucibleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.inventoryLabelY = IMAGE_HEIGHT - 94;
    }

    @Override
    protected void init() {
        super.init();
        dumpButton = addRenderableWidget(Button.builder(Component.translatable("gui.godofforge.crucible.dump"),
                        button -> ClientPacketDistributor.sendToServer(new MenuActionPayload(menu.containerId, CrucibleMenu.ACTION_DUMP)))
                .size(40, 12)
                .pos(leftPos + TANK_X + TANK_W / 2 - 20, topPos + TANK_Y + TANK_H + 3)
                .build());
    }

    private ForgeMaterial.HeadStats metalStats() {
        int id = menu.getMetalId();
        if (id < 0 || minecraft.level == null) {
            return null;
        }
        Registry<ForgeMaterial> materials = minecraft.level.registryAccess().lookupOrThrow(ForgeMaterial.REGISTRY_KEY);
        ForgeMaterial material = materials.byId(id);
        return material == null ? null : material.head().orElse(null);
    }

    private Component metalName() {
        int id = menu.getMetalId();
        if (id < 0 || minecraft.level == null) {
            return Component.translatable("gui.godofforge.crucible.empty");
        }
        Registry<ForgeMaterial> materials = minecraft.level.registryAccess().lookupOrThrow(ForgeMaterial.REGISTRY_KEY);
        ForgeMaterial material = materials.byId(id);
        Identifier key = material == null ? null : materials.getKey(material);
        return key == null ? Component.literal("?") : Component.translatable(ForgeMaterial.translationKey(key));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        dumpButton.active = menu.getMetalAmount() > 0;

        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, COLOR_DARK);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, COLOR_PANEL);

        for (Slot slot : menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, COLOR_DARK);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, COLOR_SLOT);
        }

        ForgeMaterial.HeadStats stats = metalStats();

        // temperature gauge (bottom -> top)
        int temp = menu.getTemperature();
        gauge(graphics, x + TEMP_X, y + TEMP_Y, TEMP_W, TEMP_H, Math.min(1f, temp / (float) GAUGE_MAX_TEMP), COLOR_HEAT);
        if (stats != null) {
            int markY = y + TEMP_Y + TEMP_H - Math.round(TEMP_H * Math.min(1f, stats.meltingPoint() / (float) GAUGE_MAX_TEMP));
            graphics.fill(x + TEMP_X - 2, markY, x + TEMP_X + TEMP_W + 2, markY + 1, COLOR_MELT_MARK);
        }
        graphics.text(font, temp + "°C", x + TEMP_X + TEMP_W + 4, y + TEMP_Y, COLOR_TEXT);
        String source = menu.getHeatSourceMax() > CrucibleBlockEntity.AMBIENT_TEMP
                ? Component.translatable("gui.godofforge.crucible.heating", menu.getHeatSourceMax()).getString()
                : Component.translatable("gui.godofforge.crucible.no_heat").getString();
        graphics.text(font, source, x + imageWidth - 8 - font.width(source), y + 6, COLOR_TEXT);

        // molten metal gauge
        int color = stats != null ? stats.color() : COLOR_SLOT;
        gauge(graphics, x + TANK_X, y + TANK_Y, TANK_W, TANK_H, menu.getMetalAmount() / (float) CrucibleBlockEntity.CAPACITY, color);
        int nuggets = menu.getMetalAmount();
        String amount = (nuggets / ForgeMaterial.NUGGETS_PER_INGOT) + (nuggets % ForgeMaterial.NUGGETS_PER_INGOT == 0 ? ""
                : " " + (nuggets % ForgeMaterial.NUGGETS_PER_INGOT) + "/9");
        graphics.text(font, metalName().getString(), x + TANK_X + TANK_W + 4, y + TANK_Y, COLOR_TEXT);
        graphics.text(font, amount + "/" + CrucibleBlockEntity.CAPACITY / ForgeMaterial.NUGGETS_PER_INGOT, x + TANK_X + TANK_W + 4, y + TANK_Y + 10, COLOR_TEXT);

        // melt progress: small bar under the input slot
        int melt = Math.round(16f * menu.getMeltProgress() / CrucibleBlockEntity.MELT_TICKS);
        graphics.fill(x + CrucibleMenu.INPUT_X, y + CrucibleMenu.INPUT_Y + 18, x + CrucibleMenu.INPUT_X + melt, y + CrucibleMenu.INPUT_Y + 20, COLOR_HEAT);

        // cast progress arrow -> output
        graphics.fill(x + ARROW_X, y + ARROW_Y + 3, x + ARROW_X + ARROW_W, y + ARROW_Y + 6, COLOR_DARK);
        int cast = Math.round((float) ARROW_W * menu.getCastProgress() / CrucibleBlockEntity.CAST_TICKS);
        graphics.fill(x + ARROW_X, y + ARROW_Y + 3, x + ARROW_X + cast, y + ARROW_Y + 6, COLOR_PROGRESS);
    }

    /** Vertical gauge filled from the bottom. */
    private static void gauge(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float fraction, int color) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, COLOR_DARK);
        graphics.fill(x, y, x + width, y + height, COLOR_GAUGE_BG);
        int filled = Math.round(height * Math.max(0f, Math.min(1f, fraction)));
        if (filled > 0) {
            graphics.fill(x, y + height - filled, x + width, y + height, color);
        }
    }
}
