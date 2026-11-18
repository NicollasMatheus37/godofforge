package com.eris_mods.godofforge.block;

import com.eris_mods.godofforge.menu.MoldTableMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Mold Table: where wooden (and later clay) molds are carved.
 * Like the vanilla crafting table it has no inventory of its own; items are returned when the GUI closes.
 */
public class MoldTableBlock extends Block {
    private static final Component TITLE = Component.translatable("container.godofforge.mold_table");

    public MoldTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> new MoldTableMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
                    TITLE), pos);
        }
        return InteractionResult.SUCCESS;
    }
}
