package vswe.stevescarts.guis.buttons;

import net.minecraft.entity.player.EntityPlayer;
import vswe.stevescarts.computer.ComputerTask;
import vswe.stevescarts.modules.workers.ModuleComputer;

public class ButtonControlType extends ButtonAssembly {
	private int typeId;

	public ButtonControlType(final ModuleComputer module, final LOCATION loc, final int id) {
		super(module, loc);
		typeId = id;
	}

	@Override
	public String toString() {
		return "Change to " + ComputerTask.getControlTypeName(typeId);
	}

	@Override
	public boolean isVisible() {
		if (!super.isVisible()) {
			return false;
		}
		if (module instanceof ModuleComputer && ((ModuleComputer) module).getSelectedTasks() != null && ((ModuleComputer) module).getSelectedTasks().size() > 0) {
			for (final ComputerTask task : ((ModuleComputer) module).getSelectedTasks()) {
				if (!ComputerTask.isControl(task.getType())) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public int texture() {
		return ComputerTask.getControlImage(typeId);
	}

	@Override
	public int ColorCode() {
		return 3;
	}

	@Override
	public boolean isEnabled() {
		for (final ComputerTask task : ((ModuleComputer) module).getSelectedTasks()) {
			if (task.getControlType() != typeId) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onServerClick(final EntityPlayer player, final int mousebutton, final boolean ctrlKey, final boolean shiftKey) {
		for (final ComputerTask task : ((ModuleComputer) module).getSelectedTasks()) {
			task.setControlType(typeId);
		}
	}
}
