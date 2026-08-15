package com.creatorskit.models.dataloaders;

import com.creatorskit.models.datatypes.NpcDefinition;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.nio.BufferUnderflowException;
import java.util.Set;

@Accessors(chain = true)
@Data
@Slf4j
public class NpcLoader
{
    public static final int REV_210_NPC_ARCHIVE_REV = 1493;

    private int defaultHeadIconArchive = -1;
    private boolean rev210HeadIcons = true;
    private boolean rev233 = true;

    public NpcDefinition load(Set<Integer> unknownOpcodes, int id, byte[] b)
    {
        NpcDefinition def = new NpcDefinition(id);
        InputStream is = new InputStream(b);

        while (is.remaining() > 0)
        {
            int offset = is.getOffset();
            int opcode = is.readUnsignedByte();

            if (opcode == 0)
            {
                return def;
            }

            try
            {
                if (!decodeValues(opcode, def, is))
                {
                    if (unknownOpcodes.add(opcode))
                    {
                        log.warn(
                                "Creator's Kit: Unknown Npc opcode {} " +
                                        "(first seen on id={}, offset={})",
                                opcode,
                                id,
                                offset
                        );
                    }

                    return null;
                }
            }
            catch (BufferUnderflowException e)
            {
                return null;
            }
        }

        return def;
    }

