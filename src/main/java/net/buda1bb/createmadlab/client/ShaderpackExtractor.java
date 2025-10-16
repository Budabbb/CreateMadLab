package net.buda1bb.createmadlab.client;

import net.buda1bb.createmadlab.CreateMadLab;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ShaderpackExtractor {

    private static final String SHADERPACK_NAME = "createmadlab_shaders";
    private static final String RESOURCE_PATH = "assets/" + CreateMadLab.MOD_ID + "/shaderpacks/" + SHADERPACK_NAME;

    public static void extractShaderpackStructure() {
        try {
            Path shaderpacksDir = FMLPaths.GAMEDIR.get().resolve("shaderpacks");
            Path targetDir = shaderpacksDir.resolve(SHADERPACK_NAME);
            Path shadersDir = targetDir.resolve("shaders");

            // Only create the folder structure, not the shader files
            if (!Files.exists(shadersDir)) {
                Files.createDirectories(shadersDir);
                System.out.println("[" + CreateMadLab.MOD_ID + "] Created shaderpack folder structure");
            }

        } catch (IOException e) {
            System.err.println("[" + CreateMadLab.MOD_ID + "] Failed to create shaderpack structure: " + e.getMessage());
        }
    }

    public static void extractShaderFiles() {
        try {
            Path shaderpacksDir = FMLPaths.GAMEDIR.get().resolve("shaderpacks");
            Path targetDir = shaderpacksDir.resolve(SHADERPACK_NAME);
            Path shadersDir = targetDir.resolve("shaders");

            // Make sure structure exists
            Files.createDirectories(shadersDir);

            // Detect if running from a jar
            String codePath = ShaderpackExtractor.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath();

            if (codePath.endsWith(".jar")) {
                extractShaderFilesFromJar(new File(codePath), RESOURCE_PATH + "/shaders", shadersDir);
            } else {
                // Development environment
                URL resourceUrl = ShaderpackExtractor.class.getClassLoader().getResource(RESOURCE_PATH + "/shaders");
                if (resourceUrl == null) {
                    System.err.println("[" + CreateMadLab.MOD_ID + "] Could not find shader files at: " + RESOURCE_PATH + "/shaders");
                    return;
                }

                try {
                    Path sourceDir = Paths.get(resourceUrl.toURI());
                    copyFolder(sourceDir, shadersDir);
                    System.out.println("[" + CreateMadLab.MOD_ID + "] Shader files copied from compiled resources.");
                } catch (FileSystemNotFoundException | URISyntaxException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("[" + CreateMadLab.MOD_ID + "] Shader files extracted to: " + shadersDir);

        } catch (IOException e) {
            System.err.println("[" + CreateMadLab.MOD_ID + "] Failed to extract shader files: " + e.getMessage());
        }
    }

    public static void deleteShaderFiles() {
        try {
            Path shaderpacksDir = FMLPaths.GAMEDIR.get().resolve("shaderpacks");
            Path shadersDir = shaderpacksDir.resolve(SHADERPACK_NAME).resolve("shaders");

            if (Files.exists(shadersDir)) {
                // Delete all files in shaders folder but keep the folder structure
                Files.walk(shadersDir)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                                System.out.println("[" + CreateMadLab.MOD_ID + "] Deleted: " + path.getFileName());
                            } catch (IOException e) {
                                System.err.println("[" + CreateMadLab.MOD_ID + "] Failed to delete: " + path.getFileName());
                            }
                        });

                System.out.println("[" + CreateMadLab.MOD_ID + "] All shader files deleted");
            }

        } catch (IOException e) {
            System.err.println("[" + CreateMadLab.MOD_ID + "] Failed to delete shader files: " + e.getMessage());
        }
    }

    private static void extractShaderFilesFromJar(File jarFile, String resourcePath, Path targetDir) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith(resourcePath + "/") && !entry.isDirectory()) {
                    String filename = entry.getName().substring(resourcePath.length() + 1);
                    Path filePath = targetDir.resolve(filename);

                    Files.createDirectories(filePath.getParent());
                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("[" + CreateMadLab.MOD_ID + "] Extracted: " + filename);
                    }
                }
            }
        }
    }

    private static void copyFolder(Path source, Path target) throws IOException {
        Files.walk(source).forEach(path -> {
            try {
                Path dest = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}