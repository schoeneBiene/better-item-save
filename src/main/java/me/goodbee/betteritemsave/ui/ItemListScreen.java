package me.goodbee.betteritemsave.ui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import me.goodbee.betteritemsave.BetterItemSave;
import me.goodbee.betteritemsave.ItemSaver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ItemListScreen extends BaseOwoScreen<FlowLayout> {
    private final ItemSaver itemSaver;
    private final Map<Path, CheckboxComponent> files = new HashMap<>();

    public ItemListScreen(ItemSaver itemSaver) {
        this.itemSaver = itemSaver;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout container = Containers.verticalFlow(Sizing.fill(50), Sizing.content());

        container
                .padding(Insets.of(10))
                .surface(Surface.DARK_PANEL)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        ButtonComponent buttonComponent = Components.button(Component.literal("Give"), button -> {
            for(Map.Entry<Path, CheckboxComponent> itr : files.entrySet()) {
                BetterItemSave.LOGGER.debug(itr.getKey().toString());
                BetterItemSave.LOGGER.debug(itr.getValue().toString());
                if(itr.getValue().selected()) {
                    ItemStack item = itemSaver.readItem(itr.getKey());

                    if(item == null) {
                        Minecraft.getInstance().player.displayClientMessage(Component.literal("An error occured while giving you an item, check the logs for more information."), false);
                        continue;
                    }

                    LocalPlayer player = Minecraft.getInstance().player;

                    boolean found = false;

                    for(int i = 0; i <= 8; i++) {
                        ItemStack itemStack = player.getInventory().getItem(i);

                        if(itemStack.isEmpty()) {
                            Minecraft.getInstance().getConnection().send(new ServerboundSetCreativeModeSlotPacket(i + 36, item));
                            player.inventoryMenu.getSlot(i + 36).set(item);

                            found = true;
                            break;
                        }
                    }

                    if(!found) {
                        player.displayClientMessage(Component.literal("An item wasn't given, as your hotbar is full!"), false);
                    }
                }
            }
        });

        buttonComponent.margins(Insets.bottom(10));

        container.child(buttonComponent);

        FlowLayout scrollChild = Containers.verticalFlow(Sizing.content(), Sizing.content());
        try {
            addFiles(itemSaver.basePath, scrollChild);

            ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(Sizing.content(), Sizing.fill(25), scrollChild);
            container.child(scrollContainer);
        } catch (IOException e) {
            rootComponent.child(Components.label(Component.literal("An error occured! Check the logs for more information.")));

            BetterItemSave.LOGGER.error("Could not get files!", e);
        }


        rootComponent.child(container);
    }

    protected void addFiles(Path currentPath, FlowLayout container) throws IOException {
        DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath);

        for(Path item : stream) {
            if(Files.isDirectory(item)) {
                CollapsibleContainer collapsibleContainer = Containers.collapsible(Sizing.content(), Sizing.content(), Component.literal(item.getFileName().toString()), false);

                addFiles(item, collapsibleContainer);

                container.child(collapsibleContainer);
            } else {
                CheckboxComponent checkbox = Components.checkbox(Component.literal(item.getFileName().toString()));
                container.child(checkbox);
                files.put(item, checkbox);
            }
        }
    }
}