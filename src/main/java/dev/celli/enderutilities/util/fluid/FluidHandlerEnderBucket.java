package dev.celli.enderutilities.util.fluid;

import dev.celli.enderutilities.item.ItemEnderBucket;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

public class FluidHandlerEnderBucket implements IFluidHandlerItem {
    private final ItemEnderBucket bucket;
    private final ItemStack container;

    public FluidHandlerEnderBucket(ItemEnderBucket bucket, ItemStack container) {
        this.bucket = bucket;
        this.container = container;
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return bucket.getFluid(container, null);
    }

    @Override
    public int getTankCapacity(int tank) {
        return bucket.getCapacity(container, null);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return bucket.fill(container, resource, action.execute(), null);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        FluidStack stored = bucket.getFluid(container, null);
        if (resource.isEmpty() || stored.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, stored)) {
            return FluidStack.EMPTY;
        }
        return bucket.drain(container, resource.getAmount(), action.execute(), null);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return bucket.drain(container, maxDrain, action.execute(), null);
    }
}
