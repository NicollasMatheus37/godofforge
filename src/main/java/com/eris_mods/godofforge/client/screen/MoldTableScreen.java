package com.eris_mods.godofforge.client.screen;

import java.util.List;

import com.eris_mods.godofforge.casting.MoldPattern;
import com.eris_mods.godofforge.item.MoldItem;
import com.eris_mods.godofforge.knowledge.KnowledgeHelper;
import com.eris_mods.godofforge.menu.MoldTableMenu;
import com.eris_mods.godofforge.network.MenuActionPayload;
import com.eris_mods.godofforge.network.MoldTableCarvePayload;

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
 * Client-side GUI of the Mold Table.
 * <ul>
 *   <li><b>Normal mode</b> (step 1.4): a known mold is selected in the side list; the grid previews its shape
 *       and Confirm makes it directly.</li>
 *   <li><b>Research mode</b> (step 1.3): nothing selected; the player carves the 5x5 grid and Confirm checks it.</li>
 * </ul>
 * Grid cells are small buttons: a solid cell is a button, a carved cell is hidden so the dark "hole" shows.
 * The server owns the real research grid and validates everything; this screen only displays and sends clicks.
 * <p>
 * Placeholder visuals (flat colors). A proper GUI texture is in the visual backlog.
 */
public class MoldTableScreen extends AbstractContainerScreen<MoldTableMenu> {
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 198;
    private static final int CELL = 13;
    private static final int GRID_SIZE = CELL * MoldPattern.SIZE; // 65 px
    private static final int GRID_X = 56;
    private static final int GRID_Y = 18;
    private static final int BUTTONS_Y = 87;

    // Side panel with the known molds (left of the main GUI)
    private static final int LIST_WIDTH = 84;
    private static final int LIST_X = -LIST_WIDTH - 2;
    private static final int LIST_ENTRY_HEIGHT = 14;
    private static final int LIST_MAX_ENTRIES = 10; // scrolling comes later (visual backlog)

    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_PANEL_DARK = 0xFF555555;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_HOLE = 0xFF2B1D12;
    private static final int COLOR_TEXT = 0xFF404040;

    private final Button[] cells = new Button[MoldTableMenu.GRID_CELLS];
    private final Button[] listButtons = new Button[LIST_MAX_ENTRIES];
    private Button confirmButton;
    private Button resetButton;

    /** Known patterns shown in the list, refreshed every frame from the synced player knowledge. */
    private List<Identifier> knownPatterns = List.of();
    /** Selected known pattern (normal mode), or null for research mode. */
    private Identifier selected;

    public MoldTableScreen(MoldTableMenu menu, Inventory inventory, Component title) {
        // In 26.x the GUI size is final and must be passed to the constructor.
        super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.inventoryLabelY = IMAGE_HEIGHT - 94;
    }

    @Override
    protected void init() {
        super.init();
        for (int cell = 0; cell < cells.length; cell++) {
            final int action = cell;
            int column = cell % MoldPattern.SIZE;
            int row = cell / MoldPattern.SIZE;
            cells[cell] = addRenderableWidget(Button.builder(Component.empty(), button -> sendAction(action))
                    .size(CELL, CELL)
                    .pos(leftPos + GRID_X + column * CELL, topPos + GRID_Y + row * CELL)
                    .build());
        }
        for (int i = 0; i < listButtons.length; i++) {
            final int index = i;
            listButtons[i] = addRenderableWidget(Button.builder(Component.empty(), button -> select(index))
                    .size(LIST_WIDTH - 6, LIST_ENTRY_HEIGHT - 1)
                    .pos(leftPos + LIST_X + 3, topPos + 16 + i * LIST_ENTRY_HEIGHT)
                    .build());
        }
        confirmButton = addRenderableWidget(Button.builder(Component.translatable("gui.godofforge.mold_table.confirm"),
                        button -> confirm())
                .size(54, 12)
                .pos(leftPos + 33, topPos + BUTTONS_Y)
                .build());
        resetButton = addRenderableWidget(Button.builder(Component.translatable("gui.godofforge.mold_table.reset"),
                        button -> reset())
                .size(54, 12)
                .pos(leftPos + 89, topPos + BUTTONS_Y)
                .build());
    }

    // ---------------------------------------------------------------- actions

    private void sendAction(int action) {
        ClientPacketDistributor.sendToServer(new MenuActionPayload(menu.containerId, action));
    }

