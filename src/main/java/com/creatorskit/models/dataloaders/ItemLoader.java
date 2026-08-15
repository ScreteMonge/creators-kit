package com.creatorskit.models.dataloaders;

import com.creatorskit.models.datatypes.ItemDefinition;
import lombok.extern.slf4j.Slf4j;

import java.nio.BufferUnderflowException;
import java.util.Set;

@Slf4j
public class ItemLoader
{
    public ItemDefinition load(Set<Integer> unknownOpcodes, int id, byte[] b)
    {
        ItemDefinition def = new ItemDefinition(id);
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
                                "Creator's Kit: Unknown Item opcode {} " +
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

    private boolean decodeValues(int opcode, ItemDefinition def, InputStream stream)
    {
        if (opcode == 1)
        {
            def.inventoryModel = stream.readUnsignedShort();
        }
        else if (opcode == 2)
        {
            def.name = stream.readString();
        }
        else if (opcode == 3)
        {
            stream.readString();
        }
        else if (opcode == 4)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 5)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 6)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 7)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 8)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 9)
        {
            stream.readString();
        }
        else if (opcode == 11)
        {
            //def.stackable = 1;
        }
        else if (opcode == 12)
        {
            stream.readInt();
        }
        else if (opcode == 13)
        {
            def.wearPos1 = stream.readByte();
        }
        else if (opcode == 14)
        {
            def.wearPos2 = stream.readByte();
        }
        else if (opcode == 15)
        {
            //def.tradeable = false;
        }
        else if (opcode == 16)
        {
            //def.members = true;
        }
        else if (opcode == 23)
        {
            def.maleModel0 = stream.readUnsignedShort();
            def.maleOffset = stream.readUnsignedByte();
        }
        else if (opcode == 24)
        {
            def.maleModel1 = stream.readUnsignedShort();
        }
        else if (opcode == 25)
        {
            def.femaleModel0 = stream.readUnsignedShort();
            def.femaleOffset = stream.readUnsignedByte();
        }
        else if (opcode == 26)
        {
            def.femaleModel1 = stream.readUnsignedShort();
        }
        else if (opcode == 27)
        {
            def.wearPos3 = stream.readByte();
        }
        else if (opcode >= 30 && opcode < 35)
        {
            stream.readString();
        }
        else if (opcode >= 35 && opcode < 40)
        {
            stream.readString();
        }
        else if (opcode == 40)
        {
            int var5 = stream.readUnsignedByte();
            def.colorFind = new short[var5];
            def.colorReplace = new short[var5];

            for (int var4 = 0; var4 < var5; ++var4)
            {
                def.colorFind[var4] = (short) stream.readUnsignedShort();
                def.colorReplace[var4] = (short) stream.readUnsignedShort();
            }

        }
        else if (opcode == 41)
        {
            int var5 = stream.readUnsignedByte();
            def.textureFind = new short[var5];
            def.textureReplace = new short[var5];

            for (int var4 = 0; var4 < var5; ++var4)
            {
                def.textureFind[var4] = (short) stream.readUnsignedShort();
                def.textureReplace[var4] = (short) stream.readUnsignedShort();
            }
        }
        else if (opcode == 42)
        {
            stream.readByte();
        }
        else if (opcode == 43)
        {
            stream.readUnsignedByte();

            while (true)
            {
                int subopId = stream.readUnsignedByte() - 1;
                if (subopId == -1)
                {
                    break;
                }

                stream.readString();
            }
        }
        else if (opcode == 44)
        {
            def.inventoryModel = stream.readInt();
        }
        else if (opcode == 45)
        {
            def.maleModel0 = stream.readInt();
            def.maleOffset = stream.readUnsignedByte();
        }
        else if (opcode == 46)
        {
            def.maleModel1 = stream.readInt();
        }
        else if (opcode == 47)
        {
            def.maleModel2 = stream.readInt();
        }
        else if (opcode == 48)
        {
            def.femaleModel0 = stream.readInt();
            def.femaleOffset = stream.readUnsignedByte();
        }
        else if (opcode == 49)
        {
            def.femaleModel1 = stream.readInt();
        }
        else if (opcode == 50)
        {
            def.femaleModel2 = stream.readInt();
        }
        else if (opcode == 51)
        {
            def.maleHeadModel = stream.readInt();
        }
        else if (opcode == 52)
        {
            def.maleHeadModel2 = stream.readInt();
        }
        else if (opcode == 53)
        {
            def.femaleHeadModel = stream.readInt();
        }
        else if (opcode == 54)
        {
            def.femaleHeadModel2 = stream.readInt();
        }
        else if (opcode == 65)
        {
            //def.geTradeable = true;
        }
        else if (opcode == 75)
        {
            stream.readShort();
        }
        else if (opcode == 78)
        {
            def.maleModel2 = stream.readUnsignedShort();
        }
        else if (opcode == 79)
        {
            def.femaleModel2 = stream.readUnsignedShort();
        }
        else if (opcode == 90)
        {
            def.maleHeadModel = stream.readUnsignedShort();
        }
        else if (opcode == 91)
        {
            def.femaleHeadModel = stream.readUnsignedShort();
        }
        else if (opcode == 92)
        {
            def.maleHeadModel2 = stream.readUnsignedShort();
        }
        else if (opcode == 93)
        {
            def.femaleHeadModel2 = stream.readUnsignedShort();
        }
        else if (opcode == 94)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 95)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 97)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 98)
        {
            stream.readUnsignedShort();
        }
        else if (opcode >= 100 && opcode < 110)
        {
            stream.readUnsignedShort();
            stream.readUnsignedShort();
        }
        else if (opcode == 110)
        {
            def.resizeX = stream.readUnsignedShort();
        }
        else if (opcode == 111)
        {
            def.resizeY = stream.readUnsignedShort();
        }
        else if (opcode == 112)
        {
            def.resizeZ = stream.readUnsignedShort();
        }
        else if (opcode == 113)
        {
            stream.readByte();
        }
        else if (opcode == 114)
        {
            stream.readByte();
        }
        else if (opcode == 115)
        {
            stream.readUnsignedByte();
        }
        else if (opcode == 139)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 140)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 148)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 149)
        {
            stream.readUnsignedShort();
        }
        else if (opcode == 160)
        {
            //def.stackable = 2;
        }
        else if (opcode == 200)
        {
            stream.readUnsignedByte();
            stream.readUnsignedByte();
            stream.readString();
        }
        else if (opcode == 201)
        {
            stream.readUnsignedByte();
            stream.readUnsignedShort();
            stream.readUnsignedShort();
            stream.readInt();
            stream.readInt();
            stream.readString();
        }
        else if (opcode == 202)
        {
            stream.readUnsignedByte();
            stream.readUnsignedShort();
            stream.readUnsignedShort();
            stream.readUnsignedShort();
            stream.readInt();
            stream.readInt();
            stream.readString();
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
        else
        {
            return false;
        }

        return true;
    }
}
