package me.goodbee.betteritemsave.files;

import me.goodbee.betteritemsave.BetterItemSave;
import me.goodbee.betteritemsave.ItemSaver;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemFileList {
    private ConcurrentMap<Path, ItemFile> itemFileMap = new ConcurrentHashMap<>();
    private final ItemSaver itemSaver;
    private final Path basePath;

    public ItemFileList(ItemSaver itemSaver, Path basePath) {
        this.itemSaver = itemSaver;
        this.basePath = basePath;
    }

    public void recheckFolder() throws IOException {
        List<Path> files;

        try(Stream<Path> paths = Files.walk(basePath)) {
            files = paths.filter(Files::isRegularFile).collect(Collectors.toList());
        }

        for (Path path : files) {
            if(!itemFileMap.containsKey(path) || !(DigestUtils.md5Hex(Files.readAllBytes(path)).equals(itemFileMap.get(path).hash))) {
                itemFileMap.put(path, new ItemFile(path, itemSaver));
            }

            itemFileMap.get(path).exists = true;
        }

        for(Map.Entry<Path, ItemFile> entry : itemFileMap.entrySet()) {
            if(entry.getValue().exists) {
                entry.getValue().exists = false;
            } else {
                itemFileMap.remove(entry.getKey());
            }
        }
    }

    public Map<Path, ItemFile> getItemFileMap() {
        return itemFileMap;
    }
}