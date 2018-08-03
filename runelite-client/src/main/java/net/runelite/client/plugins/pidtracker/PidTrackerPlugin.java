/*
 * Copyright (c) 2018 Mike278
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.pidtracker;

import com.google.common.eventbus.Subscribe;
import com.google.common.graph.Graph;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.queries.PlayerQuery;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.apache.commons.lang3.tuple.ImmutablePair;

@PluginDescriptor(
	name = "Pid Tracker",
	enabledByDefault = false
)
@Slf4j
public class PidTrackerPlugin extends Plugin
{
	/**
	 * ===RANGE/MAGE PID===
	 * <p>
	 * A > B
	 * Tick [1] onProjectileMoved: PlayerA launched a projectile at PlayerB; expected to land on [4]
	 * Tick [2] ...
	 * Tick [3] onHitsplatApplied: PlayerA's hitsplat applied on PlayerB
	 * Tick [4] Projectile lands on PlayerB
	 * Tick [4] onTick: PlayerA > PlayerB
	 * <p>
	 * B > A
	 * Tick [1] onProjectileMoved: PlayerA launched a projectile at PlayerB; expected to land on [4]
	 * Tick [2] ...
	 * Tick [3] ...
	 * Tick [4] Projectile lands on PlayerB
	 * Tick [4] onHitsplatApplied: PlayerA's hitsplat applied on PlayerB
	 * Tick [4] onTick: PlayerB > PlayerA
	 * <p>
	 * <p>
	 * ===MELEE PID===
	 * <p>
	 * A > B
	 * Tick [1] onAnimationChanged: PlayerA attacked PlayerB
	 * Tick [1] onHitsplatApplied: PlayerA's hitsplat applied on PlayerB
	 * Tick [2] onTick: PlayerA > PlayerB
	 * .. OR ..
	 * Tick [1] onHitsplatApplied: PlayerA's hitsplat applied on PlayerB
	 * Tick [1] onAnimationChanged: PlayerA attacked PlayerB
	 * Tick [2] onTick: PlayerA > PlayerB
	 * <p>
	 * B > A
	 * Tick [1] onAnimationChanged: PlayerA attacked PlayerB
	 * Tick [2] onHitsplatApplied: PlayerA's hitsplat applied on PlayerB
	 * Tick [2] onTick: PlayerB > PlayerA
	 */


	// http://services.runescape.com/m=news/team-structure-pvp-changes-and-upcoming-poll?oldschool=1
	// "Currently, PID is randomised once every 100 to 150 game ticks (60 to 90 seconds) per player."
	public static final int MAX_PID_AGE = 150;
	public static final int CYCLES_PER_TICK = 30;

	private List<PendingPidCheck> pendingPidChecks = new ArrayList<>();
	public MutableValueGraph<String, Integer> pidGraph = ValueGraphBuilder.directed().build();

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PidOverlay pidOverlay;


	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(pidOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(pidOverlay);
	}

	@Subscribe
	private void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();
		int currentTick = client.getTickCount();
		int currentCycle = client.getGameCycle();

		// Only process onProjectileMoved if the projectile has just launched - skip otherwise.
		if (currentCycle >= projectile.getStartMovementCycle())
		{
			return;
		}

		// Skip if projectile isn't targetting a player
		if (projectile.getInteracting() == null || !(projectile.getInteracting() instanceof Player))
		{
			return;
		}

		// Skip if we're unable to determine who created this projectile.
		Player attacker = getProjectileCreator(projectile);
		if (attacker == null)
		{
			return;
		}

		// Skip if attacker == target. Not sure how this is possible, but it happens in practice.
		Player target = (Player) projectile.getInteracting();
		if (attacker == target)
		{
			return;
		}

		// Register a PendingPidCheck for attacker and target on the tick we expect the projectile to land.
		int landsOnTick = currentTick + calcProjectileRemainingTravelTicks(projectile, currentCycle);
		pendingPidChecks.add(new PendingPidCheck(attacker, target, landsOnTick - 1, landsOnTick));
	}

	@Subscribe
	private void onAnimationChanged(AnimationChanged anim)
	{
		if (anim.getActor() == null ||
			!(anim.getActor() instanceof Player) ||
			!(anim.getActor().getInteracting() instanceof Player) ||
			!CombatAnimations.MELEE.contains(anim.getActor().getAnimation()))
		{
			return;
		}

		int currentTick = client.getTickCount();
		Player attacker = (Player) anim.getActor();
		Player target = (Player) attacker.getInteracting();

		// If onHitsplatApplied fires before before onAnimationChanged it'll register a PidCheck with a NULL attacker.
		// Lets see if any of those NULL attackers could be this anim.getActor()...
		List<PendingPidCheck> matchingPidChecks = getMatchingMeleePidChecks(currentTick, target);
		if (!matchingPidChecks.isEmpty())
		{
			matchingPidChecks.forEach(p -> p.setAttacker(attacker));
		}
		else
		{
			// Otherwise, onAnimationChanged has fired first - simply register a PendingPidCheck for this attacker and target
			PendingPidCheck pendingPidCheck = new PendingPidCheck(attacker, target, currentTick, currentTick + 1);
			pendingPidCheck.setMelee(true);
			pendingPidChecks.add(pendingPidCheck);
		}

	}

	@Subscribe
	void onHitsplatApplied(HitsplatApplied hitsplatApplied)
	{
		if (!(hitsplatApplied.getActor() instanceof Player))
		{
			return;
		}

		Player appliedOnPlayer = (Player) hitsplatApplied.getActor();
		int currentTick = client.getTickCount();

		// Find all pid checks that have this "appliedOnPlayer" as a target
		List<PendingPidCheck> pidChecksWithThisTarget = pendingPidChecks.stream()
			.filter(pidCheck -> pidCheck.getTarget().equals(appliedOnPlayer))
			.collect(Collectors.toList());

		// For each pid check found, +1 the number of hitsplats applied on this tick
		if (pidChecksWithThisTarget != null && !pidChecksWithThisTarget.isEmpty())
		{
			pidChecksWithThisTarget.forEach(pidCheck -> pidCheck.incrementCountForTick(currentTick));
		}
		else
		{
			// No pending pid checks were found with this "appliedOnPlayer" as a target
			// Let's add this as a melee pid check with a null attacker and let onAnimationChanged set the attacker
			PendingPidCheck pendingPidCheck = new PendingPidCheck(null, appliedOnPlayer, currentTick, currentTick + 1);
			pendingPidCheck.setMelee(true);
			pendingPidCheck.setCountForTick(currentTick, 1);
			pendingPidChecks.add(pendingPidCheck);
		}


	}

	@Subscribe
	void onTick(GameTick tick)
	{
		int currentTick = client.getTickCount();

		// - Find all PidChecks that are schedule to finish on this tick.
		// - Do a final hitsplat count
		// - Call determinePid() on each pidCheck
		pendingPidChecks.stream()
			.filter(pidCheck -> pidCheck.finishesOn(currentTick))
			.forEach(finishedPidCheck ->
			{
				finishedPidCheck.setCountForTick(currentTick, countActorHitsplats(finishedPidCheck.getTarget()));
				finishedPidCheck.determinePid(this::onNewPidAssignment);
			});

		// Cull off finished pidchecks
		pendingPidChecks.removeIf(pendingPidCheck -> pendingPidCheck.getTick2() == currentTick);

		// Cull off expired pids
		cullExpiredPids(currentTick);
	}

	/**
	 * Removes all pid connections that have expired (i.e. created longer than MAX_PID_AGE ticks ago)
	 */
	private void cullExpiredPids(int currentTick)
	{
		// pidGraph.edges().removeIf(...) will cause a ConcurrentModificationException, since each edge removal
		// changes the whole graph. Instead, we must first collect all edges that meet the expiry criteria ...
		List<ImmutablePair<String, String>> edgesToCull = pidGraph.edges().stream()
			.filter(next ->
			{
				Integer assignedOnTick = pidGraph.edgeValue(next.nodeU(), next.nodeV()).orElse(Integer.MIN_VALUE);
				return currentTick >= assignedOnTick + MAX_PID_AGE;
			})
			.map(edge -> new ImmutablePair<>(edge.nodeU(), edge.nodeV()))
			.collect(Collectors.toList());

		// ... then iterate over those edges, check if it still exists in the graph, and if so delete
		for (ImmutablePair<String, String> edge : edgesToCull)
		{
			if (pidGraph.hasEdgeConnecting(edge.getLeft(), edge.getRight()))
			{
				pidGraph.removeEdge(edge.getLeft(), edge.getRight());
			}
		}
	}

	private Player getProjectileCreator(Projectile projectile)
	{
		WorldPoint launchPoint = WorldPoint.fromLocal(client, projectile.getX1(), projectile.getY1(), client.getPlane());
		Player[] players = new PlayerQuery().atWorldPoint(launchPoint).result(client);

		// Projectile creator is only guaranteed if there's only 1 person on the tile.
		return players.length == 1 ? players[0] : null;
	}

	private int calcProjectileRemainingTravelTicks(Projectile projectile, int currentCycle)
	{
		return (int) Math.ceil((projectile.getEndCycle() - currentCycle) / (double) CYCLES_PER_TICK);
	}

	private int countActorHitsplats(Actor actor)
	{
		if (actor == null)
		{
			return -1;
		}
		return (int) Arrays.stream(actor.getHitsplatCycles()).filter(remainingCycles -> remainingCycles > 0).count();
	}

	private List<PendingPidCheck> getMatchingMeleePidChecks(int currentTick, Player target)
	{
		return pendingPidChecks.stream()
			.filter(p -> p.isMelee() && p.getTick1() == currentTick && p.getAttacker() == null && p.getTarget() == target)
			.collect(Collectors.toList());
	}

	private void onNewPidAssignment(String pidder, String piddee)
	{

		int currentTick = client.getTickCount();

		// Take a copy of the pidGraph in its current state and try to add an edge for this new assignment.
		MutableValueGraph<String, Integer> pidGraphCopy = Graphs.copyOf(pidGraph);
		pidGraphCopy.putEdgeValue(pidder, piddee, currentTick);

		// If adding the new edge would create a cycle, that means a pid swap has occurred somewhere.
		// In this case, we completely remove both nodes from the graph (and by extension, all incident edges)
		if (Graphs.hasCycle(pidGraphCopy.asGraph()))
		{
			pidGraph.removeNode(pidder);
			pidGraph.removeNode(piddee);
		}

		pidGraph.putEdgeValue(pidder, piddee, currentTick);
	}

	public Optional<Boolean> wePidPlayer(String them)
	{
		if (them == null)
		{
			return Optional.empty();
		}

		String us = client.getLocalPlayer().getName();
		if (them.equals(us) || !pidGraph.nodes().contains(us) || !pidGraph.nodes().contains(them))
		{
			return Optional.empty();
		}

		Graph<String> transitiveClosure = Graphs.transitiveClosure(pidGraph.asGraph());

		Set<String> peopleWePid = transitiveClosure.successors(us);
		if (peopleWePid != null && peopleWePid.contains(them))
		{
			return Optional.of(true);
		}

		Set<String> peopleTheyPid = transitiveClosure.successors(them);
		if (peopleTheyPid != null && peopleTheyPid.contains(us))
		{
			return Optional.of(false);
		}

		return Optional.empty();
	}
}