package com.eris_mods.godofforge.menu;

import com.eris_mods.godofforge.casting.MoldMaterial;
import com.eris_mods.godofforge.casting.MoldPattern;
import com.eris_mods.godofforge.item.MoldItem;
import com.eris_mods.godofforge.knowledge.KnowledgeHelper;
import com.eris_mods.godofforge.knowledge.SmithingKnowledge;
import com.eris_mods.godofforge.registry.ModBlocks;
import com.eris_mods.godofforge.registry.ModDataComponents;
import com.eris_mods.godofforge.registry.ModItems;
import com.eris_mods.godofforge.registry.ModMenus;

import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side logic of the Mold Table GUI.
 * <p>
 * Research mode (step 1.3): the player carves cells of a 5x5 grid and presses Confirm.
 * The server compares the carving with every {@link MoldPattern}:
 * a match produces the mold and teaches the pattern; no match destroys the material.
 * <p>
 * The grid lives on the server and is mirrored to the client through two {@link DataSlot}s,
 * so a modified client cannot fake a carving.
 */
public class MoldTableMenu extends AbstractContainerMenu {
    public static final int GRID_CELLS = MoldPattern.SIZE * MoldPattern.SIZE;

    /** Button/action ids sent by the client (see {@code MoldTableActionPayload}). 0..24 = carve that cell. */
    public static final int ACTION_CONFIRM = 100;
    public static final int ACTION_RESET = 101;

    public static final int MATERIAL_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final int INVENTORY_START = 2;
    private static final int HOTBAR_END = INVENTORY_START + 36;

    // Slot positions inside the GUI texture (shared with the screen).
    public static final int MATERIAL_SLOT_X = 20;
    public static final int MATERIAL_SLOT_Y = 43;
    public static final int OUTPUT_SLOT_X = 140;
    public static final int OUTPUT_SLOT_Y = 43;
    public static final int INVENTORY_Y = 116;

