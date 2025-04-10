package com.creatorskit.swing.combobox;

import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

//Code adapted from trashgod, see: https://stackoverflow.com/a/7605780
public class JSearchableComboBox extends JComboBox<Object>
{
    private final List<Object> itemBackup = new ArrayList<>();
    private final DefaultComboBoxModel<Object> model;
    private boolean showingAll = false;

    public JSearchableComboBox()
    {
        super();
        this.setRenderer(new SearchRenderer());
        this.setEditor(new SearchComboBoxEditor());
        this.setEditable(true);
        DefaultComboBoxModel<Object> dcbm = new DefaultComboBoxModel<>();
        this.model = dcbm;
        this.setModel(dcbm);

        final JTextField jtf = (JTextField) this.getEditor().getEditorComponent();

        jtf.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                if (e.isControlDown())
                {
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_CONTROL)
                {
                    return;
                }

                searchAndListEntries(jtf.getText());
            }
        });
    }

    public void initialize(List<Object> list)
    {
        model.addElement(new Object[]{"", "", 0});
        for (Object o : list)
        {
            Object addition = new Object[]{o, "", 0};
            itemBackup.add(addition);
            model.addElement(addition);
        }
    }

    public Object getSelectedData()
    {
        Object obj = getSelectedItem();
        if (obj == null)
        {
            return null;
        }

        Object[] o = (Object[]) getSelectedItem();
        return o[0];
    }

    public void showAll()
    {
        if (showingAll)
        {
            showPopUp();
            return;
        }

        showingAll = true;
        this.removeAllItems();
        model.addElement(new Object[]{"", "", 0});

        for (int i = 0; i < itemBackup.size(); i++)
        {
            model.addElement(itemBackup.get(i));
        }

        showPopUp();
    }

    private void searchAndListEntries(Object searchFor)
    {
        List<Object> found = new ArrayList<>();

        if (searchFor instanceof String)
        {
            String searched = (String) searchFor;
            if (searched.length() < 3)
            {
                return;
            }
        }

        showingAll = false;
        for (int i = 0; i < this.itemBackup.size(); i++)
        {
            Object tmp = this.itemBackup.get(i);
            if (tmp == null || searchFor == null)
            {
                continue;
            }

            Object[] o = (Object[]) tmp;
            String s = o[0].toString();

            if (searchFor instanceof String)
            {
                String search = (String) searchFor;
                if (s.matches("(?i).*" + Pattern.quote(search) + ".*"))
                {
                    found.add(new Object[]{((Object[]) tmp)[0], searchFor, ((Object[]) tmp)[2]});
                }
            }
        }

        this.removeAllItems();
        model.addElement(new Object[]{searchFor, searchFor, 0});

        for (int i = 0; i < found.size(); i++)
        {
            model.addElement(found.get(i));
        }

        showPopUp();
    }

    private void showPopUp()
    {
        this.setPopupVisible(true);
        BasicComboPopup popup = (BasicComboPopup) this.getAccessibleContext().getAccessibleChild(0);
        Window popupWindow = SwingUtilities.windowForComponent(popup);
        Window comboWindow = SwingUtilities.windowForComponent(this);

        if (comboWindow.equals(popupWindow))
        {
            Component c = popup.getParent();
            Dimension d = c.getPreferredSize();
            c.setSize(d);
        }
        else
        {
            popupWindow.pack();
        }
    }

    @Override
    public void updateUI()
    {
        super.updateUI();
        setUI(new BasicComboBoxUI()
        {
            @Override protected JButton createArrowButton()
            {
                return new JButton()
                {
                    @Override public int getWidth()
                    {
                        return 0;
                    }
                };
            }
        });
        setBorder(BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR));
    }
}
