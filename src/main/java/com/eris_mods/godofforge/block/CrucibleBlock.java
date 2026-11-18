package com.eris_mods.godofforge.block;

import com.eris_mods.godofforge.blockentity.CrucibleBlockEntity;
import com.eris_mods.godofforge.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Crucible block: place it on a lit campfire or a furnace. All logic lives in {@link CrucibleBlockEntity}. */
public class CrucibleBlock extends Block implements EntityBlock {
    public CrucibleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.CRUCIBLE.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> ((CrucibleBlockEntity) blockEntity).serverTick(tickLevel, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
            player.openMenu(crucible, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
