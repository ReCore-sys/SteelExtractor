package com.steelextractor.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashSet;
import java.util.LinkedHashSet;

@Mixin(TreeFeature.class)
public abstract class TreeFeatureMixin {
    @Redirect(
        method = "place",
        at = @At(
            value = "INVOKE",
            target = "Lcom/google/common/collect/Sets;newHashSet()Ljava/util/HashSet;",
            ordinal = 0
        )
    )
    private HashSet<BlockPos> steelExtractor$useInsertionOrderedRootSet() {
        return new LinkedHashSet<>();
    }

    @Redirect(
        method = "place",
        at = @At(
            value = "INVOKE",
            target = "Lcom/google/common/collect/Sets;newHashSet()Ljava/util/HashSet;",
            ordinal = 1
        )
    )
    private HashSet<BlockPos> steelExtractor$useInsertionOrderedTrunkSet() {
        return new LinkedHashSet<>();
    }

    @Redirect(
        method = "place",
        at = @At(
            value = "INVOKE",
            target = "Lcom/google/common/collect/Sets;newHashSet()Ljava/util/HashSet;",
            ordinal = 2
        )
    )
    private HashSet<BlockPos> steelExtractor$useInsertionOrderedFoliageSet() {
        return new LinkedHashSet<>();
    }

    @Redirect(
        method = "place",
        at = @At(
            value = "INVOKE",
            target = "Lcom/google/common/collect/Sets;newHashSet()Ljava/util/HashSet;",
            ordinal = 3
        )
    )
    private HashSet<BlockPos> steelExtractor$useInsertionOrderedDecorationSet() {
        return new LinkedHashSet<>();
    }

    @Redirect(method = "updateLeaves", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newHashSet()Ljava/util/HashSet;"))
    private static HashSet<BlockPos> steelExtractor$useInsertionOrderedLeafFrontierSet() {
        return new LinkedHashSet<>();
    }

}
