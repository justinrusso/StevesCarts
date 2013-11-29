package vswe.stevescarts.Items;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import vswe.stevescarts.StevesCarts;

public class ItemBlockDetector extends ItemBlock {

   
    public ItemBlockDetector(int i)
    {
        super(i);
        setHasSubtypes(true);
        setMaxDamage(0);
        setCreativeTab(StevesCarts.tabsSC2Blocks);		
    }


 	@Override
    public String getUnlocalizedName(ItemStack item)
    {

		if (item != null) {
			return "item." + StevesCarts.instance.localStart + "BlockDetector" + item.getItemDamage();
		}	
	
        return "item.unknown";
    }	

 	
    @Override
    public int getMetadata(int dmg) {
        return dmg;
    } 	

}
