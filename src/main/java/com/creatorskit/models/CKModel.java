package com.creatorskit.models;

import com.creatorskit.programming.CKAnimationController;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Model;

@Getter
@Setter
@AllArgsConstructor
public class CKModel
{
    private String name;
    private CKModelComposition[] modelComps;
    private CustomModelType type;
    private BlenderModel blenderModel;
    private CustomLighting customLighting;
    private boolean priority;

    public Model getBaseModel(Client client)
    {
        return getModel(client, null, null);
    }

    public Model getModel(Client client, CKAnimationController ac, CKAnimationController poseAC)
    {
        Model[] models = new Model[modelComps.length];

        for (int i = 0; i < modelComps.length; i++)
        {
            CKModelComposition comp = modelComps[i];
            Model m = comp.getModel();

            if (ac != null)
            {
                ac.animate(m, poseAC);
            }
            else if (poseAC != null)
            {
                poseAC.animate(m);
            }

            //swapping y and z, making y positive to align with traditional axes
            m.scale(comp.getSx(), comp.getSz(), comp.getSy());
            switch (comp.getRotate())
            {
                case 90:
                    m.rotateY90Ccw();
                    break;
                case 180:
                    m.rotateY180Ccw();
                    break;
                case 270:
                    m.rotateY270Ccw();
                    break;
                case 0:
                default:
                    break;
            }

            m.translate(comp.getTx(), -comp.getTz(), comp.getTy());
            models[i] = m;
        }

        return client.mergeModels(models);
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}