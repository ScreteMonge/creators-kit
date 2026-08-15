package com.creatorskit.models.dataloaders;

import com.creatorskit.models.datatypes.ObjectDefinition;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.nio.BufferUnderflowException;
import java.util.Set;

@Accessors(chain = true)
@Data
@Slf4j
public class ObjectLoader
{
    public static final int REV_220_OBJ_ARCHIVE_REV = 1673;

    private boolean rev220SoundData = true;

    public ObjectDefinition load(Set<Integer> unknownOpcodes, int id, byte[] b)
    {
        ObjectDefinition def = new ObjectDefinition();
        InputStream is = new InputStream(b);

        def.setId(id);

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
                if (!processOp(opcode, def, is))
                {
                    if (unknownOpcodes.add(opcode))
                    {
                        log.warn(
                                "Creator's Kit: Unknown Object opcode {} " +
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

    private boolean processOp(int opcode, ObjectDefinition def, InputStream is)
    {
        if (opcode == 1)
        {
            int length = is.readUnsignedByte();
            if (length > 0)
            {
                int[] objectTypes = new int[length];
                int[] objectModels = new int[length];

                for (int index = 0; index < length; ++index)
                {
                    objectModels[index] = is.readUnsignedShort();
                    objectTypes[index] = is.readUnsignedByte();
                }

                def.setObjectTypes(objectTypes);
                def.setObjectModels(objectModels);
            }
        }
        else if (opcode == 2)
        {
            def.setName(is.readString());
        }
        else if (opcode == 5)
        {
            int length = is.readUnsignedByte();
            if (length > 0)
            {
                def.setObjectTypes(null);
                int[] objectModels = new int[length];

                for (int index = 0; index < length; ++index)
                {
                    objectModels[index] = is.readUnsignedShort();
                }

                def.setObjectModels(objectModels);
            }
        }
        else if (opcode == 6)
        {
            int length = is.readUnsignedByte();
            if (length > 0)
            {
                int[] objectTypes = new int[length];
                int[] objectModels = new int[length];

                for (int index = 0; index < length; ++index)
                {
                    objectModels[index] = is.readInt();
                    objectTypes[index] = is.readUnsignedByte();
                }

                def.setObjectTypes(objectTypes);
                def.setObjectModels(objectModels);
            }
        }
        else if (opcode == 7)
        {
            int length = is.readUnsignedByte();
            if (length > 0)
            {
                def.setObjectTypes(null);
                int[] objectModels = new int[length];

                for (int index = 0; index < length; ++index)
                {
                    objectModels[index] = is.readInt();
                }

                def.setObjectModels(objectModels);
            }
        }
        else if (opcode == 14)
        {
            is.readUnsignedByte();
        }
        else if (opcode == 15)
        {
            is.readUnsignedByte();
        }
        else if (opcode == 17)
        {
            //def.setInteractType(0);
            //def.setBlocksProjectile(false);
        }
        else if (opcode == 18)
        {
            //def.setBlocksProjectile(false);
        }
        else if (opcode == 19)
        {
            is.readUnsignedByte();
        }
        else if (opcode == 21)
        {
            //def.setContouredGround(0);
        }
        else if (opcode == 22)
        {
            //def.setMergeNormals(true);
        }
        else if (opcode == 23)
        {
            //def.setModelClipped(true);
        }
        else if (opcode == 24)
        {
            def.setAnimationID(is.readUnsignedShort());
            if (def.getAnimationID() == 0xFFFF)
            {
                def.setAnimationID(-1);
            }
        }
        else if (opcode == 27)
        {
            //def.setInteractType(1);
        }
        else if (opcode == 28)
        {
            is.readUnsignedByte();
        }
        else if (opcode == 29)
        {
            def.setAmbient(is.readByte());
        }
        else if (opcode == 39)
        {
            def.setContrast(is.readByte() * 25);
        }
        else if (opcode >= 30 && opcode < 35)
        {
            is.readString();
        }
        else if (opcode == 40)
        {
            int length = is.readUnsignedByte();
            short[] recolorToFind = new short[length];
            short[] recolorToReplace = new short[length];

            for (int index = 0; index < length; ++index)
            {
                recolorToFind[index] = is.readShort();
                recolorToReplace[index] = is.readShort();
            }

            def.setRecolorToFind(recolorToFind);
            def.setRecolorToReplace(recolorToReplace);
        }
        else if (opcode == 41)
        {
            int length = is.readUnsignedByte();
            short[] retextureToFind = new short[length];
            short[] textureToReplace = new short[length];

            for (int index = 0; index < length; ++index)
            {
                retextureToFind[index] = is.readShort();
                textureToReplace[index] = is.readShort();
            }

            def.setRetextureToFind(retextureToFind);
            def.setTextureToReplace(textureToReplace);
        }
        else if (opcode == 61)
        {
            is.readUnsignedShort();
        }
        else if (opcode == 62)
        {
            //def.setRotated(true);
        }
        else if (opcode == 64)
        {
            //def.setShadow(false);
        }
        else if (opcode == 65)
        {
            def.setModelSizeX(is.readUnsignedShort());
        }
        else if (opcode == 66)
        {
            def.setModelSizeHeight(is.readUnsignedShort());
        }
        else if (opcode == 67)
        {
            def.setModelSizeY(is.readUnsignedShort());
        }
        else if (opcode == 68)
        {
            is.readUnsignedShort();
        }
        else if (opcode == 69)
        {
            is.readByte();
        }
        else if (opcode == 70)
        {
            is.readUnsignedShort();
        }
        else if (opcode == 71)
        {
            is.readUnsignedShort();
        }
        else if (opcode == 72)
        {
            is.readUnsignedShort();
        }
        else if (opcode == 73)
        {
            //def.setObstructsGround(true);
        }
        else if (opcode == 74)
        {
            //def.setHollow(true);
        }
        else if (opcode == 75)
        {
            is.readUnsignedByte();
        }
        else if (opcode == 77)
        {
            is.readUnsignedShort();
            is.readUnsignedShort();
            int length = is.readUnsignedByte();

            for (int index = 0; index <= length; ++index)
            {
                is.readUnsignedShort();
            }
        }
        else if (opcode == 78)
        {
            is.readUnsignedShort();
            is.readUnsignedByte();
            if (rev220SoundData)
            {
                is.readUnsignedByte();
            }
        }
        else if (opcode == 79)
        {
            is.readUnsignedShort();
            is.readUnsignedShort();
            is.readUnsignedByte();
            if (rev220SoundData)
            {
                is.readUnsignedByte();
            }
            int length = is.readUnsignedByte();

            for (int index = 0; index < length; ++index)
            {
                is.readUnsignedShort();
            }
        }
        else if (opcode == 81)
        {
            is.readUnsignedByte();
        }
        else if (opcode == 82)
        {
            is.readUnsignedShort();
        }
        else if (opcode == 89)
        {
            //def.setRandomizeAnimStart(true);
        }
        else if (opcode == 90)
        {
            //def.setDeferAnimChange(true);
        }
        else if (opcode == 91)
        {
            is.readUnsignedByte();
        }
        else if (opcode == 92)
        {
            is.readUnsignedShort();
            is.readUnsignedShort();
            is.readUnsignedShort();

            int length = is.readUnsignedByte();

            for (int index = 0; index <= length; ++index)
            {
                is.readUnsignedShort();
            }
        }
        else if (opcode == 93)
        {
            is.readUnsignedByte();
            is.readUnsignedShort();
            is.readUnsignedByte();
            is.readUnsignedShort();
        }
        else if (opcode == 94)
        {
            //def.setUnknown1(true);
        }
        else if (opcode == 95)
        {
            is.readUnsignedByte();
        }
        else if (opcode == 96)
        {
            is.readUnsignedByte();
        }
        else if (opcode == 100)
        {
            is.readUnsignedByte();
            is.readUnsignedByte();
            is.readString();
        }
        else if (opcode == 101)
        {
            is.readUnsignedByte();
            is.readUnsignedShort();
            is.readUnsignedShort();
            is.readInt();
            is.readInt();
            is.readString();
        }
        else if (opcode == 102)
        {
            is.readUnsignedByte();
            is.readUnsignedShort();
            is.readUnsignedShort();
            is.readUnsignedShort();
            is.readInt();
            is.readInt();
            is.readString();
        }
        else if (opcode == 249)
        {
            int count = is.readUnsignedByte();

            for (int i = 0; i < count; i++)
            {
                int type = is.readUnsignedByte();

                is.read24BitInt();

                if (type == 1)
                {
                    is.readString();
                }
                else if (type == 2)
                {
                    is.readLong();
                }
                else
                {
                    is.readInt();
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