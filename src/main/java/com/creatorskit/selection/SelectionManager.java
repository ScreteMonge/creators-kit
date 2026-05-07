package com.creatorskit.selection;

import com.creatorskit.Character;
import lombok.Getter;

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
	private final LinkedHashSet<Character> selected = new LinkedHashSet<>();
	@Getter
    private Character firstSelected;
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

	public void select(Character c)
	{
		if (c == null)
		{
			clear();
			return;
		}

		if (selected.size() == 1 && firstSelected == c)
		{
			return;
		}

		selected.clear();
		selected.add(c);
		firstSelected = c;
		fireChanged();
	}

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

		firstSelected = last;
		fireChanged();
	}

	public void add(Character c)
	{
		if (!selected.add(c))
		{
			return;
		}

		firstSelected = c;
		fireChanged();
	}

	public void remove(Character c)
	{
		if (!selected.remove(c))
		{
			return;
		}

		if (firstSelected == c)
		{
			firstSelected = lastInSet();
		}

		fireChanged();
	}

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
		selected.clear();
		firstSelected = null;
		fireChanged();
	}

	public void addListener(SelectionListener listener)
	{
		listeners.add(listener);
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
