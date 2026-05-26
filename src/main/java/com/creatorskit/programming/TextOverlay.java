package com.creatorskit.programming;

import com.creatorskit.Character;
import com.creatorskit.CreatorsPlugin;
import com.creatorskit.CKObject;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import com.creatorskit.swing.timesheet.keyframe.KeyFrameType;
import com.creatorskit.swing.timesheet.keyframe.TextKeyFrame;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TextOverlay extends Overlay
{
    private final Client client;
    private final CreatorsPlugin plugin;
    private final int TEXT_BUFFER = 4;

    @Inject
    private TextOverlay(Client client, CreatorsPlugin plugin)
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.client = client;
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return null;
        }

        graphics.setFont(FontManager.getRunescapeBoldFont());

        ArrayList<Character> characters = plugin.getCharacters();
        for (int i = 0; i < characters.size(); i++)
        {
            Character character = characters.get(i);
            if (!character.isActive())
            {
                continue;
            }

            TextKeyFrame textKeyFrame = (TextKeyFrame) character.getCurrentKeyFrame(KeyFrameType.TEXT);
            if (textKeyFrame == null)
            {
                continue;
            }

            CKObject ckObject = character.getCkObject();
            if (!ckObject.isActive())
            {
                continue;
            }

            LocalPoint lp = ckObject.getLocation();
            if (lp == null || !lp.isInScene())
            {
                continue;
            }

            double duration = textKeyFrame.getDuration();
            double startTick = textKeyFrame.getTick();
            double currentTick = plugin.getCurrentTick();
            if (currentTick > duration + startTick)
            {
                continue;
            }

            String text = textKeyFrame.getText();
            if (text.isEmpty())
            {
                continue;
            }
            // Parse <col=HEX>...</col> tags into segments and strip them
            // for canvas centering. An untagged string parses to a
            // single yellow segment, so behaviour is unchanged when no
            // tags are present.
            List<Segment> segments = parseColorTags(text, Color.YELLOW);
            String plainText = stripColorTags(text);
            if (plainText.isEmpty())
            {
                continue;
            }

            Model model = ckObject.getModel();
            if (model == null)
            {
                continue;
            }

            model.calculateBoundsCylinder();
            int height = model.getModelHeight();

            Point point = Perspective.getCanvasTextLocation(client, graphics, lp, plainText, height);
            if (point == null)
            {
                continue;
            }

            // Walk segments left-to-right, advancing X by each segment's
            // pixel width. OverlayUtil.renderTextLocation paints the
            // existing shadow + colour for each segment so styled +
            // unstyled text share the same rendering pipeline.
            FontMetrics fm = graphics.getFontMetrics();
            int x = point.getX();
            int y = point.getY() + TEXT_BUFFER;
            for (Segment s : segments)
            {
                if (s.text.isEmpty()) continue;
                OverlayUtil.renderTextLocation(graphics, new Point(x, y), s.text, s.color);
                x += fm.stringWidth(s.text);
            }
        }

        return null;
    }

    /**
     * Minimal segment carrier: a contiguous run of characters that
     * share a single colour. Built by {@link #parseColorTags} and
     * consumed by the render loop.
     */
    private static final class Segment
    {
        final String text;
        final Color color;

        Segment(String text, Color color)
        {
            this.text = text;
            this.color = color;
        }
    }

    /**
     * Parses inline <col=HEX>...</col> tags into a list of coloured
     * segments. Supports nested-style switches (every <col=...> changes
     * the current colour, every </col> reverts to {@code defaultColor})
     * to match OSRS's chat / overlay convention.
     *
     * <p>Malformed tags pass through as literal text -- e.g. an
     * unterminated "<col=" or a non-hex value -- so users never see a
     * silent text drop.
     */
    private static List<Segment> parseColorTags(String input, Color defaultColor)
    {
        List<Segment> out = new ArrayList<>();
        Color current = defaultColor;
        StringBuilder buf = new StringBuilder();
        int i = 0;
        int len = input.length();
        while (i < len)
        {
            char c = input.charAt(i);
            if (c == '<')
            {
                int gt = input.indexOf('>', i);
                if (gt > i)
                {
                    String tag = input.substring(i + 1, gt);
                    if (tag.startsWith("col="))
                    {
                        Color parsed = parseHexColor(tag.substring(4));
                        if (parsed != null)
                        {
                            if (buf.length() > 0)
                            {
                                out.add(new Segment(buf.toString(), current));
                                buf.setLength(0);
                            }
                            current = parsed;
                            i = gt + 1;
                            continue;
                        }
                    }
                    else if (tag.equals("/col"))
                    {
                        if (buf.length() > 0)
                        {
                            out.add(new Segment(buf.toString(), current));
                            buf.setLength(0);
                        }
                        current = defaultColor;
                        i = gt + 1;
                        continue;
                    }
                }
            }
            buf.append(c);
            i++;
        }
        if (buf.length() > 0)
        {
            out.add(new Segment(buf.toString(), current));
        }
        return out;
    }

    /**
     * Returns the same string with every well-formed <col=HEX> and
     * </col> tag stripped. Used for canvas centering (the perspective
     * helper measures pixel width from the visible glyphs, which must
     * exclude the tag markup).
     */
    private static String stripColorTags(String input)
    {
        StringBuilder out = new StringBuilder(input.length());
        int i = 0;
        int len = input.length();
        while (i < len)
        {
            char c = input.charAt(i);
            if (c == '<')
            {
                int gt = input.indexOf('>', i);
                if (gt > i)
                {
                    String tag = input.substring(i + 1, gt);
                    boolean isOpen = tag.startsWith("col=") && parseHexColor(tag.substring(4)) != null;
                    boolean isClose = tag.equals("/col");
                    if (isOpen || isClose)
                    {
                        i = gt + 1;
                        continue;
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /**
     * Parses a 6-hex-digit (#RRGGBB / RRGGBB) colour string. Returns
     * null on any malformation so the parser falls back to treating
     * the tag as literal text.
     */
    private static Color parseHexColor(String hex)
    {
        if (hex == null) return null;
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        if (s.length() != 6) return null;
        try
        {
            return new Color(Integer.parseInt(s, 16));
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }
}