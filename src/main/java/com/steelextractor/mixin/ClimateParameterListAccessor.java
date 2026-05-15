package com.steelextractor.mixin;

import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Climate.ParameterList.class)
public interface ClimateParameterListAccessor {
    @Accessor("index")
    Object steel_extractor$getIndex();
}
