package com.steelextractor.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.world.level.biome.Climate$RTree")
public abstract class ClimateRTreeMixin implements SteelExtractorRTreeCacheResetter {

    @Shadow
    @Final
    private ThreadLocal<?> lastResult;

    @Override
    public void steel_extractor$resetLastResult() {
        this.lastResult.remove();
    }
}
