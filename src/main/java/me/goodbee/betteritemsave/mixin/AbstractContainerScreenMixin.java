package me.goodbee.betteritemsave.mixin;

import me.goodbee.betteritemsave.BetterItemSave;
import me.goodbee.betteritemsave.ui.ItemListScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin extends Screen {
    protected AbstractContainerScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At(value = "HEAD"))
    protected void init(CallbackInfo ci) {
        if(minecraft.player.getAbilities().instabuild) {
            this.addRenderableWidget(Button.builder(Component.literal("Better Item Saver"), button -> {
                this.minecraft.setScreen(new ItemListScreen(BetterItemSave.itemSaver));
            }).bounds(10, 10, 120, 20).build());
        }
    }
}