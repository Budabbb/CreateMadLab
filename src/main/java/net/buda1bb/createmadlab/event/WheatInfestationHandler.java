package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.block.ErgotInfestedWheatBlock;
import net.buda1bb.createmadlab.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WheatInfestationHandler {

    private static final int MATURE_WHEAT_AGE = 7;

    // Base chance
    private static final float BASE_CHANCE = 0.001f; // 0.1%

    // Weather modifiers
    private static final float RAINING_BONUS = 0.005f; // +0.5% when raining
    private static final float RECENT_RAIN_BONUS = 0.002f; // +0.2% after rain
    private static final int RECENT_RAIN_DURATION = 12000; // 10 minutes (20 ticks * 60 seconds * 10)

    // Temperature thresholds
    private static final float OPTIMAL_TEMP_MIN = 0.5f;
    private static final float OPTIMAL_TEMP_MAX = 0.9f;
    private static final float EXTREME_TEMP_MIN = 0.3f;
    private static final float EXTREME_TEMP_MAX = 1.2f;
    private static final float TEMP_BONUS = 0.005f;
    private static final float TEMP_PENALTY = 0.002f;

    // Infection spread
    private static final int SEARCH_RADIUS = 5;
    private static final float[] INFECTION_MULTIPLIERS = {
            1.0f,   // 0 infections
            1.5f,   // 1-2 infections
            2.0f,   // 3-5 infections
            3.0f,   // 6-10 infections
            4.0f,   // 11-15 infections
            5.0f    // 16+ infections
    };
    private static final int[] INFECTION_THRESHOLDS = {0, 1, 3, 6, 11, 16};
    private static final int MAX_COUNTED_INFECTIONS = 16;

    // Track rain end time per dimension
    private static final Map<ResourceKey<Level>, Long> rainEndTimes = new HashMap<>();

    // Track previous rain state per dimension
    private static final Map<ResourceKey<Level>, Boolean> wasRaining = new HashMap<>();

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level instanceof ServerLevel serverLevel) {
            updateRainTracking(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onBlockGrow(BlockEvent.CropGrowEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        BlockState originalState = event.getOriginalState();

        if (state.getBlock() == Blocks.WHEAT && state.hasProperty(CropBlock.AGE) && originalState.hasProperty(CropBlock.AGE)) {
            int age = state.getValue(CropBlock.AGE);
            int originalAge = originalState.getValue(CropBlock.AGE);

            if (age == MATURE_WHEAT_AGE && originalAge < MATURE_WHEAT_AGE) {
                float infectionChance = calculateInfectionChance(serverLevel, pos);

                if (serverLevel.random.nextFloat() < infectionChance) {
                    serverLevel.setBlock(pos, ModBlocks.ERGOT_INFESTED_WHEAT.get()
                            .defaultBlockState()
                            .setValue(ErgotInfestedWheatBlock.AGE, MATURE_WHEAT_AGE), 3);
                }
            }
        }
    }

    private static void updateRainTracking(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        boolean isCurrentlyRaining = level.isRaining();

        Boolean previouslyRaining = wasRaining.put(dimension, isCurrentlyRaining);
        if (Boolean.TRUE.equals(previouslyRaining) && !isCurrentlyRaining) {
            rainEndTimes.put(dimension, level.getGameTime());
        }
    }

    private static float calculateInfectionChance(ServerLevel level, BlockPos pos) {
        float chance = BASE_CHANCE;

        chance += getWeatherModifier(level, pos);

        chance += getTemperatureModifier(level, pos);

        // Ensure chance is at least 0 before applying multiplier
        chance = Math.max(chance, 0.0f);

        float spreadMultiplier = getInfectionSpreadMultiplier(level, pos);
        chance *= spreadMultiplier;

        // Apply final caps
        chance = Math.max(chance, 0.0001f);  // Minimum 0.01%
        chance = Math.min(chance, 0.25f);    // Maximum 25%

        return chance;
    }

    private static float getWeatherModifier(ServerLevel level, BlockPos pos) {
        float modifier = 0.0f;

        if (level.isRainingAt(pos.above())) {
            modifier += RAINING_BONUS;
        }

        Long rainEndTime = rainEndTimes.get(level.dimension());
        if (rainEndTime != null) {
            long ticksSinceRainStopped = level.getGameTime() - rainEndTime;
            if (ticksSinceRainStopped > 0 && ticksSinceRainStopped < RECENT_RAIN_DURATION) {
                modifier += RECENT_RAIN_BONUS;
            }
        }

        return modifier;
    }

    private static float getTemperatureModifier(ServerLevel level, BlockPos pos) {
        Biome biome = level.getBiome(pos).value();
        float temperature = biome.getBaseTemperature();

        if (temperature >= OPTIMAL_TEMP_MIN && temperature <= OPTIMAL_TEMP_MAX) {
            return TEMP_BONUS;
        } else if (temperature < EXTREME_TEMP_MIN || temperature > EXTREME_TEMP_MAX) {
            return -TEMP_PENALTY;
        }

        return 0.0f;
    }

    private static float getInfectionSpreadMultiplier(ServerLevel level, BlockPos pos) {
        if (!isSearchAreaLoaded(level, pos)) {
            return 1.0f;
        }

        int infectionCount = countNearbyInfections(level, pos);

        for (int i = INFECTION_THRESHOLDS.length - 1; i >= 0; i--) {
            if (infectionCount >= INFECTION_THRESHOLDS[i]) {
                return INFECTION_MULTIPLIERS[i];
            }
        }

        return 1.0f;
    }

    private static boolean isSearchAreaLoaded(ServerLevel level, BlockPos center) {
        int minChunkX = SectionPos.blockToSectionCoord(center.getX() - SEARCH_RADIUS);
        int maxChunkX = SectionPos.blockToSectionCoord(center.getX() + SEARCH_RADIUS);
        int minChunkZ = SectionPos.blockToSectionCoord(center.getZ() - SEARCH_RADIUS);
        int maxChunkZ = SectionPos.blockToSectionCoord(center.getZ() + SEARCH_RADIUS);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static int countNearbyInfections(ServerLevel level, BlockPos center) {
        int count = 0;
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();
        Block infestedWheat = ModBlocks.ERGOT_INFESTED_WHEAT.get();
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        // Search in an 11x11x3 area, centered on the wheat and checking 1 block above and below.
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    checkPos.set(centerX + dx, centerY + dy, centerZ + dz);
                    if (level.getBlockState(checkPos).getBlock() == infestedWheat) {
                        count++;
                        if (count >= MAX_COUNTED_INFECTIONS) {
                            return count;
                        }
                    }
                }
            }
        }

        return count;
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ResourceKey<Level> dimension = serverLevel.dimension();
            rainEndTimes.remove(dimension);
            wasRaining.remove(dimension);
        }
    }
}
