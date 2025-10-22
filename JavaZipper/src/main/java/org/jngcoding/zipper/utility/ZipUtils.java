package org.jngcoding.zipper.utility;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ZipUtils {
    private static final int BUFFER_SIZE = 8192;

    private ZipUtils() {}

    public static void zipFiles(List<File> sources, File targetZip) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(targetZip)))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            for (File src : sources) {
                if (src.isDirectory()) {
                    addDirectoryToZip(zos, src, src.getName() + File.separator, buffer);
                } else {
                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src))) {
                        ZipEntry entry = new ZipEntry(src.getName());
                        zos.putNextEntry(entry);
                        int read;
                        while ((read = bis.read(buffer)) != -1) {
                            zos.write(buffer, 0, read);
                        }
                        zos.closeEntry();
                    }
                }
            }
        }
    }

    private static void addDirectoryToZip(ZipOutputStream zos, File dir, String basePath, byte[] buffer) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            String entryName = basePath + child.getName();
            if (child.isDirectory()) {
                addDirectoryToZip(zos, child, entryName + File.separator, buffer);
            } else {
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(child))) {
                    ZipEntry entry = new ZipEntry(entryName.replace(File.separatorChar, '/'));
                    zos.putNextEntry(entry);
                    int read;
                    while ((read = bis.read(buffer)) != -1) {
                        zos.write(buffer, 0, read);
                    }
                    zos.closeEntry();
                }
            }
        }
    }

    public static void unzip(File zipFile, File targetDir) throws IOException {
        if (!targetDir.exists()) {
            Files.createDirectories(targetDir.toPath());
        }
        byte[] buffer = new byte[BUFFER_SIZE];
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = targetDir.toPath().resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Path parent = outPath.getParent();
                    if (parent != null) Files.createDirectories(parent);
                    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(outPath))) {
                        int len;
                        while ((len = zis.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
}
