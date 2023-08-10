package kludwisz.obbycg;

import java.util.HashSet;
import java.util.List;

import com.seedfinding.latticg.util.LCG;
import com.seedfinding.mcbiome.biome.Biome;
import com.seedfinding.mcbiome.source.BiomeSource;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.util.block.BlockBox;
import com.seedfinding.mccore.util.math.DistanceMetric;
import com.seedfinding.mccore.util.math.NextLongReverser;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.util.pos.RPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.loot.LootContext;
import com.seedfinding.mcfeature.loot.MCLootTables;
import com.seedfinding.mcfeature.structure.Village;
import com.seedfinding.mcmath.util.Mth;
import com.seedfinding.mcreversal.ChunkRandomReverser;
import com.seedfinding.mcterrain.TerrainGenerator;

import profotoce59.enumType.VillageType;
import profotoce59.properties.VillageGenerator;
import profotoce59.reecriture.VillagePools.VillageStructureLoot;

public class WorldSeedFinder {
	private static final Village VILLAGE = new Village(MCVersion.v1_16_1);
	private static final ChunkRand rand = new ChunkRand();
	private static final int R = 100;
	
	public static void run(long lootseed) {
		List<Long> internalDecorators = NextLongReverser.getSeeds(lootseed);
		
		for (long seed : internalDecorators) {
			long decorator = seed ^ LCG.JAVA.multiplier;
			long population = ChunkRandomReverser.reverseDecoratorSeed(decorator, 11, 4, MCVersion.v1_16_1);
			
			// looking for blacksmiths within a 2R x 2R area of chunks
			for (int x=-R; x<=R; x++) for (int z=-R; z<=R; z++) {
				if (z == R) System.out.print(".");
				
				List<Long> structseeds = ChunkRandomReverser.reversePopulationSeed(population, x<<4, z<<4, MCVersion.v1_16_1);
				CPos chunk = new CPos(x,z);
				RPos reg = chunk.toRegionPos(VILLAGE.getSpacing());
				
				for (long ss : structseeds) {
					CPos [] villages = {
							VILLAGE.getInRegion(ss, reg.getX(), reg.getZ(), rand),
							VILLAGE.getInRegion(ss, reg.getX(), reg.getZ()-1, rand),
							VILLAGE.getInRegion(ss, reg.getX()-1, reg.getZ(), rand),
							VILLAGE.getInRegion(ss, reg.getX()-1, reg.getZ()-1, rand)};
					
					for (CPos vill : villages) {
						if (vill == null) 
							continue;
						if (vill.distanceTo(chunk, DistanceMetric.EUCLIDEAN) <= 5)
							process(ss & Mth.MASK_48, vill, chunk);
					}
				}
			}
		}
	}
	
	private static void process(long structseed, CPos vill, CPos targetChunk) {
		BlockBox chunkBox = new BlockBox(targetChunk.getX()<<4, targetChunk.getZ()<<4, (targetChunk.getX()<<4)+15, (targetChunk.getZ()<<4)+15);
		long worldseed;
		HashSet<VillageType> usedTypes = new HashSet<>();
		
		for (long up=0; up < 10000; up++) {
			if (usedTypes.size() == 5) return; // checked all possible village types
			
			worldseed = (up<<48) | structseed;
			BiomeSource obs = BiomeSource.of(Dimension.OVERWORLD, MCVersion.v1_16_1, worldseed);
			
			// getting village type
			Biome biome = obs.getBiomeForNoiseGen((vill.getX() << 2) + 2, 0, (vill.getZ() << 2) + 2);
			VillageType type = VillageType.getType(biome, MCVersion.v1_16_1, false);
			if (type == null) continue;
			if (usedTypes.contains(type)) continue;
			
			// generating village
			TerrainGenerator otg = TerrainGenerator.of(obs);
			VillageGenerator gen = new VillageGenerator(MCVersion.v1_16_1, false);
			if (!gen.generate(otg, vill.getX(), vill.getZ(), rand)) continue;
			usedTypes.add(type);
			
			// finding first intersecting feature
			for (VillageGenerator.Piece p : gen.pieces) {
				if (!p.getName().contains("weaponsmith")) {
					if (VillageGenerator.Piece.featureList.contains(p.getName()) && chunkBox.contains(p.pos))
						break;
					if (VillageStructureLoot.STRUCTURE_LOOT_OFFSETS.get(p.getName()) == null) 
						continue;
					if (!VillageStructureLoot.STRUCTURE_LOOT_OFFSETS.get(p.getName()).isEmpty()) 
						break;
					continue;
				}
				if (VillageStructureLoot.STRUCTURE_LOOT_OFFSETS.get(p.getName()).isEmpty()) 
					continue;
				
				BPos offset = VillageStructureLoot.STRUCTURE_LOOT_OFFSETS.get(p.getName()).get(0);
				BPos lootpos = p.pos.add(VillageGenerator.Piece.getTransformedPos(offset, p.rotation));
				
				if (chunkBox.contains(lootpos)) {
					System.out.println("\nfound " + worldseed + " : " + vill + " " + targetChunk.toBlockPos());
					long pop = rand.setPopulationSeed(worldseed, targetChunk.getX()<<4, targetChunk.getZ()<<4, MCVersion.v1_16_1);
					rand.setDecoratorSeed(pop, 40011, MCVersion.v1_16_1);
					LootContext a = new LootContext(rand.nextLong(), MCVersion.v1_16_1);
					System.out.println(MCLootTables.VILLAGE_WEAPONSMITH_CHEST.get().generate(a));
				}
			}
			continue;
		}
	}
}
