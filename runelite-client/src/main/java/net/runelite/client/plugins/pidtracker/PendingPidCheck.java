package net.runelite.client.plugins.pidtracker;

import java.util.function.BiConsumer;
import lombok.Data;
import net.runelite.api.Player;

@Data
public class PendingPidCheck
{
	private Player attacker;
	private Player target;
	private int tick1;
	private int tick2;
	private boolean melee = false;

	public PendingPidCheck(Player attacker, Player target, int tick1, int tick2)
	{
		this.attacker = attacker;
		this.target = target;
		this.tick1 = tick1;
		this.tick2 = tick2;
	}

	private int tick1Hitsplats;
	private int tick2Hitsplats;

	public void incrementCountForTick(int tick)
	{
		if (tick == tick1)
		{
			tick1Hitsplats++;
		}
		else if (tick == tick2)
		{
			tick2Hitsplats++;
		}
	}

	public void setCountForTick(int tick, int count)
	{
		if (tick == tick1)
		{
			tick1Hitsplats = count;
		}
		else if (tick == tick2)
		{
			tick2Hitsplats = count;
		}
	}

	public boolean finishesOn(int currentTick)
	{
		return currentTick == tick2;
	}

	public void determinePid(BiConsumer<String, String> onNewPidAssignment)
	{
		if (attacker == null)
		{
			return;
		}
		// Our hitsplat was applied on the first tick, and on the second tick it was still the only hitsplat.
		if (tick1Hitsplats == 1 && tick2Hitsplats == 1)
		{
			onNewPidAssignment.accept(attacker.getName(), target.getName());
		}

		// There was no hitsplat on the first tick, and on the second tick ours was the only hitsplat.
		if (tick1Hitsplats == 0 && tick2Hitsplats == 1)
		{
			onNewPidAssignment.accept(target.getName(), attacker.getName()); // Target pids attacker
		}

		return;
	}
}
