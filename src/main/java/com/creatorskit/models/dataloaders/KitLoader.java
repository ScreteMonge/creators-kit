package com.creatorskit.models.dataloaders;

import com.creatorskit.models.datatypes.KitDefinition;

public class KitLoader
{
    public KitDefinition load(int id, byte[] b)
    {
        KitDefinition def = new KitDefinition(id);
        InputStream is = new InputStream(b);

        for (;;)
        {
            int opcode = is.readUnsignedByte();
            if (opcode == 0)
            {
                break;
            }

            if (opcode == 1)
            {
                def.bodyPartId = is.readUnsignedByte();
            }
            else if (opcode == 2)
            {
                int length = is.readUnsignedByte();
                def.models = new int[length];

                for (int index = 0; index < length; ++index)
                {
                    def.models[index] = is.readUnsignedShort();
                }
            }
            else if (opcode == 3)
            {
                //def.nonSelectable = true;
            }
            else if (opcode == 5)
            {
                int length = is.readUnsignedByte();
                def.models = new int[length];

                for (int index = 0; index < length; ++index)
                {
                    def.models[index] = is.readInt();
                }
            }
            else if (opcode == 40)
            {
                int length = is.readUnsignedByte();
                def.recolorToFind = new short[length];
                def.recolorToReplace = new short[length];

                for (int index = 0; index < length; ++index)
                {
                    def.recolorToFind[index] = is.readShort();
                    def.recolorToReplace[index] = is.readShort();
                }
            }
            else if (opcode == 41)
            {
                int length = is.readUnsignedByte();
                def.retextureToFind = new short[length];
                def.retextureToReplace = new short[length];

                for (int index = 0; index < length; ++index)
                {
                    def.retextureToFind[index] = is.readShort();
                    def.retextureToReplace[index] = is.readShort();
                }
            }
            else if (opcode >= 60 && opcode < 70)
            {
                def.chatheadModels[opcode - 60] = is.readUnsignedShort();
            }
            else if (opcode >= 70 && opcode < 80)
            {
                def.chatheadModels[opcode - 70] = is.readInt();
            }
        }

        return def;
    }
}