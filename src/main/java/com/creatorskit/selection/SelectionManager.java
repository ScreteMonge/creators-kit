package com.creatorskit.selection;

import com.creatorskit.Character;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Single source of truth for which Characters are currently selected.
 *
 * Insertion order is preserved (LinkedHashSet) so the most-recently-added
 * Character can be treated as the "primary" — used to keep existing
 * single-selection call sites working unchanged via {@link #getPrimary()}.
 */
@Singleton
public class SelectionManager
{
	private final LinkedHashSet<Character> selected = new LinkedHashSet<>();
	private Character primary;
	private final List<SelectionListener> listeners = new ArrayList<>();

	public Character getPrimary()
	{
		return primary;
	}

	public Set<Character> getSelected()
	{
		return Collections.unmodifiableSet(selected);
	}

	public boolean isSelected(Character c)
	{
		return c != null && selected.contains(c);
	}

	public boolean isEmpty()
	{
		return selected.isEmpty();
	}

	public int size()
	{
		return selected.size();
	}

	/**
	 * Replace the entire selection with this single Character. A null argument clears.
	 */
	public void select(Character c)
	{
		if (c == null)
		{
			clear();
			return;
		}
		if (selected.size() == 1 && primary == c)
		{
			return;
		}
		selected.clear();
		selected.add(c);
		primary = c;
		fireChanged();
	}

	/**
	 * Replace the entire selection with the given Characters. The last element of the
	 * iteration becomes the primary. A null or empty collection clears the selection.
	 */
	public void selectAll(Collection<Character> characters)
	{
		if (characters == null || characters.isEmpty())
		{
			clear();
			return;
		}
		selected.clear();
		Character last = null;
		for (Character c : characters)
		{
			if (c != null && selected.add(c))
			{
				last = c;
			}
		}
		primary = last;
		fireChanged();
	}

	/**
	 * Add c to the selection without removing existing entries. Becomes the new primary.
	 */
	public void add(Character c)
	{
		if (c == null || !selected.add(c))
		{
			return;
		}
		primary = c;
		fireChanged();
	}

	/**
	 * Remove c from the selection. If it was the primary, primary falls back to the
	 * most-recently-added remaining entry, or null if the selection is now empty.
	 */
	public void remove(Character c)
	{
		if (c == null || !selected.remove(c))
		{
			return;
		}
		if (primary == c)
		{
			primary = lastInSet();
		}
		fireChanged();
	}

	/**
	 * Toggle membership: add if absent, remove if present.
	 */
	public void toggle(Character c)
	{
		if (c == null)
		{
			return;
		}
		if (selected.contains(c))
		{
			remove(c);
		}
		else
		{
			add(c);
		}
	}

	public void clear()
	{
		if (selected.isEmpty())
		{
			return;
		}
		selected.clear();
		primary = null;
		fireChanged();
	}

	public void addListener(SelectionListener listener)
	{
		if (listener != null)
		{
			listeners.add(listener);
		}
	}

	public void removeListener(SelectionListener listener)
	{
		listeners.remove(listener);
	}

	private void fireChanged()
	{
		for (SelectionListener l : new ArrayList<>(listeners))
		{
			l.selectionChanged(this);
		}
	}

	private Character lastInSet()
	{
		Character last = null;
		for (Character c : selected)
		{
			last = c;
		}
		return last;
	}

	public interface SelectionListener
	{
		void selectionChanged(SelectionManager manager);
	}
}