    private boolean decodeValues(int opcode, NpcDefinition def, InputStream stream)
    {
        int length;
        int index;
        if (opcode == 1)
        {
            length = stream.readUnsignedByte();
            def.models = new int[length];

            for (index = 0; index < length; ++index)
            {
                def.models[index] = stream.readUnsignedShort();
            }
        }
        else if (opcode == 2)
        {
            def.name = stream.readString();
        }
        else if (opcode == 12)
        {
            def.size = stream.readUnsignedByte();
        }
        else if (opcode == 13)
        {
            def.standingAnimation = stream.readUnsignedShort();
        }
        else if (opcode == 14)
        {
            def.walkingAnimation = stream.readUnsignedShort();
        }
        else if (opcode == 15)
        {
            def.idleRotateLeftAnimation = stream.readUnsignedShort();
        }
        else if (opcode == 16)
        {
            def.idleRotateRightAnimation = stream.readUnsignedShort();
        }
        else if (opcode == 17)
        {
            def.walkingAnimation = stream.readUnsignedShort();
            def.rotate180Animation = stream.readUnsignedShort();
            def.rotateLeftAnimation = stream.readUnsignedShort();
            def.rotateRightAnimation = stream.readUnsignedShort();
        }
        else if (opcode == 18)
        {
            stream.readUnsignedShort();
        }
        else if (opcode >= 30 && opcode < 35)
        {
            stream.readString();
        }
        else if (opcode == 40)
        {
            length = stream.readUnsignedByte();
            def.recolorToFind = new short[length];
            def.recolorToReplace = new short[length];

            for (index = 0; index < length; ++index)
            {
                def.recolorToFind[index] = (short) stream.readUnsignedShort();
                def.recolorToReplace[index] = (short) stream.readUnsignedShort();
            }

        }
        else if (opcode == 41)
        {
            length = stream.readUnsignedByte();

            for (index = 0; index < length; ++index)
            {
                stream.readUnsignedShort();
                stream.readUnsignedShort();
            }

        }
        else if (opcode == 60)
        {
            length = stream.readUnsignedByte();

            for (index = 0; index < length; ++index)
            {
                stream.readUnsignedShort();
            }
        }
        else if (opcode == 61)
        {
            length = stream.readUnsignedByte();
            def.models = new int[length];

            for (index = 0; index < length; ++index)
            {
                def.models[index] = stream.readInt();
            }
        }
        else if (opcode == 62)
        {
            length = stream.readUnsignedByte();

            for (index = 0; index < length; ++index)
            {
                stream.readInt();
            }
        }
        else if (opcode == 74)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 75)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 76)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 77)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 78)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 79)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 93)
        {

        }
        else if (opcode == 95)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 97)
        {
            def.widthScale = stream.readUnsignedShort();
        }
        else if (opcode == 98)
        {
            def.heightScale = stream.readUnsignedShort();
        }
        else if (opcode == 99)
        {
            //def.renderPriority = 1;
        }
        else if (opcode == 100)
        {
            stream.readByte();
        }
        else if (opcode == 101)
        {
            stream.readByte();
        }
        else if (opcode == 102)
        {
            int bitfield = stream.readUnsignedByte();

            int len = 0;
            for (int bits = bitfield; bits != 0; bits >>= 1)
            {
                len++;
            }

            for (int i = 0; i < len; i++)
            {
                if ((bitfield & (1 << i)) != 0)
                {
                    stream.readBigSmart2();
                    stream.readUnsignedShortSmartMinusOne();
                }
            }
        }
        else if (opcode == 103)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 106)
        {
            stream.readUnsignedShort();
            stream.readUnsignedShort();

            length = stream.readUnsignedByte();

            for (index = 0; index <= length; ++index)
            {
                stream.readUnsignedShort();
            }
        }
        else if (opcode == 107)
        {
            //def.isInteractable = false;
        }
        else if (opcode == 109)
        {
            //def.rotationFlag = false;
        }
        else if (opcode == 111 && !rev233)
        {
            // removed in 220
            //def.isFollower = true;
            //def.lowPriorityFollowerOps = true;
        }
        else if (opcode == 111 && rev233)
        {
            //def.renderPriority = 2;
        }
        else if (opcode == 114)
        {
            def.runAnimation = stream.readUnsignedShort();
        }
        else if (opcode == 115)
        {
            def.runAnimation = stream.readUnsignedShort();
            def.runRotate180Animation = stream.readUnsignedShort();
            def.runRotateLeftAnimation = stream.readUnsignedShort();
            def.runRotateRightAnimation = stream.readUnsignedShort();
        }
        else if (opcode == 116)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 117)
        {
            stream.readUnsignedShort();
            stream.readUnsignedShort();
            stream.readUnsignedShort();
            stream.readUnsignedShort();
        }
        else if (opcode == 118)
        {
            stream.readUnsignedShort();
            stream.readUnsignedShort();
            stream.readUnsignedShort();

            length = stream.readUnsignedByte();

            for (index = 0; index <= length; ++index)
            {
                stream.readUnsignedShort();
            }
        }
        else if (opcode == 122)
        {
            //def.isFollower = true;
        }
        else if (opcode == 123)
        {
            //def.lowPriorityFollowerOps = true;
        }
        else if (opcode == 124)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 126)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 129)
        {
            //def.unknown1 = true;
        }
        else if (opcode == 130)
        {
            //def.idleAnimRestart = true;
        }
        else if (opcode == 145)
        {
            //def.canHideForOverlap = true;
        }
        else if (opcode == 146)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 147)
        {
            //def.zbuf = false;
        }
        else if (opcode == 249)
        {
            int count = stream.readUnsignedByte();

            for (int i = 0; i < count; i++)
            {
                int type = stream.readUnsignedByte();

                stream.read24BitInt();

                if (type == 1)
                {
                    stream.readString();
                }
                else if (type == 2)
                {
                    stream.readLong();
                }
                else
                {
                    stream.readInt();
                }
            }
        }
        else if (opcode == 251)
        {
            stream.readUnsignedByte();
            stream.readUnsignedByte();
            stream.readString();
        }
        else if (opcode == 252)
        {
            stream.readUnsignedByte();
            stream.readUnsignedShort();
            stream.readUnsignedShort();
            stream.readInt();
            stream.readInt();
            stream.readString();
        }
        else if (opcode == 253)
        {
            stream.readUnsignedByte();
            stream.readUnsignedShort();
            stream.readUnsignedShort();
            stream.readUnsignedShort();
            stream.readInt();
            stream.readInt();
            stream.readString();
        }
        else
        {
            return false;
        }

        return true;
    }
}