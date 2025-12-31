package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.block.ErgotInfestedWheatBlock;
import net.buda1bb.createmadlab.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = CreateMadLab.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WheatInfestationHandler {

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

    // Track rain end time per dimension
    private static final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, Long> rainEndTimes = new HashMap<>();

    // Track previous rain state per dimension
    private static final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, Boolean> wasRaining = new HashMap<>();

    @SubscribeEvent
    public static void onBlockGrow(BlockEvent.CropGrowEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        if (state.getBlock() == Blocks.WHEAT && state.hasProperty(CropBlock.AGE)) {
            int age = state.getValue(CropBlock.AGE);

            if (age == 7) {
                updateRainTracking(serverLevel);

                // Calculate infection chance
                float infectionChance = calculateInfectionChance(serverLevel, pos);

                if (serverLevel.random.nextFloat() < infectionChance) {
                    serverLevel.setBlock(pos, ModBlocks.ERGOT_INFESTED_WHEAT.get()
                            .defaultBlockState()
                            .setValue(ErgotInfestedWheatBlock.AGE, 7),3);
                }
            }
        }
    }

    private static void updateRainTracking(ServerLevel level) {
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension = level.dimension();
        boolean isCurrentlyRaining = level.isRaining();

        Boolean previouslyRaining = wasRaining.get(dimension);

        if (previouslyRaining != null && previouslyRaining && !isCurrentlyRaining) {
            rainEndTimes.put(dimension, level.getGameTime());
        }

        wasRaining.put(dimension, isCurrentlyRaining);
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

        if (level.isRaining() && level.canSeeSky(pos.above())) {
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
        int infectionCount = countNearbyInfections(level, pos);

        for (int i = INFECTION_THRESHOLDS.length - 1; i >= 0; i--) {
            if (infectionCount >= INFECTION_THRESHOLDS[i]) {
                return INFECTION_MULTIPLIERS[i];
            }
        }

        return 1.0f;
    }

    private static int countNearbyInfections(ServerLevel level, BlockPos center) {
        int count = 0;

        // Search in a 5x5x3 area (centered on the wheat, checking 1 block above and below)
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos checkPos = center.offset(dx, dy, dz);

                    if (checkPos.equals(center)) continue;

                    if (level.getBlockState(checkPos).getBlock() == ModBlocks.ERGOT_INFESTED_WHEAT.get()) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension = serverLevel.dimension();
            rainEndTimes.remove(dimension);
            wasRaining.remove(dimension);
        }
    }
}