    private void select(int index) {
        if (index < knownPatterns.size()) {
            selected = knownPatterns.get(index);
            sendAction(MoldTableMenu.ACTION_RESET); // discard any half-carved research grid
        }
    }

    private void confirm() {
        if (selected != null) {
            ClientPacketDistributor.sendToServer(new MoldTableCarvePayload(menu.containerId, selected));
        } else {
            sendAction(MoldTableMenu.ACTION_CONFIRM);
        }
    }

    /** Research mode: clears the grid. Normal mode: deselects and goes back to research mode. */
    private void reset() {
        if (selected != null) {
            selected = null;
        } else {
            sendAction(MoldTableMenu.ACTION_RESET);
        }
    }

    // ---------------------------------------------------------------- state -> widgets

    private Registry<MoldPattern> patternRegistry() {
        return minecraft.level.registryAccess().lookupOrThrow(MoldPattern.REGISTRY_KEY);
    }

    /** Keeps the widgets in sync with the server grid and the player's knowledge. */
    private void updateWidgets() {
        Registry<MoldPattern> registry = patternRegistry();
        knownPatterns = KnowledgeHelper.get(minecraft.player).knownPatterns().stream()
                .filter(id -> registry.getValue(id) != null)
                .sorted()
                .toList();
        if (selected != null && !knownPatterns.contains(selected)) {
            selected = null;
        }

        for (int i = 0; i < listButtons.length; i++) {
            Button button = listButtons[i];
            button.visible = i < knownPatterns.size();
            if (button.visible) {
                Identifier id = knownPatterns.get(i);
                button.setMessage(MoldItem.patternName(id));
                button.active = !id.equals(selected); // the selected entry is shown "pressed"
            }
        }

        boolean hasMaterial = menu.hasMaterial();
        MoldPattern preview = selected == null ? null : registry.getValue(selected);
        boolean anyCarved = false;
        for (int cell = 0; cell < cells.length; cell++) {
            boolean carved = preview != null
                    ? preview.isCarved(cell % MoldPattern.SIZE, cell / MoldPattern.SIZE)
                    : menu.isCarved(cell);
            anyCarved |= carved;
            cells[cell].visible = !carved;
            cells[cell].active = hasMaterial && preview == null; // the preview is read-only
        }
        confirmButton.active = hasMaterial && anyCarved;
        resetButton.active = preview != null || anyCarved;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        updateWidgets();

        int x = leftPos;
        int y = topPos;
        // main panel
        graphics.fill(x, y, x + imageWidth, y + imageHeight, COLOR_PANEL_DARK);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, COLOR_PANEL);

        // side panel: known molds
        int listHeight = 18 + Math.max(1, Math.min(knownPatterns.size(), LIST_MAX_ENTRIES)) * LIST_ENTRY_HEIGHT;
        graphics.fill(x + LIST_X, y, x + LIST_X + LIST_WIDTH, y + listHeight, COLOR_PANEL_DARK);
        graphics.fill(x + LIST_X + 1, y + 1, x + LIST_X + LIST_WIDTH - 1, y + listHeight - 1, COLOR_PANEL);
        graphics.text(font, Component.translatable("gui.godofforge.mold_table.known").getString(), x + LIST_X + 4, y + 5, COLOR_TEXT);
        if (knownPatterns.isEmpty()) {
            graphics.text(font, Component.translatable("gui.godofforge.mold_table.none_known").getString(), x + LIST_X + 4, y + 19, COLOR_TEXT);
        }

        // slot backgrounds
        for (Slot slot : menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, COLOR_SLOT);
        }

        // the carving area: carved (hidden) cells reveal this dark color
        graphics.fill(x + GRID_X - 1, y + GRID_Y - 1, x + GRID_X + GRID_SIZE + 1, y + GRID_Y + GRID_SIZE + 1, COLOR_PANEL_DARK);
        graphics.fill(x + GRID_X, y + GRID_Y, x + GRID_X + GRID_SIZE, y + GRID_Y + GRID_SIZE, COLOR_HOLE);

        // mode label above the material slot
        String mode = selected == null
                ? Component.translatable("gui.godofforge.mold_table.mode_research").getString()
                : Component.translatable("gui.godofforge.mold_table.mode_normal").getString();
        graphics.text(font, mode, x + 8, y + MoldTableMenu.MATERIAL_SLOT_Y - 12, COLOR_TEXT);
    }
}