    private final ContainerLevelAccess access;
    private final SimpleContainer container = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            MoldTableMenu.this.slotsChanged(this);
        }
    };
    private final boolean[] carved = new boolean[GRID_CELLS];
    // 25 cells split in two 13/12-bit halves: data slots are synced as shorts.
    private final DataSlot gridLow = DataSlot.standalone();
    private final DataSlot gridHigh = DataSlot.standalone();

    /** Client constructor, called when the server opens the GUI. */
    public MoldTableMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
        extraData.readBlockPos(); // position of the table (sent by openMenu(provider, pos)); not needed yet
    }

    /** Server constructor. */
    public MoldTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.MOLD_TABLE.get(), containerId);
        this.access = access;

        addSlot(new Slot(container, MATERIAL_SLOT, MATERIAL_SLOT_X, MATERIAL_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return materialOf(stack) != null;
            }
        });
        addSlot(new Slot(container, OUTPUT_SLOT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, INVENTORY_Y + 58));
        }

        addDataSlot(gridLow);
        addDataSlot(gridHigh);
    }

    // ---------------------------------------------------------------- grid

    /** Works on both sides: the client reads the synced data slots. */
    public boolean isCarved(int cell) {
        int bits = cell < 13 ? gridLow.get() : gridHigh.get();
        int bit = cell < 13 ? cell : cell - 13;
        return (bits >> bit & 1) == 1;
    }

    public boolean hasMaterial() {
        return !container.getItem(MATERIAL_SLOT).isEmpty();
    }

    private void syncGrid() {
        int low = 0;
        int high = 0;
        for (int cell = 0; cell < GRID_CELLS; cell++) {
            if (carved[cell]) {
                if (cell < 13) {
                    low |= 1 << cell;
                } else {
                    high |= 1 << (cell - 13);
                }
            }
        }
        gridLow.set(low);
        gridHigh.set(high);
        broadcastChanges();
    }

    private void clearGrid() {
        java.util.Arrays.fill(carved, false);
        syncGrid();
    }

    // ---------------------------------------------------------------- actions (server)

    /** Called on the server when the client presses a grid cell or a button. */
    public void handleAction(ServerPlayer player, int action) {
        if (action >= 0 && action < GRID_CELLS) {
            if (!hasMaterial()) {
                player.sendOverlayMessage(Component.translatable("message.godofforge.mold_table.no_material"));
                return;
            }
            carved[action] = true; // carving cannot be undone (use Reset)
            syncGrid();
        } else if (action == ACTION_RESET) {
            clearGrid();
        } else if (action == ACTION_CONFIRM) {
            confirm(player);
        }
    }

    private void confirm(ServerPlayer player) {
        ItemStack materialStack = container.getItem(MATERIAL_SLOT);
        MoldMaterial material = materialOf(materialStack);
        if (material == null) {
            player.sendOverlayMessage(Component.translatable("message.godofforge.mold_table.no_material"));
            return;
        }
        if (!container.getItem(OUTPUT_SLOT).isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.godofforge.mold_table.output_full"));
            return;
        }
        boolean anyCarved = false;
        for (boolean cell : carved) {
            anyCarved |= cell;
        }
        if (!anyCarved) {
            player.sendOverlayMessage(Component.translatable("message.godofforge.mold_table.nothing_carved"));
            return;
        }

        Identifier matched = findPattern(player, material);
        // The material is used up whether or not the carving was right.
        container.removeItem(MATERIAL_SLOT, 1);

        if (matched == null) {
            player.sendOverlayMessage(Component.translatable("message.godofforge.mold_table.failed"));
        } else {
            container.setItem(OUTPUT_SLOT, createMold(matched));

            SmithingKnowledge before = KnowledgeHelper.get(player);
            if (!before.knows(matched)) {
                KnowledgeHelper.update(player, k -> k.learn(matched));
                player.sendOverlayMessage(Component.translatable("message.godofforge.mold_table.learned", MoldItem.patternName(matched)));
            } else {
                player.sendOverlayMessage(Component.translatable("message.godofforge.mold_table.success", MoldItem.patternName(matched)));
            }
        }
        clearGrid();
    }

    /**
     * Normal mode (step 1.4): make a mold the player already knows, no carving needed.
     * Everything is re-checked here because the request comes from the client.
     */
    public void carveKnown(ServerPlayer player, Identifier patternId) {
        Registry<MoldPattern> patterns = player.level().registryAccess().lookupOrThrow(MoldPattern.REGISTRY_KEY);
        MoldPattern pattern = patterns.getValue(patternId);
        if (pattern == null || !KnowledgeHelper.get(player).knows(patternId)) {
            return; // unknown or not learned: ignore (a normal client never sends this)
        }
        MoldMaterial material = materialOf(container.getItem(MATERIAL_SLOT));
        if (material == null) {
            player.sendOverlayMessage(Component.translatable("message.godofforge.mold_table.no_material"));
            return;
        }
        if (material != pattern.material()) {
            player.sendOverlayMessage(Component.translatable("message.godofforge.mold_table.wrong_material"));
            return;
        }
        if (!container.getItem(OUTPUT_SLOT).isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.godofforge.mold_table.output_full"));
            return;
        }
        container.removeItem(MATERIAL_SLOT, 1);
        container.setItem(OUTPUT_SLOT, createMold(patternId));
        player.sendOverlayMessage(Component.translatable("message.godofforge.mold_table.success", MoldItem.patternName(patternId)));
    }

    private static ItemStack createMold(Identifier patternId) {
        ItemStack mold = new ItemStack(ModItems.WOODEN_MOLD.get());
        mold.set(ModDataComponents.MOLD_PATTERN.get(), patternId);
        return mold;
    }

    private Identifier findPattern(ServerPlayer player, MoldMaterial material) {
        Registry<MoldPattern> patterns = player.level().registryAccess().lookupOrThrow(MoldPattern.REGISTRY_KEY);
        for (Identifier id : patterns.keySet()) {
            MoldPattern pattern = patterns.getValue(id);
            // TODO (Phase 2): respect the pattern tier vs. the player's mastery and the mastery-skip config.
            if (pattern != null && pattern.material() == material && pattern.matches(carved)) {
                return id;
            }
        }
        return null;
    }

    /** Which mold material an item counts as, or null if it can't be carved. Clay comes in Phase 2. */
    public static MoldMaterial materialOf(ItemStack stack) {
        if (stack.is(ItemTags.PLANKS)) {
            return MoldMaterial.WOOD;
        }
        return null;
    }

    // ---------------------------------------------------------------- container plumbing

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == OUTPUT_SLOT || index == MATERIAL_SLOT) {
            // table -> player inventory
            if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (materialOf(stack) != null) {
            // player inventory -> material slot
            if (!moveItemStackTo(stack, MATERIAL_SLOT, MATERIAL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.MOLD_TABLE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Like the crafting table: give the items back when the GUI closes.
        access.execute((level, pos) -> clearContainer(player, container));
    }
}
