package me.goodbee.betteritemsave.ui;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.ExcludeFromScreen;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.RangeConstraint;
import me.goodbee.betteritemsave.BetterItemSave;
import org.apache.logging.log4j.core.config.plugins.validation.Constraint;

@Modmenu(modId = BetterItemSave.MOD_ID)
@Config(name = "better-item-save", wrapperName = "Config")
public class ConfigModel {
    @RangeConstraint(min = 1, max = 100)
    public int horizontalMenuSize = 50;
    @RangeConstraint(min = 1, max = 100)
    public int verticalMenuSize = 50;
    public ShowItemPreviewOptions showItemPreview = ShowItemPreviewOptions.ENABLED;

    public enum ShowItemPreviewOptions {
        ENABLED,
        DISABLED
    }
}