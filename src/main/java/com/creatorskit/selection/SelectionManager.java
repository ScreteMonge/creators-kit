package com.creatorskit.selection;

import com.creatorskit.Character;
import lombok.Getter;
import lombok.Setter;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class SelectionManager
{
	@Getter
    private Character primary;
	private final LinkedHashSet<Character> selected = new LinkedHashSet<>();
	private final List<SelectionListener> listeners = new ArrayList<>();

    public Set<Character> getSelected()
	{
		return Collections.unmodifiableSet(selected);
	}

	public boolean contains(Character c)
	{
		return c != null && selected.contains(c);
	}

	public boolean isEmpty()
	{
		return selected.isEmpty();
	}

	public int getSelectionSize()
	{
		return selected.size();
	}

	public void select(Character c, SelectionOrigin origin)
	{
		if (c == null)
		{
			clear(origin);
			return;
		}

		if (selected.size() == 1 && primary == c)
		{
			return;
		}

		clear(origin);
		selected.add(c);
		primary = c;
		fireChanged(origin);
	}

	public void selectAll(Collection<Character> characters, SelectionOrigin origin)
	{
		if (characters == null || characters.isEmpty())
		{
			clear(origin);
			return;
		}

		clear(origin);
		Character last = null;
		for (Character c : characters)
		{
			if (selected.add(c))
			{
				last = c;
			}
		}

		primary = last;
		fireChanged(origin);
	}

	public void add(Character c, SelectionOrigin origin)
	{
		primary = c;
		selected.add(c);
		fireChanged(origin);
	}

	public void remove(Character c, SelectionOrigin origin)
	{
		if (!selected.remove(c))
		{
			return;
		}

		if (primary == c)
		{
			primary = lastInSet();
		}

		fireChanged(origin);
	}

	public void toggle(Character c, SelectionOrigin origin)
	{
		if (c == null)
		{
			return;
		}
		if (selected.contains(c))
		{
			remove(c, origin);
		}
		else
		{
			add(c, origin);
		}
	}

	public void clear(SelectionOrigin origin)
	{
		selected.clear();
		primary = null;
		fireChanged(origin);
	}

	public void addListener(SelectionListener listener)
	{
		listeners.add(listener);
	}

	private void fireChanged(SelectionOrigin origin)
	{
		for (SelectionListener l : new ArrayList<>(listeners))
		{
			l.selectionChanged(this, origin);
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
		void selectionChanged(SelectionManager manager, SelectionOrigin origin);
	}
}
