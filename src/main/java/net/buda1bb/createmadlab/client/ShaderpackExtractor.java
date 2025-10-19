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
        Path shaderpacksDir = FMLPaths.GAMEDIR.get().resolve("shaderpacks");
        Path targetDir = shaderpacksDir.resolve(SHADERPACK_NAME);
        Path shadersDir = targetDir.resolve("shaders");

        try {
            Files.createDirectories(shadersDir);
        } catch (IOException e) {
            // Silent fail
        }
    }

    public static void activateShaders(double dose) {
        Path shaderpacksDir = FMLPaths.GAMEDIR.get().resolve("shaderpacks");
        Path targetDir = shaderpacksDir.resolve(SHADERPACK_NAME);
        Path shadersDir = targetDir.resolve("shaders");

        try {
            Files.createDirectories(shadersDir);
            deleteShaderFiles();

            if (dose >= 2.0) {
                extractEffectShaders("lsd_high", shadersDir);
            } else {
                extractEffectShaders("lsd", shadersDir);
            }

        } catch (IOException e) {
            // Silent fail
        }
    }

    public static void deactivateShaders() {
        deleteShaderFiles();
    }

    private static void extractEffectShaders(String effectType, Path targetDir) {
        String effectResourcePath = RESOURCE_PATH + "/shaders/" + effectType;

        String codePath = ShaderpackExtractor.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath();

        try {
            if (codePath.endsWith(".jar")) {
                extractShaderFilesFromJar(new File(codePath), effectResourcePath, targetDir);
            } else {
                URL resourceUrl = ShaderpackExtractor.class.getClassLoader().getResource(effectResourcePath);
                if (resourceUrl == null) return;

                try {
                    Path sourceDir = Paths.get(resourceUrl.toURI());
                    copyFolder(sourceDir, targetDir);
                } catch (FileSystemNotFoundException | URISyntaxException e) {
                    // Silent fail
                }
            }
        } catch (IOException e) {
            // Silent fail
        }
    }

    public static void deleteShaderFiles() {
        try {
            Path shaderpacksDir = FMLPaths.GAMEDIR.get().resolve("shaderpacks");
            Path shadersDir = shaderpacksDir.resolve(SHADERPACK_NAME).resolve("shaders");

            if (Files.exists(shadersDir)) {
                Files.walk(shadersDir)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Silent fail
                            }
                        });
            }
        } catch (IOException e) {
            // Silent fail
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
                // Silent fail
            }
        });
    }
}