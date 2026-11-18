package com.eris_mods.godofforge.menu;

import java.util.function.IntSupplier;

import com.eris_mods.godofforge.blockentity.CrucibleBlockEntity;
import com.eris_mods.godofforge.registry.ModBlocks;
import com.eris_mods.godofforge.registry.ModItems;
import com.eris_mods.godofforge.registry.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Crucible GUI. Slots: metal input, mold, output (take only), then the player inventory.
 * Temperature, molten metal and progress are mirrored to the client through {@link DataSlot}s.
 */
public class CrucibleMenu extends AbstractContainerMenu implements ActionMenu {
    public static final int ACTION_DUMP = 0;

    public static final int INPUT_X = 44;
    public static final int INPUT_Y = 20;
    public static final int MOLD_X = 44;
    public static final int MOLD_Y = 52;
    public static final int OUTPUT_X = 134;
    public static final int OUTPUT_Y = 36;
    public static final int INVENTORY_Y = 98;

    private static final int INVENTORY_START = 3;
    private static final int HOTBAR_END = INVENTORY_START + 36;

    private final Container container;
    private final ContainerLevelAccess access;
    /** Null on the client. */
    private final CrucibleBlockEntity crucible;
    private final Player player;

    private final DataSlot temperature;
    private final DataSlot heatSourceMax;
    private final DataSlot metalAmount;
    private final DataSlot metalId;
    private final DataSlot meltProgress;
    private final DataSlot castProgress;

    /** Client constructor. */
    public CrucibleMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory, new SimpleContainer(3), ContainerLevelAccess.NULL, null);
        extraData.readBlockPos();
    }

    /** Server constructor. */
    public CrucibleMenu(int containerId, Inventory inventory, CrucibleBlockEntity crucible) {
        this(containerId, inventory, crucible.getContainer(),
                ContainerLevelAccess.create(crucible.getLevel(), crucible.getBlockPos()), crucible);
    }

    private CrucibleMenu(int containerId, Inventory inventory, Container container, ContainerLevelAccess access, CrucibleBlockEntity crucible) {
        super(ModMenus.CRUCIBLE.get(), containerId);
        this.container = container;
        this.access = access;
        this.crucible = crucible;
        this.player = inventory.player;

        addSlot(new Slot(container, CrucibleBlockEntity.SLOT_INPUT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CrucibleBlockEntity.isMeltable(player.level(), stack);
            }
        });
        addSlot(new Slot(container, CrucibleBlockEntity.SLOT_MOLD, MOLD_X, MOLD_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.WOODEN_MOLD.get());
            }
        });
        addSlot(new Slot(container, CrucibleBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y) {
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

        temperature = addDataSlot(slot(crucible == null ? null : crucible::getTemperature));
        heatSourceMax = addDataSlot(slot(crucible == null ? null : crucible::getHeatSourceMax));
        metalAmount = addDataSlot(slot(crucible == null ? null : crucible::getMetalAmount));
        metalId = addDataSlot(slot(crucible == null ? null : () -> crucible.getMetalNetworkId(crucible.getLevel())));
        meltProgress = addDataSlot(slot(crucible == null ? null : crucible::getMeltProgress));
        castProgress = addDataSlot(slot(crucible == null ? null : crucible::getCastProgress));
    }

    /** Server: a slot that reads the block entity. Client: a plain slot that receives synced values. */
    private static DataSlot slot(IntSupplier source) {
        if (source == null) {
            return DataSlot.standalone();
        }
        return new DataSlot() {
            @Override
            public int get() {
                return source.getAsInt();
            }

            @Override
            public void set(int value) {
                // read-only on the server
            }
        };
    }

    // ---- values for the screen ----
    public int getTemperature() { return temperature.get(); }
    public int getHeatSourceMax() { return heatSourceMax.get(); }
    public int getMetalAmount() { return metalAmount.get(); }
    public int getMetalId() { return metalId.get(); }
    public int getMeltProgress() { return meltProgress.get(); }
    public int getCastProgress() { return castProgress.get(); }

    @Override
    public void handleAction(ServerPlayer serverPlayer, int action) {
        if (action == ACTION_DUMP && crucible != null && crucible.getMetalAmount() > 0) {
            crucible.dumpMetal();
            serverPlayer.sendOverlayMessage(Component.translatable("message.godofforge.crucible.dumped"));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < INVENTORY_START) {
            if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItems.WOODEN_MOLD.get())) {
            if (!moveItemStackTo(stack, CrucibleBlockEntity.SLOT_MOLD, CrucibleBlockEntity.SLOT_MOLD + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (CrucibleBlockEntity.isMeltable(player.level(), stack)) {
            if (!moveItemStackTo(stack, CrucibleBlockEntity.SLOT_INPUT, CrucibleBlockEntity.SLOT_INPUT + 1, false)) {
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
        return stillValid(access, player, ModBlocks.CRUCIBLE.get());
    }
}
