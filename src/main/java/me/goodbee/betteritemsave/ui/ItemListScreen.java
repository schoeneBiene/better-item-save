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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemListScreen extends BaseOwoScreen<FlowLayout> {
    private final ItemSaver itemSaver;
    private final Map<Path, CheckboxComponent> selectedItems = new HashMap<>();
    private boolean multiselectEnabled = false;

    public ItemListScreen(ItemSaver itemSaver) {
        this.itemSaver = itemSaver;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    ButtonComponent giveButton;

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

        FlowLayout buttonRow = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        buttonRow.margins(Insets.bottom(5));

        giveButton = Components.button(Component.literal("Give"), button -> {
            for(Map.Entry<Path, CheckboxComponent> itr : selectedItems.entrySet()) {
                ItemStack item = itemSaver.readItem(itr.getKey());

                if (item == null) {
                    Minecraft.getInstance().player.displayClientMessage(Component.literal("An error occured while giving you the item, check the logs for more information."), false);
                    return;
                }

                LocalPlayer player = Minecraft.getInstance().player;

                boolean found = false;

                for (int i = 0; i <= 8; i++) {
                    ItemStack itemStack = player.getInventory().getItem(i);

                    if (itemStack.isEmpty()) {
                        Minecraft.getInstance().getConnection().send(new ServerboundSetCreativeModeSlotPacket(i + 36, item));
                        player.inventoryMenu.getSlot(i + 36).set(item);

                        found = true;
                        break;
                    }
                }

                if(!found) {
                    player.displayClientMessage(Component.literal("The item wasn't given, as your hotbar is full!"), false);
                }
            }
        });
        giveButton.margins(Insets.right(5));
        giveButton.active(false);
        buttonRow.child(giveButton);

        ButtonComponent refreshButton = Components.button(Component.literal("Refresh"), button -> {
            Minecraft.getInstance().setScreen(new ItemListScreen(itemSaver));
        });
        refreshButton.margins(Insets.right(5));
        buttonRow.child(refreshButton);

        CheckboxComponent multiselectCheckbox = Components.checkbox(Component.literal("Multiselect"));
        multiselectCheckbox.onChanged(b -> multiselectEnabled = b);
        multiselectCheckbox.margins(Insets.top(1));
        buttonRow.child(multiselectCheckbox);

        container.child(buttonRow);

        FlowLayout scrollChild = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        try {
            addFiles(itemSaver.basePath, scrollChild);

            ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(Sizing.content(), Sizing.fill(50), scrollChild);
            container.child(scrollContainer);
        } catch (IOException e) {
            rootComponent.child(Components.label(Component.literal("An error occured! Check the logs for more information.")));

            BetterItemSave.LOGGER.error("Could not get files!", e);
        }


        rootComponent.child(container);
    }

    protected void addFiles(Path currentPath, FlowLayout container) throws IOException {
        DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath);
        ArrayList<Path> sorted = new ArrayList<>();

        for(Path item : stream) {
            sorted.add(item);
        }

        sorted.sort((a, b) -> {
            boolean aDir = Files.isDirectory(a);
            boolean bDir = Files.isDirectory(b);

            if (aDir && !bDir) return -1;
            if (!aDir && bDir) return 1;
            return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
        });

        for(Path item : sorted) {
            String label = item.getFileName().toString();
            if(Files.isDirectory(item)) {
                CollapsibleContainer collapsibleContainer = Containers.collapsible(Sizing.content(), Sizing.content(), Component.literal(label), false);

                addFiles(item, collapsibleContainer);

                container.child(collapsibleContainer);
            } else {
                if(label.endsWith(".nbt")) {
                    label = label.substring(0, label.length() - 4);
                }

                CheckboxComponent checkbox = Components.checkbox(Component.literal(label));

                checkbox.onChanged(b -> {
                   if(!b) {
                       selectedItems.remove(item);

                       if(selectedItems.isEmpty()) {
                           giveButton.active(false);
                       }
                   } else {
                       if(!multiselectEnabled) {
                           Map<Path, CheckboxComponent> toRemove = new HashMap<>(selectedItems);

                           for(Map.Entry<Path, CheckboxComponent> itr : toRemove.entrySet()) {
                               selectedItems.remove(itr.getKey()).checked(false);
                           }
                       }

                       selectedItems.put(item, checkbox);

                       giveButton.active(true);
                   }
                });

                container.child(checkbox);
            }
        }
    }
}