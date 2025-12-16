package me.goodbee.betteritemsave.ui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import me.goodbee.betteritemsave.BetterItemSave;
import me.goodbee.betteritemsave.ItemSaver;
import me.goodbee.betteritemsave.files.ItemFile;
import me.goodbee.betteritemsave.files.ItemFileList;
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
import java.util.*;

public class ItemListScreen extends BaseOwoScreen<FlowLayout> {
    private final ItemSaver itemSaver;
    private final ItemFileList itemFileList;
    private final Map<Path, CheckboxComponent> selectedItems = new HashMap<>();
    private boolean multiselectEnabled = false;

    public ItemListScreen(ItemSaver itemSaver, ItemFileList itemFileList) {
        this.itemSaver = itemSaver;
        this.itemFileList = itemFileList;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    ButtonComponent refreshButton;
    protected void setIsRefreshing(boolean newValue) {
        if(refreshButton == null) return;

        if(newValue) {
            refreshButton.active(false);
            refreshButton.setMessage(Component.translatable("text.menu.better-item-save.refreshButtonRefreshing"));
        } else {
            refreshButton.active(true);
            refreshButton.setMessage(Component.translatable("text.menu.better-item-save.refreshButton"));
        }
    }

    ButtonComponent giveButton;
    FlowLayout scrollChild;

    protected void updateItems() {
        setIsRefreshing(true);
        new Thread(() -> {
            try {
                itemFileList.recheckFolder();

                Minecraft.getInstance().execute(() -> {
                    scrollChild.clearChildren();
                    try {
                        addFiles(BetterItemSave.BASE_PATH, scrollChild);
                        setIsRefreshing(false);
                    } catch (IOException e) {
                        scrollChild.child(Components.label(Component.translatable("text.menu.better-item-save.error")));

                        BetterItemSave.LOGGER.error("Could not get files!", e);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout container = Containers.verticalFlow(Sizing.fill(BetterItemSave.CONFIG.horizontalMenuSize()), Sizing.content());

        container
                .padding(Insets.of(10))
                .surface(Surface.DARK_PANEL)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        FlowLayout buttonRow = Containers.horizontalFlow(Sizing.fill(), Sizing.content());
        buttonRow.margins(Insets.bottom(5));

        giveButton = Components.button(Component.translatable("text.menu.better-item-save.giveButton"), button -> {
            for(Map.Entry<Path, CheckboxComponent> itr : selectedItems.entrySet()) {
                ItemFile itemFile = itemFileList.getItemFileMap().get(itr.getKey());

                if (itemFile == null || itemFile.itemStack == null) {
                    Minecraft.getInstance().player.displayClientMessage(Component.translatable("text.menu.better-item-save.itemGiveError"), false);
                    return;
                }

                ItemStack item = itemFile.itemStack;

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
                    player.displayClientMessage(Component.translatable("text.menu.better-item-save.hotbarFullError"), false);
                }
            }
        });
        giveButton.margins(Insets.right(5));
        giveButton.active(false);
        buttonRow.child(giveButton);

        refreshButton = Components.button(Component.translatable("text.menu.better-item-save.refreshButton"), button -> updateItems());
        refreshButton.margins(Insets.right(5));
        buttonRow.child(refreshButton);

        CheckboxComponent multiselectCheckbox = Components.checkbox(Component.translatable("text.menu.better-item-save.multiselectLabel"));
        multiselectCheckbox.onChanged(b -> multiselectEnabled = b);
        multiselectCheckbox.margins(Insets.top(1));
        buttonRow.child(multiselectCheckbox);

        container.child(buttonRow);

        scrollChild = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        try {
            addFiles(BetterItemSave.BASE_PATH, scrollChild);
        } catch (IOException e) {
            rootComponent.child(Components.label(Component.translatable("text.menu.better-item-save.error")));

            BetterItemSave.LOGGER.error("Could not get files!", e);
        }

        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(Sizing.content(), Sizing.fill(BetterItemSave.CONFIG.verticalMenuSize()), scrollChild);
        container.child(scrollContainer);

        rootComponent.child(container);

        updateItems();
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
                checkbox.margins(Insets.right(5));

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

                if(selectedItems.containsKey(item)) {
                    checkbox.checked(true);
                }

                FlowLayout horizontalFlow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
                horizontalFlow.child(checkbox);

                if(BetterItemSave.CONFIG.showItemPreview() == ConfigModel.ShowItemPreviewOptions.ENABLED) {
                    if(itemFileList.getItemFileMap().get(item) == null) {
                        continue;
                    }

                    ItemStack itemStack = itemFileList.getItemFileMap().get(item).itemStack;

                    ItemComponent itemComponent = Components.item(itemStack);
                    itemComponent.setTooltipFromStack(true);
                    itemComponent.showOverlay(true);
                    horizontalFlow.child(itemComponent);
                }

                container.child(horizontalFlow);
            }
        }
    }
}