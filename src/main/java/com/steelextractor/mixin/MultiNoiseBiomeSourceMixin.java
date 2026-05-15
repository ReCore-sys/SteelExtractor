package com.steelextractor.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin implements SteelExtractorBiomeCacheResetter {

    @Shadow
    @Final
    private Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> parameters;

    @Override
    public void steel_extractor$resetBiomeCache() {
        Climate.ParameterList<Holder<Biome>> parameterList = this.parameters.map(
            direct -> direct,
            preset -> preset.value().parameters()
        );
        Object index = ((ClimateParameterListAccessor) (Object) parameterList).steel_extractor$getIndex();
        ((SteelExtractorRTreeCacheResetter) index).steel_extractor$resetLastResult();
    }
}
