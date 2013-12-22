package vswe.stevescarts.Helpers;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import vswe.stevescarts.Blocks.Blocks;
import vswe.stevescarts.Items.Items;
import vswe.stevescarts.StevesCarts;

public class StorageBlock {

	private String name;
	private ItemStack item;
	public StorageBlock(String name, ItemStack item) {
		this.name = name;
		this.item = item.copy();
		this.item.stackSize = 9;
	}
	
	public String getName() {
		return name;
	}

	public void loadRecipe(int i) {
		ItemStack block = new ItemStack(Blocks.STORAGE.getBlock(), 1, i);
		
		//compress
		RecipeHelper.addRecipe(block, new Object[][] {
			{item, item, item},
			{item, item, item},
			{item, item, item}
		});	
		
		//restore
        RecipeHelper.addRecipe(item, new Object[][] {
				{block}
		});
	}
	
	
}