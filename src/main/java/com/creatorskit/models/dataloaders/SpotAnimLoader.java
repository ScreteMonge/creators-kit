package com.creatorskit.models.dataloaders;

import com.creatorskit.models.datatypes.SpotAnimDefinition;
import lombok.extern.slf4j.Slf4j;

import java.nio.BufferUnderflowException;
import java.util.Set;

@Slf4j
public class SpotAnimLoader
{
    public SpotAnimDefinition load(Set<Integer> unknownOpcodes, int id, byte[] b)
    {
        SpotAnimDefinition def = new SpotAnimDefinition();
        InputStream is = new InputStream(b);
        def.id = id;

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
                                "Creator's Kit: Unknown SpotAnim opcode {} " +
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

    private boolean decodeValues(int opcode, SpotAnimDefinition def, InputStream stream)
    {
        if (opcode == 1)
        {
            def.modelId = stream.readUnsignedShort();
        }
        else if (opcode == 2)
        {
            def.animationId = stream.readUnsignedShort();
        }
        else if (opcode == 3)
        {
            def.modelId = stream.readInt();
        }
        else if (opcode == 4)
        {
            def.resizeX = stream.readUnsignedShort();
        }
        else if (opcode == 5)
        {
            def.resizeY = stream.readUnsignedShort();
        }
        else if (opcode == 6)
        {
            def.rotation = stream.readUnsignedShort();
        }
        else if (opcode == 7)
        {
            def.ambient = stream.readUnsignedByte();
        }
        else if (opcode == 8)
        {
            def.contrast = stream.readUnsignedByte();
        }
        else if (opcode == 9)
        {
            def.name = stream.readString();
        }
        else if (opcode == 40)
        {
            int var3 = stream.readUnsignedByte();
            def.recolorToFind = new short[var3];
            def.recolorToReplace = new short[var3];

            for (int var4 = 0; var4 < var3; ++var4)
            {
                def.recolorToFind[var4] = (short) stream.readUnsignedShort();
                def.recolorToReplace[var4] = (short) stream.readUnsignedShort();
            }
        }
        else if (opcode == 41)
        {
            int var3 = stream.readUnsignedByte();
            def.textureToFind = new short[var3];
            def.textureToReplace = new short[var3];

            for (int var4 = 0; var4 < var3; ++var4)
            {
                def.textureToFind[var4] = (short) stream.readUnsignedShort();
                def.textureToReplace[var4] = (short) stream.readUnsignedShort();
            }
        }
        else
        {
            return false;
        }

        return true;
    }
}
