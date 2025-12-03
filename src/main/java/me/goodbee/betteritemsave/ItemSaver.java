package me.goodbee.betteritemsave;

import com.mojang.serialization.DataResult;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// TODO: proper error handling
// TODO: I should probably give this a better name
public class ItemSaver {
    public Path basePath;
    public ItemSaver(Path basePath) {
        this.basePath = basePath;
    }

    public void saveItem(String saveTo, ItemStack itemStack) {
        Path savePath = basePath.resolve(saveTo);

        try {
            Files.createDirectories(savePath.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, Minecraft.getInstance().getConnection().registryAccess());

        Tag stackNbt = ItemStack.CODEC.encodeStart(ops, itemStack).getOrThrow();

        CompoundTag nbt = new CompoundTag();
        nbt.put("item", stackNbt);
        nbt.putInt("version", 1);

        try {
            NbtIo.write(nbt, savePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public @Nullable ItemStack readItem(Path path) {
        if(!Files.exists(path)) {
            return null;
        }

        CompoundTag nbt;

        try {
            nbt = NbtIo.read(path);
        } catch(IOException e) {
            BetterItemSave.LOGGER.error("Failed to read item", e);
            return null;
        }

        Tag stackNbt = nbt.get("item");

        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, Minecraft.getInstance().getConnection().registryAccess());
        var dataResult = ItemStack.CODEC.parse(ops, stackNbt);

        if(dataResult.isError()) {
            return null;
        }

        return dataResult.getOrThrow();
    }
}