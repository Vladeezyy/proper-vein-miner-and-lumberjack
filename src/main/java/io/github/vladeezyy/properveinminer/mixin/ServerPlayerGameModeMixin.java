package io.github.vladeezyy.properveinminer.mixin;

import io.github.vladeezyy.properveinminer.VeinMiner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The one hook: a player broke a block. The state is taken before the break; once the break went through (events,
 * protection, tool) the rest of the vein follows.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow
    protected ServerLevel level;

    @Shadow
    @Final
    protected ServerPlayer player;

    @Unique
    private BlockState properveinminer$state;

    @Unique
    private VeinMiner.Kind properveinminer$kind;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void properveinminer$before(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (VeinMiner.breaking()) {
            return;
        }
        this.properveinminer$state = this.level.getBlockState(pos);
        this.properveinminer$kind = VeinMiner.kind(this.player, this.properveinminer$state);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void properveinminer$after(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        VeinMiner.Kind kind = this.properveinminer$kind;
        BlockState state = this.properveinminer$state;
        if (VeinMiner.breaking()) {
            return;
        }
        this.properveinminer$kind = null;
        this.properveinminer$state = null;
        if (kind != null && cir.getReturnValueZ()) {
            VeinMiner.breakVein(this.player, this.level, pos, state, kind);
        }
    }
}
