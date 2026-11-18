package com.eris_mods.godofforge.blockentity;

import java.util.Optional;

import com.eris_mods.godofforge.casting.MoldPattern;
import com.eris_mods.godofforge.material.ForgeMaterial;
import com.eris_mods.godofforge.menu.CrucibleMenu;
import com.eris_mods.godofforge.part.PartItem;
import com.eris_mods.godofforge.registry.ModBlockEntities;
import com.eris_mods.godofforge.registry.ModDataComponents;
import com.eris_mods.godofforge.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The crucible: melts simple metals and pours them into molds.
 * <p>
 * Heat comes from the block below:
 * <ul>
 *   <li>lit campfire: up to {@value #CAMPFIRE_MAX_TEMP} C, heats slowly, no fuel needed</li>
 *   <li>furnace: up to {@value #FURNACE_MAX_TEMP} C, heats fast, burns coal/charcoal from the furnace's fuel slot</li>
 * </ul>
 * Molten metal is measured in nuggets (9 = one ingot). One metal at a time.
 */
public class CrucibleBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_MOLD = 1;
    public static final int SLOT_OUTPUT = 2;

    public static final int AMBIENT_TEMP = 20;
    public static final int CAMPFIRE_MAX_TEMP = 1100;
    public static final int FURNACE_MAX_TEMP = 1300;
    private static final float CAMPFIRE_HEAT_PER_TICK = 1.5f;
    private static final float FURNACE_HEAT_PER_TICK = 5f;
    private static final float COOL_PER_TICK = 2f;
    /** Burn time of one coal/charcoal taken from the furnace (same as vanilla coal: 80 s). */
    private static final int COAL_BURN_TICKS = 1600;

    public static final int CAPACITY = 4 * ForgeMaterial.NUGGETS_PER_INGOT;
    public static final int MELT_TICKS = 40;
    public static final int CAST_TICKS = 60;

    private final SimpleContainer container = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            CrucibleBlockEntity.this.setChanged();
        }
    };

    private float temperature = AMBIENT_TEMP;
    private int heatSourceMax = AMBIENT_TEMP;
    private int furnaceBurnTicks;
    /** Material of the molten metal, or null when empty. */
    private Identifier metal;
    private int metalAmount;
    private int meltProgress;
    private int castProgress;

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE.get(), pos, state);
    }

    public SimpleContainer getContainer() {
        return container;
    }

    // ------------------------------------------------------------------ tick (server only)

    public void serverTick(Level level, BlockPos pos) {
        Registry<ForgeMaterial> materials = level.registryAccess().lookupOrThrow(ForgeMaterial.REGISTRY_KEY);
        updateHeat(level, pos);
        tickMelting(materials);
        tickCasting(level, materials);
        setChanged();
    }

    private void updateHeat(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        int target = AMBIENT_TEMP;
        float rate = COOL_PER_TICK;

        if (belowState.getBlock() instanceof CampfireBlock && belowState.getValue(BlockStateProperties.LIT)) {
            target = CAMPFIRE_MAX_TEMP;
            rate = CAMPFIRE_HEAT_PER_TICK;
        } else if (level.getBlockEntity(below) instanceof AbstractFurnaceBlockEntity furnace) {
            if (furnaceBurnTicks <= 0 && hasWork()) {
                // take one coal/charcoal from the furnace fuel slot (slot 1)
                ItemStack fuel = furnace.getItem(1);
                if (fuel.is(ItemTags.COALS)) {
                    furnace.removeItem(1, 1);
                    furnace.setChanged();
                    furnaceBurnTicks = COAL_BURN_TICKS;
                }
            }
            if (furnaceBurnTicks > 0) {
                furnaceBurnTicks--;
                target = FURNACE_MAX_TEMP;
                rate = FURNACE_HEAT_PER_TICK;
            }
        }
        heatSourceMax = target;

        if (temperature < target) {
            temperature = Math.min(target, temperature + rate);
        } else if (temperature > target) {
            temperature = Math.max(target, temperature - COOL_PER_TICK);
        }
    }

    /** Only burn fuel when there is something to melt or cast. */
    private boolean hasWork() {
        return !container.getItem(SLOT_INPUT).isEmpty() || metalAmount > 0;
    }

    private void tickMelting(Registry<ForgeMaterial> materials) {
        ItemStack input = container.getItem(SLOT_INPUT);
        Melt melt = input.isEmpty() ? null : findMelt(materials, input.getItem());
        boolean canMelt = melt != null
                && temperature >= melt.stats().meltingPoint()
                && (metal == null || metal.equals(melt.material()))
                && metalAmount + melt.amount() <= CAPACITY;
        if (!canMelt) {
            meltProgress = 0;
            return;
        }
        if (++meltProgress >= MELT_TICKS) {
            meltProgress = 0;
            input.shrink(1);
            container.setChanged();
            metal = melt.material();
            metalAmount += melt.amount();
        }
    }

    private void tickCasting(Level level, Registry<ForgeMaterial> materials) {
        MoldPattern pattern = moldPattern(level, container.getItem(SLOT_MOLD));
        ForgeMaterial.HeadStats stats = metal == null ? null
                : Optional.ofNullable(materials.getValue(metal)).flatMap(ForgeMaterial::head).orElse(null);
        Item partItem = pattern == null ? null : BuiltInRegistries.ITEM.getValue(pattern.part());
        boolean canCast = pattern != null && stats != null
                && partItem instanceof PartItem
                && pattern.material() == stats.mold()
                && metalAmount >= pattern.cost() * ForgeMaterial.NUGGETS_PER_INGOT
                && container.getItem(SLOT_OUTPUT).isEmpty();
        if (!canCast) {
            castProgress = 0;
            return;
        }
        if (++castProgress >= CAST_TICKS) {
            castProgress = 0;
            metalAmount -= pattern.cost() * ForgeMaterial.NUGGETS_PER_INGOT;
            container.setItem(SLOT_OUTPUT, PartItem.withMaterial(partItem, metal));
            // wooden molds burn up after a single use (decided 18/11)
            container.getItem(SLOT_MOLD).shrink(1);
            container.setChanged();
            if (metalAmount <= 0) {
                clearMetal();
            }
        }
    }

    private static MoldPattern moldPattern(Level level, ItemStack mold) {
        if (!mold.is(ModItems.WOODEN_MOLD.get())) {
            return null;
        }
        Identifier id = mold.get(ModDataComponents.MOLD_PATTERN.get());
        return id == null ? null : level.registryAccess().lookupOrThrow(MoldPattern.REGISTRY_KEY).getValue(id);
    }

    private record Melt(Identifier material, ForgeMaterial.HeadStats stats, int amount) {
    }

    private static Melt findMelt(Registry<ForgeMaterial> materials, Item item) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        for (Identifier id : materials.keySet()) {
            ForgeMaterial material = materials.getValue(id);
            if (material == null || material.head().isEmpty()) {
                continue;
            }
            int amount = material.head().get().meltAmount(itemId);
            if (amount > 0) {
                return new Melt(id, material.head().get(), amount);
            }
        }
        return null;
    }

    /** True if the item can be melted in a crucible (any material). Used by the input slot. */
    public static boolean isMeltable(Level level, ItemStack stack) {
        return findMelt(level.registryAccess().lookupOrThrow(ForgeMaterial.REGISTRY_KEY), stack.getItem()) != null;
    }

    /** "Empty" button: the molten metal is poured out and lost. */
    public void dumpMetal() {
        clearMetal();
        setChanged();
    }

    private void clearMetal() {
        metal = null;
        metalAmount = 0;
        castProgress = 0;
    }

    // ------------------------------------------------------------------ values shown in the GUI

    public int getTemperature() {
        return Math.round(temperature);
    }

    public int getHeatSourceMax() {
        return heatSourceMax;
    }

    public int getMetalAmount() {
        return metalAmount;
    }

    /** Network id of the molten metal in the material registry, or -1. */
    public int getMetalNetworkId(Level level) {
        if (metal == null) {
            return -1;
        }
        Registry<ForgeMaterial> materials = level.registryAccess().lookupOrThrow(ForgeMaterial.REGISTRY_KEY);
        ForgeMaterial material = materials.getValue(metal);
        return material == null ? -1 : materials.getId(material);
    }

    public int getMeltProgress() {
        return meltProgress;
    }

    public int getCastProgress() {
        return castProgress;
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.godofforge.crucible");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CrucibleMenu(containerId, inventory, this);
    }

    // ------------------------------------------------------------------ save / load / break

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            output.store("item" + slot, ItemStack.OPTIONAL_CODEC, container.getItem(slot));
        }
        output.putInt("temperature", Math.round(temperature));
        output.putInt("furnace_burn", furnaceBurnTicks);
        output.putInt("metal_amount", metalAmount);
        if (metal != null) {
            output.store("metal", Identifier.CODEC, metal);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            container.setItem(slot, input.read("item" + slot, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        }
        temperature = input.getIntOr("temperature", AMBIENT_TEMP);
        furnaceBurnTicks = input.getIntOr("furnace_burn", 0);
        metalAmount = input.getIntOr("metal_amount", 0);
        metal = input.read("metal", Identifier.CODEC).orElse(null);
        if (metal == null) {
            metalAmount = 0;
        }
    }

    /** Drop the items when the block is broken (molten metal is lost). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            Containers.dropContents(level, pos, container);
        }
    }
}
