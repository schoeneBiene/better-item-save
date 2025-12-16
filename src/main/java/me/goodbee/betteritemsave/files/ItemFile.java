package me.goodbee.betteritemsave.files;

import me.goodbee.betteritemsave.ItemSaver;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ItemFile {
    public String hash;
    public Path location;
    public ItemStack itemStack;
    public boolean exists;

    public ItemFile(Path location, ItemSaver itemSaver) {
        this.location = location;

        this.itemStack = itemSaver.readItem(location);

        if(this.itemStack == null) {
            throw new RuntimeException("Could not read ItemStack!");
        }

        try {
            this.hash = DigestUtils.md5Hex(Files.readAllBytes(location));
        } catch (IOException err) {
            this.hash = null;
        }
    }
}