package kludwisz.obbycg;

import java.util.List;

import com.seedfinding.latticg.reversal.DynamicProgram;
import com.seedfinding.latticg.reversal.calltype.java.JavaCalls;
import com.seedfinding.latticg.util.LCG;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.loot.LootContext;
import com.seedfinding.mcfeature.loot.MCLootTables;
import com.seedfinding.mcfeature.loot.item.ItemStack;
import com.seedfinding.mcfeature.loot.item.Items;

public class LootSeedFinder {
	private static final int maxCount = 54;
	
	public static void run() {
		DynamicProgram device = DynamicProgram.create(LCG.JAVA);
		device.add(JavaCalls.nextInt(6).equalTo(5)); // 8 rolls
		
		// 78 - 82 incl-incl => obby
		device.add(JavaCalls.nextInt(94).betweenII(78, 82));
		device.skip(1);
		device.add(JavaCalls.nextInt(94).betweenII(78, 82));
		device.skip(1);
		device.add(JavaCalls.nextInt(94).betweenII(78, 82));
		device.skip(1);
		device.add(JavaCalls.nextInt(94).betweenII(78, 82));
		device.skip(1);
		device.add(JavaCalls.nextInt(94).betweenII(78, 82));
		device.skip(1);
		device.add(JavaCalls.nextInt(94).betweenII(78, 82));
		device.skip(1);
		device.add(JavaCalls.nextInt(94).betweenII(78, 82));
		device.skip(1);
		device.add(JavaCalls.nextInt(94).betweenII(78, 82));
		
		device.reverse().forEach(xoredLootseed -> {
			LootContext ctx = new LootContext(0, MCVersion.v1_16_1);
			ctx.setSeed(xoredLootseed, false);
			List<ItemStack> chest = MCLootTables.VILLAGE_WEAPONSMITH_CHEST.get().generate(ctx);
			int count = 0;
			for (ItemStack i : chest) {
				if (i.getItem().getName() == Items.OBSIDIAN.getName()) {
					count += i.getCount();
				}
			}

			// if (count > maxCount) {
			//	   maxCount = count;
			//	   System.out.println(maxCount);
			// }
			
			if (count == maxCount) {
				System.out.println(xoredLootseed ^ LCG.JAVA.multiplier);
			}
		});
	}
}
