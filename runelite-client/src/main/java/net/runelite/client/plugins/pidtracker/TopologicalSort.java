package net.runelite.client.plugins.pidtracker;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.graph.Graph;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

//https://github.com/jrtom/jung/pull/174/files
public class TopologicalSort
{
	public static <N> Set<N> sort(Graph<N> graph)
	{
		return new TopologicallySortedNodes<>(graph);
	}

	private static class TopologicallySortedNodes<N> extends AbstractSet<N>
	{
		private final Graph<N> graph;

		private TopologicallySortedNodes(Graph<N> graph)
		{
			this.graph = checkNotNull(graph, "graph");
		}

		@Override
		public UnmodifiableIterator<N> iterator()
		{
			return new TopologicalOrderIterator<>(graph);
		}

		@Override
		public int size()
		{
			return graph.nodes().size();
		}

		@Override
		public boolean remove(Object o)
		{
			throw new UnsupportedOperationException();
		}
	}

	private static class TopologicalOrderIterator<N> extends AbstractIterator<N>
	{
		private final Graph<N> graph;
		private final Queue<N> roots;
		private final Map<N, Integer> nonRootsToInDegree;

		private TopologicalOrderIterator(Graph<N> graph)
		{
			this.graph = checkNotNull(graph, "graph");
			this.roots =
				graph
					.nodes()
					.stream()
					.filter(node -> graph.inDegree(node) == 0)
					.collect(toCollection(ArrayDeque::new));
			this.nonRootsToInDegree =
				graph
					.nodes()
					.stream()
					.filter(node -> graph.inDegree(node) > 0)
					.collect(toMap(node -> node, graph::inDegree, (a, b) -> a, HashMap::new));
		}

		@Override
		protected N computeNext()
		{
			// Kahn's algorithm
			if (!roots.isEmpty())
			{
				N next = roots.remove();
				for (N successor : graph.successors(next))
				{
					int newInDegree = nonRootsToInDegree.get(successor) - 1;
					nonRootsToInDegree.put(successor, newInDegree);
					if (newInDegree == 0)
					{
						nonRootsToInDegree.remove(successor);
						roots.add(successor);
					}
				}
				return next;
			}
			checkState(nonRootsToInDegree.isEmpty(), "graph has at least one cycle");
			return endOfData();
		}
	}
}
